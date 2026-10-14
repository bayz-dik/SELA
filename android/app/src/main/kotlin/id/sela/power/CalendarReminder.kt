package id.sela.power

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat

/**
 * Satu acara kalender yang relevan untuk pengingat berangkat.
 *
 * @param title Judul acara.
 * @param location Lokasi acara (bisa null bila tidak diisi).
 * @param startMs Waktu mulai dalam milidetik epoch.
 */
data class CalEvent(
    val title: String,
    val location: String?,
    val startMs: Long,
)

/**
 * Pengingat berangkat dari kalender HP (fitur #57).
 *
 * [upcomingEvents] membaca [CalendarContract.Instances] untuk 1 jam ke depan,
 * diurutkan dari yang paling dekat. Hanya membaca kalender LOKAL di HP
 * pengguna — tidak ada sinkronisasi ke server SELA (SELA standalone memang
 * tidak punya server).
 *
 * WAJIB: izin runtime [Manifest.permission.READ_CALENDAR]. Tanpa izin,
 * kembalikan [emptyList] diam-diam (pemanggil bisa memakai ini untuk
 * memutuskan apakah perlu meminta izin).
 */
object CalendarReminder {
    fun upcomingEvents(context: Context): List<CalEvent> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val now = System.currentTimeMillis()
        val end = now + 3_600_000L
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let { builder ->
            ContentUris.appendId(builder, now)
            ContentUris.appendId(builder, end)
            builder.build()
        }
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
        )
        return try {
            val events = mutableListOf<CalEvent>()
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
                val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                while (cursor.moveToNext()) {
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) else null
                    if (title.isNullOrBlank()) continue
                    events += CalEvent(
                        title = title,
                        location = if (locIdx >= 0) cursor.getString(locIdx) else null,
                        startMs = if (beginIdx >= 0) cursor.getLong(beginIdx) else now,
                    )
                }
            }
            events
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /**
     * Teks pengingat siap tampil, mis.:
     * `Ada acara "Rapat" di Kantor — berangkat sekarang?`
     */
    fun reminderText(e: CalEvent): String {
        val where = if (!e.location.isNullOrBlank()) " di ${e.location}" else ""
        return "Ada acara \"${e.title}\"$where — berangkat sekarang?"
    }
}
