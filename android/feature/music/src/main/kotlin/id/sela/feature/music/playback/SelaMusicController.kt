package id.sela.feature.music.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import id.sela.feature.music.data.MusicDao
import id.sela.feature.music.data.MusicRepository
import id.sela.feature.music.data.PlayHistoryEntity
import id.sela.feature.music.data.SelaSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Status playback untuk UI. */
data class MusicPlayerState(
    val currentSong: SelaSong? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleOn: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<SelaSong> = emptyList(),
    val queueIndex: Int = -1,
    val error: String? = null,
)

/**
 * Pengendali musik untuk UI. Membungkus MediaController (tersambung ke MusicPlaybackService)
 * dan mengekspos state reaktif. Queue lagu disimpan di sini; URL di-resolve saat akan diputar.
 */
@Singleton
class SelaMusicController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val streamResolver: StreamResolver,
    private val musicDao: MusicDao,
    private val musicRepository: MusicRepository,
) {
    private val tag = "SelaMusicController"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _state = MutableStateFlow(MusicPlayerState())
    val state: StateFlow<MusicPlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var songsById = java.util.concurrent.ConcurrentHashMap<String, SelaSong>()
    private var tickerJob: kotlinx.coroutines.Job? = null
    private var autoplayEnabled = true

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                try {
                    controller = future.get()
                    controller?.addListener(controllerListener)
                    syncState()
                    Log.i(tag, "Terhubung ke MusicPlaybackService")
                } catch (t: Throwable) {
                    Log.e(tag, "Gagal terhubung: ${t.message}")
                    _state.value = _state.value.copy(error = "Tidak bisa terhubung ke layanan musik")
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    private val controllerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState()
            // Kelola progress ticker berdasarkan status putar
            if (player.isPlaying) startTicker() else stopTicker()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            mediaItem?.let {
                recordHistory(it.mediaId)
                // (b) Resolve URL jika item baru masih pending (transisi otomatis)
                ensureResolved(it.mediaId)
                // Pre-resolve item berikutnya
                preResolveNext()
            }
            syncState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // (d) Autoplay: antrean habis -> tambah lagu terkait
            if (playbackState == Player.STATE_ENDED) {
                val c = controller ?: return
                if (c.repeatMode == Player.REPEAT_MODE_OFF && autoplayEnabled) {
                    triggerAutoplay()
                }
            }
            syncState()
        }
    }

    /** (b) Pastikan MediaItem punya URL; resolve jika masih pending. */
    private fun ensureResolved(songId: String) {
        val c = controller ?: return
        val index = (0 until c.mediaItemCount).firstOrNull {
            c.getMediaItemAt(it).mediaId == songId
        } ?: return
        val item = c.getMediaItemAt(index)
        if (item.localConfiguration?.uri != null) return
        val song = songsById[songId] ?: return
        scope.launch(Dispatchers.IO) {
            val resolved = streamResolver.resolve(songId).getOrNull() ?: return@launch
            scope.launch(Dispatchers.Main) {
                val cc = controller ?: return@launch
                // Pastikan index masih valid dan item masih pending
                if (index < cc.mediaItemCount &&
                    cc.getMediaItemAt(index).mediaId == songId &&
                    cc.getMediaItemAt(index).localConfiguration?.uri == null
                ) {
                    cc.replaceMediaItem(
                        index,
                        MediaItem.Builder()
                            .setMediaId(songId)
                            .setUri(resolved.streamUrl)
                            .setMediaMetadata(song.toMediaMetadata())
                            .build(),
                    )
                }
            }
        }
    }

    /** Pre-resolve lagu berikutnya agar transisi mulus. */
    private fun preResolveNext() {
        val c = controller ?: return
        val nextIndex = c.currentMediaItemIndex + 1
        if (nextIndex < c.mediaItemCount) {
            val nextId = c.getMediaItemAt(nextIndex).mediaId
            scope.launch(Dispatchers.IO) {
                streamResolver.resolve(nextId)
            }
        }
    }

    /** (d) Autoplay: ambil lagu terkait dan tambahkan ke antrean. */
    private fun triggerAutoplay() {
        val c = controller ?: return
        val lastId = c.currentMediaItem?.mediaId ?: return
        if (_state.value.queue.isNotEmpty()) return // masih ada antrean
        scope.launch(Dispatchers.IO) {
            val related = musicRepository.relatedSongs(lastId).getOrNull()
                ?.filter { it.id != lastId }
                ?.take(5)
                .orEmpty()
            if (related.isEmpty()) return@launch
            related.forEach { songsById[it.id] = it }
            scope.launch(Dispatchers.Main) {
                val cc = controller ?: return@launch
                val items = related.map { it.toPendingMediaItem() }
                cc.addMediaItems(items)
                // Resolve yang pertama agar langsung bisa diputar
                related.firstOrNull()?.let { ensureResolved(it.id) }
                Log.i(tag, "Autoplay: ${related.size} lagu ditambahkan")
            }
        }
    }

    /** (c) Progress ticker: update posisi tiap 1 detik saat memutar. */
    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _state.value = _state.value.copy(
                        positionMs = c.currentPosition,
                        durationMs = c.duration.coerceAtLeast(0L),
                    )
                }
                delay(1000L)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
        syncState() // final sync posisi
    }

    private fun syncState() {
        val c = controller ?: return
        val mediaItem = c.currentMediaItem
        val song = mediaItem?.let { songsById[it.mediaId] }
        val queue = (0 until c.mediaItemCount).mapNotNull { i ->
            songsById[c.getMediaItemAt(i).mediaId]
        }
        _state.value = _state.value.copy(
            currentSong = song,
            isPlaying = c.isPlaying,
            isLoading = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition,
            durationMs = c.duration.coerceAtLeast(0L),
            shuffleOn = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            queue = queue,
            queueIndex = c.currentMediaItemIndex,
            error = null,
        )
    }

    private fun recordHistory(songId: String) {
        val song = songsById[songId] ?: return
        scope.launch(Dispatchers.IO) {
            musicDao.insertHistory(
                PlayHistoryEntity(
                    songId = song.id,
                    title = song.title,
                    artistNames = song.artistNames,
                    thumbnailUrl = song.thumbnailUrl,
                ),
            )
            // Batasi 200 entri
            musicDao.pruneHistory(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
        }
    }

    /** Putar daftar lagu mulai dari index tertentu. URL di-resolve lazy per lagu. */
    fun playQueue(songs: List<SelaSong>, startIndex: Int = 0) {
        val c = controller ?: run {
            _state.value = _state.value.copy(error = "Layanan musik belum siap")
            return
        }
        if (songs.isEmpty()) return
        songs.forEach { songsById[it.id] = it }
        _state.value = _state.value.copy(isLoading = true, error = null)
        scope.launch(Dispatchers.IO) {
            val first = songs.getOrNull(startIndex) ?: return@launch
            val resolved = streamResolver.resolve(first.id).getOrNull()
            scope.launch(Dispatchers.Main) {
                if (resolved == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Gagal memuat lagu. Cek koneksi internet.",
                    )
                    return@launch
                }
                val items = songs.map { song ->
                    if (song.id == first.id) {
                        MediaItem.Builder()
                            .setMediaId(song.id)
                            .setUri(resolved.streamUrl)
                            .setMediaMetadata(song.toMediaMetadata())
                            .build()
                    } else {
                        song.toPendingMediaItem()
                    }
                }
                c.setMediaItems(items, startIndex.coerceIn(items.indices), 0L)
                c.prepare()
                c.play()
                // Pre-resolve lagu berikutnya di background
                scope.launch(Dispatchers.IO) {
                    songs.getOrNull(startIndex + 1)?.let { streamResolver.resolve(it.id) }
                }
            }
        }
    }

    /** Putar satu lagu (mengganti antrean). */
    fun playSong(song: SelaSong) = playQueue(listOf(song), 0)

    /** Tambah ke antrean tanpa menghentikan yang sedang diputar. */
    fun addToQueue(song: SelaSong) {
        val c = controller ?: return
        songsById[song.id] = song
        c.addMediaItem(song.toPendingMediaItem())
        syncState()
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = controller?.seekToNext()
    fun previous() = controller?.seekToPrevious()
    fun seekTo(ms: Long) = controller?.seekTo(ms)

    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
        syncState()
    }

    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        syncState()
    }

    fun seekToIndex(index: Int) {
        val c = controller ?: return
        if (index !in 0 until c.mediaItemCount) return
        val songId = c.getMediaItemAt(index).mediaId
        val song = songsById[songId] ?: return
        // Resolve URL sebelum pindah jika masih pending
        scope.launch(Dispatchers.IO) {
            val item = c.getMediaItemAt(index)
            val needsResolve = item.localConfiguration?.uri == null
            if (needsResolve) {
                val resolved = streamResolver.resolve(songId).getOrNull()
                scope.launch(Dispatchers.Main) {
                    if (resolved != null) {
                        val newItem = MediaItem.Builder()
                            .setMediaId(songId)
                            .setUri(resolved.streamUrl)
                            .setMediaMetadata(song.toMediaMetadata())
                            .build()
                        c.replaceMediaItem(index, newItem)
                    }
                    c.seekTo(index, 0L)
                    c.play()
                }
            } else {
                scope.launch(Dispatchers.Main) {
                    c.seekTo(index, 0L)
                    c.play()
                }
            }
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
        syncState()
    }

    fun removeQueueItem(index: Int) {
        controller?.removeMediaItem(index)
        syncState()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun release() {
        scope.cancel()
        controller?.removeListener(controllerListener)
        controller?.release()
        controller = null
    }

    private fun SelaSong.toMediaMetadata(): androidx.media3.common.MediaMetadata =
        androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artistNames)
            .setAlbumTitle(albumName)
            .setArtworkUri(android.net.Uri.parse(thumbnailUrl ?: ""))
            .build()
}
