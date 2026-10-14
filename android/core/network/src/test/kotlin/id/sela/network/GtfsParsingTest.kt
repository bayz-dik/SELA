package id.sela.network

import id.sela.model.ServiceType
import org.junit.Assert.*
import org.junit.Test

/**
 * Test parsing data GTFS lokal (assets/gtfs, file JSON) tanpa dependency Android.
 */
class GtfsParsingTest {
    @Test fun parsesRoutesAndFallsBackCodeToId() {
        val routes = GtfsJsonParser.parseRoutes(
            """[{"id":"1","code":"1","name":"Blok M - Kota","color":"#D62126"},
                {"id":"6B","code":"","name":"Ragunan - Monas","color":null},
                {"id":"","code":"X","name":"Rusak","color":null}]""",
        )
        assertEquals(2, routes.size)
        val r1 = routes[0]
        assertEquals("1", r1.id)
        assertEquals("1", r1.shortName)
        assertEquals("Blok M - Kota", r1.longName)
        assertEquals(ServiceType.BUS, r1.serviceType)
        assertEquals("#D62126", r1.color)
        // code kosong -> pakai id; color null -> null
        assertEquals("6B", routes[1].shortName)
        assertNull(routes[1].color)
    }

    @Test fun parsesStopsWithRouteRelations() {
        val stops = GtfsJsonParser.parseStops(
            """[{"id":"s1","name":"Monas","lat":-6.1754,"lon":106.8272,"routes":["1","2"],"code":"MNS"},
                {"id":"s2","name":"Tanpa Rute","lat":-6.2,"lon":106.8,"routes":[]}]""",
        )
        assertEquals(2, stops.size)
        assertEquals(listOf("1", "2"), stops[0].routeIds)
        assertEquals("MNS", stops[0].code)
        assertTrue(stops[1].routeIds.isEmpty())
    }

    @Test fun skipsInvalidRows() {
        val stops = GtfsJsonParser.parseStops(
            """[{"id":"bad","name":"Koordinat Rusak","lat":100.0,"lon":106.8,"routes":[]},
                {"id":"","name":"Tanpa ID","lat":-6.2,"lon":106.8,"routes":[]},
                {"id":"ok","name":"Bagus","lat":-6.2,"lon":106.8,"routes":[]}]""",
        )
        assertEquals(1, stops.size)
        assertEquals("ok", stops.single().id)
    }

    @Test fun emptyAndMalformedJsonYieldEmptyLists() {
        assertTrue(GtfsJsonParser.parseRoutes("[]").isEmpty())
        assertTrue(GtfsJsonParser.parseStops("[]").isEmpty())
    }
}
