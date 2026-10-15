package id.sela.feature.map

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import org.junit.After
import id.sela.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
    private val models = mutableListOf<MapViewModel>()
    private fun model(repo: Repo, saved: SavedStateHandle): MapViewModel =
        MapViewModel(TransitUseCase(repo), saved).also { models += it }
    @After fun tearDown() {
        models.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }
    private class Repo : TransitRepository {
        override val data = MutableStateFlow(TransitData(stops = listOf(Stop("s", "Halte", lat = -6.2, lon = 106.8))))
        override val serviceFilter = MutableStateFlow<ServiceType?>(null)
        var refreshes = 0
        override suspend fun refresh() { refreshes++ }
        override suspend fun setFilter(filter: ServiceType?) { serviceFilter.value = filter }
        override suspend fun toggleFavorite(id: String, kind: String) { data.value = data.value.copy(favorites = setOf(Favorite(id, kind))) }
        override suspend fun routeShape(id: String) = emptyList<GeoPoint>()
        override suspend fun runRealtime() = Unit
    }
    @Test fun selectionAndCameraSavedAcrossRecreation() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repo = Repo(); val saved = SavedStateHandle()
            val vm = model(repo, saved)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            vm.select("route", "1"); vm.camera(MapCamera(-6.3, 106.9, 14.0)); runCurrent()
            val restored = model(repo, saved)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { restored.uiState.collect() }; runCurrent()
            assertEquals("1", restored.uiState.value.selectedRouteId)
            assertEquals(MapCamera(-6.3, 106.9, 14.0), restored.uiState.value.camera)
            assertTrue(restored.uiState.value.shape.isEmpty())
        } finally { backgroundScope.cancel(); runCurrent() }
    }
    @Test fun selectingStopRecentersAndDismissClearsSavedSelection() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val saved = SavedStateHandle(); val vm = model(Repo(), saved)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            vm.select("stop", "s"); runCurrent()
            assertEquals(-6.2, vm.uiState.value.camera.lat, 0.0)
            assertEquals("s", vm.uiState.value.selectedStopId)
            vm.dismiss(); assertNull(vm.uiState.value.selectedId); assertFalse(saved.contains("id"))
        } finally { backgroundScope.cancel(); runCurrent() }
    }
    @Test fun filterQueryAndBoundsAreExposed() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val vm = model(Repo(), SavedStateHandle())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
            vm.filter(ServiceType.JAKLINGKO); vm.query("Monas"); vm.bounds(VisibleBounds(-7.0, 106.0, -6.0, 107.0)); runCurrent()
            assertEquals(ServiceType.JAKLINGKO, vm.uiState.value.serviceFilter)
            assertEquals("Monas", vm.uiState.value.query)
            assertNotNull(vm.uiState.value.visibleBounds)
        } finally { backgroundScope.cancel(); runCurrent() }
    }
}
