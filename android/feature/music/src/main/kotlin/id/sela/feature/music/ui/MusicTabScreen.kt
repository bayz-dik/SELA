package id.sela.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.sela.designsystem.SelaPastel
import id.sela.feature.music.data.SelaSong
import kotlinx.coroutines.launch

/**
 * Tab Musik SELA: navigasi internal (beranda/cari/pustaka + detail).
 * Navigasi antar-bagian lewat drawer hamburger — SATU navbar saja di app
 * (dock utama SELA), tidak ada navbar kedua.
 * Now Playing tampil sebagai bottom sheet penuh agar mini-player & peta tetap jalan.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTabScreen(
    viewModel: MusicViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val nav = rememberNavController()
    val navBackStack by nav.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var showNowPlaying by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val playerState by viewModel.controller.state.collectAsState()
    var favoriteIds by remember { mutableStateOf(setOf<String>()) }

    // Sinkron favorit untuk now playing
    LaunchedEffect(playerState.currentSong?.id) {
        playerState.currentSong?.let { song ->
            if (viewModel.isFavorite(song.id)) favoriteIds += song.id
            else favoriteIds -= song.id
        }
    }

    val openNowPlaying: () -> Unit = { showNowPlaying = true }
    val playAndOpen: (SelaSong) -> Unit = { song ->
        viewModel.playSong(song)
        openNowPlaying()
    }
    val playQueueAndOpen: (List<SelaSong>, Int) -> Unit = { songs, index ->
        viewModel.playQueue(songs, index)
        openNowPlaying()
    }

    val sections = listOf(
        MusicSection("Beranda", Icons.Default.Home, "music_home"),
        MusicSection("Cari", Icons.Default.Search, "music_search"),
        MusicSection("Pustaka", Icons.Default.LibraryMusic, "music_library"),
    )
    val currentSection = sections.firstOrNull {
        currentRoute == it.route || (it.route == "music_home" && (currentRoute == null || !currentRoute.startsWith("music_")))
    } ?: sections[0]
    val isTopLevel = currentRoute in setOf("music_home", "music_search", "music_library", null)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Column(
                Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .background(SelaPastel.surface)
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Text(
                        "Musik",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SelaPastel.ink,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = SelaPastel.inkSoft)
                    }
                }
                Spacer(Modifier.height(8.dp))
                sections.forEach { section ->
                    val selected = section.route == currentSection.route ||
                        (section.route == "music_home" && isTopLevel && currentSection.route == "music_home")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) SelaPastel.blueSoft else Color.Transparent)
                            .clickable {
                                nav.navigate(section.route) { launchSingleTop = true }
                                scope.launch { drawerState.close() }
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        Icon(
                            section.icon,
                            contentDescription = null,
                            tint = if (selected) SelaPastel.blueDeep else SelaPastel.inkSoft,
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            section.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) SelaPastel.blueDeep else SelaPastel.ink,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        },
        modifier = modifier,
    ) {
        Column(Modifier.fillMaxSize()) {
            // Bar atas musik: hamburger + judul bagian (bukan navbar kedua)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu musik", tint = SelaPastel.ink)
                }
                Text(
                    when {
                        currentRoute == "music_search" -> "Cari"
                        currentRoute == "music_library" -> "Pustaka"
                        else -> "Musik"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = SelaPastel.ink,
                )
            }
            Box(Modifier.weight(1f)) {
                NavHost(nav, startDestination = "music_home") {
                    composable("music_home") {
                        MusicHomeScreen(
                            viewModel = viewModel,
                            onSongClick = playAndOpen,
                        )
                    }
                    composable("music_search") {
                        MusicSearchScreen(
                            viewModel = viewModel,
                            onSongClick = playAndOpen,
                            onArtistClick = { a -> nav.navigate("music_artist/${a.id}") },
                            onAlbumClick = { a -> nav.navigate("music_album/${a.id}") },
                            onPlaylistClick = { p -> nav.navigate("music_playlist/${p.id}") },
                        )
                    }
                    composable("music_library") {
                        MusicLibraryScreen(
                            viewModel = viewModel,
                            onSongClick = playAndOpen,
                            onPlaylistClick = { p -> nav.navigate("music_local_playlist/${p.playlistId}") },
                        )
                    }
                    composable(
                        "music_artist/{artistId}",
                        arguments = listOf(navArgument("artistId") { type = NavType.StringType }),
                    ) { backStack ->
                        val artistId = backStack.arguments?.getString("artistId").orEmpty()
                        val artistName = "" // nama diambil di detail
                        ArtistDetailScreen(
                            artistId = artistId,
                            artistName = artistName,
                            viewModel = viewModel,
                            onSongClick = playAndOpen,
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "music_album/{albumId}",
                        arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
                    ) { backStack ->
                        val albumId = backStack.arguments?.getString("albumId").orEmpty()
                        AlbumDetailScreen(
                            albumId = albumId,
                            viewModel = viewModel,
                            onSongClick = playAndOpen,
                            onPlayAll = { songs -> playQueueAndOpen(songs, 0) },
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "music_playlist/{playlistId}",
                        arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
                    ) { backStack ->
                        val playlistId = backStack.arguments?.getString("playlistId").orEmpty()
                        PlaylistDetailScreen(
                            playlistId = playlistId,
                            viewModel = viewModel,
                            onSongClick = playAndOpen,
                            onPlayAll = { songs -> playQueueAndOpen(songs, 0) },
                            onBack = { nav.popBackStack() },
                        )
                    }
                    composable(
                        "music_local_playlist/{playlistId}",
                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                    ) { backStack ->
                        // Placeholder: daftar playlist lokal diambil dari library; detail disederhanakan
                        val pid = backStack.arguments?.getLong("playlistId") ?: 0L
                        LocalPlaylistDetailById(
                            playlistId = pid,
                            viewModel = viewModel,
                            onSongClick = playAndOpen,
                            onPlayAll = { songs -> playQueueAndOpen(songs, 0) },
                            onBack = { nav.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    // Now Playing sebagai bottom sheet penuh
    if (showNowPlaying && playerState.currentSong != null) {
        ModalBottomSheet(
            onDismissRequest = { showNowPlaying = false },
            sheetState = sheetState,
            containerColor = SelaPastel.background,
            dragHandle = null,
        ) {
            val song = playerState.currentSong!!
            NowPlayingScreen(
                state = playerState,
                isFavorite = song.id in favoriteIds,
                onToggleFavorite = {
                    viewModel.toggleFavorite(song)
                    favoriteIds = if (song.id in favoriteIds) favoriteIds - song.id else favoriteIds + song.id
                },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onSeekTo = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeat() },
                onSeekToIndex = { viewModel.controller.seekToIndex(it) },
                onDismiss = {
                    scope.launch { sheetState.hide() }
                    showNowPlaying = false
                },
            )
        }
    }
}

private data class MusicSection(val label: String, val icon: ImageVector, val route: String)

@Composable
private fun LocalPlaylistDetailById(
    playlistId: Long,
    viewModel: MusicViewModel = hiltViewModel(),
    onSongClick: (SelaSong) -> Unit = {},
    onPlayAll: (List<SelaSong>) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val playlists by viewModel.playlists.collectAsState()
    val playlist = playlists.firstOrNull { it.playlistId == playlistId }
    if (playlist == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Playlist tidak ditemukan", color = SelaPastel.inkSoft)
        }
    } else {
        LocalPlaylistDetailScreen(
            playlist = playlist,
            viewModel = viewModel,
            onSongClick = onSongClick,
            onPlayAll = onPlayAll,
            onBack = onBack,
        )
    }
}

