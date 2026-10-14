package id.sela.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Kelompok riwayat perjalanan per hari untuk layar Riwayat (fitur #2).
 *
 * Fungsi murni (tanpa API Android) supaya bisa di-unit-test.
 * Label: "Hari ini", "Kemarin", atau tanggal Indonesia ("14 Okt 2026").
 */
data class HistoryDayGroup(
    val label: String,
    val entries: List<TripHistory>,
)

private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id"))

fun groupHistoryByDay(
    history: List<TripHistory>,
    nowMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<HistoryDayGroup> {
    if (history.isEmpty()) return emptyList()
    val today = LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), zoneId)
    val yesterday = today.minusDays(1)
    return history
        .groupBy { LocalDate.ofInstant(Instant.ofEpochMilli(it.timestamp), zoneId) }
        .entries
        .sortedByDescending { it.key }
        .map { (date, entries) ->
            val label = when (date) {
                today -> "Hari ini"
                yesterday -> "Kemarin"
                else -> date.format(dateFormatter)
            }
            HistoryDayGroup(label, entries.sortedByDescending { it.timestamp })
        }
}
