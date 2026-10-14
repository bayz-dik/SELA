package id.sela.feature.unity

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Activity full-screen untuk mode 3D Unity (§V4: Unity as a Library).
 *
 * Pola: Unity dirender full-screen di Activity khusus (bukan embedded view di
 * Compose) — pola UAL paling robust. Tombol × kembali ke peta 2D.
 *
 * Data awal (kamera, stop, rute) di-pass via intent extras sebagai JSON;
 * update berikutnya via [UnityBridge].
 */
class UnityMapActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CAMERA_JSON = "extra_camera_json"
        const val EXTRA_STOPS_JSON = "extra_stops_json"
        const val EXTRA_ROUTES_JSON = "extra_routes_json"
    }

    private val bridge = UnityBridge.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                UnityMapScreen(
                    onClose = { finish() },
                    initialCameraJson = intent.getStringExtra(EXTRA_CAMERA_JSON),
                    initialStopsJson = intent.getStringExtra(EXTRA_STOPS_JSON),
                    initialRoutesJson = intent.getStringExtra(EXTRA_ROUTES_JSON),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bridge.onResume()
    }

    override fun onPause() {
        bridge.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        bridge.onDestroy(isFinishing)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        bridge.onWindowFocusChanged(hasFocus)
    }
}

@Composable
private fun UnityMapScreen(
    onClose: () -> Unit,
    initialCameraJson: String?,
    initialStopsJson: String?,
    initialRoutesJson: String?,
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var unityReady by remember { mutableStateOf(false) }
    var cameraMode by remember { mutableStateOf("free") } // free | follow
    var showQualityDialog by remember { mutableStateOf(false) }
    var qualityTier by remember { mutableStateOf("auto") }
    val bridge = remember { UnityBridge.get() }

    DisposableEffect(Unit) {
        val listener = object : UnityBridge.Listener {
            override fun onUnityReady() {
                unityReady = true
                // Kirim data awal setelah engine siap.
                initialCameraJson?.let { bridge.send(method = "SetCamera", payload = it) }
                initialStopsJson?.let { bridge.setStopsJson(it) }
                initialRoutesJson?.let { bridge.setRoutesJson(it) }
                bridge.setQualityTier(qualityTier)
                // Waktu Jakarta saat ini
                val hour = java.util.Calendar.getInstance(
                    java.util.TimeZone.getTimeZone("Asia/Jakarta"))
                    .get(java.util.Calendar.HOUR_OF_DAY) +
                    java.util.Calendar.getInstance(
                        java.util.TimeZone.getTimeZone("Asia/Jakarta"))
                        .get(java.util.Calendar.MINUTE) / 60f
                bridge.setTimeOfDay(hour)
            }

            override fun onStopSelected(stopId: String) {
                // TODO: teruskan ke ViewModel 2D (bottom sheet halte).
            }

            override fun onCameraChanged(lat: Double, lon: Double, zoom: Float, bearing: Float, pitch: Float) {
                // TODO: sinkronkan state kamera untuk kembali ke 2D tanpa lompatan.
            }
        }
        bridge.listener = listener
        onDispose { bridge.listener = null }
    }

    Box(Modifier.fillMaxSize()) {
        if (UnityAvailability.is3dSupported(context)) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).also { container ->
                        (activity as? android.app.Activity)?.let { act ->
                            bridge.attach(act, container)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            UnityUnavailableMessage(
                reason = UnityAvailability.unsupportedReason(context),
                onClose = onClose,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // HUD 3D (spec §9): tombol kembali, mode kamera, recenter, kualitas.
        // Tombol kembali ke 2D — selalu di atas Unity view.
        FloatingActionButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Kembali ke peta 2D")
        }

        // Kolom kontrol kanan bawah: Free/Follow, Recenter, Kualitas.
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Toggle Free / Follow (§7, §21)
            SmallFloatingActionButton(
                onClick = {
                    cameraMode = if (cameraMode == "free") "follow" else "free"
                    // Kirim ulang kamera dengan mode baru (posisi dipertahankan Unity)
                    bridge.send(
                        method = "SetCamera",
                        payload = """{"mode":"$cameraMode"}""",
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Icon(
                    if (cameraMode == "free") Icons.Filled.GpsFixed
                    else Icons.Filled.MyLocation,
                    contentDescription = if (cameraMode == "free")
                        "Aktifkan Smart Follow" else "Kembali ke Free Camera",
                )
            }
            // Recenter: kembali ke Follow + reset bearing ke utara
            SmallFloatingActionButton(
                onClick = {
                    cameraMode = "follow"
                    bridge.send(
                        method = "SetCamera",
                        payload = """{"mode":"follow","bearing":0}""",
                    )
                },
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Icon(Icons.Filled.Explore, contentDescription = "Recenter + utara")
            }
            // Kualitas grafis (§V3: Low/Medium/High/Ultra/Auto)
            SmallFloatingActionButton(
                onClick = { showQualityDialog = true },
            ) {
                Icon(Icons.Filled.Tune, contentDescription = "Kualitas grafis: $qualityTier")
            }
        }

        // Indikator mode kamera
        if (unityReady) {
            Text(
                text = if (cameraMode == "follow") "SMART FOLLOW" else "FREE CAMERA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp),
            )
        }
    }

    // Dialog pilih kualitas
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Kualitas Grafis 3D") },
            text = {
                Column {
                    listOf("auto", "low", "medium", "high", "ultra").forEach { tier ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            TextButton(
                                onClick = {
                                    qualityTier = tier
                                    bridge.setQualityTier(tier)
                                    showQualityDialog = false
                                },
                            ) {
                                Text(
                                    text = (if (tier == qualityTier) "● " else "○ ") +
                                        tier.replaceFirstChar { it.uppercase() } +
                                        if (tier == "auto") " (otomatis)" else "",
                                    color = if (tier == qualityTier)
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Tutup")
                }
            },
        )
    }
}
