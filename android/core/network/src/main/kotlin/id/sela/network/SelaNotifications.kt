package id.sela.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import id.sela.database.NotificationHistoryEntity
import id.sela.database.TransitDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Notifikasi SELA (fitur #4, #78, #112, #113).
 *
 * - Channel "Peringatan bus" untuk notifikasi bus dekat & gangguan (bisa diatur
 *   pengguna untuk menembus Do Not Disturb lewat pengaturan sistem — fitur #113).
 * - Setiap notifikasi yang diposting dicatat ke tabel notification_history
 *   sehingga bisa dilihat lagi (fitur #112 "Notification history").
 * - [postOngoingEta]: notifikasi "ongoing" untuk ETA favorit — tampil di lock
 *   screen (VISIBILITY_PUBLIC) dan diperbarui di tempat (fitur #73).
 */
object SelaNotifications {
    const val CHANNEL_ALERTS = "sela_alerts"
    const val CHANNEL_STATUS = "sela_status"
    const val ACTION_OPEN_APP = "id.sela.action.OPEN_APP"
    /** ID tetap supaya ETA favorit selalu update notifikasi yang sama. */
    const val ONGOING_ETA_ID = 7001

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                "Peringatan bus",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Bus favorit sudah dekat & info gangguan rute" },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_STATUS,
                "Info aplikasi",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Info umum SELA" },
        )
    }

    private fun smallIcon(context: Context): Int {
        val id = context.resources.getIdentifier("ic_stat_sela", "drawable", context.packageName)
        return if (id != 0) id else android.R.drawable.ic_dialog_info
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(ACTION_OPEN_APP).setPackage(context.packageName)
        return PendingIntent.getActivity(
            context, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Posting notifikasi peringatan bus + catat ke riwayat. Mengembalikan true
     * jika berhasil diposting (izin ada).
     */
    fun postBusAlert(
        context: Context,
        dao: TransitDao,
        scope: CoroutineScope,
        title: String,
        body: String,
        kind: String = "arrival",
        notificationId: Int = System.currentTimeMillis().toInt(),
    ): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
                .setSmallIcon(smallIcon(context))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            scope.launch {
                runCatching {
                    dao.insertNotification(
                        NotificationHistoryEntity(
                            title = title, body = body, kind = kind,
                            timestamp = System.currentTimeMillis(),
                        ),
                    )
                    dao.pruneNotifications(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
                }
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Notifikasi ETA "ongoing" (fitur #73 "Lock screen ETA").
     *
     * setOngoing(true) + VISIBILITY_PUBLIC → tampil di lock screen dan tidak
     * bisa di-swipe; setOnlyAlertOnce(true) → update berkala tidak bunyi/getar
     * ulang. ID notifikasi tetap ([ONGOING_ETA_ID]) supaya tiap update
     * menimpa kartu yang sama, bukan menumpuk.
     *
     * Batasan jujur: ini padanan Android untuk "live activity" (#89). Always-On
     * Display beneran (#74) tergantung dukungan OEM/lock-screen masing-masing
     * HP — SELA tidak bisa memaksa AOD menyala.
     */
    fun postOngoingEta(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = ONGOING_ETA_ID,
    ): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return try {
            val notification = NotificationCompat.Builder(context, CHANNEL_STATUS)
                .setSmallIcon(smallIcon(context))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(openAppIntent(context))
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /** Hapus kartu ETA ongoing (mis. tidak ada lagi bus ≤15 menit). */
    fun cancelOngoingEta(context: Context, notificationId: Int = ONGOING_ETA_ID) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
