package id.sela.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Test parsing payload MQTT langsung dari broker TransJakarta
 * (wss://mqtt.tj.co.id:8084/mqtt, topic /mobile_armada/#).
 * Tidak ada server perantara — format ini milik TJ.
 */
class MqttVehicleParsingTest {
    private val client = MqttVehicleClient(Gson())
    private fun json(body: String): JsonObject = Gson().fromJson(body, JsonObject::class.java)

    @Test fun parsesFullPayloadWithSnappedCoordinates() {
        val v = client.parseVehicle(
            "/mobile_armada/tj/1/trip-9/B123",
            json("""{"route_code":"1A","bus_body_no":"TJ-101","trip_id":"trip-9","snap_latitude":-6.2,"snap_longitude":106.8,"latitude":-6.2001,"longitude":106.8001,"bearing":370,"gps_timestamp":"2026-10-14T06:00:00Z"}"""),
        )
        assertNotNull(v)
        assertEquals("TJ-101", v!!.id)
        assertEquals("1A", v.routeId)
        assertEquals(-6.2, v.lat, 0.0)
        assertEquals(106.8, v.lon, 0.0)
        assertEquals(10f, v.bearing) // 370 dinormalisasi ke 10
        assertEquals("trip-9", v.tripId)
        assertEquals("2026-10-14T06:00:00Z", v.observedAt)
    }

    @Test fun fallsBackToTopicSegmentsWhenPayloadLacksIds() {
        val v = client.parseVehicle(
            "/mobile_armada/tj/6B/trip-3/B456",
            json("""{"latitude":-6.3,"longitude":106.9}"""),
        )
        assertNotNull(v)
        assertEquals("6B", v!!.routeId)
        assertEquals("B456", v.id)
        assertEquals("trip-3", v.tripId)
    }

    @Test fun negativeBearingNormalized() {
        val v = client.parseVehicle(
            "/mobile_armada/tj/1/t/1",
            json("""{"latitude":-6.2,"longitude":106.8,"bearing":-10}"""),
        )
        assertNotNull(v)
        assertEquals(350f, v!!.bearing)
    }

    @Test fun rejectsCoordinatesOutsideJakarta() {
        assertNull(
            client.parseVehicle(
                "/mobile_armada/tj/1/t/1",
                json("""{"latitude":-6.2,"longitude":114.0}"""),
            ),
        )
        assertNull(
            client.parseVehicle(
                "/mobile_armada/tj/1/t/1",
                json("""{"latitude":0.0,"longitude":106.8}"""),
            ),
        )
    }

    @Test fun rejectsShortTopicAndMissingCoordinates() {
        assertNull(client.parseVehicle("/mobile_armada/tj", json("""{"latitude":-6.2,"longitude":106.8}""")))
        assertNull(client.parseVehicle("/mobile_armada/tj/1/t/1", json("""{"bearing":90}""")))
    }

    @Test fun malformedNumbersAreSkippedSafely() {
        val v = client.parseVehicle(
            "/mobile_armada/tj/1/t/1",
            json("""{"latitude":"-6.2","longitude":"106.8","bearing":"not-a-number"}"""),
        )
        assertNotNull(v)
        assertEquals(-6.2, v!!.lat, 0.0)
        assertEquals(0f, v.bearing)
    }

    @Test fun brokerConstantsMatchTransJakartaFeed() {
        assertEquals("wss://mqtt.tj.co.id:8084/mqtt", MqttVehicleClient.BROKER_URL)
        assertEquals("/mobile_armada/#", MqttVehicleClient.TOPIC)
    }

    @Test fun clientIdFollowsMqttJsPattern() {
        // Broker TJ menolak pola clientId selain mqttjs_<8 hex> (CONNACK rc=5)
        val ids = (1..20).map { client.newClientId() }.toSet()
        assertEquals(20, ids.size) // unik
        ids.forEach { assertTrue(it.matches(Regex("mqttjs_[0-9a-f]{8}"))) }
    }
}
