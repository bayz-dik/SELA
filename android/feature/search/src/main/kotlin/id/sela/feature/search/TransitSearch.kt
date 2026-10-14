package id.sela.feature.search

import id.sela.model.*
import java.util.Locale

fun filterStops(stops: List<Stop>, routes: List<Route>, filter: ServiceType?): List<Stop> {
    if (filter == null) return stops
    val routeIds = routes.filter { it.displayServiceType == filter }.mapTo(hashSetOf()) { it.id }
    return stops.filter { stop -> stop.routeIds.any { it in routeIds } }
}
fun searchStops(stops: List<Stop>, query: String): List<Stop> {
    val needle = query.trim().lowercase(Locale.ROOT)
    return stops.filter { needle.isEmpty() || it.name.lowercase(Locale.ROOT).contains(needle) || it.code.orEmpty().lowercase(Locale.ROOT).contains(needle) }
}
fun searchRoutes(routes: List<Route>, query: String, filter: ServiceType?): List<Route> {
    val needle = query.trim().lowercase(Locale.ROOT)
    return routes.filter { (filter == null || it.displayServiceType == filter) &&
        (needle.isEmpty() || "${it.shortName} ${it.longName}".lowercase(Locale.ROOT).contains(needle)) }
}
