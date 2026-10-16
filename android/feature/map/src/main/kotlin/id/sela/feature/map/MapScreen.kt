package id.sela.feature.map

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** B21: follow mode 2D (port dari web) — snap ke bus terdekat dalam radius ini. */
private const val FOLLOW_SNAP_METERS = 30.0
/** B21: follow mode 2D — lepas snap bus bila jarak user↔bus melebihi ini. */
private const val FOLLOW_RELEASE_METERS = 60.0

/** Wordmark "SELA" versi pastel — kapital rapat, tinta gelap. */
private val PastelWordmark = TextStyle(
    fontWeight = FontWeight.ExtraBold,
    fontSize = 20.sp,
    lineHeight = 24.sp,
    letterSpacing = 2.sp,
)

// ---------------------------------------------------------------------------
// Bilah atas
// ---------------------------------------------------------------------------

/** Pil status koneksi — kaca melayang, titik denyut pastel saat live. */
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
        shape = SelaPastelShapes.pill,
        color = Color.White.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
        modifier = Modifier
            .pastelShadow(elevation = SelaPastelShadows.soft, shape = SelaPastelShapes.pill)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PastelPulseDot(
                active = active,
                color = if (status == ConnectionStatus.ERROR) SelaV7.danger else SelaV7.limeDeep,
            )
            Text(text, style = MaterialTheme.typography.labelMedium, color = SelaV7.ink)
        }
    }
}

@Composable
private fun SelaMapStatusStrip(state: MapUiState) {
    val offline = state.connectionStatus == ConnectionStatus.OFFLINE
    val error = state.connectionStatus == ConnectionStatus.ERROR
    val title = when {
        offline -> "Rute lokal tetap tersedia"
        error -> "Data live belum bisa dimuat"
        state.connectionStatus == ConnectionStatus.LIVE -> "Terhubung, menunggu posisi bus"
        else -> "Sedang menyambungkan bus live"
    }
    Surface(
        color = Color.White.copy(alpha = 0.95f),
        shape = SelaPastelShapes.md,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.90f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SelaMascot(
                expression = when {
                    offline -> SelaMascotExpression.Offline
                    error -> SelaMascotExpression.Error
                    state.connectionStatus == ConnectionStatus.LIVE -> SelaMascotExpression.WaitingBus
                    else -> SelaMascotExpression.Searching
                },
                diameter = 40.dp,
                animate = !offline,
                bodyColor = if (error) SelaV7.warningSoft else SelaV7.periSoft,
            )
            Text(title, style = MaterialTheme.typography.labelMedium, color = SelaV7.ink)
        }
    }
}

@Composable
fun ServiceLegend() {
    Surface(
        shape = SelaPastelShapes.pill,
        color = Color.White.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
        modifier = Modifier.pastelShadow(
            elevation = SelaPastelShadows.soft,
            shape = SelaPastelShapes.pill,
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ServiceType.entries.forEach {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = it.pastelColor(), modifier = Modifier.size(10.dp)) {}
                    Text(it.label(), style = MaterialTheme.typography.labelMedium, color = SelaV7.inkSoft)
                }
            }
        }
    }
}

/** Chip filter kaca — versi GlassPill dengan titik warna layanan. */
@Composable
private fun FilterGlassPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.pastelShadow(
            elevation = SelaPastelShadows.soft,
            shape = SelaPastelShapes.pill,
        ),
        shape = SelaPastelShapes.pill,
        color = if (selected) Color.White.copy(alpha = 0.94f) else Color.White.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dotColor != null) {
                Surface(shape = CircleShape, color = dotColor, modifier = Modifier.size(8.dp)) {}
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) SelaV7.ink else SelaV7.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = label },
    ) {
        Icon(icon, contentDescription = null, tint = SelaV7.ink, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SelaV7.inkSoft)
    }
}

