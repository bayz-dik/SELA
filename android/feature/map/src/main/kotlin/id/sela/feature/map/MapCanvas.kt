@file:Suppress("DEPRECATION")
package id.sela.feature.map

import android.animation.ValueAnimator
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.sela.designsystem.SelaPastel
import id.sela.designsystem.pastelColor
import androidx.compose.ui.graphics.toArgb
import id.sela.model.*
import id.sela.feature.search.filterStops
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.*
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val TAG = "SelaMap"

/** Batas marker bus di peta agar tidak bertumpuk menutupi tampilan. */
private const val MAX_VEHICLE_MARKERS = 120
/**
 * B24: batas tunggu style selesai dimuat (sukses/gagal) sebelum percobaan
 * dianggap gagal — menangani jaringan macet yang tak memicu callback apa pun.
 */
private const val STYLE_LOAD_TIMEOUT_MS = 20_000L
/**
 * B24: batas tunggu frame peta ter-render penuh setelah style siap —
 * menangani tile yang gagal diam-diam (tak memicu OnDidFailLoadingMap).
 */
private const val FIRST_FRAME_TIMEOUT_MS = 25_000L

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun MapCanvas(state: MapUiState, styleUrl: String, modifier: Modifier = Modifier,
    onSelect: (String, String) -> Unit, onCamera: (MapCamera) -> Unit, onError: (String) -> Unit, onBounds: (VisibleBounds) -> Unit = {},
    show3dBuildings: Boolean = false,
    /** 3D Ultra: aktifkan tilt gesture dua-jari (hanya saat mode 3D). */
    tiltGesturesEnabled: Boolean = false,
    /** 3D Ultra: dipanggil saat user menggerakkan kamera manual (untuk Follow → Free). */
    onManualGesture: () -> Unit = {},
    /** B21: follow mode 2D aktif — tampilkan titik biru posisi user. */
    followActive: Boolean = false,
    /** B21: posisi user terakhir (hanya dipakai saat [followActive]). */
    userLocation: GeoPoint? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val selection by rememberUpdatedState(onSelect)
    val cameraCallback by rememberUpdatedState(onCamera)
    val boundsCallback by rememberUpdatedState(onBounds)
    val errorCallback by rememberUpdatedState(onError)
    val gestureCallback by rememberUpdatedState(onManualGesture)
    val requestRef = rememberUpdatedState(styleUrl)
    val mapView = remember(context) { MapLibre.getInstance(context); MapView(context).apply { onCreate(null) } }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    val mapRef = rememberUpdatedState(map)
    var styleReady by remember { mutableStateOf(false) }
    // B20: epoch naik tiap style selesai diganti — efek anotasi di-key padanya
    // agar dibuat ulang, aman baik anotasi bertahan maupun hancur saat ganti style.
    var styleEpoch by remember { mutableStateOf(0) }
    // B24: styleUrl yang rantainya sudah berhasil dimuat (pada index berapa pun).
    var appliedRequest by remember { mutableStateOf<String?>(null) }
    // B24: rantai fallback style — gelap → terang; satelit tanpa fallback.
    val styleChain = remember(styleUrl) { styleFallbackChain(styleUrl) }
    val chainRef = rememberUpdatedState(styleChain)
    var chainIndex by remember { mutableStateOf(0) }
    var stylePending by remember { mutableStateOf(false) }
    var awaitingFrame by remember { mutableStateOf(false) }
    var styleToken by remember { mutableStateOf(0) }
    var frameToken by remember { mutableStateOf(0) }
    val destroyedRef = remember { mutableStateOf(false) }

    /**
     * B24: muat style pada [index] rantai fallback. Dipakai saat init, saat
     * user ganti style manual, dan saat fallback otomatis.
     */
    val loadStyleAt = remember {
        { nativeMap: MapLibreMap, index: Int ->
            val chain = chainRef.value
            if (!destroyedRef.value && index in chain.indices) {
                val requested = requestRef.value
                chainIndex = index
                stylePending = true
                awaitingFrame = false
                styleToken++
                Log.i(TAG, "Memuat style peta (percobaan ${index + 1}/${chain.size})")
                applyMapStyle(nativeMap, chain[index]) {
                    // Abaikan callback basi — user sudah meminta style lain
                    // atau MapView sudah diganti/dihancurkan.
                    if (destroyedRef.value || mapRef.value !== nativeMap ||
                        requestRef.value != requested
                    ) return@applyMapStyle
                    stylePending = false
                    awaitingFrame = true
                    frameToken++
                    val first = !styleReady
                    styleReady = true
                    // B20: bangun ulang anotasi tiap style selesai dimuat.
                    if (!first) styleEpoch++
                    appliedRequest = requested
                    Log.i(TAG, "Style peta percobaan ${index + 1}/${chain.size} berhasil dimuat")
                    nativeMap.projection.visibleRegion.latLngBounds.let {
                        boundsCallback(VisibleBounds(it.latitudeSouth, it.longitudeWest, it.latitudeNorth, it.longitudeEast))
                    }
                }
            }
        }
    }
    /**
     * B24: satu percobaan style gagal (error native / timeout watchdog).
     * Maju ke fallback berikut; bila rantai habis, tampilkan pesan yang
     * jelas ke user — bukan peta hitam misterius.
     */
    val failStyleAttempt = remember {
        { reason: String ->
            if (!destroyedRef.value && (stylePending || awaitingFrame)) {
                stylePending = false
                awaitingFrame = false
                val chain = chainRef.value
                val next = chainIndex + 1
                if (next < chain.size) {
                    Log.w(TAG, "Style ${chainIndex + 1}/${chain.size} gagal ($reason) — coba fallback ${next + 1}/${chain.size}")
                    mapRef.value?.let { loadStyleAt(it, next) }
                } else {
                    Log.e(TAG, "Semua ${chain.size} style peta gagal dimuat. Alasan terakhir: $reason")
                    errorCallback(
                        "Peta tidak dapat dimuat. Periksa koneksi internet, lalu ganti " +
                            "mode peta (gelap/satelit) untuk mencoba lagi. " +
                            "Daftar halte dan rute tetap dapat dibuka."
                    )
                }
            }
        }
    }
    // B24: watchdog style — jaringan macet tanpa callback sukses/gagal.
    LaunchedEffect(styleToken) {
        if (styleToken == 0 || destroyedRef.value) return@LaunchedEffect
        delay(STYLE_LOAD_TIMEOUT_MS)
        if (!destroyedRef.value && stylePending) {
            Log.w(TAG, "Timeout ${STYLE_LOAD_TIMEOUT_MS}ms menunggu style peta")
            failStyleAttempt("timeout ${STYLE_LOAD_TIMEOUT_MS}ms tanpa respons")
        }
    }
    // B24: watchdog frame — style OK tapi tile tak kunjung tampil penuh.
    LaunchedEffect(frameToken) {
        if (frameToken == 0 || destroyedRef.value) return@LaunchedEffect
        delay(FIRST_FRAME_TIMEOUT_MS)
        if (!destroyedRef.value && awaitingFrame) {
            Log.w(TAG, "Timeout ${FIRST_FRAME_TIMEOUT_MS}ms menunggu tile peta tampil")
            failStyleAttempt("timeout ${FIRST_FRAME_TIMEOUT_MS}ms menunggu tile tampil")
        }
    }
    val userDot = remember(context) { userDotIcon(context) }
    val userMarkers = remember { mutableMapOf<String, Marker>() }
    val markerIds = remember { mutableMapOf<Long, Pair<String, String>>() }
    val icons = remember { mutableMapOf<String, Icon>() }
    val stopMarkers = remember { mutableMapOf<String, Marker>() }
    val vehicleMarkers = remember { mutableMapOf<String, Marker>() }
    val routeLines = remember { mutableListOf<Polyline>() }
    val routes = remember(state.data.routes) { state.data.routes.associateBy { it.id } }
    val routeColor = routes[state.selectedId]?.displayServiceType?.pastelColor()?.toArgb() ?: SelaPastel.mint.toArgb()
    val initialCamera = remember { state.camera }
    DisposableEffect(mapView, lifecycle) {
        // Efek baru = mapView baru/hidup lagi (pengganti `var destroyed` lokal).
        destroyedRef.value = false
        var started = false
        var resumed = false
        fun stop() {
            if (resumed) { mapView.onPause(); resumed = false }
            if (started) { mapView.onStop(); started = false }
        }
        fun destroy() { if (!destroyedRef.value) { stop(); mapView.onDestroy(); destroyedRef.value = true } }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started && !destroyedRef.value) { mapView.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> if (!resumed && !destroyedRef.value) { mapView.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> if (resumed) { mapView.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> stop()
                Lifecycle.Event.ON_DESTROY -> destroy()
                else -> Unit
            }
        }
        val memory = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            override fun onLowMemory() { if (!destroyedRef.value) mapView.onLowMemory() }
            override fun onTrimMemory(level: Int) { if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) onLowMemory() }
        }
        context.registerComponentCallbacks(memory)
        // B24: catat alasan ASLI ke logcat (dulu dibuang), lalu fallback berlapis.
        // onDidFailLoadingMap hanya untuk kegagalan level style/map
        // (StyleParseError/StyleLoadError/NotFoundError) — bukan tile satuan.
        mapView.addOnDidFailLoadingMapListener { reason ->
            Log.e(TAG, "Peta gagal dimuat: $reason")
            failStyleAttempt("OnDidFailLoadingMap: $reason")
        }
        // B24: frame penuh pertama = peta benar-benar tampil, bukan hitam.
        mapView.addOnDidFinishRenderingFrameListener { fully, _, _ ->
            if (fully && !destroyedRef.value && awaitingFrame) {
                awaitingFrame = false
                Log.i(TAG, "Frame peta ter-render penuh — peta tampil")
            }
        }
        mapView.getMapAsync { nativeMap ->
            if (!destroyedRef.value) {
                map = nativeMap
                nativeMap.cameraPosition = CameraPosition.Builder().target(LatLng(initialCamera.lat, initialCamera.lon)).zoom(initialCamera.zoom).build()
                nativeMap.uiSettings.isAttributionEnabled = true
                nativeMap.uiSettings.isTiltGesturesEnabled = tiltGesturesEnabled
                nativeMap.setOnMarkerClickListener { marker -> markerIds[marker.id]?.let { selection(it.first, it.second) }; true }
                // 3D Ultra: gesture manual (API_GESTURE) membatalkan mode Follow → Free (§7/§21).
                nativeMap.addOnCameraMoveStartedListener { reason ->
                    if (!destroyedRef.value && reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                        gestureCallback()
                    }
                }
                nativeMap.addOnCameraIdleListener {
                    nativeMap.projection.visibleRegion.latLngBounds.let { boundsCallback(VisibleBounds(it.latitudeSouth, it.longitudeWest, it.latitudeNorth, it.longitudeEast)) }
                    if (!destroyedRef.value) nativeMap.cameraPosition.let { position -> position.target?.let { cameraCallback(MapCamera(it.latitude, it.longitude, position.zoom, position.tilt, position.bearing)) } }
                }
                loadStyleAt(nativeMap, 0)
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            context.unregisterComponentCallbacks(memory)
            destroy()
            markerIds.clear()
            stopMarkers.clear()
            vehicleMarkers.clear()
            routeLines.clear()
            icons.clear()
        }
    }
    // B20: ganti style gelap ↔ satelit tanpa membuat ulang MapView.
    // B24: tiap styleUrl baru mulai dari index 0 rantai fallback-nya.
    LaunchedEffect(map, styleChain) {
        val nativeMap = map ?: return@LaunchedEffect
        if (!styleReady || appliedRequest == styleUrl) return@LaunchedEffect
        loadStyleAt(nativeMap, 0)
    }
    // B21: titik biru #4285f4 posisi user — hanya tampil saat follow mode aktif.
    // Sengaja tidak didaftarkan ke markerIds: ketuk titik biru = no-op.
    LaunchedEffect(map, styleReady, styleEpoch, followActive, userLocation) {
        val nativeMap = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        val loc = if (followActive) userLocation else null
        if (loc == null) {
            removeMissingMarkers(nativeMap, userMarkers, emptySet(), markerIds)
        } else {
            removeMissingMarkers(nativeMap, userMarkers, setOf(USER_MARKER_ID), markerIds)
            updateTransitMarker(
                nativeMap, userMarkers, USER_MARKER_ID,
                LatLng(loc.lat, loc.lon), "Lokasi saya", null, userDot,
            )
        }
    }
    // Gedung 3D (#51) — layer fill-extrusion di atas style yang sudah siap.
    // Batasan jujur: butuh data building dari style; gagal diam-diam bila tidak ada.
    // B20: di-key pada styleEpoch agar layer dipasang ulang tiap ganti style.
    LaunchedEffect(map, styleReady, styleEpoch, show3dBuildings) {
        val nativeMap = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        runCatching {
            val style = nativeMap.style ?: return@runCatching
            val existing = style.getLayer("sela-3d-buildings")
            if (show3dBuildings && existing == null) {
                val layer = org.maplibre.android.style.layers.FillExtrusionLayer(
                    "sela-3d-buildings", "openmaptiles",
                ).withProperties(
                    org.maplibre.android.style.layers.PropertyFactory.fillExtrusionColor("#8a94a6"),
                    org.maplibre.android.style.layers.PropertyFactory.fillExtrusionHeight(
                        org.maplibre.android.style.expressions.Expression.get("render_height"),
                    ),
                    org.maplibre.android.style.layers.PropertyFactory.fillExtrusionBase(
                        org.maplibre.android.style.expressions.Expression.get("render_min_height"),
                    ),
                    org.maplibre.android.style.layers.PropertyFactory.fillExtrusionOpacity(0.55f),
                )
                layer.withSourceLayer("building")
                layer.minZoom = 15f
                style.addLayer(layer)
            } else if (!show3dBuildings && existing != null) {
                style.removeLayer(existing)
            }
        }
    }
    // Stops and the route stay intact when only vehicle telemetry changes.
    // B20: di-key pada styleEpoch agar marker dibuat ulang tiap ganti style.
    LaunchedEffect(map, styleReady, styleEpoch, state.data.stops, routes, state.filter, state.visibleBounds) {
        val nativeMap = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        val visibleStops = filterStops(state.data.stops, state.data.routes, state.filter).filter { stop ->
            stop.lat in -90.0..90.0 && stop.lon in -180.0..180.0 &&
                (state.visibleBounds?.let { stop.lat in it.south..it.north && stop.lon in it.west..it.east } ?: false)
        }
        removeMissingMarkers(nativeMap, stopMarkers, visibleStops.mapTo(mutableSetOf()) { it.id }, markerIds)
        visibleStops.forEach { stop ->
            val type = state.filter ?: stop.routeIds.mapNotNull { routes[it]?.displayServiceType }.minByOrNull { it.ordinal }
            val color = type?.pastelColor()?.toArgb() ?: SelaPastel.inkFaint.toArgb()
            val icon = icons.getOrPut("stop-$color") { transitIcon(context, color, bus = false) }
            val marker = updateTransitMarker(nativeMap, stopMarkers, stop.id, LatLng(stop.lat, stop.lon), "Halte ${stop.name}", null, icon)
            markerIds[marker.id] = "stop" to stop.id
        }
    }
    LaunchedEffect(map, styleReady, styleEpoch, state.data.vehicles, routes, state.filter, state.visibleBounds, state.selectedRouteId) {
        val nativeMap = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        val bounds = state.visibleBounds
        val selectedRouteId = state.selectedRouteId
        // BUGFIX: batasi marker bus agar tidak bertumpuk menutupi peta.
        // 1) Hanya tampilkan bus dalam bounds terlihat. 2) Maks 120 marker —
        //    prioritaskan rute yang dipilih, lalu yang terdekat ke tengah peta.
        val centerLat = bounds?.let { (it.south + it.north) / 2 } ?: state.camera.lat
        val centerLon = bounds?.let { (it.west + it.east) / 2 } ?: state.camera.lon
        val vehicles = state.data.vehicles
            .filter { vehicle ->
                !vehicle.isStale && (state.filter == null || routes[vehicle.routeId]?.displayServiceType == state.filter) &&
                    vehicle.lat in -90.0..90.0 && vehicle.lon in -180.0..180.0 &&
                    (bounds?.let { vehicle.lat in it.south..it.north && vehicle.lon in it.west..it.east } ?: true)
            }
            .sortedWith(
                compareBy(
                    { if (selectedRouteId != null && it.routeId == selectedRouteId) 0 else 1 },
                    { (it.lat - centerLat) * (it.lat - centerLat) + (it.lon - centerLon) * (it.lon - centerLon) },
                ),
            )
            .take(MAX_VEHICLE_MARKERS)
        removeMissingMarkers(nativeMap, vehicleMarkers, vehicles.mapTo(mutableSetOf()) { it.id }, markerIds)
        vehicles.forEach { vehicle ->
            val color = routes[vehicle.routeId]?.displayServiceType?.pastelColor()?.toArgb() ?: SelaPastel.inkFaint.toArgb()
            val icon = icons.getOrPut("bus-$color") { transitIcon(context, color, bus = true) }
            // Reported coordinates only: no interpolation or implied live movement.
            val marker = updateTransitMarker(nativeMap, vehicleMarkers, vehicle.id, LatLng(vehicle.lat, vehicle.lon),
                "Bus ${vehicle.id}", "Diperbarui ${vehicle.observedAt}", icon)
            markerIds[marker.id] = "vehicle" to vehicle.id
        }
    }
    LaunchedEffect(map, styleReady, styleEpoch, state.shape, routeColor) {
        val nativeMap = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        routeLines.forEach { nativeMap.removeAnnotation(it) }
        routeLines.clear()
        val points = state.shape.filter { it.lat in -90.0..90.0 && it.lon in -180.0..180.0 }.map { LatLng(it.lat, it.lon) }
        if (points.size > 1) {
            // Casing gelap membuat warna layanan tetap terbaca di atas peta malam.
            routeLines += nativeMap.addPolyline(PolylineOptions().addAll(points).color(SelaPastel.ink.toArgb()).width(10f))
            routeLines += nativeMap.addPolyline(PolylineOptions().addAll(points).color(routeColor).width(5.5f))
        }
    }
    LaunchedEffect(map, state.cameraRequest) {
        val nativeMap = map ?: return@LaunchedEffect
        if (state.cameraRequest > 0) {
            // 3D Ultra: tilt & bearing ikut dianimasikan (§7 spec).
            val update = CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                .target(LatLng(state.camera.lat, state.camera.lon)).zoom(state.camera.zoom)
                .tilt(state.camera.tilt).bearing(state.camera.bearing).build())
            nativeMap.cancelTransitions()
            if (ValueAnimator.areAnimatorsEnabled()) nativeMap.easeCamera(update, 800)
            else nativeMap.moveCamera(update)
        }
    }
    // 3D Ultra: sinkronkan tilt gesture dengan mode (mati di 2D agar tidak konflik).
    LaunchedEffect(map, tiltGesturesEnabled) {
        map?.uiSettings?.isTiltGesturesEnabled = tiltGesturesEnabled
    }
    // Fitur #75: gesture rahasia — triple-tap cepat di peta → info koridor 1.
    // Batasan jujur: gesture di layar HP yang hidup, bukan di layar mati
    // (Android tidak mengizinkan app pihak ketiga menangkap sentuhan saat layar mati).
    val tripleTap = remember {
        var taps = 0
        var lastTap = 0L
        android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: android.view.MotionEvent): Boolean {
                val now = System.currentTimeMillis()
                taps = if (now - lastTap < 500) taps + 1 else 1
                lastTap = now
                if (taps >= 3) {
                    taps = 0
                    errorCallback("Koridor 1: Blok M – Kota (BRT). Ketuk halte untuk ETA live.")
                }
                return false
            }
        })
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier.pointerInteropFilter { event ->
            tripleTap.onTouchEvent(event)
            false // jangan telan gesture peta
        },
    )
}

