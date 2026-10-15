package id.sela.network

import id.sela.database.NotificationHistoryEntity
import id.sela.database.TransitDao
import id.sela.model.NotificationItem
import id.sela.model.TripHistory
import id.sela.model.TripStats
import id.sela.model.computeTripStats
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Facade untuk layar Pengaturan: menggabungkan riwayat perjalanan (#2),
 * mode offline (#5), pembaruan data (#1), dan notifikasi (#4, #112)
 * dalam satu API yang bisa di-inject feature:settings.
 */
@Singleton
class SelaSettings @Inject constructor(
    private val repository: StandaloneTransitRepository,
    private val preferences: TransitPreferences,
    private val dao: TransitDao,
    private val checker: DataUpdateChecker,
) {
    val tripHistory: Flow<List<TripHistory>> = repository.tripHistoryFlow()

    val notifications: Flow<List<NotificationItem>> =
        dao.notificationHistoryFlow().map { rows ->
            rows.map { NotificationItem(it.title, it.body, it.kind, it.timestamp) }
        }

    val offlineMode: Flow<Boolean> = preferences.offlineMode
    val arrivalAlerts: Flow<Boolean> = preferences.arrivalAlertsEnabled
    val gtfsVersion: Flow<String> = preferences.gtfsVersion
    val gtfsUpdatedAt: Flow<Long> = preferences.gtfsUpdatedAt
    val gtfsUpdateUrl: Flow<String> = preferences.gtfsUpdateUrl

    suspend fun toggleOfflineMode() {
        preferences.setOfflineMode(!preferences.offlineMode.first())
    }

    suspend fun toggleArrivalAlerts() {
        preferences.setArrivalAlertsEnabled(!preferences.arrivalAlertsEnabled.first())
    }

    suspend fun setGtfsUpdateUrl(url: String) {
        preferences.setGtfsUpdateUrl(url)
    }

    suspend fun checkDataUpdate(): DataUpdateChecker.CheckResult = checker.checkNow()

    suspend fun resetDataToBundled(): DataUpdateChecker.CheckResult = checker.resetToBundled()

    suspend fun clearTripHistory() = repository.clearTripHistory()

    suspend fun clearNotifications() {
        runCatching { dao.clearNotifications() }
    }

    /** Statistik riwayat: "minggu ini naik Nx", rute & halte tersering (fitur #2). */
    suspend fun tripStats(): TripStats = runCatching {
        val rows = dao.tripHistory(limit = 500).map {
            TripHistory(it.routeId, it.routeName, it.stopId, it.stopName, it.timestamp)
        }
        computeTripStats(rows, System.currentTimeMillis())
    }.getOrDefault(TripStats(0, null, null))

    /**
     * Ekspor riwayat perjalanan sebagai CSV (fitur #116).
     * Format: timestamp_iso,route_id,route_name,stop_id,stop_name
     */
    suspend fun exportHistoryCsv(): String = runCatching {
        val rows = dao.tripHistory(limit = 1000)
        val sb = StringBuilder("timestamp_iso,route_id,route_name,stop_id,stop_name\n")
        for (r in rows) {
            val iso = java.time.Instant.ofEpochMilli(r.timestamp).toString()
            sb.append(csvCell(iso)).append(',')
                .append(csvCell(r.routeId)).append(',')
                .append(csvCell(r.routeName)).append(',')
                .append(csvCell(r.stopId ?: "")).append(',')
                .append(csvCell(r.stopName ?: "")).append('\n')
        }
        sb.toString()
    }.getOrDefault("")

    private fun csvCell(v: String): String =
        "\"" + v.replace("\"", "\"\"") + "\""
}