/** Dock kaca: Rute · Halte · Favorit · Bus + tombol lingkaran Lokasi saya. */
@Composable
private fun MapDock(
    onLocate: () -> Unit,
    onRoutes: () -> Unit,
    onStops: () -> Unit,
    onFavorites: () -> Unit,
    onVehicles: () -> Unit,
    followActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        GlassPillNavBar(modifier = Modifier.weight(1f)) {
            DockButton(Icons.Default.Route, "Rute", onRoutes)
            DockButton(Icons.Default.LocationOn, "Halte", onStops)
            DockButton(Icons.Default.Favorite, "Favorit", onFavorites)
            DockButton(Icons.Default.DirectionsBus, "Bus", onVehicles)
        }
        Surface(
            onClick = onLocate,
            shape = CircleShape,
            color = if (followActive) SelaV7.lime.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.9f),
            contentColor = if (followActive) SelaV7.limeDeep else SelaV7.periDeep,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f)),
            modifier = Modifier
                .size(64.dp)
                .pastelShadow(elevation = SelaPastelShadows.card, shape = CircleShape)
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
    PastelSheetHeader(title = "Pilih rute", subtitle = "${routes.size} rute tersedia")
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        if (routes.isEmpty()) item { PastelEmptyState("Rute tidak ditemukan.") }
        items(routes, key = { it.id }) { route ->
            val tint = route.displayServiceType.pastelColor()
            val deep = route.displayServiceType.pastelDeepColor()
            PastelListRow(
                title = route.longName.ifBlank { "Rute ${route.shortName}" },
                subtitle = "Rute ${route.shortName} · ${route.displayServiceType.label()}",
                leadingIcon = Icons.Default.Route,
                leadingTint = tint,
                leadingDeep = deep,
                onClick = { onSelect(route.id) },
                modifier = Modifier.padding(horizontal = 12.dp),
                trailing = { PastelRouteBadge(route.shortName, tint = tint, deep = deep) },
            )
        }
    }
}

