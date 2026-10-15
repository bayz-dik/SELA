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
    private val pending = ConcurrentLinkedQueue<PendingMessage>()
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
        /**
         * A1: batas retry per pesan. deliver() yang selalu gagal sebelumnya
         * me-requeue tanpa batas di Dispatchers.Main → UI freeze permanen.
         */
        private const val MAX_DELIVERY_ATTEMPTS = 3

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

    /**
     * Nama class konkret UnityPlayer di Unity 6000.x.
     *
     * PENTING (temuan 2026-10-15, terbukti via javap pada unity-classes.jar
     * hasil export Unity 6000.6.5f1):
     * - `com.unity3d.player.UnityPlayer` adalah class ABSTRAK sejak Unity 2022+,
     *   dengan constructor protected `(Context, ..., IUnityPlayerLifecycleEvents)`.
     *   Kode lama memakai `getDeclaredConstructor(Activity::class.java)` pada
     *   class abstrak -> NoSuchMethodException -> ditelan catch -> black screen.
     * - Class konkret yang benar: `UnityPlayerForActivityOrService` dengan
     *   constructor publik `(Context)`.
     * - View didapat via `getView()` (bukan cast player ke FrameLayout).
     */
    private const val UNITY_PLAYER_CLASS = "com.unity3d.player.UnityPlayerForActivityOrService"

    /** Buat player dan tempelkan view-nya ke [container]. Idempotent. */
    fun attach(activity: Activity, container: FrameLayout) {
        if (player != null) {
            // Reparent singleton view (Unity FrameLayout hanya satu).
            reparent(container)
            return
        }
        try {
            val playerClass = Class.forName(UNITY_PLAYER_CLASS)
            val ctor = playerClass.getConstructor(android.content.Context::class.java)
            val newPlayer = ctor.newInstance(activity)
            player = newPlayer
            // getView() mengembalikan FrameLayout (com.unity3d.player.C).
            val view = playerClass.getMethod("getView").invoke(newPlayer) as FrameLayout
            playerView = view
            container.addView(
                view,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            // Unity butuh fokus window sintetis saat di-embed.
            invokeOn(newPlayer, "setRunWithoutFocus", true)
            // PENTING: resume player segera setelah dibuat.
            // Activity.onResume() bisa jalan sebelum Compose membuat player,
            // sehingga invokePlayer("resume") di onResume() menjadi no-op.
            // Tanpa resume(), Unity tidak render → black screen.
            invokeOn(newPlayer, "resume")
            Log.i(TAG, "UnityPlayer resumed setelah attach")
            // Pastikan view dapat fokus untuk input.
            try {
                view.requestFocus()
            } catch (_: Exception) { }
            // Resume lagi setelah layout valid — Unity tidak render jika view 0x0
            // saat player dibuat (kasus umum di Compose AndroidView).
            view.viewTreeObserver.addOnGlobalLayoutListener(
                object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (view.width > 0 && view.height > 0) {
                            view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            invokeOn(newPlayer, "resume")
                            Log.i(TAG, "UnityPlayer resumed setelah layout ${view.width}x${view.height}")
                        }
                    }
                }
            )
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
            lastError = e.toString()
        }
    }

    /** Error terakhir saat attach gagal — dibaca UI untuk pesan yang jelas. */
    @Volatile
    var lastError: String? = null
        private set

    private fun invokeOn(target: Any, method: String, vararg args: Any?) {
        try {
            val paramTypes = args.map {
                when (it) {
                    is Boolean -> Boolean::class.javaPrimitiveType!!
                    else -> it?.javaClass ?: Any::class.java
                }
            }.toTypedArray()
            target.javaClass.getMethod(method, *paramTypes).invoke(target, *args)
        } catch (e: Exception) {
            Log.w(TAG, "player.$method gagal", e)
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

    /**
     * A1: pesan antrean membawa hitungan percobaan agar retry terbatas —
     * setelah [MAX_DELIVERY_ATTEMPTS]x gagal, pesan di-drop dan dicatat ke
     * [lastError], tidak pernah requeue tanpa batas.
     */
    private data class PendingMessage(
        val gameObject: String,
        val method: String,
        val payload: String,
        val attempts: Int = 0,
    )

    /** Kirim pesan ke Unity. Diantrekan jika Unity belum ready. */
    fun send(gameObject: String = BRIDGE_OBJECT, method: String, payload: String = "") {
        if (!unityReady) {
            pending.add(PendingMessage(gameObject, method, payload))
            return
        }
        if (!deliver(gameObject, method, payload)) {
            // Gagal saat Unity diklaim ready — coba lagi terbatas via flush berikutnya.
            pending.add(PendingMessage(gameObject, method, payload, attempts = 1))
        }
    }

    private fun flushPending() {
        while (true) {
            val msg = pending.poll() ?: break
            if (!deliver(msg.gameObject, msg.method, msg.payload)) {
                if (msg.attempts + 1 < MAX_DELIVERY_ATTEMPTS) {
                    pending.add(msg.copy(attempts = msg.attempts + 1))
                } else {
                    val dropped =
                        "Pesan Unity dibuang setelah $MAX_DELIVERY_ATTEMPTS percobaan gagal: ${msg.method}"
                    lastError = dropped
                    Log.w(TAG, dropped)
                }
            }
        }
    }

    /** @return true bila pesan terkirim; false bila gagal (tanpa requeue di sini). */
    private fun deliver(gameObject: String, method: String, payload: String): Boolean {
        return try {
            val playerClass = Class.forName("com.unity3d.player.UnityPlayer")
            playerClass.getMethod(
                "UnitySendMessage",
                String::class.java, String::class.java, String::class.java,
            ).invoke(null, gameObject, method, payload)
            true
        } catch (e: Exception) {
            Log.w(TAG, "UnitySendMessage gagal: $method", e)
            false
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
