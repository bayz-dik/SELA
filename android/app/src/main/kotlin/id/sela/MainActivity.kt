package id.sela

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import id.sela.a11y.A11y
import id.sela.ar.ArScreen
import id.sela.assistant.KonduktorScreen
import id.sela.designsystem.SelaTheme
import id.sela.feature.map.*
import id.sela.feature.settings.HistoryScreen
import id.sela.feature.settings.OfflineMapScreen
import id.sela.feature.settings.SettingsScreen
import id.sela.feature.settings.TripPlannerScreen
import id.sela.mesh.MeshScreen
import id.sela.network.SelaPrefs
import id.sela.network.TransitPreferences
import id.sela.power.GoHomeFab
import id.sela.power.GoHomeHelper
import id.sela.power.KonvoiShare
import id.sela.power.TouristCard
import id.sela.sos.SosScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ThemePrefsEntryPoint {
    fun prefs(): TransitPreferences
    fun selaPrefs(): SelaPrefs
}

@AndroidEntryPoint class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionOnce()
        // Fitur #66/#121/#122: terapkan mode aksesibilitas (lansia/anak/tunanetra)
        // Non-blocking: hanya recreate jika fontScale belum sesuai (cegah infinite loop)
        lifecycleScope.launch {
            runCatching {
                val entry = EntryPointAccessors.fromApplication(
                    applicationContext, ThemePrefsEntryPoint::class.java)
                val mode = entry.selaPrefs().uiMode.first()
                val targetScale = when (mode) {
                    SelaPrefs.MODE_LANSIA -> 1.35f
                    SelaPrefs.MODE_ANAK -> 1.15f
                    else -> 1.0f
                }
                if (resources.configuration.fontScale != targetScale) {
                    A11y.applyUiMode(this@MainActivity, mode)
                    recreate()
                }
            }
        }
        setContent {
            val prefs = remember {
                EntryPointAccessors.fromApplication(
                    applicationContext, ThemePrefsEntryPoint::class.java).prefs()
            }
            val amoled by prefs.amoledDark.collectAsState(initial = false)
            val materialYou by prefs.materialYou.collectAsState(initial = false)
            val map3d by prefs.map3dEnabled.collectAsState(initial = false)
            val touristMode by prefs.touristMode.collectAsState(initial = false)
            val context = LocalContext.current
            SelaTheme(amoled = amoled, dynamicColor = materialYou) {
                SelaScaffold(
                    show3dBuildings = map3d,
                    stopExtras = StopSheetExtras(
                        showTourist = touristMode,
                        touristContent = { TouristCard() },
                        onKonvoi = { est ->
                            val text = KonvoiShare.shareText(
                                routeName = est.routeId,
                                stopName = "halte",
                                etaMinutes = est.etaMinutes.roundToInt(),
                            )
                            context.startActivity(
                                android.content.Intent.createChooser(
                                    KonvoiShare.shareIntent(text), "Ajak bareng via",
                                ),
                            )
                        },
                    ),
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Fitur #77: app shortcuts — simpan aksi untuk dibaca SelaScaffold
        intent.getStringExtra("sela_action")?.let { action ->
            getSharedPreferences("sela_prefs", MODE_PRIVATE).edit()
                .putString("pending_shortcut_action", action).apply()
        }
    }

    /** Minta izin notifikasi sekali saja (Android 13+), untuk peringatan bus dekat. */
    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val prefs = getSharedPreferences("sela_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("notif_perm_asked", false)) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            prefs.edit().putBoolean("notif_perm_asked", true).apply()
            return
        }
        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            prefs.edit().putBoolean("notif_perm_asked", true).apply()
        }
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
@Composable fun SelaScaffold(
    show3dBuildings: Boolean = false,
    stopExtras: StopSheetExtras = StopSheetExtras(),
) {
    val nav = rememberNavController()
    val map: MapViewModel = hiltViewModel()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { map.runRealtime() }
    }
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // Fitur #77: tangani app shortcut yang dikirim via intent
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(currentRoute) {
        val action = context.getSharedPreferences("sela_prefs", android.content.Context.MODE_PRIVATE)
            .getString("pending_shortcut_action", null)
        if (action != null) {
            context.getSharedPreferences("sela_prefs", android.content.Context.MODE_PRIVATE)
                .edit().remove("pending_shortcut_action").apply()
            when (action) {
                "halte_terdekat", "favorit", "cari_rute" -> nav.navigate("map") { launchSingleTop = true }
            }
        }
    }
    val showBottomBar = currentRoute in listOf("map", "history", "trip-planner", "konduktor", "settings")
    Scaffold(bottomBar = {
        if (showBottomBar) {
        NavigationBar {
            listOf(
                Triple("map", "Peta", Icons.Default.Map),
                Triple("history", "Riwayat", Icons.Default.History),
                Triple("trip-planner", "Rencana", Icons.Default.Route),
                Triple("konduktor", "Kondektur", Icons.Default.Chat),
                Triple("settings", "Pengaturan", Icons.Default.Settings),
            ).forEach { (route, label, icon) ->
                NavigationBarItem(
                    selected = currentRoute == route,
                    onClick = { nav.navigate(route) { launchSingleTop = true; popUpTo(nav.graph.startDestinationId) { saveState = true }; restoreState = true } },
                    icon = { Icon(icon, contentDescription = null) },
                    label = { Text(label) },
                )
            }
        }
        }
    }) { padding ->
        NavHost(nav, startDestination = "map", modifier = Modifier.fillMaxSize().padding(padding)) {
            composable("map") {
                Box(Modifier.fillMaxSize()) {
                    MapScreen(map, show3dBuildings = show3dBuildings, stopExtras = stopExtras)
                    // Fitur #10 (suara), #76 (mini/PiP), #53 (pulang) — overlay modul app
                    MapActionOverlays(
                        viewModel = map,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }
            composable("settings") { SettingsScreen(onNavigate = { route -> nav.navigate(route) }) }
            composable("history") { HistoryScreen(onBack = { nav.popBackStack() }) }
            composable("peta-offline") { OfflineMapScreen(onBack = { nav.popBackStack() }) }
            composable("trip-planner") { TripPlannerScreen(onBack = { nav.popBackStack() }) }
            composable("konduktor") { KonduktorScreen() }
            composable("sos") { SosScreen() }
            composable("mesh") { MeshScreen() }
            composable(
                "ar?mode={mode}&targetStopId={targetStopId}",
                arguments = listOf(
                    androidx.navigation.navArgument("mode") { defaultValue = "halte" },
                    androidx.navigation.navArgument("targetStopId") { nullable = true; defaultValue = null },
                ),
            ) { entry ->
                ArScreen(
                    mode = entry.arguments?.getString("mode") ?: "halte",
                    targetStopId = entry.arguments?.getString("targetStopId"),
                )
            }
        }
    }
}