private fun removeMissingMarkers(
    map: MapLibreMap,
    markers: MutableMap<String, Marker>,
    retainedIds: Set<String>,
    markerIds: MutableMap<Long, Pair<String, String>>,
) {
    val iterator = markers.iterator()
    while (iterator.hasNext()) {
        val (id, marker) = iterator.next()
        if (id !in retainedIds) {
            markerIds.remove(marker.id)
            map.removeAnnotation(marker)
            iterator.remove()
        }
    }
}

private fun updateTransitMarker(
    map: MapLibreMap,
    markers: MutableMap<String, Marker>,
    id: String,
    position: LatLng,
    title: String,
    snippet: String?,
    icon: Icon,
): Marker {
    val marker = markers[id]
    if (marker == null) {
        return map.addMarker(MarkerOptions().position(position).title(title).snippet(snippet).icon(icon))
            .also { markers[id] = it }
    }
    // SDK setters propagate changes; avoid native work for unchanged annotations.
    if (marker.position != position) marker.position = position
    if (marker.icon != icon) marker.icon = icon
    if (marker.title != title) marker.title = title
    if (marker.snippet != snippet) marker.snippet = snippet
    return marker
}

/** Id marker titik biru posisi user (follow mode). */
private const val USER_MARKER_ID = "sela-user-dot"

