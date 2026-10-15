package id.sela.feature.music.playback

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import id.sela.feature.music.data.ResolvedStream
import id.sela.feature.music.data.SelaSong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Foreground playback service SELA Music berbasis Media3.
 * - Notification + lock screen control otomatis via MediaSession
 * - Audio focus, headset disconnect, bluetooth: ditangani ExoPlayer
 * - Tetap hidup saat peta MapLibre dibuka / tab berganti
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaLibraryService() {

    @Inject lateinit var streamResolver: StreamResolver

    private val tag = "SelaMusicService"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var player: ExoPlayer? = null
    private var mediaSession: MediaLibrarySession? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** (e) Cache audio sementara: 200MB, LRU. Mengurangi re-download lagu yang diputar ulang. */
    private val audioCache: SimpleCache by lazy {
        val cacheDir = File(cacheDir, "sela_music_audio").apply { mkdirs() }
        SimpleCache(
            cacheDir,
            LeastRecentlyUsedCacheEvictor(200L * 1024 * 1024),
            androidx.media3.database.StandaloneDatabaseProvider(this),
        )
    }

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        // CacheDataSource: baca dari cache jika ada, download + simpan jika belum
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(audioCache)
            .setUpstreamDataSourceFactory(OkHttpDataSource.Factory(okHttpClient))
            .setCacheWriteDataSinkFactory(null) // default: tulis ke cache
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // handleAudioFocus = true
            .setHandleAudioBecomingNoisy(true) // headset dicabut -> pause
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(cacheDataSourceFactory),
            )
            .build()
        player.addListener(servicePlayerListener)
        this.player = player
        mediaSession = MediaLibrarySession.Builder(this, player, SelaMusicLibraryCallback())
            .build()
        Log.i(tag, "Service created")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Jangan stop saat user swipe dari recent apps jika sedang memutar
        if (player?.playWhenReady != true || player?.playbackState == Player.STATE_IDLE) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession?.release()
        player?.removeListener(servicePlayerListener)
        player?.release()
        player = null
        mediaSession = null
        // Jangan release cache di sini (Singleton lifecycle); biarkan untuk sesi berikutnya
        Log.i(tag, "Service destroyed")
        super.onDestroy()
    }

    private val servicePlayerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            Log.e(tag, "Player error: ${error.errorCodeName} ${error.message}")
            // Coba resolve ulang URL (kemungkinan kedaluwarsa), lalu retry sekali
            val currentId = player?.currentMediaItem?.mediaId ?: return
            serviceScope.launch(Dispatchers.IO) {
                streamResolver.invalidate(currentId)
                val resolved = streamResolver.resolve(currentId).getOrNull() ?: return@launch
                launch(Dispatchers.Main) {
                    val p = player ?: return@launch
                    val current = p.currentMediaItem ?: return@launch
                    val index = p.currentMediaItemIndex
                    p.replaceMediaItem(index, resolved.toResolvedMediaItem(current))
                    p.prepare()
                    p.play()
                }
            }
        }
    }

    private inner class SelaMusicLibraryCallback : MediaLibrarySession.Callback

    private fun ResolvedStream.toResolvedMediaItem(
        base: MediaItem,
    ): MediaItem = MediaItem.Builder()
        .setMediaId(songId)
        .setUri(streamUrl)
        .setMediaMetadata(base.mediaMetadata)
        .build()
}

/** Helper: MediaItem dari SelaSong tanpa URL (diisi saat resolve). */
fun SelaSong.toPendingMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistNames)
                .setAlbumTitle(albumName)
                .setArtworkUri(android.net.Uri.parse(thumbnailUrl ?: ""))
                .build(),
        )
        .build()
