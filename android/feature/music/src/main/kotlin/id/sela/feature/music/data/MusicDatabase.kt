package id.sela.feature.music.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "music_favorites")
data class FavoriteSongEntity(
    @PrimaryKey val songId: String,
    val title: String,
    val artistNames: String,
    val albumName: String?,
    val durationSec: Int?,
    val thumbnailUrl: String?,
    val addedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "music_playlists")
data class MusicPlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "music_playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongEntity(
    val playlistId: Long,
    val songId: String,
    val title: String,
    val artistNames: String,
    val albumName: String?,
    val durationSec: Int?,
    val thumbnailUrl: String?,
    val position: Int,
)

@Entity(tableName = "music_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val title: String,
    val artistNames: String,
    val thumbnailUrl: String?,
    val playedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "music_search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long = System.currentTimeMillis(),
)

@Dao
interface MusicDao {
    // Favorites
    @Query("SELECT * FROM music_favorites ORDER BY addedAt DESC")
    fun favorites(): Flow<List<FavoriteSongEntity>>

    @Query("SELECT COUNT(*) FROM music_favorites WHERE songId = :songId")
    suspend fun isFavorite(songId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(song: FavoriteSongEntity)

    @Query("DELETE FROM music_favorites WHERE songId = :songId")
    suspend fun removeFavorite(songId: String)

    // Playlists
    @Query("SELECT * FROM music_playlists ORDER BY createdAt DESC")
    fun playlists(): Flow<List<MusicPlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: MusicPlaylistEntity): Long

    @Query("DELETE FROM music_playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM music_playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Query("SELECT * FROM music_playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    fun playlistSongs(playlistId: Long): Flow<List<PlaylistSongEntity>>

    @Query("SELECT COUNT(*) FROM music_playlist_songs WHERE playlistId = :playlistId")
    suspend fun playlistSongCount(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(song: PlaylistSongEntity)

    @Query("DELETE FROM music_playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    // History
    @Query("SELECT * FROM music_history ORDER BY playedAt DESC LIMIT 100")
    fun recentPlays(): Flow<List<PlayHistoryEntity>>

    @Insert
    suspend fun insertHistory(entry: PlayHistoryEntity)

    @Query("DELETE FROM music_history WHERE playedAt < :beforeMs")
    suspend fun pruneHistory(beforeMs: Long)

    // Search history
    @Query("SELECT * FROM music_search_history ORDER BY searchedAt DESC LIMIT 20")
    fun searchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(entry: SearchHistoryEntity)

    @Query("DELETE FROM music_search_history WHERE `query` = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM music_search_history")
    suspend fun clearSearchHistory()
}

@Database(
    entities = [
        FavoriteSongEntity::class,
        MusicPlaylistEntity::class,
        PlaylistSongEntity::class,
        PlayHistoryEntity::class,
        SearchHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
}

internal fun SelaSong.toFavoriteEntity() = FavoriteSongEntity(
    songId = id,
    title = title,
    artistNames = artistNames,
    albumName = albumName,
    durationSec = durationSec,
    thumbnailUrl = thumbnailUrl,
)

internal fun FavoriteSongEntity.toSelaSong() = SelaSong(
    id = songId,
    title = title,
    artistNames = artistNames,
    albumName = albumName,
    durationSec = durationSec,
    thumbnailUrl = thumbnailUrl,
)

internal fun PlaylistSongEntity.toSelaSong() = SelaSong(
    id = songId,
    title = title,
    artistNames = artistNames,
    albumName = albumName,
    durationSec = durationSec,
    thumbnailUrl = thumbnailUrl,
)

internal fun PlayHistoryEntity.toSelaSong() = SelaSong(
    id = songId,
    title = title,
    artistNames = artistNames,
    thumbnailUrl = thumbnailUrl,
)
