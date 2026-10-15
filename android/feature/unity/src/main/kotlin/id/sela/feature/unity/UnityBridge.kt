package id.sela.feature.unity

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.FrameLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Pengelola lifecycle UnityPlayer + jembatan pesan Kotlin ↔ Unity.
 *
 * Pola dari riset Unity as a Library:
 * - Player dibuat dengan Activity (bukan app context) — kalau tidak, black screen.
 * - Jangan pernah panggil `quit()` — mematikan seluruh proses Android.
 * - `UnitySendMessage` diam-diam dibuang saat Unity masih boot → antrean + retry
 *   sampai Unity lapor `onUnityReady`.
 * - Semua akses UnityPlayer via reflection agar modul tetap compile tanpa
 *   unity-classes.jar (fallback saat export belum ada).
 */
class UnityBridge private constructor() {

    private var player: Any? = null
    private var playerView: FrameLayout? = null
    private var unityReady = false
    private val pending = ConcurrentLinkedQueue<Triple<String, String, String>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())

    var listener: Listener? = null

    interface Listener {
        fun onUnityReady()
        fun onStopSelected(stopId: String)
        fun onCameraChanged(lat: Double, lon: Double, zoom: Float, bearing: Float, pitch: Float)
    }

    companion object {
        private const val TAG = "UnityBridge"
        private const val BRIDGE_OBJECT = "SelaBridge"

        @Volatile
        private var instance: UnityBridge? = null

        fun get(): UnityBridge =
            instance ?: synchronized(this) {
                instance ?: UnityBridge().also { instance = it }
            }

        /** Dipanggil dari Unity (via AndroidJavaProxy) saat engine selesai boot. */
        @JvmStatic
        fun nativeOnUnityReady() {
            get().onReadyInternal()
        }

        @JvmStatic
        fun nativeOnStopSelected(stopId: String) {
            get().listener?.onStopSelected(stopId)
        }

        @JvmStatic
        fun nativeOnCameraChanged(lat: Double, lon: Double, zoom: Float, bearing: Float, pitch: Float) {
            get().listener?.onCameraChanged(lat, lon, zoom, bearing, pitch)
        }
    }

    /** Buat player dan tempelkan view-nya ke [container]. Idempotent. */
    fun attach(activity: Activity, container: FrameLayout) {
        if (player != null) {
            // Reparent singleton view (Unity FrameLayout hanya satu).
            reparent(container)
            return
        }
        try {
            val playerClass = Class.forName("com.unity3d.player.UnityPlayer")
            val ctor = playerClass.getDeclaredConstructor(Activity::class.java)
            val newPlayer = ctor.newInstance(activity)
            player = newPlayer
            val view = newPlayer as FrameLayout
            playerView = view
            container.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            // Unity butuh fokus window sintetis saat di-embed.
            try {
                playerClass.getMethod("setRunWithoutFocus", Boolean::class.java)
                    .invoke(newPlayer, true)
            } catch (_: Exception) { /* opsional di versi lama */ }
            // PENTING: resume player segera setelah dibuat.
            // Activity.onResume() bisa jalan sebelum Compose membuat player,
            // sehingga invokePlayer("resume") di onResume() menjadi no-op.
            // Tanpa resume(), Unity tidak render → black screen.
            try {
                playerClass.getMethod("resume").invoke(newPlayer)
                Log.i(TAG, "UnityPlayer resumed setelah attach")
            } catch (e: Exception) {
                Log.w(TAG, "player.resume() setelah attach gagal", e)
            }
            // Pastikan view dapat fokus untuk input.
            try {
                view.requestFocus()
            } catch (_: Exception) { }
            scope.launch {
                // Beri waktu boot, lalu flush antrean saat ready.
                var tries = 0
                while (!unityReady && tries < 40) {
                    delay(500)
                    tries++
                }
                flushPending()
            }
            Log.i(TAG, "UnityPlayer attached")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuat UnityPlayer", e)
        }
    }

    private fun reparent(container: FrameLayout) {
        val view = playerView ?: return
        (view.parent as? android.view.ViewGroup)?.removeView(view)
        container.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun onReadyInternal() {
        unityReady = true
        mainHandler.post {
            listener?.onUnityReady()
            flushPending()
        }
    }

    /** Kirim pesan ke Unity. Diantrekan jika Unity belum ready. */
    fun send(gameObject: String = BRIDGE_OBJECT, method: String, payload: String = "") {
        if (!unityReady) {
            pending.add(Triple(gameObject, method, payload))
            return
        }
        deliver(gameObject, method, payload)
    }

    private fun flushPending() {
        while (true) {
            val (go, method, payload) = pending.poll() ?: break
            deliver(go, method, payload)
        }
    }

    private fun deliver(gameObject: String, method: String, payload: String) {
        try {
            val playerClass = Class.forName("com.unity3d.player.UnityPlayer")
            playerClass.getMethod(
                "UnitySendMessage",
                String::class.java, String::class.java, String::class.java,
            ).invoke(null, gameObject, method, payload)
        } catch (e: Exception) {
            Log.w(TAG, "UnitySendMessage gagal: $method", e)
            // Masukkan lagi ke antrean untuk retry berikutnya.
            pending.add(Triple(gameObject, method, payload))
        }
    }

    // ---------- lifecycle: teruskan dari Activity ----------

    fun onResume() = invokePlayer("resume")
    fun onPause() = invokePlayer("pause")

    /**
     * Jangan panggil quit() — membunuh proses. destroy() hanya saat Activity
     * benar-benar finishing.
     */
    fun onDestroy(finishing: Boolean) {
        if (finishing) {
            invokePlayer("destroy")
            player = null
            playerView = null
            unityReady = false
            instance = null
        }
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        try {
            player?.let {
                it.javaClass.getMethod("windowFocusChanged", Boolean::class.java)
                    .invoke(it, hasFocus)
            }
        } catch (_: Exception) { }
    }

    private fun invokePlayer(method: String) {
        try {
            player?.let { it.javaClass.getMethod(method).invoke(it) }
        } catch (e: Exception) {
            Log.w(TAG, "player.$method gagal", e)
        }
    }

    // ---------- helper pesan ber-tipe ----------

    fun setCamera(lat: Double, lon: Double, zoom: Float, bearing: Float, pitch: Float, mode: String) {
        send(
            method = "SetCamera",
            payload = """{"lat":$lat,"lon":$lon,"zoom":$zoom,"bearing":$bearing,"pitch":$pitch,"mode":"$mode"}""",
        )
    }

    fun setGps(lat: Double, lon: Double, accuracy: Float, bearing: Float) {
        send(
            method = "SetGps",
            payload = """{"lat":$lat,"lon":$lon,"accuracy":$accuracy,"bearing":$bearing}""",
        )
    }

    fun setTimeOfDay(hourJakarta: Float) {
        send(method = "SetTimeOfDay", payload = """{"hour":$hourJakarta}""")
    }

    fun setStopsJson(json: String) = send(method = "SetStops", payload = json)
    fun setRoutesJson(json: String) = send(method = "SetRoutes", payload = json)
    fun setBusesJson(json: String) = send(method = "SetBuses", payload = json)

    fun setQualityTier(tier: String) =
        send(method = "SetQualityTier", payload = """{"tier":"$tier"}""")

    fun selectStop(stopId: String) =
        send(method = "SelectStop", payload = """{"id":"$stopId"}""")

    fun selectRoute(routeId: String) =
        send(method = "SelectRoute", payload = """{"id":"$routeId"}""")
}
