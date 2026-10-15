package id.sela.model

import org.junit.Assert.*
import org.junit.Test

class TripPlannerTest {
    private val routeA = Route("A", "1", "Koridor 1", ServiceType.BRT)
    private val routeB = Route("B", "2", "Koridor 2", ServiceType.BRT)
    private val routeC = Route("C", "3", "Koridor 3", ServiceType.BRT)

    private fun stop(id: String, vararg routes: String) =
        Stop(id, "Halte $id", null, -6.2, 106.8, routes.toList())

    @Test
    fun directRouteIsPreferred() {
        val origin = stop("o", "A", "C")
        val dest = stop("d", "A", "B")
        val plans = TripPlanner.planTrip(origin, dest, listOf(origin, dest), listOf(routeA, routeB, routeC))
        assertTrue(plans.isNotEmpty())
        assertTrue(plans.first().isDirect)
        assertEquals("A", plans.first().legs.single().route.id)
    }

    @Test
    fun singleTransferViaInterchange() {
        val origin = stop("o", "A")
        val dest = stop("d", "B")
        val interchange = stop("x", "A", "B")
        val plans = TripPlanner.planTrip(origin, dest, listOf(origin, dest, interchange), listOf(routeA, routeB))
        assertEquals(1, plans.size)
        val plan = plans.single()
        assertEquals(2, plan.legs.size)
        assertEquals(1, plan.transfers)
        assertEquals("A", plan.legs[0].route.id)
        assertEquals("B", plan.legs[1].route.id)
        assertEquals("x", plan.legs[0].toStop.id)
        assertEquals("x", plan.legs[1].fromStop.id)
    }

    @Test
    fun interchangeMustNotBeOriginOrDest() {
        val origin = stop("o", "A", "B")
        val dest = stop("d", "B")
        val plans = TripPlanner.planTrip(origin, dest, listOf(origin, dest), listOf(routeA, routeB))
        // Rute B melayani keduanya -> langsung, bukan transit lewat origin sendiri.
        assertEquals(1, plans.size)
        assertTrue(plans.single().isDirect)
    }

    @Test
    fun noRouteReturnsEmpty() {
        val origin = stop("o", "A")
        val dest = stop("d", "B")
        val plans = TripPlanner.planTrip(origin, dest, listOf(origin, dest), listOf(routeA, routeB))
        assertTrue(plans.isEmpty())
    }

    @Test
    fun sameOriginAndDestReturnsEmpty() {
        val origin = stop("o", "A")
        val plans = TripPlanner.planTrip(origin, origin, listOf(origin), listOf(routeA))
        assertTrue(plans.isEmpty())
    }

    @Test
    fun resultsAreCapped() {
        val origin = stop("o", "A")
        val dest = stop("d", "B")
        val interchanges = (1..10).map { stop("x$it", "A", "B") }
        val plans = TripPlanner.planTrip(
            origin, dest,
            listOf(origin, dest) + interchanges,
            listOf(routeA, routeB),
        )
        assertEquals(TripPlanner.MAX_PLANS, plans.size)
    }
}
