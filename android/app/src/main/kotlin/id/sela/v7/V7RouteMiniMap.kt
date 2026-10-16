package id.sela.v7

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import id.sela.model.TripPlan
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView

/**
 * Mini MapLibre untuk Detail Rute V7 — menampilkan polyline rute yang dipilih
 * melalui halte-halte tiap leg, dengan kamera pas ke bounds rute.
 *
 * Ringan: tidak memakai MapViewModel, hanya gambar statis rute.
 */
@Composable
fun V7RouteMiniMap(
    plan: TripPlan,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Titik-titik rute: dari tiap leg (from -> to)
    val points = remember(plan) {
        val pts = mutableListOf<LatLng>()
        plan.legs.forEach { leg ->
            val from = LatLng(leg.fromStop.lat, leg.fromStop.lon)
            val to = LatLng(leg.toStop.lat, leg.toStop.lon)
            if (pts.isEmpty()) pts += from
            pts += to
        }
        pts
    }

    val mapView = remember(context) {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    // Teruskan lifecycle ke MapView
    DisposableEffect(lifecycle, mapView) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = mapView.onStart()
            override fun onResume(owner: LifecycleOwner) = mapView.onResume()
            override fun onPause(owner: LifecycleOwner) = mapView.onPause()
            override fun onStop(owner: LifecycleOwner) = mapView.onStop()
            override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.fillMaxSize(),
        update = { mv ->
            mv.getMapAsync { map ->
                map.setStyle("https://tiles.openfreemap.org/styles/bright") {
                    if (points.size >= 2) {
                        // Polyline rute: garis navy tebal + lime di atasnya
                        map.addPolyline(
                            PolylineOptions().addAll(points)
                                .color(0xFF06133E.toInt()).width(9f),
                        )
                        map.addPolyline(
                            PolylineOptions().addAll(points)
                                .color(0xFFA9F500.toInt()).width(5f),
                        )
                        // Kamera pas ke bounds
                        val bounds = LatLngBounds.Builder().includes(points).build()
                        map.cameraPosition = CameraPosition.Builder()
                            .target(bounds.center)
                            .zoom(12.0)
                            .build()
                    } else if (points.size == 1) {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(points[0]).zoom(14.0).build()
                    }
                }
            }
        },
    )
}
