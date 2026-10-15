package id.sela.feature.music.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val selectedTab = when (navBackStack?.destination?.route) {
        "music_search" -> 1
        "music_library" -> 2
        else -> 0
    }
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

    Column(modifier = modifier.fillMaxSize()) {
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
        // Sub-navigasi musik (di atas dock utama SELA)
        NavigationBar(
            containerColor = SelaPastel.surface,
            tonalElevation = 0.dp,
        ) {
            val items = listOf(
                Triple("Beranda", Icons.Default.Home, "music_home"),
                Triple("Cari", Icons.Default.Search, "music_search"),
                Triple("Pustaka", Icons.Default.LibraryMusic, "music_library"),
            )
            items.forEachIndexed { index, (label, icon, route) ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = {
                        nav.navigate(route) { launchSingleTop = true }
                    },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SelaPastel.blueDeep,
                        selectedTextColor = SelaPastel.blueDeep,
                        unselectedIconColor = SelaPastel.inkFaint,
                        unselectedTextColor = SelaPastel.inkFaint,
                        indicatorColor = SelaPastel.blueSoft,
                    ),
                )
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

