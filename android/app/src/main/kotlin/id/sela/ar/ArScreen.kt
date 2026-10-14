package id.sela.ar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import id.sela.model.EtaPredictor
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Layar "AR" kompas (fitur #7 AR halte, #88 AR navigasi).
 *
 * Batasan jujur: ini BUKAN ARCore — tidak ada depth/plane detection, tidak ada
 * penanda 3D yang menempel di dunia nyata. Yang ditampilkan: preview kamera
 * (CameraX) + overlay panah kompas yang dihitung dari sensor ROTATION_VECTOR
 * dan bearing great-circle ke halte. Akurasinya tergantung kalibrasi kompas HP
 * (goayangkan HP membentuk angka 8 bila panah ngaco).
 *
 * Mode:
 * - "halte": panah ke halte terdekat.
 * - "navigasi": panah ke halte tujuan ([targetStopId], mis. dari favorit).
 */
@Composable
fun ArScreen(
    mode: String = "halte",
    targetStopId: String? = null,
    viewModel: ArViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val data by viewModel.data.collectAsState()

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasCamera = grants[Manifest.permission.CAMERA] == true
        hasLocation = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!hasCamera || !hasLocation) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION),
            )
        }
    }

    var azimuth by remember { mutableStateOf(0.0) } // arah hadap HP, 0=utara
    var location by remember { mutableStateOf<Location?>(null) }

    // --- Sensor kompas (rotation vector) ---
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientation = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientation)
                azimuth = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0)
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (rotationVector != null) {
            sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // --- Lokasi pengguna (LocationManager, tanpa dependensi baru) ---
    DisposableEffect(context, hasLocation) {
        if (!hasLocation) {
            onDispose { /* menunggu izin lokasi */ }
        } else {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { loc -> location = loc }
        location = runCatching {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
        runCatching {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 2f, listener)
        }
        onDispose { runCatching { locationManager.removeUpdates(listener) } }
        }
    }

    // --- Preview kamera (CameraX) ---
    val previewView = remember { PreviewView(context) }
    LaunchedEffect(hasCamera) {
        if (!hasCamera) return@LaunchedEffect
        val provider = runCatching {
            suspendCoroutine { cont: Continuation<ProcessCameraProvider> ->
                val future = ProcessCameraProvider.getInstance(context)
                future.addListener(
                    { cont.resume(future.get()) },
                    ContextCompat.getMainExecutor(context),
                )
            }
        }.getOrNull() ?: return@LaunchedEffect
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView.surfaceProvider)
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
        }
    }
    DisposableEffect(lifecycleOwner) {
        onDispose { runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() } }
    }

    val userLoc = location
    val target = remember(data, userLoc, mode, targetStopId) {
        if (mode == "navigasi" && targetStopId != null) viewModel.stopById(targetStopId)
        else if (userLoc != null) viewModel.nearestStop(userLoc.latitude, userLoc.longitude)
        else null
    }
    val distanceM = if (target != null && userLoc != null) {
        EtaPredictor.haversineM(userLoc.latitude, userLoc.longitude, target.lat, target.lon)
    } else null
    val bearing = if (target != null && userLoc != null) {
        ArViewModel.bearingTo(userLoc.latitude, userLoc.longitude, target.lat, target.lon)
    } else null
    val etaMin = target?.let { viewModel.fastestEtaMinutes(it) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCamera) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Izin kamera dibutuhkan untuk mode AR.", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Panah arah di tengah layar.
        if (bearing != null) {
            val relative = ArViewModel.relativeAngle(bearing, azimuth).toFloat()
            Icon(
                imageVector = Icons.Filled.Navigation,
                contentDescription = "Arah ke ${target?.name}",
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(96.dp)
                    .graphicsLayer { rotationZ = relative },
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        // Kartu info halte di bawah.
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(if (mode == "navigasi") "Navigasi halte" else "Halte terdekat") },
                )
                if (target == null) {
                    AssistChip(onClick = {}, label = { Text("Menunggu lokasi…") })
                }
            }
            if (target != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(target.name, style = MaterialTheme.typography.titleMedium)
                        val info = buildString {
                            if (distanceM != null) append("• ${distanceM.toInt()} m ")
                            if (target.routeIds.isNotEmpty()) {
                                append("• koridor ${target.routeIds.take(4).joinToString(", ")}")
                            }
                            if (etaMin != null) append(" • bus ±${etaMin.toInt()} mnt")
                        }.trimStart('•', ' ')
                        Text(info, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Panah mengikuti kompas HP — goyangkan HP membentuk angka 8 bila arah ngaco.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
