package id.sela.feature.search
import id.sela.model.*
import org.junit.Assert.*
import org.junit.Test
class TransitSearchTest {
    private val routes = listOf(Route("1", "1", "Blok M", ServiceType.BUS), Route("JAK.01", "JAK.01", "JakLingko", ServiceType.BUS))
    @Test fun nullableSourceClassificationStillAllowsJakFilter() { assertEquals("JAK.01", searchRoutes(routes, " jak ", ServiceType.JAKLINGKO).single().id) }
    @Test fun stopFilterUsesPersistableRouteRelations() {
        val stops = listOf(Stop("s", "Halte", lat = -6.2, lon = 106.8, routeIds = listOf("1")))
        assertEquals(1, filterStops(stops, routes, ServiceType.BRT).size)
        assertTrue(filterStops(stops, routes, ServiceType.JAKLINGKO).isEmpty())
    }
    @Test fun stopSearchTrimsAndIgnoresCase() { assertEquals("s", searchStops(listOf(Stop("s", "Bundaran HI", lat = -6.2, lon = 106.8)), " hi ").single().id) }
}
