package id.sela.model

/**
 * Perencana rute offline (fitur #9).
 *
 * Perutean STRUKTURAL murni dari data GTFS statis: hanya memakai relasi
 * halte↔rute ([Stop.routeIds]). Tidak ada jadwal keberangkatan, frekuensi,
 * maupun waktu tempuh — jadi hasilnya adalah "rute-rute yang secara
 * struktur melayani kedua halte", BUKAN janji perjalanan tercepat.
 * UI wajib melabelinya sebagai rencana struktural, bukan estimasi waktu.
 */
data class Leg(
    val route: Route,
    val fromStop: Stop,
    val toStop: Stop,
)

data class TripPlan(
    val legs: List<Leg>,
) {
    /** Jumlah transit (ganti bus). 0 = langsung. */
    val transfers: Int get() = (legs.size - 1).coerceAtLeast(0)
    val isDirect: Boolean get() = legs.size == 1
}

object TripPlanner {
    /** Hasil dibatasi supaya daftar tetap ringkas. */
    const val MAX_PLANS = 5

    /**
     * Rencanakan perjalanan dari [origin] ke [dest] memakai daftar semua
     * halte [stops] dan rute [routes].
     *
     * - Langsung: ada rute yang melayani kedua halte.
     * - 1x transit: rute A dari origin × rute B ke dest, dengan halte
     *   interchange yang routeIds-nya memuat keduanya.
     *
     * Hasil diurutkan: langsung dulu, lalu transit tersedikit,
     * dibatasi [MAX_PLANS].
     */
    fun planTrip(origin: Stop, dest: Stop, stops: List<Stop>, routes: List<Route>): List<TripPlan> {
        if (origin.id == dest.id) return emptyList()
        val routeById = routes.associateBy { it.id }
        val originRouteIds = origin.routeIds.toSet()
        val destRouteIds = dest.routeIds.toSet()
        if (originRouteIds.isEmpty() || destRouteIds.isEmpty()) return emptyList()

        val plans = mutableListOf<TripPlan>()

        // Langsung: rute yang melayani keduanya.
        (originRouteIds intersect destRouteIds)
            .mapNotNull(routeById::get)
            .forEach { route -> plans += TripPlan(listOf(Leg(route, origin, dest))) }

        // 1x transit lewat halte interchange.
        for (stop in stops) {
            if (stop.id == origin.id || stop.id == dest.id) continue
            val viaA = stop.routeIds.firstOrNull { it in originRouteIds } ?: continue
            val viaB = stop.routeIds.firstOrNull { it in destRouteIds && it != viaA } ?: continue
            val routeA = routeById[viaA] ?: continue
            val routeB = routeById[viaB] ?: continue
            plans += TripPlan(listOf(Leg(routeA, origin, stop), Leg(routeB, stop, dest)))
        }

        return plans
            .sortedWith(compareBy({ if (it.isDirect) 0 else 1 }, { it.transfers }))
            .take(MAX_PLANS)
    }
}
