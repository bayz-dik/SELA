package id.sela.feature.music.playback

import android.util.Log
import id.sela.feature.music.data.ResolvedStream
import id.sela.feature.music.data.isExpired
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.YouTubeClient
import moe.rukamori.archivetune.innertube.models.response.PlayerResponse
import moe.rukamori.archivetune.innertube.NewPipeUtils
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengubah videoId YouTube Music menjadi URL audio yang bisa diputar ExoPlayer.
 * Cache per lagu; URL kedaluwarsa di-resolve ulang otomatis.
 */
@Singleton
class StreamResolver @Inject constructor() {
    private val tag = "SelaStreamResolver"
    private val cache = mutableMapOf<String, ResolvedStream>()
    private val mutex = Mutex()

    suspend fun resolve(songId: String): Result<ResolvedStream> = withContext(Dispatchers.IO) {
        mutex.withLock {
            cache[songId]?.takeIf { !it.isExpired() }?.let { return@withContext Result.success(it) }
        }
        // Coba ANDROID_MUSIC dulu (paling ringan), fallback WEB_REMIX
        val clients = listOf(YouTubeClient.ANDROID_MUSIC, YouTubeClient.WEB_REMIX)
        var lastError: Throwable? = null
        for (client in clients) {
            try {
                val response = YouTube.player(
                    videoId = songId,
                    client = client,
                ).getOrThrow()
                val stream = pickBestAudioStream(response, songId, client)
                    ?: continue
                mutex.withLock { cache[songId] = stream }
                Log.i(tag, "Resolved $songId via ${client.clientName}")
                return@withContext Result.success(stream)
            } catch (t: Throwable) {
                Log.w(tag, "Resolve gagal via ${client.clientName}: ${t.message}")
                lastError = t
            }
        }
        Result.failure(lastError ?: IllegalStateException("Tidak ada stream audio untuk $songId"))
    }

    private suspend fun pickBestAudioStream(
        response: PlayerResponse,
        songId: String,
        client: YouTubeClient,
    ): ResolvedStream? {
        val streamingData = response.streamingData ?: return null
        if (response.playabilityStatus.status != "OK") {
            Log.w(tag, "Playability ${response.playabilityStatus.status}: ${response.playabilityStatus.reason}")
            return null
        }
        val audioFormats = streamingData.adaptiveFormats
            .filter { it.isAudio }
            .sortedByDescending { it.bitrate }
        val format = audioFormats.firstOrNull() ?: return null
        val url = NewPipeUtils.getStreamUrl(format, songId, client).getOrNull() ?: return null
        val expiresMs = url.toHttpUrlOrNull()
            ?.queryParameter("expire")?.toLongOrNull()?.times(1000L)
            ?: (System.currentTimeMillis() + 5 * 60_000L)
        return ResolvedStream(
            songId = songId,
            streamUrl = url,
            expiresAtMs = expiresMs,
            mimeType = format.mimeType,
        )
    }

    suspend fun invalidate(songId: String) {
        mutex.withLock { cache.remove(songId) }
    }

    suspend fun clear() {
        mutex.withLock { cache.clear() }
    }
}
