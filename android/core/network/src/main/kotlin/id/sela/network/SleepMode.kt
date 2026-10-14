package id.sela.network

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mode tidur (fitur #70): bila aktif, peringatan "bus dekat" dikirim sebagai
 * notifikasi full-screen dengan getaran kuat — cocok saat pengguna ketiduran
 * di bus dan harus bangun di halte tujuan.
 *
 * Dipakai oleh ArrivalAlertMonitor: ganti pemanggilan postBusAlert biasa
 * dengan [postSleepAlert] bila prefs.sleepMode aktif (lihat WIRING).
 *
 * KDoc jujur: full-screen intent hanya benar-benar full-screen bila HP
 * terkunci; bila HP sedang dipakai, tampil sebagai heads-up biasa.
 * Getaran kuat mengikuti pola SOS SelaHaptics (modul :app).
 */
@Singleton
class SleepMode @Inject constructor() {

    fun postSleepAlert(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = 7001,
    ): Boolean {
        SelaNotifications.createChannels(context)
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
            val fullScreen = PendingIntent.getActivity(
                context, 1, launch,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val smallIcon = context.resources
                .getIdentifier("ic_stat_sela", "drawable", context.packageName)
                .takeIf { it != 0 } ?: android.R.drawable.ic_dialog_alert
            val notification: Notification = NotificationCompat.Builder(context, SelaNotifications.CHANNEL_ALERTS)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(fullScreen)
                .setFullScreenIntent(fullScreen, true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 800, 300, 800))
                .build()
            val manager = context.getSystemService(android.app.NotificationManager::class.java)
            manager.notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
