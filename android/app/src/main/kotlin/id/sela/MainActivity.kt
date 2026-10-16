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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import id.sela.a11y.A11y
import id.sela.ar.ArScreen
import id.sela.assistant.KonduktorScreen
import id.sela.designsystem.GlassPillNavBar
import id.sela.designsystem.SelaDockDestination
import id.sela.designsystem.SelaFloatingDock
import id.sela.designsystem.SelaPastel
import id.sela.designsystem.SelaPastelShapes
import id.sela.designsystem.SelaPastelTheme
import id.sela.designsystem.SelaV7
import id.sela.designsystem.SelaV7Theme
import id.sela.designsystem.components.V7BottomNav
import id.sela.v7.V7SplashScreen
import id.sela.v7.V7LoadingScreen
import id.sela.v7.V7LoginScreen
import id.sela.v7.V7HomeScreen
import id.sela.v7.V7RouteSearchScreen
import id.sela.v7.V7RouteDetailScreen
import id.sela.v7.V7MusicScreen
import id.sela.v7.V7AskScreen
import id.sela.v7.V7ChatMessage
import id.sela.v7.V7OtherScreen
import id.sela.v7.V7SettingsScreen
import id.sela.v7.V7ProfileScreen
import id.sela.v7.V7DrawerContent
import id.sela.feature.map.*
import id.sela.feature.music.di.MusicControllerEntryPoint
import id.sela.feature.music.playback.SelaMusicController
import id.sela.feature.music.ui.MiniPlayer
import id.sela.feature.music.ui.MusicTabScreen
import id.sela.feature.settings.HistoryScreen
import id.sela.feature.settings.OfflineMapScreen
import id.sela.feature.auth.AuthViewModel
import id.sela.feature.auth.SelaLoginScreen
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
            val map3d by prefs.map3dEnabled.collectAsState(initial = false)
            val touristMode by prefs.touristMode.collectAsState(initial = false)
            val context = LocalContext.current
            // Tema V7 "Plopp" — satu-satunya tema redesign.
            SelaV7Theme {
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
/** Satu item navigasi bawah — pil kaca, aktif = putih solid + ikon biru. */
@Composable
private fun PastelNavItem(
    route: String,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = SelaPastelShapes.pill,
        color = if (selected) Color.White.copy(alpha = 0.95f) else Color.Transparent,
        modifier = Modifier.semantics { this.selected = selected; role = Role.Tab },
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) SelaPastel.blueDeep else SelaPastel.inkSoft,
                modifier = Modifier.size(22.dp),
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) SelaPastel.ink else SelaPastel.inkSoft,
            )
        }
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
    val homeState by map.uiState.collectAsStateWithLifecycle()
    // V7: bottom nav hanya di 5 tab utama (Beranda, Peta, Musik, Tanya, Lainnya)
    val showBottomBar = currentRoute in listOf("home", "map", "music", "konduktor", "lainnya")
    // Drawer state untuk side menu V7
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // SELA Music: mini-player global (di atas nav, terlihat di semua tab utama)
    val musicControllerEntry = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            MusicControllerEntryPoint::class.java,
        )
    }
    val musicController: SelaMusicController = remember { musicControllerEntry.musicController() }
    val musicState by musicController.state.collectAsStateWithLifecycle()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            V7DrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    nav.navigate(route) {
                        launchSingleTop = true
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                        restoreState = true
                    }
                },
                onClose = { scope.launch { drawerState.close() } },
            )
        },
    ) {
    Scaffold(
        containerColor = SelaV7.background,
        bottomBar = {
            if (showBottomBar) {
                Column(Modifier.fillMaxWidth()) {
                    // Mini-player mengambang di atas nav (tap -> buka tab Musik)
                    if (musicState.currentSong != null && currentRoute != "music") {
                        MiniPlayer(
                            state = musicState,
                            onOpenNowPlaying = {
                                nav.navigate("music") { launchSingleTop = true }
                            },
                            onTogglePlayPause = { musicController.togglePlayPause() },
                            onNext = { musicController.next() },
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Box(
                        Modifier.fillMaxWidth().navigationBarsPadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                    V7BottomNav(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            nav.navigate(route) {
                                launchSingleTop = true
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                restoreState = true
                            }
                        },
                    )
                    } // Box nav
                } // Column (mini-player + nav)
            }
        },
    ) { padding ->
        // V7: Login gate — splash dulu, lalu cek session
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.uiState.collectAsStateWithLifecycle()
            // State untuk plan yang dipilih (navigasi Cari Rute -> Detail Rute)
            var selectedPlan by remember { mutableStateOf<id.sela.model.TripPlan?>(null) }
            // State chat Tanya SELA
            var chatMessages by remember {
                mutableStateOf(listOf(V7ChatMessage("Halo! Ada yang bisa saya bantu?", false)))
            }
            val startDest = "splash"
            NavHost(nav, startDestination = startDest, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable("splash") {
                V7SplashScreen(
                    onTimeout = {
                        nav.navigate(if (authState.isSignedIn) "home" else "login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    },
                )
            }
            composable("login") {
                val activity = LocalContext.current as android.app.Activity
                V7LoginScreen(
                    onGoogleSignIn = {
                        // Web Client ID dari BuildConfig; kosong = belum dikonfigurasi
                        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
                        authViewModel.signIn(activity, webClientId)
                    },
                    isLoading = authState.isLoading,
                    errorMessage = authState.errorMessage,
                )
                // Setelah login sukses, navigasi ke home
                LaunchedEffect(authState.isSignedIn) {
                    if (authState.isSignedIn) {
                        nav.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            }
            composable("home") {
                V7HomeScreen(
                    state = homeState,
                    userName = authState.user?.displayName,
                    onOpenMap = { nav.navigate("map") { launchSingleTop = true } },
                    onPlanTrip = { nav.navigate("trip-planner") { launchSingleTop = true } },
                    onOpenMusic = { nav.navigate("music") { launchSingleTop = true } },
                    onOpenAssistant = { nav.navigate("konduktor") { launchSingleTop = true } },
                    onOpenFavorites = { nav.navigate("lainnya") { launchSingleTop = true } },
                    onOpenOther = { nav.navigate("lainnya") { launchSingleTop = true } },
                )
            }
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
            // V7: Lainnya (pengganti settings sebagai tab)
            composable("lainnya") {
                V7OtherScreen(
                    onNavigate = { route ->
                        when (route) {
                            "favorit", "notifikasi" -> { /* TODO */ }
                            else -> nav.navigate(route)
                        }
                    },
                )
            }
            // V7: Pengaturan terpisah (tanpa bottom nav)
            composable("pengaturan") {
                V7SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onNavigate = { route -> nav.navigate(route) },
                    onLogout = {
                        authViewModel.signOut()
                        nav.navigate("login") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            // V7: Profil terpisah
            composable("profile") {
                V7ProfileScreen(
                    onBack = { nav.popBackStack() },
                    userName = authState.user?.displayName ?: "Pengguna SELA",
                    userEmail = authState.user?.email ?: "pengguna@gmail.com",
                    onNavigate = { route -> nav.navigate(route) },
                )
            }
            // Legacy settings (tetap untuk kompatibilitas)
            composable("settings") {
                V7SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onNavigate = { route -> nav.navigate(route) },
                    onLogout = {
                        authViewModel.signOut()
                        nav.navigate("login") {
                            popUpTo("login") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable("history") { HistoryScreen(onBack = { nav.popBackStack() }) }
            composable("peta-offline") { OfflineMapScreen(onBack = { nav.popBackStack() }) }
            // V7: Cari Rute (pengganti trip-planner)
            composable("trip-planner") {
                V7RouteSearchScreen(
                    onBack = { nav.popBackStack() },
                    onSelectPlan = { plan ->
                        selectedPlan = plan
                        nav.navigate("route-detail")
                    },
                )
            }
            // V7: Detail Rute terpisah
            composable("route-detail") {
                val plan = selectedPlan
                if (plan != null) {
                    V7RouteDetailScreen(
                        plan = plan,
                        onBack = { nav.popBackStack() },
                        onStartNavigation = {
                            nav.navigate("map") { launchSingleTop = true }
                        },
                        onSave = { /* TODO: simpan favorit */ },
                    )
                } else {
                    nav.popBackStack()
                }
            }
            composable("konduktor") {
                V7AskScreen(
                    messages = chatMessages,
                    onSend = { text ->
                        chatMessages = chatMessages + V7ChatMessage(text, true)
                        // TODO: hubungkan ke SelaAiLogic untuk jawaban AI
                        chatMessages = chatMessages + V7ChatMessage(
                            "Maaf, AI belum terhubung. Coba lagi nanti.",
                            false,
                        )
                    },
                )
            }
            composable("music") { V7MusicScreen() }
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
    } // ModalNavigationDrawer
}
