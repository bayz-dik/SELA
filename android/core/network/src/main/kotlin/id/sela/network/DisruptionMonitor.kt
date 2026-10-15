package id.sela.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.database.TransitDao
import id.sela.model.ConnectionStatus
import id.sela.model.TransitData
import id.sela.model.TransitRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Monitor gangguan rute (fitur #14).
 *
 * Heuristik lokal untuk rute favorit: kalau dalam 30 menit terakhir rute ini
 * PERNAH terpantau ada kendaraan, tapi sekarang feed LIVE dan 0 kendaraan —
 * kemungkinan rutenya terganggu (atau feed-nya yang putus). Kirim notifikasi
 * 1x per jam per rute.
 *
 * Batasan jujur: ini BUKAN info resmi TransJakarta. Penyebab paling umum
 * "0 kendaraan" justru feed MQTT yang terputus sesaat atau bus sedang tidak
 * beroperasi di luar jam layanan — bukan gangguan beneran. Notifikasi menyebut
 * ini eksplisit supaya tidak menyesatkan.
 */
@Singleton
class DisruptionMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransitRepository,
    private val dao: TransitDao,
    private val preferences: TransitPreferences,
    @TransitScope private val scope: CoroutineScope,
) {
    companion object {
        const val SEEN_WINDOW_MS = 30 * 60 * 1000L
        const val COOLDOWN_MS = 60 * 60 * 1000L
    }

    private var job: Job? = null
    /** routeId → terakhir terlihat ada kendaraan (epoch millis). */
    private val lastSeenWithVehicles = mutableMapOf<String, Long>()
    private val lastNotifiedAt = mutableMapOf<String, Long>()

    fun start() {
        if (job != null) return
        job = scope.launch {
            combine(repository.data, preferences.offlineMode) { data, offline -> data to offline }
                .distinctUntilChanged()
                .collect { (data, offline) ->
                    if (offline) return@collect
                    check(data)
                }
        }
    }

    private fun check(data: TransitData) {
        val favRouteIds = data.favorites.filter { it.kind == "route" }.map { it.id }.toSet()
        if (favRouteIds.isEmpty()) return
        val now = System.currentTimeMillis()
        val byRoute = data.vehicles.groupBy { it.routeId }

        for (routeId in favRouteIds) {
            val vehicles = byRoute[routeId].orEmpty().filter { !it.isStale }
            if (vehicles.isNotEmpty()) {
                lastSeenWithVehicles[routeId] = now
                continue
            }
            val lastSeen = lastSeenWithVehicles[routeId] ?: continue
            if (now - lastSeen > SEEN_WINDOW_MS) {
                // Sudah >30 menit tanpa kendaraan — pola normal (mis. luar jam
                // layanan), jangan ganggu pengguna.
                lastSeenWithVehicles.remove(routeId)
                continue
            }
            if (data.connectionStatus != ConnectionStatus.LIVE) continue
            if (now - (lastNotifiedAt[routeId] ?: 0L) < COOLDOWN_MS) continue
            lastNotifiedAt[routeId] = now

            val routeName = data.routes.find { it.id == routeId }?.shortName ?: routeId
            SelaNotifications.postBusAlert(
                context = context,
                dao = dao,
                scope = scope,
                title = "Rute $routeName kemungkinan terganggu",
                body = "30 menit terakhir tidak ada bus $routeName yang terpantau. " +
                    "Bisa jadi gangguan, bus di luar jam layanan, atau feed data yang putus. " +
                    "(Deteksi otomatis dari HP-mu, bukan info resmi TransJakarta.)",
                kind = "disruption",
                notificationId = "disruption-$routeId".hashCode(),
            )
        }
    }
}
