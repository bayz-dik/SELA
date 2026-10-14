package id.sela.model

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class VehicleReconcilerTest {
    private val now = Instant.parse("2026-01-01T00:02:00Z")
    private fun vehicle(at: String = "2026-01-01T00:01:30Z", route: String = "A", stale: Boolean = false) =
        Vehicle("v", route, -6.2, 106.8, observedAt = at, isStale = stale)
    @Test fun olderUpdatesCannotMoveVehicleBackwards() {
        val store = VehicleReconciler(); store.update(vehicle()); store.update(vehicle("2026-01-01T00:01:00Z", "B"))
        assertEquals("A", store.values(now).single().routeId)
    }
    @Test fun equalTimestampStaleUpdateIsAcceptedWithoutMovingPosition() {
        val store = VehicleReconciler(); store.update(vehicle()); store.update(vehicle(stale = true).copy(lat = 0.0))
        assertTrue(store.values(now).single().isStale); assertEquals(-6.2, store.values(now).single().lat, 0.0)
    }
    @Test fun staleAtExactlyNinetySeconds() {
        val store = VehicleReconciler(); store.update(vehicle("2026-01-01T00:00:30Z"))
        assertFalse(store.values(now.minusMillis(1)).single().isStale); assertTrue(store.values(now).single().isStale)
    }
    @Test fun snapshotCannotResurrectConcurrentRemoval() {
        val store = VehicleReconciler(); store.update(vehicle()); val barrier = store.snapshotBarrier()
        store.remove("v", "A"); store.reconcile(listOf(vehicle()), barrier)
        assertTrue(store.values(now).isEmpty())
    }
    @Test fun snapshotKeepsConcurrentPositionAndRemovesAbsentOldRows() {
        val store = VehicleReconciler(); store.update(vehicle()); val barrier = store.snapshotBarrier()
        store.update(vehicle("2026-01-01T00:01:40Z")); store.reconcile(emptyList(), barrier)
        assertEquals(1, store.values(now).size)
        store.reconcile(emptyList(), store.snapshotBarrier()); assertTrue(store.values(now).isEmpty())
    }
    @Test fun previousRouteRemovalCannotEraseNewRoute() {
        val store = VehicleReconciler(); store.update(vehicle(route = "B")); store.remove("v", "A")
        assertEquals("B", store.values(now).single().routeId)
    }
    @Test fun malformedPositionAndTimestampAreIgnored() {
        val store = VehicleReconciler(); store.update(vehicle().copy(lat = Double.NaN)); store.update(vehicle("garbage"))
        assertTrue(store.values(now).isEmpty())
    }
    @Test fun snapshotDoesNotReplaceNewerTimestampEvenWithOlderRevision() {
        val store = VehicleReconciler(); store.update(vehicle()); val barrier = store.snapshotBarrier()
        store.reconcile(listOf(vehicle("2026-01-01T00:01:00Z")), barrier)
        assertEquals(vehicle().observedAt, store.values(now).single().observedAt)
    }
    @Test fun defaultsAreOfflineAndEmpty() { val data = TransitData(); assertEquals(ConnectionStatus.OFFLINE, data.connectionStatus); assertTrue(data.stops.isEmpty()); assertFalse(data.isLoading) }
    @Test fun useCaseDelegatesEveryMember() = runBlocking {
        val calls = mutableListOf<String>()
        val repository = object : TransitRepository {
            override val data = MutableStateFlow(TransitData())
            override val serviceFilter = flowOf<ServiceType?>(null)
            override suspend fun refresh() { calls += "refresh" }
            override suspend fun setFilter(filter: ServiceType?) { calls += "filter:$filter" }
            override suspend fun toggleFavorite(id: String, kind: String) { calls += "$kind:$id" }
            override suspend fun routeShape(id: String): List<GeoPoint> { calls += "shape:$id"; return listOf(GeoPoint(1.0, 2.0)) }
            override suspend fun runRealtime() { calls += "realtime" }
        }
        val useCase = TransitUseCase(repository)
        assertSame(repository.data, useCase.data); assertSame(repository.serviceFilter, useCase.serviceFilter)
        useCase.refresh(); useCase.setFilter(ServiceType.BRT); useCase.toggleFavorite("A", "route")
        assertEquals(listOf(GeoPoint(1.0, 2.0)), useCase.routeShape("A")); useCase.runRealtime()
        assertEquals(listOf("refresh", "filter:BRT", "route:A", "shape:A", "realtime"), calls)
    }
}
