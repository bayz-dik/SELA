package id.sela.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.room.Room
import id.sela.R
import id.sela.database.TransitDatabase
import id.sela.network.TransitPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Widget home screen SELA (fitur #95, #111).
 *
 * Menampilkan status koneksi live, jumlah bus aktif, dan jumlah favorit —
 * tanpa membuka aplikasi. Tombol ‹ › mengganti filter rute widget tanpa
 * membuka app (fitur #111 "app widget interaktif").
 */
class SelaWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV_ROUTE = "id.sela.widget.PREV_ROUTE"
        const val ACTION_NEXT_ROUTE = "id.sela.widget.NEXT_ROUTE"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, SelaWidgetProvider::class.java),
            )
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        setPackage(context.packageName)
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    },
                )
            }
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        scope.launch {
            try {
                val prefs = TransitPreferences(context.applicationContext)
                val snap = prefs.snapshot()
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    TransitDatabase::class.java,
                    "sela-transit.db",
                ).build()
                val favorites = runCatching { db.transitDao().favoriteCountAll() }.getOrDefault(0)
                val routes = runCatching { db.transitDao().routes() }.getOrDefault(emptyList())
                db.close()

                val status = (snap["widgetStatus"] as? String) ?: "OFFLINE"
                val count = (snap["widgetVehicleCount"] as? Int) ?: 0
                val syncedAt = (snap["widgetSyncedAt"] as? Long) ?: 0L
                val routeFilter = snap["widgetRouteFilter"] as? String
                val routeLabel = routeFilter?.let { id ->
                    routes.find { it.id == id }?.shortName ?: id
                } ?: "Semua rute"

                val statusText = when (status) {
                    "LIVE" -> "Live"
                    "CONNECTING" -> "Menghubungkan…"
                    "RECONNECTING" -> "Menyambung ulang…"
                    "ERROR" -> "Akses ditolak"
                    else -> "Offline"
                }
                val timeText = if (syncedAt > 0) {
                    "Update " + SimpleDateFormat("HH:mm", Locale("id")).format(Date(syncedAt))
                } else {
                    "Belum ada data live"
                }

                // Fitur #114: info baterai HP (BUKAN konsumsi per-app — Android tidak
                // menyediakan API publik konsumsi baterai per aplikasi; label jujur).
                val batteryText = runCatching {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
                    val pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    if (pct >= 0) "Baterai HP: $pct%" else ""
                }.getOrDefault("")

                for (id in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.sela_widget).apply {
                        setTextViewText(R.id.widget_status, statusText)
                        setTextViewText(R.id.widget_vehicles, "$count bus aktif")
                        setTextViewText(R.id.widget_favorites, "$favorites favorit")
                        setTextViewText(R.id.widget_route, routeLabel)
                        setTextViewText(R.id.widget_updated, timeText)
                        setTextViewText(R.id.widget_battery, batteryText)
                        setOnClickPendingIntent(R.id.widget_root, openApp(context))
                        setOnClickPendingIntent(R.id.widget_prev, actionIntent(context, ACTION_PREV_ROUTE))
                        setOnClickPendingIntent(R.id.widget_next, actionIntent(context, ACTION_NEXT_ROUTE))
                    }
                    manager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_PREV_ROUTE, ACTION_NEXT_ROUTE -> {
                val pending = goAsync()
                scope.launch {
                    try {
                        cycleRouteFilter(context, intent.action == ACTION_NEXT_ROUTE)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private suspend fun cycleRouteFilter(context: Context, forward: Boolean) {
        val prefs = TransitPreferences(context.applicationContext)
        val db = Room.databaseBuilder(
            context.applicationContext, TransitDatabase::class.java, "sela-transit.db",
        ).build()
        try {
            val routes = db.transitDao().routes().map { it.id }
            val current = prefs.snapshot()["widgetRouteFilter"] as? String
            val next = when {
                routes.isEmpty() -> null
                current == null -> routes.firstOrNull()
                else -> {
                    val idx = routes.indexOf(current)
                    if (idx < 0) routes.firstOrNull()
                    else {
                        val n = if (forward) idx + 1 else idx - 1
                        if (n < 0 || n >= routes.size) null else routes[n]
                    }
                }
            }
            prefs.setWidgetRouteFilter(next)
        } finally {
            db.close()
        }
        refreshAll(context)
    }

    private fun openApp(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(Intent.ACTION_MAIN).setPackage(context.packageName)
        return PendingIntent.getActivity(
            context, 0, launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, SelaWidgetProvider::class.java).setAction(action)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, action.hashCode(), intent, flags)
    }
}
