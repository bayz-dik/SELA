package id.sela.network

import id.sela.model.Vehicle
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

/**
 * Test TimeMachine.predictAhead (fitur #81) — ekstrapolasi linear murni.
 */
class TimeMachineTest {
    private fun vehicle(
        bearing: Double = 90.0,
        speed: Double? = 60.0,
        lat: Double = -6.2,
        lon: Double = 106.8,
    ) = Vehicle("B1", "1", lat, lon, bearing = bearing, speedKmh = speed,
        observedAt = "2026-10-14T07:00:00Z")

    @Test fun zeroMinutesReturnsAsIs() {
        val v = vehicle()
        val out = TimeMachine.predictAhead(listOf(v), 0)
        assertEquals(v.lat, out.single().lat, 0.0)
        assertEquals(v.lon, out.single().lon, 0.0)
    }

    @Test fun movesEastOnBearing90() {
        // 60 km/jam × 60 mnt = 60 km ke timur.
        val out = TimeMachine.predictAhead(listOf(vehicle()), 60).single()
        val expectedLonDelta = 60.0 / (111.32 * Math.cos(Math.toRadians(-6.2)))
        assertEquals(0.0, out.lat - (-6.2), 0.01)
        assertTrue("lonDelta=${out.lon - 106.8}", abs((out.lon - 106.8) - expectedLonDelta) < 0.01)
    }

    @Test fun movesNorthOnBearing0() {
        // 30 km/jam × 60 mnt = 30 km ke utara.
        val out = TimeMachine.predictAhead(listOf(vehicle(bearing = 0.0, speed = 30.0)), 60).single()
        assertTrue("latDelta=${out.lat - (-6.2)}", abs((out.lat - (-6.2)) - 30.0 / 111.32) < 0.01)
        assertEquals(106.8, out.lon, 1e-9)
    }

    @Test fun vehicleWithoutSpeedUnchanged() {
        val v = vehicle(speed = null)
        val out = TimeMachine.predictAhead(listOf(v), 60).single()
        assertEquals(v.lat, out.lat, 0.0)
        assertEquals(v.lon, out.lon, 0.0)
    }

    @Test fun keepsIdentityFields() {
        val out = TimeMachine.predictAhead(listOf(vehicle()), 30).single()
        assertEquals("B1", out.id)
        assertEquals("1", out.routeId)
    }
}
