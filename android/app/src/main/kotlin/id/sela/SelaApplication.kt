package id.sela

import android.app.Application
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import id.sela.network.ArrivalAlertMonitor
import id.sela.network.CommuteReminder
import id.sela.network.DataUpdateChecker
import id.sela.network.DisruptionMonitor
import id.sela.network.SelaNotifications
import id.sela.network.TimeMachine
import id.sela.power.CalendarReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SelaAppEntryPoint {
    fun dataUpdateChecker(): DataUpdateChecker
    fun arrivalAlertMonitor(): ArrivalAlertMonitor
    fun commuteReminder(): CommuteReminder
    fun disruptionMonitor(): DisruptionMonitor
    fun timeMachine(): TimeMachine
    fun transitDao(): id.sela.database.TransitDao
    fun prefs(): id.sela.network.TransitPreferences
}

@HiltAndroidApp
class SelaApplication : Application() {
    override fun onCreate() {
        // Phase logger: catat setiap tahap startup ke crash log agar bisa
        // didiagnosis jika app crash. Ditulis langsung (bukan via MediaStore
        // yang butuh context), append ke file di cache dir.
        fun phase(name: String) {
            runCatching {
                val logFile = java.io.File(cacheDir, "sela-startup.log")
                logFile.appendText("${java.util.Date()} - $name\n")
            }
        }
        phase("APP_START")
        // Crash capturer: tulis stack trace ke Download/sela-crash.log via MediaStore
        // (works di Android 10+ tanpa permission) agar bisa dibuka Bayu via file
        // manager, lalu CHAIN ke default handler agar dialog crash tetap muncul.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val trace = android.util.Log.getStackTraceString(throwable)
                // Sertakan startup phases agar tahu sejauh mana app jalan
                val phases = runCatching {
                    java.io.File(cacheDir, "sela-startup.log").readText()
                }.getOrDefault("(no phase log)")
                val info = "SELA Crash - ${java.util.Date()}\nThread: ${thread.name}\nSTARTUP PHASES:\n$phases\nSTACK:\n$trace\n---\n"
                writeCrashLog(this, info)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        phase("CRASH_HANDLER_SET")
        super.onCreate()
        phase("SUPER_ONCREATE_DONE")
        runCatching { SelaNotifications.createChannels(this) }
        phase("NOTIF_CHANNELS_DONE")
        // Tanya Sela: siapkan penyimpanan token ChatGPT terenkripsi
        runCatching { id.sela.assistant.ChatGptAuth.init(this) }
        phase("CHATGPT_AUTH_DONE")
        val entry = runCatching {
            EntryPoints.get(this, SelaAppEntryPoint::class.java)
        }.getOrNull() ?: return
        // Fitur #1: cek pembaruan data diam-diam saat app dibuka
        runCatching { entry.dataUpdateChecker().checkInBackground() }
        // Fitur #4: monitor peringatan bus dekat (jalan selama proses hidup)
        runCatching { entry.arrivalAlertMonitor().start() }
        // Fitur #14: monitor gangguan rute favorit (heuristik lokal)
        runCatching { entry.disruptionMonitor().start() }
        // Fitur #81: time machine — rekam snapshot posisi kendaraan
        runCatching { entry.timeMachine().start() }
        // Fitur #57: pengingat kalender — cek acara 1 jam ke depan saat app dibuka
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            runCatching {
                if (!entry.prefs().calendarReminders.first()) return@launch
                // B32-minor: contentResolver.query() adalah blocking I/O —
                // jangan di Main thread (risiko ANR).
                val events = withContext(Dispatchers.IO) {
                    CalendarReminder.upcomingEvents(this@SelaApplication)
                }
                val first = events.firstOrNull() ?: return@launch
                SelaNotifications.postBusAlert(
                    context = this@SelaApplication,
                    dao = entry.transitDao(),
                    scope = this,
                    title = "Acara segera mulai",
                    body = CalendarReminder.reminderText(first),
                    kind = "calendar",
                )
            }
        }
        // Fitur #8: smart commute — pengingat pola perjalanan saat app dibuka
        val reminder = entry.commuteReminder()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            runCatching { reminder.checkAndNotify(this@SelaApplication) }
        }
    }

    companion object {
        /**
         * Tulis crash log ke Download/sela-crash.log via MediaStore.
         * Works di Android 10+ (API 29+) tanpa permission apapun.
         * Untuk API < 29, fallback ke file langsung (butuh WRITE_EXTERNAL_STORAGE
         * yang sudah ada di manifest untuk versi lama).
         */
        fun writeCrashLog(context: android.content.Context, info: String) {
            runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val fileName = "sela-crash.log"
                    // Cek apakah file sudah ada — append jika ada
                    var existing = ""
                    runCatching {
                        val queryUri = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                        resolver.query(
                            queryUri,
                            arrayOf(android.provider.MediaStore.Downloads._ID),
                            "${android.provider.MediaStore.Downloads.DISPLAY_NAME} = ?",
                            arrayOf(fileName), null,
                        )?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val id = cursor.getLong(0)
                                val uri = android.content.ContentUris.withAppendedId(queryUri, id)
                                resolver.openInputStream(uri)?.use { stream ->
                                    existing = stream.bufferedReader().readText()
                                }
                                resolver.delete(uri, null, null)
                            }
                        }
                    }
                    val values = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(
                            android.provider.MediaStore.Downloads.RELATIVE_PATH,
                            android.os.Environment.DIRECTORY_DOWNLOADS,
                        )
                    }
                    val uri = resolver.insert(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values,
                    ) ?: return
                    resolver.openOutputStream(uri)?.use { stream ->
                        stream.write((existing + info).toByteArray())
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val downloads = android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_DOWNLOADS,
                    )
                    java.io.File(downloads, "sela-crash.log").appendText(info)
                }
            }
        }
    }
}
