package id.sela.feature.map

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.sela.designsystem.*
import id.sela.feature.search.*
import id.sela.location.ForegroundLocation
import id.sela.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Style peta gelap OpenFreeMap — selaras tema Malam Kota. */
private const val SELA_MAP_STYLE = "https://tiles.openfreemap.org/styles/dark"

/**
 * B20: style satelit — tile raster gratis ESRI World Imagery.
 * MapLibre butuh objek style penuh (bukan tile URL mentah), jadi dibungkus
 * JSON inline. Atribusi Esri tampil via uiSettings.isAttributionEnabled.
 */
private const val SELA_SATELLITE_STYLE = """
{
  "version": 8,
  "name": "SELA Satelit",
  "sources": {
    "esri-imagery": {
      "type": "raster",
      "tiles": ["https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"],
      "tileSize": 256,
      "maxzoom": 19,
      "attribution": "Imagery © Esri, Maxar, Earthstar Geographics"
    }
  },
  "layers": [
    { "id": "esri-imagery", "type": "raster", "source": "esri-imagery", "minzoom": 0, "maxzoom": 22 }
  ]
}
"""

/** B21: follow mode 2D (port dari web) — snap ke bus terdekat dalam radius ini. */
private const val FOLLOW_SNAP_METERS = 30.0
/** B21: follow mode 2D — lepas snap bus bila jarak user↔bus melebihi ini. */
private const val FOLLOW_RELEASE_METERS = 60.0

// ---------------------------------------------------------------------------
// Bilah atas
// ---------------------------------------------------------------------------

