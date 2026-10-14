package id.sela.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.database.TransitDao
import id.sela.model.ArrivalEstimate
import id.sela.model.EtaPredictor
import id.sela.model.TransitRepository
import id.sela.model.Vehicle
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Monitor peringatan "bus dekat" (fitur #4).
 *
 * Berjalan selama proses aplikasi hidup. Ketika diaktifkan di Pengaturan dan
 * ada bus dari rute favorit dalam radius [ALERT_RADIUS_M] dari halte favorit,
 * kirim notifikasi (dibatasi 1x per rute+halte tiap [COOLDOWN_MS]).
 *
 * Batasan jujur: monitor ini hanya jalan selama proses app hidup. Di Android
 * modern, proses bisa dimatikan sistem saat app lama di background — jadi
 * peringatan paling andal saat app baru saja dibuka. Tidak ada server yang
 * mengirim push; semuanya lokal dari feed MQTT langsung.
 */
@Singleton
class ArrivalAlertMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TransitRepository,
    private val preferences: TransitPreferences,
    private val dao: TransitDao,
    private val sleepMode: SleepMode,
    @TransitScope private val scope: CoroutineScope,
) {
    companion object {
        const val ALERT_RADIUS_M = 2_000.0
        const val COOLDOWN_MS = 15 * 60 * 1000L
        /** ETA ongoing hanya untuk bus yang datang ≤15 menit. */
        const val ONGOING_ETA_MAX_MIN = 15.0
    }

    private var job: Job? = null
    private val lastAlertAt = mutableMapOf<String, Long>()

    fun start() {
        if (job != null) return
        job = scope.launch {
            launch {
                combine(
                    repository.data,
                    preferences.arrivalAlertsEnabled,
                    preferences.offlineMode,
                    preferences.sleepMode,
                ) { data, alertsOn, offline, sleep -> SleepInput(data, alertsOn, offline, sleep) }
                    .distinctUntilChanged()
                    .collect { (data, alertsOn, offline, sleep) ->
                        if (!alertsOn || offline) return@collect
                        check(data.vehicles, data.stops, data.favorites, sleep)
                    }
            }
            launch {
                // Fitur #73: kartu ETA ongoing di lock screen, diperbarui di tempat.
                combine(repository.data, preferences.offlineMode) { data, offline -> data to offline }
                    .distinctUntilChanged()
                    .collect { (data, offline) ->
                        if (offline) {
                            SelaNotifications.cancelOngoingEta(context)
                            return@collect
                        }
                        updateOngoingEta(data.vehicles, data.stops, data.favorites)
                    }
            }
        }
    }

    private data class SleepInput(
        val data: id.sela.model.TransitData,
        val alertsOn: Boolean,
        val offline: Boolean,
        val sleep: Boolean,
    )

    private fun check(
        vehicles: List<Vehicle>,
        stops: List<id.sela.model.Stop>,
        favorites: Set<id.sela.model.Favorite>,
        sleep: Boolean = false,
    ) {
        val favStops = stops.filter { s -> favorites.any { it.kind == "stop" && it.id == s.id } }
        if (favStops.isEmpty() || vehicles.isEmpty()) return
        val favRouteIds = favorites.filter { it.kind == "route" }.map { it.id }.toSet()
        val now = System.currentTimeMillis()

        for (stop in favStops) {
            val candidates = vehicles.filter { v ->
                (favRouteIds.isEmpty() || v.routeId in favRouteIds || v.routeId in stop.routeIds) &&
                    distanceM(stop.lat, stop.lon, v.lat, v.lon) <= ALERT_RADIUS_M
            }
            val nearest = candidates.minByOrNull { distanceM(stop.lat, stop.lon, it.lat, it.lon) }
                ?: continue
            val key = "${nearest.routeId}|${stop.id}"
            if (now - (lastAlertAt[key] ?: 0L) < COOLDOWN_MS) continue
            lastAlertAt[key] = now
            val dist = distanceM(stop.lat, stop.lon, nearest.lat, nearest.lon).roundToInt()
            val title = "Bus ${nearest.routeId} mendekat"
            val body = "Bus ${nearest.id} rute ${nearest.routeId} sekitar $dist m dari ${stop.name}."
            // Fitur #70: mode tidur — getar kuat + full-screen intent, bukan notif biasa.
            if (sleep) {
                sleepMode.postSleepAlert(context, title, body, key.hashCode())
            } else {
                SelaNotifications.postBusAlert(
                    context = context,
                    dao = dao,
                    scope = scope,
                    title = title,
                    body = body,
                    kind = "arrival",
                    notificationId = key.hashCode(),
                )
            }
        }
    }

    internal fun distanceM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    /**
     * Perbarui kartu ETA ongoing (fitur #73): ETA tercepat ≤ [ONGOING_ETA_MAX_MIN]
     * dari rute favorit di halte favorit. Kalau tidak ada, hapus kartunya.
     */
    private fun updateOngoingEta(
        vehicles: List<Vehicle>,
        stops: List<id.sela.model.Stop>,
        favorites: Set<id.sela.model.Favorite>,
    ) {
        val favStops = stops.filter { s -> favorites.any { it.kind == "stop" && it.id == s.id } }
        val favRouteIds = favorites.filter { it.kind == "route" }.map { it.id }.toSet()
        if (favStops.isEmpty() || vehicles.isEmpty()) {
            SelaNotifications.cancelOngoingEta(context)
            return
        }
        var best: ArrivalEstimate? = null
        var bestStop: id.sela.model.Stop? = null
        for (stop in favStops) {
            val relevant = if (favRouteIds.isEmpty()) vehicles
            else vehicles.filter { it.routeId in favRouteIds || it.routeId in stop.routeIds }
            val estimate = EtaPredictor.estimate(stop, relevant, limit = 1).firstOrNull()
            if (estimate != null && (best == null || estimate.etaMinutes < (best?.etaMinutes ?: Double.MAX_VALUE))) {
                best = estimate
                bestStop = stop
            }
        }
        val eta = best
        val stop = bestStop
        if (eta == null || stop == null || eta.etaMinutes > ONGOING_ETA_MAX_MIN) {
            SelaNotifications.cancelOngoingEta(context)
            return
        }
        val menit = eta.etaMinutes.roundToInt().coerceAtLeast(1)
        val routeName = "Bus ${eta.routeId}"
        SelaNotifications.postOngoingEta(
            context = context,
            title = "$routeName: $menit mnt — ${stop.name}",
            body = "$routeName (bus ${eta.vehicleId}) diperkirakan tiba di ${stop.name} " +
                "dalam $menit menit. Estimasi lokal dari posisi live.",
        )
    }
}
