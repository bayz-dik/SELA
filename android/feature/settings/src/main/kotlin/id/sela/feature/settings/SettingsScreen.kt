package id.sela.feature.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.sela.designsystem.*
import id.sela.model.Badge
import id.sela.model.CarbonFootprint
import id.sela.model.Gamification
import id.sela.model.NotificationItem
import id.sela.model.TimeCapsule
import id.sela.model.TripHistory
import id.sela.model.TripStats
import id.sela.model.WildStats
import id.sela.model.computeTripStats
import id.sela.model.computeWildStats
import id.sela.database.TransitDao
import id.sela.network.BackupManager
import id.sela.network.DataUsage
import id.sela.network.ProfileManager
import id.sela.network.SelaPrefs
import id.sela.network.SelaSettings
import id.sela.network.TransitPreferences
import id.sela.network.WeatherAdvisor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val history: List<TripHistory> = emptyList(),
    val allHistory: List<TripHistory> = emptyList(),
    val stats: TripStats = TripStats(0, null, null),
    val wildStats: WildStats = WildStats(0, 0.0, 0.0, 0, 0, 0L),
    val badges: Set<String> = emptySet(),
    val carbonKg: Double = 0.0,
    val timeCapsule: TripHistory? = null,
    val notifications: List<NotificationItem> = emptyList(),
    val offlineMode: Boolean = false,
    val arrivalAlerts: Boolean = false,
    val gtfsVersion: String = "",
    val gtfsUpdatedAt: Long = 0L,
    val gtfsUrl: String = "",
    val amoledDark: Boolean = false,
    val materialYou: Boolean = false,
    val map3dEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val batterySaver: Boolean = false,
    val voiceAnnounce: Boolean = false,
    val announceLang: String = "indonesia",
    val homeStopId: String = "",
    val sleepMode: Boolean = false,
    val touristMode: Boolean = false,
    val autoCheckin: Boolean = false,
    val calendarReminders: Boolean = false,
    val driverWatch: Boolean = false,
    val uiMode: String = "normal",
    val emergencyContact: String = "",
    val weatherAdvice: String? = null,
    val dataUsageText: String = "—",
    val profiles: List<id.sela.network.UserProfile> = emptyList(),
    val activeProfileId: String = "default",
    val message: String? = null,
    val busy: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SelaSettings,
    private val preferences: TransitPreferences,
    private val selaPrefs: SelaPrefs,
    private val weather: WeatherAdvisor,
    private val dataUsage: DataUsage,
    private val backupManager: BackupManager,
    private val profileManager: ProfileManager,
    private val dao: TransitDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
) : ViewModel() {
    private val mutable = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                settings.tripHistory.collect { list ->
                    val now = System.currentTimeMillis()
                    mutable.update {
                        it.copy(
                            history = list.take(30),
                            allHistory = list.take(500),
                            stats = computeTripStats(list, now),
                            wildStats = computeWildStats(list, now),
                            badges = Gamification.earnedBadges(list),
                            carbonKg = CarbonFootprint.co2SavedKg(list.size),
                            timeCapsule = TimeCapsule.aYearAgo(list, now),
                        )
                    }
                }
            }
            launch { settings.notifications.collect { list -> mutable.update { it.copy(notifications = list.take(30)) } } }
            launch { settings.offlineMode.collect { v -> mutable.update { it.copy(offlineMode = v) } } }
            launch { settings.arrivalAlerts.collect { v -> mutable.update { it.copy(arrivalAlerts = v) } } }
            launch { settings.gtfsVersion.collect { v -> mutable.update { it.copy(gtfsVersion = v) } } }
            launch { settings.gtfsUpdatedAt.collect { v -> mutable.update { it.copy(gtfsUpdatedAt = v) } } }
            launch { settings.gtfsUpdateUrl.collect { v -> mutable.update { it.copy(gtfsUrl = v) } } }
            launch { preferences.amoledDark.collect { v -> mutable.update { it.copy(amoledDark = v) } } }
            launch { preferences.materialYou.collect { v -> mutable.update { it.copy(materialYou = v) } } }
            launch { preferences.map3dEnabled.collect { v -> mutable.update { it.copy(map3dEnabled = v) } } }
            launch { preferences.hapticsEnabled.collect { v -> mutable.update { it.copy(hapticsEnabled = v) } } }
            launch { preferences.batterySaver.collect { v -> mutable.update { it.copy(batterySaver = v) } } }
            launch { preferences.voiceAnnounce.collect { v -> mutable.update { it.copy(voiceAnnounce = v) } } }
            launch { preferences.announceLang.collect { v -> mutable.update { it.copy(announceLang = v) } } }
            launch { preferences.homeStopId.collect { v -> mutable.update { it.copy(homeStopId = v) } } }
            launch { preferences.sleepMode.collect { v -> mutable.update { it.copy(sleepMode = v) } } }
            launch { preferences.touristMode.collect { v -> mutable.update { it.copy(touristMode = v) } } }
            launch { preferences.autoCheckin.collect { v -> mutable.update { it.copy(autoCheckin = v) } } }
            launch { preferences.calendarReminders.collect { v -> mutable.update { it.copy(calendarReminders = v) } } }
            launch { preferences.driverWatch.collect { v -> mutable.update { it.copy(driverWatch = v) } } }
            launch { selaPrefs.uiMode.collect { v -> mutable.update { it.copy(uiMode = v) } } }
            launch { selaPrefs.emergencyContact.collect { v -> mutable.update { it.copy(emergencyContact = v) } } }
            // Fitur #69: saran cuaca + transit (Open-Meteo, tanpa API key; null bila offline)
            launch {
                val advice = runCatching { weather.advise() }.getOrNull()
                if (advice != null) mutable.update { it.copy(weatherAdvice = advice.text) }
            }
            // Fitur #115: pemakaian data hari ini
            launch {
                val text = runCatching { dataUsage.todayUsageText(appContext) }.getOrDefault("—")
                mutable.update { it.copy(dataUsageText = text) }
            }
            // Fitur #120: profil pengguna
            launch { profileManager.profiles.collect { v -> mutable.update { it.copy(profiles = v) } } }
            launch { profileManager.activeProfileId.collect { v -> mutable.update { it.copy(activeProfileId = v) } } }
        }
    }

    fun toggleOffline() = viewModelScope.launch { runCatching { settings.toggleOfflineMode() } }
    fun toggleAlerts() = viewModelScope.launch { runCatching { settings.toggleArrivalAlerts() } }
    fun toggleAmoled() = viewModelScope.launch {
        runCatching { preferences.setAmoledDark(!mutable.value.amoledDark) }
    }
    fun toggleMaterialYou() = viewModelScope.launch {
        runCatching { preferences.setMaterialYou(!mutable.value.materialYou) }
    }
    fun toggleMap3d() = viewModelScope.launch {
        runCatching { preferences.setMap3dEnabled(!mutable.value.map3dEnabled) }
    }
    fun toggleHaptics() = viewModelScope.launch {
        runCatching { preferences.setHapticsEnabled(!mutable.value.hapticsEnabled) }
    }
    fun toggleBatterySaver() = viewModelScope.launch {
        runCatching { preferences.setBatterySaver(!mutable.value.batterySaver) }
    }
    fun toggleVoiceAnnounce() = viewModelScope.launch {
        runCatching { preferences.setVoiceAnnounce(!mutable.value.voiceAnnounce) }
    }
    fun setAnnounceLang(lang: String) = viewModelScope.launch {
        runCatching { preferences.setAnnounceLang(lang) }
    }
    fun saveHomeStopId(id: String) = viewModelScope.launch {
        runCatching { preferences.setHomeStopId(id.trim()) }
        say("Halte rumah disimpan.")
    }
    fun toggleSleepMode() = viewModelScope.launch {
        runCatching { preferences.setSleepMode(!mutable.value.sleepMode) }
    }
    fun toggleTouristMode() = viewModelScope.launch {
        runCatching { preferences.setTouristMode(!mutable.value.touristMode) }
    }
    fun toggleAutoCheckin() = viewModelScope.launch {
        runCatching { preferences.setAutoCheckin(!mutable.value.autoCheckin) }
        say("Check-in otomatis: heuristik lokasi, bukan pembayaran.")
    }
    fun toggleCalendarReminders() = viewModelScope.launch {
        runCatching { preferences.setCalendarReminders(!mutable.value.calendarReminders) }
    }
    fun toggleDriverWatch() = viewModelScope.launch {
        runCatching { preferences.setDriverWatch(!mutable.value.driverWatch) }
    }
    fun setUiMode(mode: String) = viewModelScope.launch {
        runCatching { selaPrefs.setUiMode(mode) }
        say("Mode tampilan diubah. Mulai ulang aplikasi untuk efek penuh.")
    }
    fun saveEmergencyContact(phone: String) = viewModelScope.launch {
        runCatching { selaPrefs.setEmergencyContact(phone.trim()) }
        say("Kontak darurat disimpan.")
    }
    /** Buat JSON backup (fitur #118). Dipakai bersama SAF CreateDocument di UI. */
    suspend fun buildBackupJson(): String = runCatching {
        val favs = dao.favorites().first().map { it.id to it.kind }
        val hist = dao.tripHistory(1000).map {
            TripHistory(it.routeId, it.routeName, it.stopId, it.stopName, it.timestamp)
        }
        backupManager.backupJson(favs, hist, mapOf("gtfs_version" to mutable.value.gtfsVersion))
    }.getOrDefault("")
    /** Terapkan JSON backup (fitur #119). */
    fun restoreBackupJson(json: String) = viewModelScope.launch {
        val data = runCatching { backupManager.restoreJson(json) }.getOrNull()
        if (data == null) {
            say("File backup tidak dikenali.")
            return@launch
        }
        runCatching {
            data.favorites.forEach { (favId, kind) ->
                if (dao.favoriteCount(favId, kind) == 0) dao.insertFavorite(
                    id.sela.database.FavoriteEntity(favId, kind),
                )
            }
            data.history.forEach { t ->
                dao.insertTrip(
                    id.sela.database.TripHistoryEntity(
                        routeId = t.routeId, routeName = t.routeName,
                        stopId = t.stopId, stopName = t.stopName, timestamp = t.timestamp,
                    ),
                )
            }
        }
        say("Backup dipulihkan: ${data.favorites.size} favorit, ${data.history.size} riwayat.")
    }
    fun addProfile(name: String) = viewModelScope.launch {
        runCatching { profileManager.addProfile(name.trim()) }
        say("Profil ditambahkan.")
    }
    fun removeProfile(id: String) = viewModelScope.launch {
        runCatching { profileManager.removeProfile(id) }
    }
    fun switchProfile(id: String) = viewModelScope.launch {
        runCatching { profileManager.setActiveProfile(id) }
        say("Profil diganti.")
    }
    fun clearHistory() = viewModelScope.launch {
        runCatching { settings.clearTripHistory() }
        say("Riwayat perjalanan dihapus.")
    }
    /** Ekspor riwayat ke CSV lalu bagikan (fitur #116). */
    fun exportCsv(onResult: (String) -> Unit) = viewModelScope.launch {
        val csv = runCatching { settings.exportHistoryCsv() }.getOrDefault("")
        if (csv.isBlank()) say("Riwayat kosong — tidak ada yang diekspor.")
        else onResult(csv)
    }
    fun clearNotifications() = viewModelScope.launch {
        runCatching { settings.clearNotifications() }
        say("Riwayat notifikasi dihapus.")
    }
    fun saveUrl(url: String) = viewModelScope.launch {
        runCatching { settings.setGtfsUpdateUrl(url) }
        say("URL pembaruan disimpan.")
    }
    fun checkUpdate() = viewModelScope.launch {
        mutable.value = mutable.value.copy(busy = true)
        val result = runCatching { settings.checkDataUpdate() }.getOrNull()
        mutable.value = mutable.value.copy(busy = false)
        say(result?.message ?: "Gagal memeriksa pembaruan.")
    }
    fun resetData() = viewModelScope.launch {
        val result = runCatching { settings.resetDataToBundled() }.getOrNull()
        say(result?.message ?: "Gagal mengembalikan data.")
    }
    fun dismissMessage() { mutable.value = mutable.value.copy(message = null) }
    fun say(text: String) { mutable.value = mutable.value.copy(message = text) }
}

