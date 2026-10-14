package id.sela.network

import android.content.Context
import android.net.TrafficStats
import android.os.Process
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pemakaian data seluler/WiFi oleh aplikasi SELA (fitur #115).
 *
 * Sumber data: [TrafficStats.getUidRxBytes]/[getUidTxBytes] untuk UID proses
 * aplikasi ini. Angka ini adalah TOTAL sejak HP dinyalakan (bukan per hari),
 * sehingga "pemakaian hari ini" dihitung dari snapshot yang disimpan saat
 * hari berganti — disimpan di SharedPreferences terpisah agar tidak
 * mengganggu [TransitPreferences] yang dimiliki batch lain.
 *
 * Keterbatasan jujur: angka adalah estimasi berbasis counter kernel; tidak
 * memisahkan WiFi vs seluler dan bisa reset saat HP restart.
 */
@Singleton
class DataUsage @Inject constructor() {

    private fun prefs(context: Context) =
        context.getSharedPreferences("sela_data_usage", Context.MODE_PRIVATE)

    private fun dayKey(): String {
        val cal = java.util.Calendar.getInstance()
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
    }

    private fun totalBytesNow(): Long {
        val uid = Process.myUid()
        val rx = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0)
        val tx = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0)
        return rx + tx
    }

    /**
     * Teks "Hari ini: X KB/MB" untuk ditampilkan di Pengaturan.
     * Menyimpan baseline harian; saat hari berganti, baseline di-reset otomatis.
     */
    fun todayUsageText(context: Context): String {
        val sp = prefs(context)
        val today = dayKey()
        val storedDay = sp.getString("day", null)
        val now = totalBytesNow()
        val baseline = if (storedDay == today) sp.getLong("baseline", now) else now
        if (storedDay != today) {
            sp.edit().putString("day", today).putLong("baseline", now).apply()
        }
        val used = (now - baseline).coerceAtLeast(0)
        return "Hari ini: ${formatBytes(used)}"
    }

    companion object {
        fun formatBytes(bytes: Long): String = when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale("id"), "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale("id"), "%.1f MB", bytes / 1024.0 / 1024.0)
        }
    }
}