/**
 * B20: terapkan style peta — URL style (gelap OpenFreeMap) atau JSON style
 * inline (satelit ESRI World Imagery). MapLibre butuh objek style penuh;
 * tile URL mentah tidak bisa di-set langsung.
 */
private fun applyMapStyle(nativeMap: MapLibreMap, style: String, onLoaded: () -> Unit) {
    val builder = if (style.trimStart().startsWith("{")) Style.Builder().fromJson(style)
    else Style.Builder().fromUrl(style)
    nativeMap.setStyle(builder) { onLoaded() }
}

/**
 * B21: titik biru #4285f4 dengan ring putih — penanda posisi user saat
 * follow mode aktif (port dari web).
 */
private fun userDotIcon(context: Context): Icon {
    val density = context.resources.displayMetrics.density
    val size = (32 * density).toInt().coerceAtLeast(32)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(size / 32f, size / 32f)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        setShadowLayer(2.5f, 0f, 1.5f, 0x66000000)
    }
    canvas.drawCircle(16f, 16f, 13f, paint)
    paint.clearShadowLayer()
    paint.color = 0xFF4285F4.toInt()
    canvas.drawCircle(16f, 16f, 10f, paint)
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

/**
 * Ikon marker untuk peta malam: ring putih sebagai halo, isi warna layanan,
 * glif bus siluet gelap. Padding transparan menampung bayangan tanpa
 * menggeser anchor.
 */
