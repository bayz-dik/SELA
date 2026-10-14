package id.sela.power

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.model.EtaPredictor
import id.sela.model.Vehicle
import id.sela.network.SelaNotifications
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Peringatan pejalan kaki: "awas, bus dekat" (fitur #79).
 *
 * [check] dipanggil berkala dengan posisi pengguna + daftar kendaraan live:
 * bila ada bus dalam radius 100 m (jarak haversine garis lurus), kirim
 * notifikasi "Awas, bus dekat — Bus {rute} ±{x} m darimu." Maksimal satu
 * notifikasi per kendaraan per 5 menit (cooldown per vehicle id).
 *
 * Notifikasi diposting langsung via [NotificationManagerCompat] dengan channel
 * [SelaNotifications.CHANNEL_ALERTS] — TANPA DAO, jadi tidak tercatat di
 * riwayat notifikasi (fitur #112). Channel dipastikan ada lewat
 * [SelaNotifications.createChannels].
 *
 * JUJUR: ini peringatan pejalan kaki berbasis posisi HP, BUKAN "mode supir"
 * beneran. Tidak ada integrasi dengan sistem kendaraan, tidak tahu arah
 * hadap bus, dan jaraknya garis lurus (bisa beda jauh dengan jarak jalan
 * kaki sebenarnya). Tetap lihat kiri-kanan sebelum menyeberang.
 */
@Singleton
class PedestrianWatch @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lastAlertMs = mutableMapOf<String, Long>()

    fun check(
        userLat: Double,
        userLon: Double,
        vehicles: List<Vehicle>,
        scope: CoroutineScope,
    ) {
        SelaNotifications.createChannels(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val now = System.currentTimeMillis()
        for (vehicle in vehicles) {
            val distM = EtaPredictor.haversineM(userLat, userLon, vehicle.lat, vehicle.lon)
            if (distM > NEARBY_RADIUS_M) continue
            if (now - (lastAlertMs[vehicle.id] ?: 0L) < COOLDOWN_MS) continue
            lastAlertMs[vehicle.id] = now

            val body = "Bus ${vehicle.routeId} ±${distM.roundToInt()} m darimu."
            try {
                val notification = NotificationCompat.Builder(context, SelaNotifications.CHANNEL_ALERTS)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Awas, bus dekat")
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                NotificationManagerCompat.from(context)
                    .notify("ped_${vehicle.id}".hashCode(), notification)
            } catch (_: SecurityException) {
                // Izin notifikasi dicabut di tengah jalan — lewati diam-diam.
            }
        }
    }

    companion object {
        private const val NEARBY_RADIUS_M = 100.0
        private const val COOLDOWN_MS = 5L * 60 * 1000
    }
}