/** Pil status koneksi — ringkas, melayang, berdenyut saat live. */
@Composable
fun ConnectionPill(status: ConnectionStatus, hasVehicles: Boolean) {
    val (active, text) = when (status) {
        ConnectionStatus.LIVE -> true to
            if (hasVehicles) "Live" else "Live · menunggu bus"
        ConnectionStatus.CONNECTING -> false to "Menghubungkan…"
        ConnectionStatus.RECONNECTING -> false to "Menyambung ulang…"
        ConnectionStatus.OFFLINE -> false to "Offline"
        ConnectionStatus.ERROR -> false to "Akses ditolak"
    }
    Surface(
        shape = SelaTokens.pill,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = SelaTokens.floatElevation,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelaPulseDot(active = active)
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Legenda warna layanan — pil melayang di atas dock. */
@Composable
fun ServiceLegend() {
    Surface(
        shape = SelaTokens.pill,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = SelaTokens.floatElevation,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceType.entries.forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = it.color(), modifier = Modifier.size(8.dp)) {}
                    Text(it.label(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dock bawah
// ---------------------------------------------------------------------------

@Composable
private fun DockButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = label },
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Dock kaca: Rute · Halte · Favorit · Bus + tombol volt Lokasi saya. */
@Composable
private fun MapDock(
    onLocate: () -> Unit,
    onRoutes: () -> Unit,
    onStops: () -> Unit,
    onFavorites: () -> Unit,
    onVehicles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Surface(
            shape = SelaTokens.pill,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = SelaTokens.floatElevation,
            modifier = Modifier.weight(1f),
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DockButton(Icons.Default.Route, "Rute", onRoutes)
                DockButton(Icons.Default.LocationOn, "Halte", onStops)
                DockButton(Icons.Default.Favorite, "Favorit", onFavorites)
                DockButton(Icons.Default.DirectionsBus, "Bus", onVehicles)
            }
        }
        Surface(
            onClick = onLocate,
            shape = CircleShape,
            color = SelaPalette.volt,
            contentColor = SelaPalette.onVolt,
            shadowElevation = SelaTokens.floatElevation,
            modifier = Modifier
                .size(64.dp)
                .semantics { contentDescription = "Lokasi saya" },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(26.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Panel daftar
// ---------------------------------------------------------------------------

@Composable
fun ColumnScope.RouteSelectorSheet(routes: List<Route>, onSelect: (String) -> Unit) {
    SelaSheetHeader(title = "Pilih rute", subtitle = "${routes.size} rute tersedia")
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        if (routes.isEmpty()) item { SelaEmptyState("Rute tidak ditemukan.") }
        items(routes, key = { it.id }) { route ->
            SelaListRow(
                title = route.longName.ifBlank { "Rute ${route.shortName}" },
                subtitle = "Rute ${route.shortName} · ${route.displayServiceType.label()}",
                leadingIcon = Icons.Default.Route,
                leadingTint = route.displayServiceType.color(),
                onClick = { onSelect(route.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
                trailing = { SelaRouteBadge(route.shortName, route.displayServiceType.color()) },
            )
        }
    }
}

@Composable
private fun ColumnScope.VehicleListSheet(state: MapUiState, onSelect: (String) -> Unit) {
    val live = state.vehicles.filter { !it.isStale }
    SelaSheetHeader(
        title = "Bus terpantau",
        subtitle = if (live.isEmpty()) "Belum ada posisi terkini" else "${live.size} bus live",
    )
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        if (live.isEmpty()) item { SelaEmptyState("Belum ada posisi bus terkini. Halte dan rute tetap tersedia.") }
        items(live, key = { it.id }) { bus ->
            val route = state.routes.find { r -> r.id == bus.routeId }
            SelaListRow(
                title = "Bus ${bus.id}",
                subtitle = route?.let { "${it.shortName} · ${it.longName}" } ?: "Rute ${bus.routeId}",
                leadingIcon = Icons.Default.DirectionsBus,
                leadingTint = route?.displayServiceType?.color() ?: MaterialTheme.colorScheme.primary,
                onClick = { onSelect(bus.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
                trailing = { SelaPulseDot(active = true) },
            )
        }
    }
}

@Composable
private fun ColumnScope.StopListSheet(
    stops: List<Stop>,
    nearby: GeoPoint?,
    onSelect: (String) -> Unit,
) {
    SelaSheetHeader(
        title = if (nearby == null) "Halte" else "Halte terdekat",
        subtitle = "${stops.size} halte",
    )
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        if (stops.isEmpty()) item { SelaEmptyState("Belum ada data halte.") }
        val ordered = nearby?.let { p ->
            stops.sortedBy { (it.lat - p.lat) * (it.lat - p.lat) + (it.lon - p.lon) * (it.lon - p.lon) }
        } ?: stops
        items(ordered, key = { it.id }) { stop ->
            SelaListRow(
                title = stop.name,
                subtitle = if (stop.routeIds.isNotEmpty()) "${stop.routeIds.size} rute" else "Halte",
                leadingIcon = Icons.Default.DirectionsBus,
                onClick = { onSelect(stop.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
fun PermissionRationale(onAllow: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gunakan lokasi?") },
        text = { Text("Lokasi hanya dipakai saat mencari posisi Anda. Tanpa izin, peta dan pencarian tetap bisa dipakai.") },
        confirmButton = { TextButton(onAllow) { Text("Lanjut") } },
        dismissButton = { TextButton(onDismiss) { Text("Tidak sekarang") } },
        shape = SelaTokens.card,
    )
}

// ---------------------------------------------------------------------------
// Layar utama
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    show3dBuildings: Boolean = false,
    stopExtras: StopSheetExtras = StopSheetExtras(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val visibleStops = remember(state.stops, state.routes, state.filter) { filterStops(state.stops, state.routes, state.filter) }
    var searchFocused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val foundStops = remember(visibleStops, state.query) { searchStops(visibleStops, state.query) }
    val foundRoutes = remember(state.routes, state.query, state.filter) { searchRoutes(state.routes, state.query, state.filter) }
    fun chooseSearch(kind: String, id: String) {
        focus.clearFocus()
        viewModel.query("")
        viewModel.select(kind, id)
    }
    BackHandler(searchFocused || state.query.isNotEmpty()) { focus.clearFocus(); viewModel.query("") }
    val context = LocalContext.current
    val location = remember(context) { ForegroundLocation(context) }
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var rationale by remember { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf<String?>(null) }
    var locationBusy by remember { mutableStateOf(false) }
    var nearby by remember { mutableStateOf<GeoPoint?>(null) }
    // B20: pilihan style peta gelap ↔ satelit — disimpan antar rotasi.
    var satellite by rememberSaveable { mutableStateOf(false) }
    val mapStyle = if (satellite) SELA_SATELLITE_STYLE else SELA_MAP_STYLE
    // B21: follow mode 2D — titik biru #4285f4, snap bus ≤30m, lepas >60m.
    var followMode by rememberSaveable { mutableStateOf(false) }
    var userPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var snappedBusId by remember { mutableStateOf<String?>(null) }
    // 3D Ultra: mode kamera (§7/§21 spec). 2D default, tidak berubah.
    var mapMode by rememberSaveable { mutableStateOf(MapMode.MAP_2D) }
    val is3d = mapMode.is3d
    // Gedung 3D: paksa tampil saat mode 3D (ekstrusi adalah inti efek 3D).
    val effective3dBuildings = show3dBuildings || is3d
    // 3D Ultra V4.0: Unity sebagai renderer 3D (MapLibre tetap untuk 2D).
    // enter3D meluncurkan UnityMapActivity full-screen; 2D tidak berubah.
    fun enter3D() {
        val cur = viewModel.uiState.value.camera
        val cameraJson =
            """{"lat":${cur.lat},"lon":${cur.lon},"zoom":${cur.zoom},"bearing":${cur.bearing},"pitch":60.0,"mode":"free"}"""
        if (id.sela.feature.unity.UnityAvailability.is3dSupported(context)) {
            val intent = android.content.Intent(
                context,
                id.sela.feature.unity.UnityMapActivity::class.java,
            ).putExtra(
                id.sela.feature.unity.UnityMapActivity.EXTRA_CAMERA_JSON,
                cameraJson,
            )
            context.startActivity(intent)
        } else {
            viewModel.message(
                "Mode 3D tidak tersedia: " +
                    id.sela.feature.unity.UnityAvailability.unsupportedReason(context),
            )
        }
    }
    fun exit3D() {
        val cur = viewModel.uiState.value.camera
        viewModel.flyTo(cur.copy(tilt = 0.0, bearing = 0.0))
        mapMode = MapMode.MAP_2D
    }
    fun startFollow() {
        if (!location.hasPermission()) return
        mapMode = MapMode.MAP_3D_FOLLOW
        scope.launch {
            val point = withTimeoutOrNull(10_000) { location.current() }
            if (point != null) {
                val cur = viewModel.uiState.value.camera
                viewModel.flyTo(
                    MapCamera(
                        point.lat, point.lon,
                        cur.zoom.coerceAtLeast(MAP_3D_FOLLOW_MIN_ZOOM),
                        tilt = MAP_3D_DEFAULT_TILT, bearing = cur.bearing,
                    )
                )
            } else {
                viewModel.message("Lokasi belum tersedia. Periksa izin dan layanan lokasi.")
                mapMode = MapMode.MAP_3D_FREE
            }
        }
    }
    // B21: tombol Lokasi saya = toggle follow (port dari web). Tap pertama
    // memusatkan ke user (perilaku locate lama tetap), tap kedua = follow mati.
    fun toggleFollow() {
        if (followMode) {
            followMode = false
            return
        }
        followMode = true
        snappedBusId = null
        scope.launch {
            val point = withTimeoutOrNull(10_000) { location.current() }
            if (point != null) {
                userPoint = point
                val cur = viewModel.uiState.value.camera
                viewModel.flyTo(
                    MapCamera(
                        point.lat, point.lon,
                        cur.zoom.coerceAtLeast(15.0),
                        tilt = cur.tilt, bearing = cur.bearing,
                    )
                )
            } else {
                followMode = false
                viewModel.message("Lokasi belum tersedia. Periksa izin dan layanan lokasi.")
            }
        }
    }
    // 3D Ultra: Smart Follow — polling GPS tiap 3 dtk; batal otomatis saat mode berubah.
    // B22: hanya polling saat app RESUMED — jangan jalan terus saat background.
    LaunchedEffect(mapMode) {
        if (mapMode == MapMode.MAP_3D_FOLLOW) {
            while (true) {
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    val point = withTimeoutOrNull(10_000) { location.current() }
                    if (point != null) {
                        val cur = viewModel.uiState.value.camera
                        viewModel.flyTo(
                            MapCamera(
                                point.lat, point.lon,
                                cur.zoom.coerceAtLeast(MAP_3D_FOLLOW_MIN_ZOOM),
                                tilt = MAP_3D_DEFAULT_TILT, bearing = cur.bearing,
                            )
                        )
                    }
                }
                delay(3000)
            }
        }
    }
    // B21: follow mode 2D — kamera mengikuti user; snap ke bus terdekat ≤30m,
    // lepas snap bila jarak user↔bus >60m. Drag manual mematikan follow (onManualGesture).
    // B22: polling hanya saat RESUMED.
    LaunchedEffect(followMode) {
        if (!followMode) {
            snappedBusId = null
            userPoint = null
            return@LaunchedEffect
        }
        while (true) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                val point = withTimeoutOrNull(10_000) { location.current() }
                userPoint = point
                if (point != null) {
                    val vehicles = viewModel.uiState.value.vehicles.filter { !it.isStale }
                    val followed = snappedBusId?.let { id -> vehicles.find { it.id == id } }
                    val followedDist = followed?.let {
                        EtaPredictor.haversineM(point.lat, point.lon, it.lat, it.lon)
                    }
                    val target: GeoPoint = if (
                        followed != null && followedDist != null &&
                        followedDist <= FOLLOW_RELEASE_METERS
                    ) {
                        // Masih snap ke bus yang sama.
                        GeoPoint(followed.lat, followed.lon)
                    } else {
                        snappedBusId = null
                        val nearest = vehicles.minByOrNull {
                            EtaPredictor.haversineM(point.lat, point.lon, it.lat, it.lon)
                        }
                        val nearestDist = nearest?.let {
                            EtaPredictor.haversineM(point.lat, point.lon, it.lat, it.lon)
                        }
                        if (nearest != null && nearestDist != null &&
                            nearestDist <= FOLLOW_SNAP_METERS
                        ) {
                            snappedBusId = nearest.id
                            GeoPoint(nearest.lat, nearest.lon)
                        } else {
                            point
                        }
                    }
                    val cur = viewModel.uiState.value.camera
                    viewModel.flyTo(
                        MapCamera(
                            target.lat, target.lon,
                            cur.zoom.coerceAtLeast(15.0),
                            tilt = cur.tilt, bearing = cur.bearing,
                        )
                    )
                }
            }
            delay(3000)
        }
    }
    fun locate() {
        scope.launch {
            locationBusy = true
            try {
                // B23: timeout 10 dtk — jangan spinner selamanya bila lokasi hang.
                val point = withTimeoutOrNull(10_000) { location.current() }
                if (point == null) viewModel.message("Lokasi belum tersedia. Periksa izin dan layanan lokasi.")
                else { nearby = point; viewModel.recenter(point) }
            } finally { locationBusy = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) locate() else viewModel.message("Izin lokasi ditolak. Peta dan pencarian tetap tersedia.")
    }

    Box(Modifier.fillMaxSize()) {
        MapCanvas(
            state,
            mapStyle,
            Modifier.fillMaxSize().semantics { contentDescription = "Peta halte dan bus Jakarta. Gunakan daftar Halte atau Rute untuk akses pembaca layar." },
            viewModel::select,
            viewModel::camera,
            viewModel::message,
            viewModel::bounds,
            show3dBuildings = effective3dBuildings,
            // 3D Ultra: tilt gesture hanya di mode 3D; gesture manual batalkan Follow → Free.
            tiltGesturesEnabled = is3d,
            // B21: drag manual juga mematikan follow mode 2D.
            onManualGesture = {
                if (mapMode == MapMode.MAP_3D_FOLLOW) mapMode = MapMode.MAP_3D_FREE
                if (followMode) followMode = false
            },
            // B21: titik biru #4285f4 posisi user saat follow mode aktif.
            followActive = followMode,
            userLocation = userPoint,
        )

        // 3D Ultra HUD (§9 spec) — kanan bawah, di atas dock melayang.
        Map3dHud(
            mode = mapMode,
            onEnter3d = ::enter3D,
            onExit3d = ::exit3D,
            onToggleFollow = {
                if (mapMode == MapMode.MAP_3D_FOLLOW) mapMode = MapMode.MAP_3D_FREE
                else if (location.hasPermission()) startFollow() else rationale = true
            },
            onRecenter = {
                if (location.hasPermission()) {
                    scope.launch {
                        val point = withTimeoutOrNull(10_000) { location.current() }
                        if (point != null) {
                            val cur = viewModel.uiState.value.camera
                            viewModel.flyTo(
                                MapCamera(point.lat, point.lon, cur.zoom.coerceAtLeast(14.0),
                                    tilt = cur.tilt, bearing = cur.bearing)
                            )
                        } else viewModel.message("Lokasi belum tersedia. Periksa izin dan layanan lokasi.")
                    }
                } else rationale = true
            },
            onResetBearing = {
                val cur = viewModel.uiState.value.camera
                viewModel.flyTo(cur.copy(bearing = 0.0))
            },
            // B20: toggle style peta gelap ↔ satelit.
            onToggleStyle = { satellite = !satellite },
            isSatellite = satellite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 224.dp, end = 12.dp),
        )

        // Bilah atas melayang
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SELA", style = SelaWordmark, color = SelaPalette.volt)
                ConnectionPill(state.connectionStatus, state.vehicles.any { !it.isStale })
            }
            SelaSearchBar(
                value = state.query,
                onChange = viewModel::query,
                onFocus = { searchFocused = it },
            )
            AnimatedVisibility(!searchFocused && state.query.isBlank()) {
                SelaSegmentedControl(
                    listOf(
                        SelaSegmentOption("Semua", state.filter == null, { viewModel.filter(null) }),
                        *ServiceType.entries.map { type ->
                            SelaSegmentOption(
                                type.label(),
                                state.filter == type,
                                { viewModel.filter(type) },
                                dotColor = type.color(),
                            )
                        }.toTypedArray(),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AnimatedVisibility(searchFocused || state.query.isNotBlank()) {
                Surface(
                    shape = SelaTokens.card,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = SelaTokens.floatElevation,
                ) {
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 340.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        item {
                            Text(
                                if (state.query.isBlank()) "Pilih halte atau rute" else "Hasil pencarian",
                                Modifier.padding(horizontal = 20.dp, vertical = 8.dp).semantics { heading() },
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        if (foundStops.isEmpty() && foundRoutes.isEmpty()) {
                            item { SelaEmptyState("Tidak ditemukan. Coba nama lain atau pilih filter Semua.") }
                        }
                        // Fitur #85: easter egg — ketik "gacor" di pencarian
                        if (state.query.trim().lowercase() == "gacor") {
                            item {
                                val gacorShown = remember { mutableStateOf(false) }
                                if (!gacorShown.value) {
                                    gacorShown.value = true
                                    SelaNoticeBanner(
                                        text = "Mode Gacor aktif 😄 — semua ETA dibulatkan ke bawah biar berasa cepat.",
                                        icon = Icons.Default.Celebration,
                                        tone = SelaNoticeTone.LIVE,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                }
                            }
                        }
                        val routes = if (state.query.isBlank()) foundRoutes.take(3) else foundRoutes
                        val stops = if (state.query.isBlank()) foundStops.take(5) else foundStops
                        items(routes, key = { "r:${it.id}" }) { r ->
                            SelaListRow(
                                title = r.longName.ifBlank { "Rute ${r.shortName}" },
                                subtitle = "Rute ${r.shortName} · ${r.displayServiceType.label()}",
                                leadingIcon = Icons.Default.Route,
                                leadingTint = r.displayServiceType.color(),
                                onClick = { chooseSearch("route", r.id) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                trailing = { SelaRouteBadge(r.shortName, r.displayServiceType.color()) },
                            )
                        }
                        items(stops, key = { "s:${it.id}" }) { s ->
                            SelaListRow(
                                title = s.name,
                                subtitle = "Halte",
                                leadingIcon = Icons.Default.DirectionsBus,
                                onClick = { chooseSearch("stop", s.id) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
            (state.message ?: state.error)?.let {
                SelaErrorState(
                    message = it,
                    onRetry = { viewModel.message(null); viewModel.refresh() },
                    // Hindari tumpuk dengan tombol aksi melayang di kanan
                    modifier = Modifier.padding(end = 76.dp),
                )
            }
        }

        if (state.isLoading || locationBusy) {
            Box(Modifier.align(Alignment.Center)) {
                SelaLoadingState()
            }
        }

        // Dock bawah melayang
        if (!searchFocused && state.query.isBlank()) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ServiceLegend()
                MapDock(
                    // B21: tombol Lokasi saya = toggle follow (port dari web).
                    onLocate = { if (location.hasPermission()) toggleFollow() else rationale = true },
                    onRoutes = { panel = "routes" },
                    onStops = { panel = "stops" },
                    onFavorites = { panel = "favorites" },
                    onVehicles = { panel = "vehicles" },
                )
            }
        }
    }

    if (rationale) PermissionRationale(
        { rationale = false; permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
        { rationale = false },
    )

    if (panel != null) {
        SelaBottomSheet(onDismiss = { panel = null }) {
            when (panel) {
                "routes" -> RouteSelectorSheet(searchRoutes(state.routes, state.query, state.filter)) {
                    panel = null; viewModel.select("route", it)
                }
                "favorites" -> FavoritesList(state.data, { kind, id -> panel = null; viewModel.select(kind, id) }, viewModel::favorite)
                "vehicles" -> VehicleListSheet(state) { panel = null; viewModel.select("vehicle", it) }
                else -> StopListSheet(visibleStops, nearby) { panel = null; viewModel.select("stop", it) }
            }
        }
    }

    if (state.selectedId != null) {
        SelaBottomSheet(onDismiss = viewModel::dismiss) {
            when (state.selectedKind) {
                "stop" -> state.stops.find { it.id == state.selectedId }?.let { stop ->
                    val arrivals = remember(stop.id, state.data.vehicles) { EtaPredictor.estimate(stop, state.data.vehicles) }
                    StopDetailSheet(
                        stop = stop,
                        arrivals = arrivals,
                        favorite = Favorite(stop.id, "stop") in state.data.favorites,
                        onFavorite = { viewModel.favorite(stop.id, "stop") },
                        onShare = { estimate ->
                            val routeName = state.routes.find { it.id == estimate.routeId }?.shortName
                                ?: estimate.routeId
                            shareEta(context, stop.name, routeName, estimate.etaMinutes)
                        },
                        extras = stopExtras,
                    )
                } ?: SelaEmptyState("Halte belum tersedia.")
                "route" -> state.routes.find { it.id == state.selectedId }?.let { route ->
                    RouteDetailSheet(
                        route,
                        Favorite(route.id, "route") in state.data.favorites,
                        state.shapeLoading,
                        state.shape.isNotEmpty(),
                        { viewModel.favorite(route.id, "route") },
                        viewModel::dismiss,
                    )
                } ?: SelaEmptyState("Rute belum tersedia.")
                "vehicle" -> state.vehicles.find { it.id == state.selectedId }?.let { vehicle ->
                    VehicleDetailSheet(vehicle, state.routes.find { it.id == vehicle.routeId }) { viewModel.select("route", it) }
                } ?: SelaEmptyState("Posisi bus sudah tidak tersedia.")
            }
        }
    }
}