@Composable
private fun ColumnScope.VehicleListSheet(state: MapUiState, onSelect: (String) -> Unit) {
    val live = state.vehicles.filter { !it.isStale }
    PastelSheetHeader(
        title = "Bus terpantau",
        subtitle = if (live.isEmpty()) "Belum ada posisi terkini" else "${live.size} bus live",
    )
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        if (live.isEmpty()) item { PastelEmptyState("Belum ada posisi bus terkini. Halte dan rute tetap tersedia.") }
        items(live, key = { it.id }) { bus ->
            val route = state.routes.find { r -> r.id == bus.routeId }
            val type = route?.displayServiceType
            SelaLiveBusCard(
                vehicleLabel = "Bus ${bus.id}",
                routeLabel = route?.let { "${it.shortName} · ${it.longName}" } ?: "Rute ${bus.routeId}",
                tint = type?.pastelColor() ?: SelaV7.periSoft,
                deep = type?.pastelDeepColor() ?: SelaV7.periDeep,
                speedKmh = bus.speedKmh,
                onClick = { onSelect(bus.id) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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
    PastelSheetHeader(
        title = if (nearby == null) "Halte" else "Halte terdekat",
        subtitle = "${stops.size} halte",
    )
    LazyColumn(
        Modifier
            .fillMaxWidth()
            .weight(1f, fill = false),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        if (stops.isEmpty()) item { PastelEmptyState("Belum ada data halte.") }
        val ordered = nearby?.let { p ->
            stops.sortedBy { (it.lat - p.lat) * (it.lat - p.lat) + (it.lon - p.lon) * (it.lon - p.lon) }
        } ?: stops
        items(ordered, key = { it.id }) { stop ->
            PastelListRow(
                title = stop.name,
                subtitle = if (stop.routeIds.isNotEmpty()) "${stop.routeIds.size} rute" else "Halte",
                leadingIcon = Icons.Default.DirectionsBus,
                leadingTint = SelaV7.lime,
                leadingDeep = SelaV7.limeDeep,
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
        title = { Text("Gunakan lokasi?", color = SelaV7.ink) },
        text = {
            Text(
                "Lokasi hanya dipakai saat mencari posisi Anda. Tanpa izin, peta dan pencarian tetap bisa dipakai.",
                color = SelaV7.inkSoft,
            )
        },
        confirmButton = { SelaButton(text = "Lanjut", onClick = onAllow) },
        dismissButton = { PastelTextButton(text = "Tidak sekarang", onClick = onDismiss) },
        shape = SelaPastelShapes.xl,
        containerColor = Color.White,
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
    // B20: pilihan style peta terang ↔ satelit — disimpan antar rotasi.
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
    // 3D: MapLibre 3D (gedung ekstrusi + kamera tilt). Unity dihapus total.
    fun enter3D() {
        val cur = viewModel.uiState.value.camera
        viewModel.flyTo(cur.copy(tilt = MAP_3D_DEFAULT_TILT))
        mapMode = MapMode.MAP_3D_FREE
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
            // B20: toggle style peta terang ↔ satelit.
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
                Text("SELA", style = PastelWordmark, color = SelaV7.ink)
                ConnectionPill(state.connectionStatus, state.vehicles.any { !it.isStale })
            }
            PastelSearchBar(
                value = state.query,
                onChange = viewModel::query,
                onFocus = { searchFocused = it },
            )
            AnimatedVisibility(
                !searchFocused && state.query.isBlank() &&
                    (state.connectionStatus != ConnectionStatus.LIVE || state.vehicles.none { !it.isStale })
            ) {
                SelaMapStatusStrip(state)
            }
            AnimatedVisibility(!searchFocused && state.query.isBlank()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterGlassPill(
                        text = "Semua",
                        selected = state.filter == null,
                        onClick = { viewModel.filter(null) },
                    )
                    ServiceType.entries.forEach { type ->
                        FilterGlassPill(
                            text = type.label(),
                            selected = state.filter == type,
                            onClick = { viewModel.filter(type) },
                            dotColor = type.pastelColor(),
                        )
                    }
                }
            }
            AnimatedVisibility(searchFocused || state.query.isNotBlank()) {
                Surface(
                    shape = SelaPastelShapes.lg,
                    color = Color.White.copy(alpha = 0.97f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.8f)),
                    modifier = Modifier.pastelShadow(
                        elevation = SelaPastelShadows.card,
                        shape = SelaPastelShapes.lg,
                    ),
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
                                color = SelaV7.ink,
                            )
                        }
                        if (foundStops.isEmpty() && foundRoutes.isEmpty()) {
                            item { PastelEmptyState("Tidak ditemukan. Coba nama lain atau pilih filter Semua.") }
                        }
                        // Fitur #85: easter egg — ketik "gacor" di pencarian
                        if (state.query.trim().lowercase() == "gacor") {
                            item {
                                val gacorShown = remember { mutableStateOf(false) }
                                if (!gacorShown.value) {
                                    gacorShown.value = true
                                    PastelNoticeBanner(
                                        text = "Mode Gacor aktif — ETA dibulatkan ke bawah.",
                                        icon = Icons.Default.Celebration,
                                        tone = PastelNoticeTone.LIVE,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                    )
                                }
                            }
                        }
                        val routes = if (state.query.isBlank()) foundRoutes.take(3) else foundRoutes
                        val stops = if (state.query.isBlank()) foundStops.take(5) else foundStops
                        items(routes, key = { "r:${it.id}" }) { r ->
                            val tint = r.displayServiceType.pastelColor()
                            val deep = r.displayServiceType.pastelDeepColor()
                            PastelListRow(
                                title = r.longName.ifBlank { "Rute ${r.shortName}" },
                                subtitle = "Rute ${r.shortName} · ${r.displayServiceType.label()}",
                                leadingIcon = Icons.Default.Route,
                                leadingTint = tint,
                                leadingDeep = deep,
                                onClick = { chooseSearch("route", r.id) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                                trailing = { PastelRouteBadge(r.shortName, tint = tint, deep = deep) },
                            )
                        }
                        items(stops, key = { "s:${it.id}" }) { s ->
                            PastelListRow(
                                title = s.name,
                                subtitle = "Halte",
                                leadingIcon = Icons.Default.DirectionsBus,
                                leadingTint = SelaV7.lime,
                                leadingDeep = SelaV7.limeDeep,
                                onClick = { chooseSearch("stop", s.id) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
            (state.message ?: state.error)?.let {
                PastelErrorState(
                    message = it,
                    onRetry = { viewModel.message(null); viewModel.refresh() },
                    // Hindari tumpuk dengan tombol aksi melayang di kanan
                    modifier = Modifier.padding(end = 76.dp),
                )
            }
        }

        if (state.isLoading || locationBusy) {
            Box(Modifier.align(Alignment.Center)) {
                PastelLoadingState()
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
                MapDock(
                    // B21: tombol Lokasi saya = toggle follow (port dari web).
                    onLocate = { if (location.hasPermission()) toggleFollow() else rationale = true },
                    onRoutes = { panel = "routes" },
                    onStops = { panel = "stops" },
                    onFavorites = { panel = "favorites" },
                    onVehicles = { panel = "vehicles" },
                    followActive = followMode,
                )
            }
        }
    }

    if (rationale) PermissionRationale(
        { rationale = false; permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
        { rationale = false },
    )

    if (panel != null) {
        PastelBottomSheet(onDismiss = { panel = null }) {
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
        PastelBottomSheet(onDismiss = viewModel::dismiss) {
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
                } ?: PastelEmptyState("Halte belum tersedia.")
                "route" -> state.routes.find { it.id == state.selectedId }?.let { route ->
                    RouteDetailSheet(
                        route,
                        Favorite(route.id, "route") in state.data.favorites,
                        state.shapeLoading,
                        state.shape.isNotEmpty(),
                        { viewModel.favorite(route.id, "route") },
                        viewModel::dismiss,
                    )
                } ?: PastelEmptyState("Rute belum tersedia.")
                "vehicle" -> state.vehicles.find { it.id == state.selectedId }?.let { vehicle ->
                    VehicleDetailSheet(vehicle, state.routes.find { it.id == vehicle.routeId }) { viewModel.select("route", it) }
                } ?: PastelEmptyState("Posisi bus sudah tidak tersedia.")
            }
        }
    }
}
