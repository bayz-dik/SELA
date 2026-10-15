package id.sela.network

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.database.TransitDao
import id.sela.database.TripHistoryEntity
import id.sela.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.MqttException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standalone TransitRepository: data GTFS dari assets lokal (atau hasil
 * pembaruan di files/), live vehicles dari MQTT langsung ke broker TJ.
 * Tidak ada dependency ke backend server.
 */
@Singleton
class StandaloneTransitRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localGtfs: LocalGtfsRepository,
    private val mqttClient: MqttVehicleClient,
    private val dao: TransitDao,
    private val preferences: TransitPreferences,
    @TransitScope private val scope: CoroutineScope,
) : TransitRepository {
    companion object {
        /** B19: tiap pesan MQTT hanya memperbarui reconciler; UI di-emit maksimal 1x per 500 ms. */
        private const val VEHICLE_EMIT_THROTTLE_MS = 500L
    }

    private val mutableData = MutableStateFlow(TransitData(isLoading = true))
    override val data: StateFlow<TransitData> = mutableData.asStateFlow()
    override val serviceFilter: Flow<ServiceType?> = preferences.filter

    private val vehicles = VehicleReconciler()
    private var lastWidgetPush = 0L

    init {
        // Load GTFS data from local assets (or updated files)
        scope.launch {
            try {
                val routes = localGtfs.routes()
                val stops = localGtfs.stops()
                mutableData.update {
                    it.copy(
                        routes = routes,
                        stops = stops,
                        isLoading = false,
                        lastSyncedAt = Instant.now().toString(),
                    )
                }
            } catch (e: Exception) {
                mutableData.update {
                    it.copy(isLoading = false, error = "Data lokal tidak dapat dibuka: ${e.message?.takeIf { it.isNotBlank() } ?: "kesalahan tak dikenal"}")
                }
            }
        }

        // Observe favorites from Room
        scope.launch {
            while (isActive) {
                try {
                    dao.favorites().collect { rows ->
                        mutableData.update {
                            it.copy(favorites = rows.map { f -> Favorite(f.id, f.kind) }.toSet())
                        }
                    }
                    delay(5_000)
                } catch (e: Exception) {
                    delay(5_000)
                }
            }
        }

        // Connect to MQTT for live vehicles (skipped in offline mode).
        // collectLatest: menyalakan mode offline membatalkan koneksi MQTT yang berjalan.
        scope.launch {
            preferences.offlineMode.collectLatest { offline ->
                if (offline) {
                    mutableData.update {
                        it.copy(connectionStatus = ConnectionStatus.OFFLINE, error = null)
                    }
                    pushWidgetSnapshot(force = true)
                } else {
                    connectMqtt()
                }
            }
        }
    }

    private suspend fun connectMqtt() {
        // Bug #1 fix: retry with exponential backoff on initial connect failure
        var attempt = 0
        var delayMs = 2000L
        val maxDelayMs = 30000L

        while (true) {
            attempt++
            mutableData.update {
                it.copy(
                    connectionStatus = if (attempt == 1) ConnectionStatus.CONNECTING else ConnectionStatus.RECONNECTING,
                    error = if (attempt > 1) "Mencoba menghubungkan ulang... (percobaan $attempt)" else it.error,
                )
            }

            try {
                mqttClient.vehicles()
                    .onEach { mqttVehicle ->
                        // Reset backoff on successful message
                        attempt = 0
                        delayMs = 2000L
                        val vehicle = Vehicle(
                            id = mqttVehicle.id,
                            routeId = mqttVehicle.routeId,
                            lat = mqttVehicle.lat,
                            lon = mqttVehicle.lon,
                            bearing = mqttVehicle.bearing.toDouble(),
                            speedKmh = mqttVehicle.speedKmh,
                            observedAt = mqttVehicle.observedAt,
                        )
                        // B19: reconciler menerima SEMUA pesan (dedup/expiry tetap
                        // akurat); yang dibatasi hanya pemancaran ke UI di bawah.
                        vehicles.update(vehicle)
                    }
                    .sample(VEHICLE_EMIT_THROTTLE_MS)
                    .collect {
                        mutableData.update {
                            it.copy(
                                vehicles = vehicles.values(),
                                connectionStatus = ConnectionStatus.LIVE,
                                error = null,
                            )
                        }
                        pushWidgetSnapshot()
                    }
                // Flow completed normally (shouldn't happen) - break and retry
                break
            } catch (e: Exception) {
                // Don't retry if scope is cancelled
                if (!scope.isActive) break

                // B16: broker menolak koneksi (CONNACK rc=5 = akses ditolak).
                // Retry selamanya tidak ada gunanya — hentikan loop dan
                // tampilkan pesan jelas ke pengguna.
                if (isMqttAccessDenied(e)) {
                    mutableData.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.ERROR,
                            error = "Akses ditolak broker (CONNACK rc=5). Tidak mencoba lagi — periksa konfigurasi koneksi.",
                        )
                    }
                    pushWidgetSnapshot(force = true)
                    break
                }

                mutableData.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.RECONNECTING,
                        error = "Koneksi live terputus. Mencoba lagi dalam ${delayMs / 1000} detik...",
                    )
                }
                pushWidgetSnapshot(force = true)

                try {
                    delay(delayMs)
                } catch (_: Exception) {
                    break // Cancelled
                }
                // Exponential backoff: 2s, 4s, 8s, 16s, 30s (max)
                delayMs = minOf(delayMs * 2, maxDelayMs)
            }
        }
    }

    /**
     * B16: deteksi penolakan akses MQTT (CONNACK rc=5 / tidak diizinkan).
     * Paho memetakan CONNACK 5 ke [MqttException.REASON_CODE_NOT_AUTHORIZED];
     * beberapa jalur lain menampilkannya lewat pesan teks.
     */
    private fun isMqttAccessDenied(e: Throwable): Boolean {
        var t: Throwable? = e
        while (t != null) {
            if (t is MqttException &&
                (t.reasonCode.toInt() == MqttException.REASON_CODE_NOT_AUTHORIZED || t.reasonCode.toInt() == 5)
            ) return true
            val msg = t.message?.lowercase()
            if (msg != null && ("not authorized" in msg || "rc=5" in msg)) return true
            t = t.cause
        }
        return false
    }

    /** Dorong snapshot ringan untuk widget (fitur #95/#111), dibatasi 1x/30 dtk. */
    private suspend fun pushWidgetSnapshot(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastWidgetPush < 30_000) return
        lastWidgetPush = now
        val snapshot = mutableData.value
        preferences.updateWidgetSnapshot(
            vehicleCount = snapshot.vehicles.size,
            status = snapshot.connectionStatus.name,
            syncedAt = now,
        )
        // Minta widget me-refresh tampilannya
        runCatching {
            val update = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                setPackage(context.packageName)
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context, "id.sela.widget.SelaWidgetProvider"),
                    ),
                )
            }
            context.sendBroadcast(update)
        }
    }

    override suspend fun refresh() {
        // Reload from local (in case data updated)
        try {
            localGtfs.clearCache()
            val routes = localGtfs.routes()
            val stops = localGtfs.stops()
            mutableData.update {
                it.copy(
                    routes = routes,
                    stops = stops,
                    lastSyncedAt = Instant.now().toString(),
                    error = null,
                )
            }
        } catch (e: Exception) {
            val msg = e.message?.takeIf { it.isNotBlank() }
                ?: "${e.javaClass.simpleName} (tanpa pesan)"
            mutableData.update { it.copy(error = "Refresh gagal: $msg") }
        }
    }

    override suspend fun setFilter(filter: ServiceType?) {
        preferences.setFilter(filter)
    }

    override suspend fun toggleFavorite(id: String, kind: String) {
        try {
            dao.toggle(id, kind)
        } catch (_: Exception) {}
    }

    override suspend fun routeShape(id: String): List<GeoPoint> {
        // Standalone: route shapes not available from local GTFS (only stops)
        // Return empty; UI should handle gracefully
        return emptyList()
    }

    override suspend fun runRealtime() {
        // MQTT connection is established in init; this is a no-op for standalone
        // Reconnection is handled automatically by MqttVehicleClient
    }

    /** Catat perjalanan ke riwayat (fitur #2). */
    override suspend fun recordTrip(routeId: String, routeName: String, stopId: String?, stopName: String?) {
        try {
            dao.insertTrip(
                TripHistoryEntity(
                    routeId = routeId,
                    routeName = routeName,
                    stopId = stopId,
                    stopName = stopName,
                    timestamp = System.currentTimeMillis(),
                )
            )
            // Prune history older than 90 days
            val ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
            dao.pruneHistory(ninetyDaysAgo)
        } catch (_: Exception) {}
    }

    override fun tripHistoryFlow(): Flow<List<TripHistory>> =
        dao.tripHistoryFlow().map { rows ->
            rows.map {
                TripHistory(
                    routeId = it.routeId,
                    routeName = it.routeName,
                    stopId = it.stopId,
                    stopName = it.stopName,
                    timestamp = it.timestamp,
                )
            }
        }

    suspend fun clearTripHistory() {
        runCatching { dao.pruneHistory(System.currentTimeMillis() + 1) }
    }
}
