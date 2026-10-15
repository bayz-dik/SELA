package id.sela.database
import id.sela.model.ServiceType
import org.junit.Assert.*
import org.junit.Test
class TransitEntityTest {
    @Test fun stopRelationsSurviveCacheMapping() {
        val stop = StopEntity("s", "Monas", null, -6.1, 106.8, "1,JAK.01").model()
        assertEquals(listOf("1", "JAK.01"), stop.routeIds)
        assertEquals(-6.1, stop.lat, 0.0)
    }
    @Test fun emptyRelationsStayEmpty() {
        assertTrue(StopEntity("s", "Monas", null, -6.1, 106.8).model().routeIds.isEmpty())
    }
    @Test fun unknownPersistedServiceIsSafelyMapped() {
        assertEquals(ServiceType.BUS, RouteEntity("1", "1", "Rute", "unknown", null).model().serviceType)
        assertEquals(ServiceType.BRT, RouteEntity("1", "1", "Rute", "BRT", null).model().serviceType)
    }
}
