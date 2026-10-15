package id.sela.model

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

enum class ServiceType { BRT, JAKLINGKO, BUS }
data class Stop(val id: String, val name: String, val code: String? = null, val lat: Double, val lon: Double, val routeIds: List<String> = emptyList())
data class Route(val id: String, val shortName: String, val longName: String, val serviceType: ServiceType, val color: String? = null)
data class Vehicle(val id: String, val routeId: String, val lat: Double, val lon: Double, val bearing: Double = 0.0, val speedKmh: Double? = null, val observedAt: String, val isStale: Boolean = false)
data class GeoPoint(val lat: Double, val lon: Double)
/** Presentation-only fallback matching the web map; does not claim backend classification. */
val Route.displayServiceType: ServiceType get() = when {
    serviceType != ServiceType.BUS -> serviceType
    shortName.uppercase().startsWith("JAK") -> ServiceType.JAKLINGKO
    Regex("^(1[0-3]|[1-9])[A-Z]*$").matches(shortName.uppercase()) -> ServiceType.BRT
    else -> ServiceType.BUS
}
enum class ConnectionStatus { CONNECTING, LIVE, RECONNECTING, OFFLINE, ERROR }
data class Favorite(val id: String, val kind: String)
/** Satu entri riwayat perjalanan (fitur #2). */
data class TripHistory(
    val routeId: String,
    val routeName: String,
    val stopId: String?,
    val stopName: String?,
    val timestamp: Long,
)
/** Satu entri riwayat notifikasi (fitur #112). */
data class NotificationItem(
    val title: String,
    val body: String,
    val kind: String,
    val timestamp: Long,
)
data class TransitData(
    val stops: List<Stop> = emptyList(), val routes: List<Route> = emptyList(),
    val vehicles: List<Vehicle> = emptyList(), val favorites: Set<Favorite> = emptySet(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.OFFLINE,
    val lastSyncedAt: String? = null, val error: String? = null, val isLoading: Boolean = false,
)
interface TransitRepository {
    val data: StateFlow<TransitData>
    val serviceFilter: Flow<ServiceType?>
    suspend fun refresh()
    suspend fun setFilter(filter: ServiceType?)
    suspend fun toggleFavorite(id: String, kind: String)
    suspend fun routeShape(id: String): List<GeoPoint>
    suspend fun runRealtime()
    /** Catat perjalanan ke riwayat (fitur #2). Implementasi default: tidak melakukan apa-apa. */
    suspend fun recordTrip(routeId: String, routeName: String, stopId: String? = null, stopName: String? = null) {}
    /** Aliran riwayat perjalanan, terbaru dulu. Implementasi default: kosong. */
    fun tripHistoryFlow(): Flow<List<TripHistory>> = emptyFlow()
}
class TransitUseCase @Inject constructor(private val repository: TransitRepository) : TransitRepository by repository