private fun transitIcon(context: Context, color: Int, bus: Boolean): Icon {
    val density = context.resources.displayMetrics.density
    val size = (48 * density).toInt().coerceAtLeast(48)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.scale(size / 48f, size / 48f)
    val dark = SelaPastel.ink.toArgb()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        setShadowLayer(2.5f, 0f, 1.5f, 0x66000000)
    }
    if (bus) {
        // Halo putih, isi warna layanan.
        canvas.drawRoundRect(4f, 4f, 44f, 44f, 13f, 13f, paint)
        paint.clearShadowLayer()
        paint.color = color
        canvas.drawRoundRect(7f, 7f, 41f, 41f, 11f, 11f, paint)
        // Siluet bus tampak depan: badan gelap, detail putih.
        paint.color = dark
        canvas.drawRoundRect(15f, 13f, 33f, 33f, 4f, 4f, paint)
        paint.color = android.graphics.Color.WHITE
        canvas.drawRoundRect(20f, 15f, 28f, 17f, 1f, 1f, paint) // panel trayek
        canvas.drawRoundRect(17f, 19f, 31f, 26f, 1.5f, 1.5f, paint) // kaca depan
        canvas.drawCircle(18.5f, 29.5f, 1.25f, paint) // lampu
        canvas.drawCircle(29.5f, 29.5f, 1.25f, paint)
        canvas.drawRect(16f, 31f, 32f, 32.5f, paint) // bumper
        paint.color = dark
        canvas.drawRect(23.25f, 19f, 24.75f, 26f, paint) // pembelah kaca
    } else {
        // Halte: halo putih, lingkaran gelap, ring + titik warna layanan.
        canvas.drawCircle(24f, 24f, 17f, paint)
        paint.clearShadowLayer()
        paint.color = dark
        canvas.drawCircle(24f, 24f, 14.5f, paint)
        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        canvas.drawCircle(24f, 24f, 10.5f, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(24f, 24f, 4f, paint)
    }
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}
