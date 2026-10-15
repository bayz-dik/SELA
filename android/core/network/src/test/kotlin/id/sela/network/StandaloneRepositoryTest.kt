package id.sela.network

import android.content.Context
import id.sela.database.FavoriteEntity
import id.sela.database.TransitDao
import id.sela.model.*
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Test StandaloneTransitRepository: data lokal + MQTT langsung, tanpa backend.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StandaloneRepositoryTest {
    private val context = mockk<Context>(relaxed = true) {
        every { packageName } returns "id.sela"
    }
    private val localGtfs = mockk<LocalGtfsRepository>()
    private val mqtt = mockk<MqttVehicleClient>()
    private val dao = mockk<TransitDao>(relaxed = true) {
        every { favorites() } returns flow {
            emit(listOf(FavoriteEntity("s1", "stop")))
            awaitCancellation()
        }
    }
    private val preferences = mockk<TransitPreferences>(relaxed = true) {
        every { filter } returns flowOf(null)
        every { offlineMode } returns flowOf(false)
    }

    private val demoRoutes = listOf(Route("1", "1", "Blok M - Kota", ServiceType.BUS, "#D62126"))
    private val demoStops = listOf(Stop("s1", "Monas", "MNS", -6.2, 106.8, listOf("1")))

    private fun repo(
        scope: CoroutineScope,
        vehicles: Flow<MqttVehicle> = flowOf(
            MqttVehicle("B1", "1", -6.21, 106.81, 90f, null, "2026-10-14T06:00:00Z", "t1"),
        ),
    ): StandaloneTransitRepository {
        coEvery { localGtfs.routes() } returns demoRoutes
        coEvery { localGtfs.stops() } returns demoStops
        every { mqtt.vehicles() } returns vehicles
        return StandaloneTransitRepository(context, localGtfs, mqtt, dao, preferences, scope)
    }

    @Test fun initLoadsLocalDataAndConnectsMqtt() = runTest {
        val r = repo(backgroundScope); runCurrent()
        val data = r.data.value
        assertFalse(data.isLoading)
        assertEquals("1", data.routes.single().id)
        assertEquals("s1", data.stops.single().id)
        assertEquals("B1", data.vehicles.single().id)
        assertEquals(ConnectionStatus.LIVE, data.connectionStatus)
        assertEquals(setOf(Favorite("s1", "stop")), data.favorites)
        assertNotNull(data.lastSyncedAt)
        coVerify { preferences.updateWidgetSnapshot(any(), any(), any()) }
    }

    @Test fun offlineModeSkipsMqttEntirely() = runTest {
        every { preferences.offlineMode } returns flowOf(true)
        val r = repo(backgroundScope); runCurrent()
        verify(exactly = 0) { mqtt.vehicles() }
        assertEquals(ConnectionStatus.OFFLINE, r.data.value.connectionStatus)
        assertTrue(r.data.value.vehicles.isEmpty())
        assertNull(r.data.value.error)
    }

    @Test fun mqttFailureReportsReconnecting() = runTest {
        val r = repo(backgroundScope, vehicles = flow { throw IOException("putus") })
        runCurrent()
        assertEquals(ConnectionStatus.RECONNECTING, r.data.value.connectionStatus)
        assertNotNull(r.data.value.error)
    }

    @Test fun localDataFailureReportsError() = runTest {
        // vehicles kosong agar MQTT tidak menimpa error GTFS dengan error=null
        val r = repo(backgroundScope, vehicles = emptyFlow())
        // Override stub SETELAH repo() — mockk memakai stub yang terakhir dideklarasikan.
        coEvery { localGtfs.routes() } throws IOException("assets rusak")
        runCurrent()
        assertFalse(r.data.value.isLoading)
        assertNotNull(r.data.value.error)
    }

    @Test fun toggleFavoriteDelegatesToDao() = runTest {
        val r = repo(backgroundScope); runCurrent()
        r.toggleFavorite("s1", "stop")
        coVerify { dao.toggle("s1", "stop") }
    }

    @Test fun recordTripInsertsAndPrunesOldEntries() = runTest {
        val r = repo(backgroundScope); runCurrent()
        r.recordTrip("1", "1 — Blok M - Kota", "s1", "Monas")
        coVerify {
            dao.insertTrip(match {
                it.routeId == "1" && it.routeName == "1 — Blok M - Kota" &&
                    it.stopId == "s1" && it.stopName == "Monas" && it.timestamp > 0
            })
        }
        coVerify { dao.pruneHistory(any()) }
    }

    @Test fun tripHistoryFlowMapsEntitiesToModels() = runTest {
        every { dao.tripHistoryFlow() } returns flowOf(
            listOf(
                id.sela.database.TripHistoryEntity(7, "1", "Koridor 1", "s1", "Monas", 12345L),
            ),
        )
        val r = repo(backgroundScope); runCurrent()
        val history = r.tripHistoryFlow().first()
        assertEquals(1, history.size)
        assertEquals("Koridor 1", history.single().routeName)
        assertEquals(12345L, history.single().timestamp)
    }

    @Test fun refreshReloadsLocalData() = runTest {
        val r = repo(backgroundScope); runCurrent()
        coEvery { localGtfs.clearCache() } just Runs
        coEvery { localGtfs.routes() } returns listOf(
            Route("2", "2", "Pulo Gadung - Monas", ServiceType.BUS, null),
        )
        r.refresh()
        coVerify { localGtfs.clearCache() }
        assertEquals("2", r.data.value.routes.single().id)
        assertNull(r.data.value.error)
    }

    @Test fun routeShapeIsEmptyWithoutInvention() = runTest {
        val r = repo(backgroundScope); runCurrent()
        assertTrue(r.routeShape("1").isEmpty())
    }

    @Test fun setFilterDelegatesToPreferences() = runTest {
        val r = repo(backgroundScope); runCurrent()
        r.setFilter(ServiceType.BRT)
        coVerify { preferences.setFilter(ServiceType.BRT) }
    }
}
