package id.sela.network

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.database.TransitDao
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Pengingat komuter pintar (fitur #8).
 *
 * Dipanggil sekali saat aplikasi dibuka ([SelaApplication.onCreate]). Kalau
 * [SmartCommute] menemukan pola perjalanan yang waktunya ~1 jam dari sekarang,
 * tampilkan satu notifikasi "Waktunya berangkat?". Dibatasi: maksimal 1x per
 * 4 jam per pola supaya tidak spam.
 *
 * Semua lokal — tidak ada server yang tahu jadwalmu.
 */
@Singleton
class CommuteReminder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val smartCommute: SmartCommute,
    private val dao: TransitDao,
    @TransitScope private val scope: CoroutineScope,
) {
    companion object {
        private val JAKARTA = TimeZone.getTimeZone("Asia/Jakarta")
        const val REMINDER_COOLDOWN_MS = 4 * 60 * 60 * 1000L
    }

    private val lastNotifiedAt = mutableMapOf<String, Long>()

    /**
     * Cek pola & kirim notifikasi bila ada saran. Aman dipanggil tiap app start.
     */
    suspend fun checkAndNotify(context: Context) {
        val patterns = try {
            smartCommute.learn()
        } catch (_: Exception) {
            return
        }
        val suggestion = smartCommute.suggestionFor(Calendar.getInstance(JAKARTA), patterns)
            ?: return
        val key = "${suggestion.routeId}|${suggestion.dayType}|${suggestion.hourBucket}"
        val now = System.currentTimeMillis()
        if (now - (lastNotifiedAt[key] ?: 0L) < REMINDER_COOLDOWN_MS) return
        lastNotifiedAt[key] = now

        val jam = if (suggestion.hourBucket == "pagi") "pagi" else "sore"
        val dari = suggestion.stopName?.let { " dari $it" } ?: ""
        SelaNotifications.postBusAlert(
            context = context,
            dao = dao,
            scope = scope,
            title = "Waktunya berangkat?",
            body = "Pola $jam: ${suggestion.routeName}$dari — biasanya kamu berangkat sekitar 1 jam dari sekarang. Siap-siap?",
            kind = "commute",
            notificationId = "commute-$key".hashCode(),
        )
    }
}
