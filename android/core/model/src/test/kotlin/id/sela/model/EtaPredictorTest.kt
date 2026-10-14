package id.sela.model

import org.junit.Assert.*
import org.junit.Test

class EtaPredictorTest {
    // Halte Monas (kira-kira), dilayani rute 1 dan 6B.
    private val stop = Stop("S1", "Monas", "MNS", -6.1754, 106.8272, listOf("1", "6B"))

    private fun vehicle(
        id: String = "B1",
        route: String = "1",
        lat: Double = -6.1754,
        lon: Double = 106.8372, // ~1.1 km timur halte
        speed: Double? = 20.0,
        stale: Boolean = false,
    ) = Vehicle(id, route, lat, lon, speedKmh = speed, observedAt = "2026-10-14T07:00:00Z", isStale = stale)

    @Test fun emptyWhenNoVehicles() {
        assertTrue(EtaPredictor.estimate(stop, emptyList()).isEmpty())
    }

    @Test fun emptyWhenStopHasNoRoutes() {
        val noRoutes = stop.copy(routeIds = emptyList())
        assertTrue(EtaPredictor.estimate(noRoutes, listOf(vehicle())).isEmpty())
    }

    @Test fun filtersVehiclesNotServingStop() {
        val others = listOf(vehicle(id = "B9", route = "9"), vehicle(id = "B1", route = "1"))
        val result = EtaPredictor.estimate(stop, others)
        assertEquals(listOf("B1"), result.map { it.vehicleId })
    }

    @Test fun skipsStaleVehicles() {
        val result = EtaPredictor.estimate(stop, listOf(vehicle(stale = true)))
        assertTrue(result.isEmpty())
    }

    @Test fun sortsByEtaAscending() {
        val near = vehicle(id = "NEAR", lon = 106.8300) // ~300 m
        val far = vehicle(id = "FAR", lon = 106.8472) // ~2.2 km
        val result = EtaPredictor.estimate(stop, listOf(far, near))
        assertEquals(listOf("NEAR", "FAR"), result.map { it.vehicleId })
        assertTrue(result[0].etaMinutes < result[1].etaMinutes)
    }

    @Test fun etaMatchesDistanceOverSpeed() {
        // 1.1 km pada 20 km/jam ≈ 3.3 menit.
        val result = EtaPredictor.estimate(stop, listOf(vehicle()))
        assertEquals(1, result.size)
        val eta = result.single()
        assertEquals("1", eta.routeId)
        assertTrue("eta=${eta.etaMinutes}", eta.etaMinutes in 2.5..4.5)
        assertTrue("dist=${eta.distanceMeters}", eta.distanceMeters in 900.0..1300.0)
    }

    @Test fun fallsBackToDefaultSpeedWithoutTelemetry() {
        // Tanpa kecepatan: 1.1 km pada 18 km/jam ≈ 3.7 menit.
        val result = EtaPredictor.estimate(stop, listOf(vehicle(speed = null)))
        assertEquals(1, result.size)
        assertTrue(result.single().etaMinutes in 3.0..4.5)
    }

    @Test fun dropsVehiclesBeyondMaxDistance() {
        // ~55 km dari halte — di luar jangkauan estimasi.
        val result = EtaPredictor.estimate(stop, listOf(vehicle(lat = -6.7, lon = 107.3)))
        assertTrue(result.isEmpty())
    }

    @Test fun respectsLimit() {
        val many = (1..10).map { vehicle(id = "B$it", lon = 106.8272 + it * 0.001) }
        assertEquals(3, EtaPredictor.estimate(stop, many, limit = 3).size)
    }

    @Test fun haversineSanity() {
        // Monas → ~1 derajat bujur di ekuator ≈ 111 km.
        val d = EtaPredictor.haversineM(-6.1754, 106.8272, -6.1754, 107.8272)
        assertTrue("d=$d", d in 105_000.0..118_000.0)
    }
}

class TripStatsTest {
    private val now = 1_760_000_000_000L // ~2026-10-14
    private val day = 24L * 60 * 60 * 1000

    private fun trip(routeName: String, stopName: String?, daysAgo: Long) = TripHistory(
        routeId = "R", routeName = routeName, stopId = "S", stopName = stopName,
        timestamp = now - daysAgo * day,
    )

    @Test fun countsTripsWithinSevenDays() {
        val history = listOf(
            trip("Koridor 1", "Monas", 1),
            trip("Koridor 1", "Monas", 3),
            trip("6B", "Dukuh", 10), // di luar minggu ini
        )
        val stats = computeTripStats(history, now)
        assertEquals(2, stats.tripsThisWeek)
    }

    @Test fun findsTopRouteAndStop() {
        val history = listOf(
            trip("Koridor 1", "Monas", 1),
            trip("Koridor 1", "Harmoni", 2),
            trip("Koridor 1", "Monas", 3),
            trip("6B", "Dukuh", 4),
        )
        val stats = computeTripStats(history, now)
        assertEquals("Koridor 1", stats.topRouteName)
        assertEquals("Monas", stats.topStopName)
    }

    @Test fun emptyHistoryGivesNulls() {
        val stats = computeTripStats(emptyList(), now)
        assertEquals(0, stats.tripsThisWeek)
        assertNull(stats.topRouteName)
        assertNull(stats.topStopName)
    }

    @Test fun ignoresBlankNamesForTops() {
        val history = listOf(trip("", null, 1), trip("", "", 2))
        val stats = computeTripStats(history, now)
        assertEquals(2, stats.tripsThisWeek)
        assertNull(stats.topRouteName)
        assertNull(stats.topStopName)
    }
}
