package id.sela.database

import androidx.room.*
import id.sela.model.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "stops")
data class StopEntity(@PrimaryKey val id: String, val name: String, val code: String?, val lat: Double, val lon: Double, val routeIds: String = "") {
    fun model() = Stop(id, name, code, lat, lon, routeIds.split(',').filter { it.isNotBlank() })
}
@Entity(tableName = "routes")
data class RouteEntity(@PrimaryKey val id: String, val shortName: String, val longName: String, val serviceType: String, val color: String?) {
    fun model() = Route(id, shortName, longName, runCatching { ServiceType.valueOf(serviceType) }.getOrDefault(ServiceType.BUS), color)
}
@Entity(tableName = "favorites", primaryKeys = ["id", "kind"])
data class FavoriteEntity(val id: String, val kind: String)
@Entity(tableName = "trip_history")
data class TripHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    val routeId: String,
    val routeName: String,
    val stopId: String?,
    val stopName: String?,
    val timestamp: Long,
)
@Entity(tableName = "notification_history")
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val notifId: Long = 0,
    val title: String,
    val body: String,
    val kind: String,
    val timestamp: Long,
)
@Entity(tableName = "cache_metadata")
data class CacheMetadata(@PrimaryKey val key: String = "static", val datasetVersion: String, val syncedAt: String)
data class CachedTransit(val stops: List<StopEntity>, val routes: List<RouteEntity>, val metadata: CacheMetadata?)

@Dao
abstract class TransitDao {
    @Query("SELECT * FROM stops ORDER BY name, id") abstract suspend fun stops(): List<StopEntity>
    @Query("SELECT * FROM routes ORDER BY shortName, id") abstract suspend fun routes(): List<RouteEntity>
    @Query("SELECT * FROM cache_metadata WHERE `key` = 'static'") abstract suspend fun metadata(): CacheMetadata?
    @Query("SELECT * FROM favorites ORDER BY kind, id") abstract fun favorites(): Flow<List<FavoriteEntity>>
    @Query("SELECT COUNT(*) FROM favorites WHERE id = :id AND kind = :kind") abstract suspend fun favoriteCount(id: String, kind: String): Int
    @Query("SELECT COUNT(*) FROM favorites") abstract suspend fun favoriteCountAll(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertStops(rows: List<StopEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertRoutes(rows: List<RouteEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertMetadata(metadata: CacheMetadata)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertFavorite(favorite: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE id = :id AND kind = :kind") abstract suspend fun deleteFavorite(id: String, kind: String)
    @Query("DELETE FROM stops") abstract suspend fun clearStops()
    @Query("DELETE FROM routes") abstract suspend fun clearRoutes()
    @Transaction open suspend fun readCache() = CachedTransit(stops(), routes(), metadata())
    /** Called only after every page of both collections has been validated. Failures retain the old cache. */
    @Transaction open suspend fun replace(stops: List<StopEntity>, routes: List<RouteEntity>, metadata: CacheMetadata) {
        clearStops(); clearRoutes()
        stops.chunked(500).forEach { insertStops(it) }
        routes.chunked(500).forEach { insertRoutes(it) }
        insertMetadata(metadata)
    }
    @Transaction open suspend fun toggle(id: String, kind: String) {
        require(kind == "stop" || kind == "route")
        require(id.isNotBlank())
        if (favoriteCount(id, kind) > 0) deleteFavorite(id, kind) else insertFavorite(FavoriteEntity(id, kind))
    }
    // Trip history
    @Query("SELECT * FROM trip_history ORDER BY timestamp DESC LIMIT :limit") abstract suspend fun tripHistory(limit: Int = 100): List<TripHistoryEntity>
    @Query("SELECT * FROM trip_history ORDER BY timestamp DESC") abstract fun tripHistoryFlow(): Flow<List<TripHistoryEntity>>
    @Insert abstract suspend fun insertTrip(entry: TripHistoryEntity): Long
    @Query("DELETE FROM trip_history WHERE timestamp < :before") abstract suspend fun pruneHistory(before: Long)
    @Query("SELECT COUNT(*) FROM trip_history") abstract suspend fun historyCount(): Int
    // Notification history (fitur #112)
    @Insert abstract suspend fun insertNotification(entry: NotificationHistoryEntity): Long
    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC LIMIT :limit") abstract suspend fun notificationHistory(limit: Int = 100): List<NotificationHistoryEntity>
    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC") abstract fun notificationHistoryFlow(): Flow<List<NotificationHistoryEntity>>
    @Query("DELETE FROM notification_history WHERE timestamp < :before") abstract suspend fun pruneNotifications(before: Long)
    @Query("DELETE FROM notification_history") abstract suspend fun clearNotifications()
}
@Database(entities = [StopEntity::class, RouteEntity::class, FavoriteEntity::class, CacheMetadata::class, TripHistoryEntity::class, NotificationHistoryEntity::class, VehicleSnapshotEntity::class], version = 4, exportSchema = false)
abstract class TransitDatabase : RoomDatabase() { abstract fun transitDao(): TransitDao; abstract fun snapshotDao(): SnapshotDao }
