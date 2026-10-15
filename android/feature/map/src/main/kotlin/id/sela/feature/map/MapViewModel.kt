package id.sela.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.sela.model.*
import id.sela.model.TransitUseCase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class MapCamera(
    val lat: Double = -6.1754,
    val lon: Double = 106.8272,
    val zoom: Double = 12.0,
    /** Pitch/tilt derajat (0 = tegak 2D, maks 60 = batas MapLibre). */
    val tilt: Double = 0.0,
    /** Bearing derajat (0 = utara). */
    val bearing: Double = 0.0,
)
data class MapUiState(
    val data: TransitData = TransitData(), val filter: ServiceType? = null,
    val query: String = "", val selectedKind: String? = null, val selectedId: String? = null,
    val shape: List<GeoPoint> = emptyList(), val shapeLoading: Boolean = false,
    val message: String? = null, val camera: MapCamera = MapCamera(), val cameraRequest: Long = 0,
    val visibleBounds: VisibleBounds? = null
) {
    val isLoading get() = data.isLoading
    val selectedRouteId get() = selectedId.takeIf { selectedKind == "route" }
    val selectedStopId get() = selectedId.takeIf { selectedKind == "stop" }
    val selectedVehicleId get() = selectedId.takeIf { selectedKind == "vehicle" }
    val serviceFilter get() = filter
    val stops get() = data.stops
    val routes get() = data.routes
    val vehicles get() = data.vehicles
    val connectionStatus get() = data.connectionStatus
    val error get() = data.error
    val lastSyncedAt get() = data.lastSyncedAt
}
data class VisibleBounds(val south: Double, val west: Double, val north: Double, val east: Double)
@HiltViewModel
class MapViewModel @Inject constructor(private val useCase: TransitUseCase, private val saved: SavedStateHandle) : ViewModel() {
    private val local = MutableStateFlow(MapUiState(query = saved["query"] ?: "", selectedKind = saved["kind"],
        selectedId = saved["id"], camera = MapCamera(saved["lat"] ?: -6.1754, saved["lon"] ?: 106.8272, saved["zoom"] ?: 12.0,
            saved["tilt"] ?: 0.0, saved["bearing"] ?: 0.0)))
    val uiState: StateFlow<MapUiState> = combine(useCase.data, useCase.serviceFilter, local) { data, filter, state -> state.copy(data = data, filter = filter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)
    private var shapeJob: Job? = null
    init {
        refresh()
        if (local.value.selectedKind == "route") local.value.selectedId?.let(::loadShape)
    }
    fun refresh() { viewModelScope.launch { safely { useCase.refresh() } } }
    suspend fun runRealtime() { safely { useCase.runRealtime() } }
    fun query(value: String) { saved["query"] = value; local.update { it.copy(query = value) } }
    fun filter(value: ServiceType?) { viewModelScope.launch { safely { useCase.setFilter(value) } } }
    fun favorite(id: String, kind: String) { viewModelScope.launch { safely { useCase.toggleFavorite(id, kind) } } }
    fun select(kind: String, id: String) {
        saved["kind"] = kind; saved["id"] = id
        shapeJob?.cancel()
        local.update { it.copy(selectedKind = kind, selectedId = id, shape = emptyList(), shapeLoading = false, message = null) }
        if (kind == "route") loadShape(id)
        val data = useCase.data.value
        val point = when (kind) {
            "stop" -> data.stops.find { it.id == id }?.let { GeoPoint(it.lat, it.lon) }
            "vehicle" -> data.vehicles.find { it.id == id }?.let { GeoPoint(it.lat, it.lon) }
            else -> null
        }
        point?.let { recenter(it) }
        // Catat ke riwayat perjalanan (fitur #2)
        viewModelScope.launch {
            safely {
                when (kind) {
                    "route" -> data.routes.find { it.id == id }?.let {
                        useCase.recordTrip(it.id, "${it.shortName} — ${it.longName}")
                    }
                    "stop" -> data.stops.find { it.id == id }?.let {
                        useCase.recordTrip("", "", it.id, it.name)
                    }
                }
            }
        }
    }
    fun dismiss() {
        shapeJob?.cancel(); saved.remove<String>("kind"); saved.remove<String>("id")
        local.update { it.copy(selectedKind = null, selectedId = null, shape = emptyList(), shapeLoading = false) }
    }
    private fun loadShape(id: String) {
        shapeJob = viewModelScope.launch {
            local.update { it.copy(shapeLoading = true) }
            try {
                val shape = useCase.routeShape(id)
                if (local.value.selectedId == id && local.value.selectedKind == "route") {
                    local.update { it.copy(shape = shape, message = if (shape.isEmpty()) "Jalur rute belum tersedia." else null) }
                    shape.firstOrNull()?.let { recenter(it, 12.0) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message("Jalur rute tidak dapat dimuat. Coba lagi.") }
            finally { local.update { it.copy(shapeLoading = false) } }
        }
    }
    fun bounds(value: VisibleBounds) { local.update { it.copy(visibleBounds = value) } }
    fun camera(value: MapCamera) {
        saved["lat"] = value.lat; saved["lon"] = value.lon; saved["zoom"] = value.zoom
        saved["tilt"] = value.tilt; saved["bearing"] = value.bearing
        local.update { it.copy(camera = value) }
    }
    fun recenter(point: GeoPoint, zoom: Double = 15.0) {
        flyTo(MapCamera(point.lat, point.lon, zoom, tilt = local.value.camera.tilt, bearing = local.value.camera.bearing))
    }
    /** Pindahkan kamera dengan animasi (termasuk tilt/bearing) — dipakai mode 3D. */
    fun flyTo(camera: MapCamera) {
        camera(camera)
        local.update { it.copy(cameraRequest = it.cameraRequest + 1) }
    }
    fun message(value: String?) { local.update { it.copy(message = value) } }
    private suspend fun safely(block: suspend () -> Unit) {
        try { block() } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { message("Data tidak dapat dimuat. Periksa koneksi lalu coba lagi.") }
    }
}
