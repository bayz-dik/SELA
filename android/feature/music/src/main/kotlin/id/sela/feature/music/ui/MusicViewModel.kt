package id.sela.feature.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.sela.feature.music.data.FavoriteSongEntity
import id.sela.feature.music.data.HomeSection
import id.sela.feature.music.data.MusicDao
import id.sela.feature.music.data.MusicPlaylistEntity
import id.sela.feature.music.data.MusicRepository
import id.sela.feature.music.data.PlaylistSongEntity
import id.sela.feature.music.data.SearchHistoryEntity
import id.sela.feature.music.data.SelaAlbum
import id.sela.feature.music.data.SelaArtist
import id.sela.feature.music.data.SelaPlaylist
import id.sela.feature.music.data.SelaSong
import id.sela.feature.music.data.toFavoriteEntity
import id.sela.feature.music.data.toSelaSong
import id.sela.feature.music.playback.SelaMusicController
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicHomeUiState(
    val sections: List<HomeSection> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

data class MusicSearchUiState(
    val query: String = "",
    val suggestions: List<String> = emptyList(),
    val songs: List<SelaSong> = emptyList(),
    val artists: List<SelaArtist> = emptyList(),
    val albums: List<SelaAlbum> = emptyList(),
    val playlists: List<SelaPlaylist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class MusicViewModel @Inject constructor(
    private val repository: MusicRepository,
    private val musicDao: MusicDao,
    val controller: SelaMusicController,
) : ViewModel() {

    private val _homeState = MutableStateFlow(MusicHomeUiState())
    val homeState: StateFlow<MusicHomeUiState> = _homeState.asStateFlow()

    private val _searchState = MutableStateFlow(MusicSearchUiState())
    val searchState: StateFlow<MusicSearchUiState> = _searchState.asStateFlow()

    val favorites: StateFlow<List<SelaSong>> =
        MutableStateFlow<List<SelaSong>>(emptyList()).also { out ->
            viewModelScope.launch {
                musicDao.favorites().collect { list -> out.value = list.map { it.toSelaSong() } }
            }
        }

    val playlists: StateFlow<List<MusicPlaylistEntity>> =
        musicDao.playlists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentPlays: StateFlow<List<SelaSong>> =
        MutableStateFlow<List<SelaSong>>(emptyList()).also { out ->
            viewModelScope.launch {
                musicDao.recentPlays().collect { list -> out.value = list.map { it.toSelaSong() } }
            }
        }

    val searchHistory: StateFlow<List<String>> =
        MutableStateFlow<List<String>>(emptyList()).also { out ->
            viewModelScope.launch {
                musicDao.searchHistory().collect { list -> out.value = list.map { it.query } }
            }
        }

    private val searchQuery = MutableStateFlow("")

    init {
        loadHome()
        viewModelScope.launch {
            searchQuery
                .debounce(400)
                .distinctUntilChanged()
                .collect { q ->
                    if (q.isBlank()) {
                        _searchState.value = _searchState.value.copy(
                            suggestions = emptyList(), isLoading = false,
                        )
                    } else {
                        loadSuggestions(q)
                    }
                }
        }
    }

    fun loadHome() {
        viewModelScope.launch {
            _homeState.value = _homeState.value.copy(isLoading = true, error = null)
            repository.home()
                .onSuccess { sections ->
                    _homeState.value = MusicHomeUiState(sections = sections, isLoading = false)
                }
                .onFailure { t ->
                    _homeState.value = MusicHomeUiState(
                        isLoading = false,
                        error = "Gagal memuat rekomendasi: ${t.message}",
                    )
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _searchState.value = _searchState.value.copy(query = query)
    }

    private fun loadSuggestions(query: String) {
        viewModelScope.launch {
            repository.searchSuggestions(query)
                .onSuccess { list ->
                    _searchState.value = _searchState.value.copy(suggestions = list)
                }
        }
    }

    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            musicDao.insertSearchQuery(SearchHistoryEntity(query = q))
            _searchState.value = _searchState.value.copy(isLoading = true, error = null)
            repository.search(q)
                .onSuccess { result ->
                    _searchState.value = _searchState.value.copy(
                        songs = result.songs,
                        artists = result.artists,
                        albums = result.albums,
                        playlists = result.playlists,
                        isLoading = false,
                    )
                }
                .onFailure { t ->
                    _searchState.value = _searchState.value.copy(
                        isLoading = false,
                        error = "Pencarian gagal: ${t.message}",
                    )
                }
        }
    }

    fun clearSearch() {
        _searchState.value = MusicSearchUiState()
        searchQuery.value = ""
    }

    fun removeSearchHistory(query: String) {
        viewModelScope.launch { musicDao.deleteSearchQuery(query) }
    }

    // --- Playback ---

    fun playSong(song: SelaSong) = controller.playSong(song)
    fun playQueue(songs: List<SelaSong>, startIndex: Int = 0) = controller.playQueue(songs, startIndex)
    fun togglePlayPause() = controller.togglePlayPause()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(ms: Long) = controller.seekTo(ms)
    fun toggleShuffle() = controller.toggleShuffle()
    fun cycleRepeat() = controller.cycleRepeat()

    // --- Favorites ---

    fun toggleFavorite(song: SelaSong) {
        viewModelScope.launch {
            val isFav = musicDao.isFavorite(song.id) > 0
            if (isFav) musicDao.removeFavorite(song.id)
            else musicDao.addFavorite(song.toFavoriteEntity())
        }
    }

    suspend fun isFavorite(songId: String): Boolean = musicDao.isFavorite(songId) > 0

    // --- Playlists ---

    fun createPlaylist(name: String, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = musicDao.insertPlaylist(MusicPlaylistEntity(name = name.trim()))
            onDone(id)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            musicDao.clearPlaylistSongs(playlistId)
            musicDao.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: SelaSong) {
        viewModelScope.launch {
            val pos = musicDao.playlistSongCount(playlistId)
            musicDao.addSongToPlaylist(
                PlaylistSongEntity(
                    playlistId = playlistId,
                    songId = song.id,
                    title = song.title,
                    artistNames = song.artistNames,
                    albumName = song.albumName,
                    durationSec = song.durationSec,
                    thumbnailUrl = song.thumbnailUrl,
                    position = pos,
                ),
            )
        }
    }

    fun playlistSongs(playlistId: Long): StateFlow<List<SelaSong>> =
        MutableStateFlow<List<SelaSong>>(emptyList()).also { out ->
            viewModelScope.launch {
                musicDao.playlistSongs(playlistId).collect { list ->
                    out.value = list.map { it.toSelaSong() }
                }
            }
        }

    // --- Detail ---

    suspend fun loadArtist(browseId: String): Result<SelaArtist> = repository.artist(browseId)
    suspend fun loadAlbum(browseId: String): Result<SelaAlbum> = repository.album(browseId)
    suspend fun loadPlaylistDetail(playlistId: String): Result<SelaPlaylist> = repository.playlist(playlistId)
}
