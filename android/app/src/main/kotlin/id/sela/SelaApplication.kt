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
        // DEBUG SEMENTARA — crash capturer: tulis stack trace ke folder Download
        // publik (Download/sela-crash.log) agar bisa dibuka Bayu via file manager
        // tanpa root/Shizuku, lalu CHAIN ke default handler agar dialog crash tetap muncul.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val trace = android.util.Log.getStackTraceString(throwable)
                val info = "SELA Crash - ${java.util.Date()}\nThread: ${thread.name}\n$trace\n---\n"
                val downloads = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS)
                java.io.File(downloads, "sela-crash.log").appendText(info)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        super.onCreate()
        runCatching { SelaNotifications.createChannels(this) }
        // Tanya Sela: siapkan penyimpanan token ChatGPT terenkripsi
        runCatching { id.sela.assistant.ChatGptAuth.init(this) }
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
                val events = CalendarReminder.upcomingEvents(this@SelaApplication)
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
}
