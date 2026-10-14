package id.sela.network

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.model.ServiceType
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.transitPreferences by preferencesDataStore(name = "sela_transit")

/**
 * Preferensi lokal SELA standalone. Semua tersimpan di perangkat (DataStore),
 * tidak ada yang dikirim ke server mana pun.
 */
@Singleton
class TransitPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val filterKey = stringPreferencesKey("service_filter")
    private val offlineKey = booleanPreferencesKey("offline_mode")
    private val gtfsUrlKey = stringPreferencesKey("gtfs_update_url")
    private val gtfsVersionKey = stringPreferencesKey("gtfs_version")
    private val gtfsUpdatedAtKey = longPreferencesKey("gtfs_updated_at")
    private val alertsKey = booleanPreferencesKey("arrival_alerts")
    private val widgetRouteKey = stringPreferencesKey("widget_route_filter")
    private val widgetCountKey = intPreferencesKey("widget_vehicle_count")
    private val widgetStatusKey = stringPreferencesKey("widget_status")
    private val widgetSyncedKey = longPreferencesKey("widget_synced_at")
    private val amoledKey = booleanPreferencesKey("amoled_dark")
    private val materialYouKey = booleanPreferencesKey("material_you")
    // Key batch lain (pola sama seperti amoledDark)
    private val map3dKey = booleanPreferencesKey("map3d_enabled") // #51
    private val hapticsKey = booleanPreferencesKey("haptics_enabled") // #93
    private val batterySaverKey = booleanPreferencesKey("battery_saver") // #55
    private val voiceAnnounceKey = booleanPreferencesKey("voice_announce") // #56
    private val announceLangKey = stringPreferencesKey("announce_lang") // #22
    private val homeStopKey = stringPreferencesKey("home_stop_id") // #53
    private val sleepModeKey = booleanPreferencesKey("sleep_mode") // #70
    private val touristModeKey = booleanPreferencesKey("tourist_mode") // #58
    private val autoCheckinKey = booleanPreferencesKey("auto_checkin") // #63
    private val calendarRemindersKey = booleanPreferencesKey("calendar_reminders") // #57
    private val driverWatchKey = booleanPreferencesKey("driver_watch") // #79

    private val data = context.transitPreferences.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val filter = data.map { prefs ->
        prefs[filterKey]?.let { runCatching { ServiceType.valueOf(it) }.getOrNull() }
    }.distinctUntilChanged()

    /** Mode offline penuh (fitur #5): matikan MQTT, semua data dari lokal. */
    val offlineMode = data.map { it[offlineKey] ?: false }.distinctUntilChanged()

    /** URL sumber pembaruan data GTFS (fitur #1). Kosong = pembaruan otomatis mati. */
    val gtfsUpdateUrl = data.map { it[gtfsUrlKey].orEmpty() }.distinctUntilChanged()
    val gtfsVersion = data.map { it[gtfsVersionKey] ?: "bundled-2026-10-14" }.distinctUntilChanged()
    val gtfsUpdatedAt = data.map { it[gtfsUpdatedAtKey] ?: 0L }.distinctUntilChanged()

    /** Peringatan "bus dekat" untuk halte/rute favorit (fitur #4). */
    val arrivalAlertsEnabled = data.map { it[alertsKey] ?: false }.distinctUntilChanged()

    /** Filter rute yang ditampilkan widget (fitur #111). null = semua. */
    val widgetRouteFilter = data.map { it[widgetRouteKey] }.distinctUntilChanged()

    // Snapshot ringan untuk widget (diperbarui repository, dibatasi frekuensinya)
    val widgetVehicleCount = data.map { it[widgetCountKey] ?: 0 }.distinctUntilChanged()
    val widgetStatus = data.map { it[widgetStatusKey] ?: "OFFLINE" }.distinctUntilChanged()
    val widgetSyncedAt = data.map { it[widgetSyncedKey] ?: 0L }.distinctUntilChanged()

    suspend fun setFilter(filter: ServiceType?) {
        context.transitPreferences.edit { prefs ->
            if (filter == null) prefs.remove(filterKey) else prefs[filterKey] = filter.name
        }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.transitPreferences.edit { it[offlineKey] = enabled }
    }

    suspend fun setGtfsUpdateUrl(url: String) {
        context.transitPreferences.edit { it[gtfsUrlKey] = url.trim() }
    }

    suspend fun setGtfsVersion(version: String, updatedAt: Long) {
        context.transitPreferences.edit { it[gtfsVersionKey] = version; it[gtfsUpdatedAtKey] = updatedAt }
    }

    suspend fun setArrivalAlertsEnabled(enabled: Boolean) {
        context.transitPreferences.edit { it[alertsKey] = enabled }
    }

    suspend fun setAmoledDark(enabled: Boolean) {
        context.transitPreferences.edit { it[amoledKey] = enabled }
    }

    suspend fun setMaterialYou(enabled: Boolean) {
        context.transitPreferences.edit { it[materialYouKey] = enabled }
    }

    /** Tampilan peta 3D (fitur #51). Default mati. */
    val map3dEnabled = data.map { it[map3dKey] ?: false }.distinctUntilChanged()
    suspend fun setMap3dEnabled(enabled: Boolean) {
        context.transitPreferences.edit { it[map3dKey] = enabled }
    }

    /** Pola getar haptic (fitur #93). Default nyala. */
    val hapticsEnabled = data.map { it[hapticsKey] ?: true }.distinctUntilChanged()
    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.transitPreferences.edit { it[hapticsKey] = enabled }
    }

    /** Mode hemat baterai (fitur #55). Default mati. */
    val batterySaver = data.map { it[batterySaverKey] ?: false }.distinctUntilChanged()
    suspend fun setBatterySaver(enabled: Boolean) {
        context.transitPreferences.edit { it[batterySaverKey] = enabled }
    }

    /** Pengumuman suara halte (fitur #56). Default mati. */
    val voiceAnnounce = data.map { it[voiceAnnounceKey] ?: false }.distinctUntilChanged()
    suspend fun setVoiceAnnounce(enabled: Boolean) {
        context.transitPreferences.edit { it[voiceAnnounceKey] = enabled }
    }

    /** Bahasa pengumuman suara (fitur #22). Default "indonesia". */
    val announceLang = data.map { it[announceLangKey] ?: "indonesia" }.distinctUntilChanged()
    suspend fun setAnnounceLang(lang: String) {
        context.transitPreferences.edit { it[announceLangKey] = lang }
    }

    /** Halte rumah untuk commute pintar (fitur #53). Kosong = belum diset. */
    val homeStopId = data.map { it[homeStopKey].orEmpty() }.distinctUntilChanged()
    suspend fun setHomeStopId(stopId: String) {
        context.transitPreferences.edit { it[homeStopKey] = stopId }
    }

    /** Mode tidur: redupkan UI & bisukan notifikasi (fitur #70). Default mati. */
    val sleepMode = data.map { it[sleepModeKey] ?: false }.distinctUntilChanged()
    suspend fun setSleepMode(enabled: Boolean) {
        context.transitPreferences.edit { it[sleepModeKey] = enabled }
    }

    /** Mode turis (fitur #58). Default mati. */
    val touristMode = data.map { it[touristModeKey] ?: false }.distinctUntilChanged()
    suspend fun setTouristMode(enabled: Boolean) {
        context.transitPreferences.edit { it[touristModeKey] = enabled }
    }

    /** Check-in otomatis di halte (fitur #63). Default mati. */
    val autoCheckin = data.map { it[autoCheckinKey] ?: false }.distinctUntilChanged()
    suspend fun setAutoCheckin(enabled: Boolean) {
        context.transitPreferences.edit { it[autoCheckinKey] = enabled }
    }

    /** Pengingat dari kalender (fitur #57). Default mati. */
    val calendarReminders = data.map { it[calendarRemindersKey] ?: false }.distinctUntilChanged()
    suspend fun setCalendarReminders(enabled: Boolean) {
        context.transitPreferences.edit { it[calendarRemindersKey] = enabled }
    }

    /** Pantau perilaku sopir (fitur #79). Default mati. */
    val driverWatch = data.map { it[driverWatchKey] ?: false }.distinctUntilChanged()
    suspend fun setDriverWatch(enabled: Boolean) {
        context.transitPreferences.edit { it[driverWatchKey] = enabled }
    }

    suspend fun setWidgetRouteFilter(routeId: String?) {
        context.transitPreferences.edit {
            if (routeId == null) it.remove(widgetRouteKey) else it[widgetRouteKey] = routeId
        }
    }

    suspend fun updateWidgetSnapshot(vehicleCount: Int, status: String, syncedAt: Long) {
        context.transitPreferences.edit {
            it[widgetCountKey] = vehicleCount
            it[widgetStatusKey] = status
            it[widgetSyncedKey] = syncedAt
        }
    }

    /** Hitam murni untuk layar AMOLED (fitur #60). Default mati. */
    val amoledDark = data.map { it[amoledKey] ?: false }.distinctUntilChanged()

    /** Warna dinamis Material You dari wallpaper, Android 12+ (fitur #91). Default mati. */
    val materialYou = data.map { it[materialYouKey] ?: false }.distinctUntilChanged()

    /** Baca sekali untuk kebutuhan sinkron (mis. widget). */
    suspend fun snapshot(): Map<String, Any?> {
        val p = data.first()
        return mapOf(
            "offlineMode" to (p[offlineKey] ?: false),
            "gtfsVersion" to (p[gtfsVersionKey] ?: "bundled-2026-10-14"),
            "gtfsUpdatedAt" to (p[gtfsUpdatedAtKey] ?: 0L),
            "arrivalAlerts" to (p[alertsKey] ?: false),
            "amoledDark" to (p[amoledKey] ?: false),
            "materialYou" to (p[materialYouKey] ?: false),
            "map3dEnabled" to (p[map3dKey] ?: false),
            "hapticsEnabled" to (p[hapticsKey] ?: true),
            "batterySaver" to (p[batterySaverKey] ?: false),
            "voiceAnnounce" to (p[voiceAnnounceKey] ?: false),
            "announceLang" to (p[announceLangKey] ?: "indonesia"),
            "homeStopId" to (p[homeStopKey].orEmpty()),
            "sleepMode" to (p[sleepModeKey] ?: false),
            "touristMode" to (p[touristModeKey] ?: false),
            "autoCheckin" to (p[autoCheckinKey] ?: false),
            "calendarReminders" to (p[calendarRemindersKey] ?: false),
            "driverWatch" to (p[driverWatchKey] ?: false),
            "widgetRouteFilter" to p[widgetRouteKey],
            "widgetVehicleCount" to (p[widgetCountKey] ?: 0),
            "widgetStatus" to (p[widgetStatusKey] ?: "OFFLINE"),
            "widgetSyncedAt" to (p[widgetSyncedKey] ?: 0L),
        )
    }
}