private fun formatTime(ts: Long): String =
    if (ts <= 0) "—"
    else SimpleDateFormat("d MMM yyyy HH:mm", Locale("id")).format(Date(ts))

private fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "—"
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format(Locale("id"), "%.1f MB", bytes / 1048576.0)
}


// ---------------------------------------------------------------------------
// Layar Pengaturan — gaya Pagi Cerah (pastel)
// ---------------------------------------------------------------------------

/** Baris judul + deskripsi + sakelar pastel. */
@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = SelaPastel.inkSoft,
            )
        }
        PastelSwitch(checked = checked, onToggle = onToggle)
    }
}

/** Warna kolom teks pastel — dipakai semua OutlinedTextField di layar ini. */
@Composable
private fun pastelFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SelaPastel.blueDeep,
    unfocusedBorderColor = SelaPastel.hairline,
    focusedLabelColor = SelaPastel.blueDeep,
    unfocusedLabelColor = SelaPastel.inkSoft,
    cursorColor = SelaPastel.blueDeep,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlDraft by remember(state.gtfsUrl) { mutableStateOf(state.gtfsUrl) }
    val dbSizeText = remember {
        runCatching {
            val f = context.getDatabasePath("sela-transit.db")
            if (f.exists()) formatBytes(f.length()) else "—"
        }.getOrDefault("—")
    }
    val segColors = SegmentedButtonDefaults.colors(
        activeContainerColor = SelaPastel.blueSoft,
        activeContentColor = SelaPastel.blueDeep,
        inactiveContainerColor = Color.White,
        inactiveContentColor = SelaPastel.inkSoft,
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(SelaPastel.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Kepala
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SelaMascot(
                        expression = SelaMascotExpression.Happy,
                        diameter = 52.dp,
                        bodyColor = SelaPastel.lavender,
                        animate = false,
                    )
                    Text(
                        "Pengaturan",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SelaPastel.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                PastelRouteBadge("SELA 0.4.0", tint = SelaPastel.lavender, deep = SelaPastel.lavenderDeep)
            }

            state.message?.let { msg ->
                PastelNoticeBanner(
                    text = msg,
                    icon = Icons.Default.Info,
                    tone = PastelNoticeTone.NEUTRAL,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Cuaca + transit (#69)
            state.weatherAdvice?.let { advice ->
                PastelCard(tint = SelaPastel.blueSoft, shadowTint = SelaPastel.blueDeep) {
                    PastelSectionTitle("Cuaca")
                    Text(
                        advice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SelaPastel.inkSoft,
                    )
                }
            }

            // Tentang
            PastelCard(tint = SelaPastel.lavenderSoft, shadowTint = SelaPastel.lavenderDeep) {
                PastelSectionTitle("Tentang SELA")
                Text(
                    "SELA berjalan mandiri tanpa server. Halte & rute dari data bawaan aplikasi (bisa diperbarui manual di bawah). Posisi bus live langsung dari broker TransJakarta via MQTT; hanya muncul saat online dan dihapus dari peta setelah 90 detik tanpa pembaruan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SelaPastel.inkSoft,
                )
            }

            // Statistik perjalanan — kartu bento
            PastelCard(tint = SelaPastel.mintSoft, shadowTint = SelaPastel.mintDeep) {
                PastelSectionTitle("Perjalananmu")
                val stats = state.stats
                BentoGrid(columns = 3) {
                    cell {
                        PastelStatCell(
                            value = "${stats.tripsThisWeek}x",
                            label = "Minggu ini",
                            icon = Icons.Default.DirectionsBus,
                            tint = SelaPastel.blueSoft,
                            deep = SelaPastel.blueDeep,
                        )
                    }
                    cell {
                        PastelStatCell(
                            value = stats.topRouteName?.take(10) ?: "—",
                            label = "Rute tersering",
                            icon = Icons.Default.Route,
                            tint = SelaPastel.lavenderSoft,
                            deep = SelaPastel.lavenderDeep,
                        )
                    }
                    cell {
                        PastelStatCell(
                            value = stats.topStopName?.take(10) ?: "—",
                            label = "Halte tersering",
                            icon = Icons.Default.LocationOn,
                            tint = SelaPastel.peachSoft,
                            deep = SelaPastel.peachDeep,
                        )
                    }
                }
            }

            // Riwayat
            PastelCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PastelSectionTitle("Riwayat perjalanan")
                    if (state.history.isNotEmpty()) {
                        PastelRouteBadge(
                            "${state.history.size}",
                            tint = SelaPastel.blue,
                            deep = SelaPastel.blueDeep,
                        )
                    }
                }
                if (state.history.isEmpty()) {
                    Text(
                        "Belum ada riwayat. Pilih rute atau halte di peta untuk mulai mencatat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SelaPastel.inkSoft,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.history.take(10).forEach { h ->
                            val label = h.routeName.ifBlank { h.stopName.orEmpty() }.ifBlank { h.routeId }
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(label, style = MaterialTheme.typography.bodyMedium, color = SelaPastel.ink, maxLines = 1)
                                Text(
                                    formatTime(h.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SelaPastel.inkSoft,
                                )
                            }
                            HorizontalDivider(color = SelaPastel.hairline)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PastelTextButton("Hapus riwayat", viewModel::clearHistory, destructive = true)
                        PastelTextButton("Ekspor CSV", { viewModel.exportCsv { csv -> shareCsv(context, csv) } })
                    }
                }
            }

            // Tampilan — AMOLED (#60) & Material You (#91)
            PastelCard(tint = SelaPastel.peachSoft, shadowTint = SelaPastel.peachDeep) {
                PastelSectionTitle("Tampilan")
                ToggleRow(
                    "Hitam AMOLED",
                    "Latar hitam murni, hemat baterai layar OLED.",
                    state.amoledDark,
                    viewModel::toggleAmoled,
                )
                HorizontalDivider(color = SelaPastel.hairline)
                ToggleRow(
                    "Material You",
                    "Warna mengikuti wallpaper perangkat (Android 12+).",
                    state.materialYou,
                    viewModel::toggleMaterialYou,
                )
            }

            // Fitur lanjutan
            PastelCard(tint = SelaPastel.blueSoft, shadowTint = SelaPastel.blueDeep) {
                PastelSectionTitle("Fitur lanjutan")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelaButton(
                        text = "Riwayat perjalanan",
                        onClick = { onNavigate("history") },
                        style = SelaButtonStyle.Soft,
                        accent = SelaPastel.blue,
                        onAccent = SelaPastel.blueDeep,
                        icon = Icons.Default.History,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SelaButton(
                        text = "Peta offline",
                        onClick = { onNavigate("peta-offline") },
                        style = SelaButtonStyle.Soft,
                        accent = SelaPastel.mint,
                        onAccent = SelaPastel.mintDeep,
                        icon = Icons.Default.Download,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SelaButton(
                        text = "Perencana perjalanan",
                        onClick = { onNavigate("trip-planner") },
                        style = SelaButtonStyle.Soft,
                        accent = SelaPastel.lavender,
                        onAccent = SelaPastel.lavenderDeep,
                        icon = Icons.Default.Route,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SelaButton(
                        text = "AR halte",
                        onClick = { onNavigate("ar?mode=halte") },
                        style = SelaButtonStyle.Soft,
                        accent = SelaPastel.purple,
                        onAccent = SelaPastel.purpleDeep,
                        icon = Icons.Default.Videocam,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SelaButton(
                        text = "Berbagi ke HP sekitar",
                        onClick = { onNavigate("mesh") },
                        style = SelaButtonStyle.Soft,
                        accent = SelaPastel.peach,
                        onAccent = SelaPastel.peachDeep,
                        icon = Icons.Default.Bluetooth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SelaButton(
                        text = "SOS darurat",
                        onClick = { onNavigate("sos") },
                        style = SelaButtonStyle.Soft,
                        accent = SelaPastel.danger,
                        onAccent = SelaPastel.dangerDeep,
                        icon = Icons.Default.Warning,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Mode offline
            PastelCard {
                PastelKeyValueRow("Pemakaian data hari ini", state.dataUsageText)
                ToggleRow(
                    "Mode offline",
                    "Matikan data live (MQTT). Semua fitur kecuali posisi bus berjalan dari penyimpanan lokal, tanpa internet.",
                    state.offlineMode,
                    viewModel::toggleOffline,
                )
            }

            // Pembaruan data
            PastelCard {
                PastelSectionTitle("Pembaruan data")
                PastelKeyValueRow("Versi data", state.gtfsVersion.ifBlank { "—" })
                PastelKeyValueRow("Terakhir diperbarui", formatTime(state.gtfsUpdatedAt))
                PastelKeyValueRow("Ukuran data lokal", dbSizeText)
                OutlinedTextField(
                    value = urlDraft,
                    onValueChange = { urlDraft = it },
                    label = { Text("URL sumber pembaruan (opsional)") },
                    placeholder = { Text("https://contoh.id/gtfs") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    shape = SelaPastelShapes.field,
                    colors = pastelFieldColors(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelaButton(
                        text = "Simpan URL",
                        onClick = { viewModel.saveUrl(urlDraft) },
                        style = SelaButtonStyle.Primary,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f),
                    )
                    SelaButton(
                        text = if (state.busy) "Memeriksa…" else "Cek",
                        onClick = viewModel::checkUpdate,
                        style = SelaButtonStyle.Soft,
                        enabled = !state.busy,
                    )
                }
                PastelTextButton("Kembali ke data bawaan", viewModel::resetData)
                Text(
                    "Kosongkan URL untuk menonaktifkan pembaruan otomatis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
            }

            // Notifikasi
            PastelCard(tint = SelaPastel.pinkSoft, shadowTint = SelaPastel.pinkDeep) {
                PastelSectionTitle("Notifikasi")
                ToggleRow(
                    "Peringatkan saat bus favorit dekat",
                    "Berlaku untuk halte favorit, radius 2 km.",
                    state.arrivalAlerts,
                    viewModel::toggleAlerts,
                )
                if (state.notifications.isNotEmpty()) {
                    HorizontalDivider(color = SelaPastel.hairline)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.notifications.take(5).forEach { n ->
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(n.title, style = MaterialTheme.typography.bodyMedium, color = SelaPastel.ink, maxLines = 1)
                                Text(
                                    formatTime(n.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SelaPastel.inkSoft,
                                )
                            }
                        }
                    }
                    PastelTextButton("Hapus riwayat notifikasi", viewModel::clearNotifications, destructive = true)
                }
            }

            // Backup & restore (#118, #119) — via Storage Access Framework, tanpa server
            PastelCard(tint = SelaPastel.lavenderSoft, shadowTint = SelaPastel.lavenderDeep) {
                PastelSectionTitle("Backup & restore")
                Text(
                    "Simpan favorit + riwayat ke file JSON (bisa ke Google Drive). Tanpa server SELA.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json"),
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val json = viewModel.buildBackupJson()
                            runCatching {
                                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                            }
                            viewModel.say("Backup tersimpan.")
                        }
                    }
                }
                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument(),
                ) { uri ->
                    if (uri != null) {
                        val json = runCatching {
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
                        }.getOrDefault("")
                        if (json.isNotBlank()) viewModel.restoreBackupJson(json)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelaButton(
                        text = "Backup",
                        onClick = { exportLauncher.launch("sela-backup.json") },
                        style = SelaButtonStyle.Soft,
                        accent = SelaPastel.lavender,
                        onAccent = SelaPastel.lavenderDeep,
                        modifier = Modifier.weight(1f),
                    )
                    SelaButton(
                        text = "Restore",
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        style = SelaButtonStyle.Soft,
                        accent = SelaPastel.lavender,
                        onAccent = SelaPastel.lavenderDeep,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Profil (#120)
            PastelCard(tint = SelaPastel.blueSoft, shadowTint = SelaPastel.blueDeep) {
                PastelSectionTitle("Profil")
                Text(
                    "Satu HP untuk sekeluarga. Profil menyimpan nama + halte rumah; riwayat & favorit tetap bersama.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
                state.profiles.forEach { profile ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RadioButton(
                                selected = profile.id == state.activeProfileId,
                                onClick = { viewModel.switchProfile(profile.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = SelaPastel.blueDeep,
                                    unselectedColor = SelaPastel.inkFaint,
                                ),
                            )
                            Text(profile.name, style = MaterialTheme.typography.bodyMedium, color = SelaPastel.ink)
                        }
                        if (profile.id != "default") {
                            PastelTextButton("Hapus", { viewModel.removeProfile(profile.id) }, destructive = true)
                        }
                    }
                }
                var profileDraft by remember { mutableStateOf("") }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = profileDraft,
                        onValueChange = { profileDraft = it },
                        label = { Text("Nama profil baru") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = SelaPastelShapes.field,
                        colors = pastelFieldColors(),
                    )
                    SelaButton(
                        text = "Tambah",
                        onClick = {
                            if (profileDraft.isNotBlank()) {
                                viewModel.addProfile(profileDraft)
                                profileDraft = ""
                            }
                        },
                        style = SelaButtonStyle.Soft,
                    )
                }
            }

            // Privasi
            PastelCard(tint = SelaPastel.mintSoft, shadowTint = SelaPastel.mintDeep) {
                PastelSectionTitle("Privasi")
                Text(
                    "Lokasi hanya diminta saat Anda menekan tombol lokasi. Tidak ada pelacakan latar belakang. Favorit, riwayat, dan pengaturan hanya tersimpan di perangkat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SelaPastel.inkSoft,
                )
            }

            // Mode tampilan (#66 tunanetra, #122 lansia, #121 anak)
            PastelCard(tint = SelaPastel.peachSoft, shadowTint = SelaPastel.peachDeep) {
                PastelSectionTitle("Mode tampilan")
                Text(
                    "Lansia: huruf lebih besar. Anak: ikon lebih besar. Tunanetra: optimal untuk TalkBack + pengumuman suara.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("normal" to "Normal", "tunanetra" to "Tunanetra", "lansia" to "Lansia", "anak" to "Anak").forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = state.uiMode == value,
                            onClick = { viewModel.setUiMode(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, 4),
                            colors = segColors,
                        ) { Text(label, maxLines = 1) }
                    }
                }
            }

            // Suara & bahasa daerah (#56, #22)
            PastelCard(tint = SelaPastel.lavenderSoft, shadowTint = SelaPastel.lavenderDeep) {
                PastelSectionTitle("Suara & bahasa")
                ToggleRow(
                    "Pengumuman suara halte",
                    "Bacakan halte berikutnya (suara kondektur).",
                    state.voiceAnnounce,
                    viewModel::toggleVoiceAnnounce,
                )
                HorizontalDivider(color = SelaPastel.hairline)
                Text("Bahasa pengumuman", style = MaterialTheme.typography.titleSmall, color = SelaPastel.ink)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("indonesia" to "Indonesia", "jawa" to "Jawa", "sunda" to "Sunda", "betawi" to "Betawi").forEachIndexed { i, (value, label) ->
                        SegmentedButton(
                            selected = state.announceLang == value,
                            onClick = { viewModel.setAnnounceLang(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, 4),
                            colors = segColors,
                        ) { Text(label, maxLines = 1) }
                    }
                }
            }

            // Kenyamanan (#93, #55, #70, #58, #51)
            PastelCard(tint = SelaPastel.mintSoft, shadowTint = SelaPastel.mintDeep) {
                PastelSectionTitle("Kenyamanan")
                ToggleRow("Getaran cerdas", "Pola getar beda tiap kejadian.", state.hapticsEnabled, viewModel::toggleHaptics)
                HorizontalDivider(color = SelaPastel.hairline)
                ToggleRow("Hemat baterai", "Kurangi frekuensi lokasi & sinkron.", state.batterySaver, viewModel::toggleBatterySaver)
                HorizontalDivider(color = SelaPastel.hairline)
                ToggleRow("Mode tidur", "Ketiduran di bus: getar kuat + layar penuh saat halte dekat.", state.sleepMode, viewModel::toggleSleepMode)
                HorizontalDivider(color = SelaPastel.hairline)
                ToggleRow("Mode turis", "Tampilkan info wisata di halte.", state.touristMode, viewModel::toggleTouristMode)
                HorizontalDivider(color = SelaPastel.hairline)
                ToggleRow("Gedung 3D", "Tampilkan gedung 3D di peta (zoom dekat).", state.map3dEnabled, viewModel::toggleMap3d)
            }

            // Otomatisasi (#63, #57, #79, #53)
            PastelCard(tint = SelaPastel.blueSoft, shadowTint = SelaPastel.blueDeep) {
                PastelSectionTitle("Otomatisasi")
                Text(
                    "Check-in otomatis hanya heuristik lokasi — bukan pembayaran dan bukan bukti naik bus.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
                ToggleRow(
                    "Check-in otomatis",
                    "Catat perjalanan saat terdeteksi di dekat bus.",
                    state.autoCheckin,
                    viewModel::toggleAutoCheckin,
                )
                HorizontalDivider(color = SelaPastel.hairline)
                ToggleRow(
                    "Pengingat kalender",
                    "Ingatkan berangkat sebelum acara (butuh izin kalender).",
                    state.calendarReminders,
                    viewModel::toggleCalendarReminders,
                )
                HorizontalDivider(color = SelaPastel.hairline)
                ToggleRow(
                    "Waspada pejalan kaki",
                    "Peringatkan bila bus sangat dekat dengan posisimu.",
                    state.driverWatch,
                    viewModel::toggleDriverWatch,
                )
                HorizontalDivider(color = SelaPastel.hairline)
                var homeDraft by remember(state.homeStopId) { mutableStateOf(state.homeStopId) }
                OutlinedTextField(
                    value = homeDraft,
                    onValueChange = { homeDraft = it },
                    label = { Text("ID halte rumah (untuk tombol Pulang)") },
                    placeholder = { Text("cth: monas") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SelaPastelShapes.field,
                    colors = pastelFieldColors(),
                )
                SelaButton(
                    text = "Simpan halte rumah",
                    onClick = { viewModel.saveHomeStopId(homeDraft) },
                    style = SelaButtonStyle.Soft,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Darurat (#21, #59, #123)
            PastelCard(tint = SelaPastel.dangerSoft, shadowTint = SelaPastel.dangerDeep) {
                PastelSectionTitle("Darurat")
                var phoneDraft by remember(state.emergencyContact) { mutableStateOf(state.emergencyContact) }
                OutlinedTextField(
                    value = phoneDraft,
                    onValueChange = { phoneDraft = it },
                    label = { Text("Kontak darurat (nomor WhatsApp)") },
                    placeholder = { Text("cth: 08123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = SelaPastelShapes.field,
                    colors = pastelFieldColors(),
                )
                SelaButton(
                    text = "Simpan kontak",
                    onClick = { viewModel.saveEmergencyContact(phoneDraft) },
                    style = SelaButtonStyle.Soft,
                    accent = SelaPastel.danger,
                    onAccent = SelaPastel.dangerDeep,
                    modifier = Modifier.fillMaxWidth(),
                )
                SelaButton(
                    text = "Buka layar SOS",
                    onClick = { onNavigate("sos") },
                    style = SelaButtonStyle.Soft,
                    accent = SelaPastel.danger,
                    onAccent = SelaPastel.dangerDeep,
                    icon = Icons.Default.Warning,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "SOS mengirim lokasi + info rute via WhatsApp. Goyang 3x juga bisa memicu SOS (aktifkan di layar SOS).",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkSoft,
                )
            }

            // Kaki
            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "Peta: © OpenFreeMap, © OpenMapTiles, © OpenStreetMap contributors",
                    style = MaterialTheme.typography.bodySmall,
                    color = SelaPastel.inkFaint,
                )
                Text(
                    "SELA 0.4.0 · Android · Standalone",
                    style = MaterialTheme.typography.labelMedium,
                    color = SelaPastel.inkSoft,
                )
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
