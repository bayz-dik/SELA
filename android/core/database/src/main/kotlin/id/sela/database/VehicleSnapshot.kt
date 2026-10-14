package id.sela.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Snapshot posisi kendaraan untuk "time machine" (fitur #81).
 *
 * Tiap ~60 detik, posisi live semua kendaraan disimpan ke tabel ini (maks 7 hari
 * ke belakang, dipangkas otomatis). Dipakai untuk:
 * - melihat posisi bus di waktu lampau ("atTime"),
 * - deteksi gangguan rute (DisruptionMonitor melihat riwayat kemunculan).
 *
 * WIRING: daftarkan entity + DAO ini ke [TransitDatabase] (entities + version
 * 3→4 + Migration(3,4) berisi CREATE TABLE vehicle_snapshots).
 */
@Entity(tableName = "vehicle_snapshots")
data class VehicleSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val snapshotId: Long = 0,
    val vehicleId: String,
    val routeId: String,
    val lat: Double,
    val lon: Double,
    /** Epoch millis saat posisi ini teramati. */
    val timestamp: Long,
)

@Dao
interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<VehicleSnapshotEntity>)

    @Query("SELECT * FROM vehicle_snapshots WHERE timestamp >= :sinceMs ORDER BY timestamp ASC")
    suspend fun since(sinceMs: Long): List<VehicleSnapshotEntity>

    @Query("DELETE FROM vehicle_snapshots WHERE timestamp < :beforeMs")
    suspend fun prune(beforeMs: Long)

    @Query("DELETE FROM vehicle_snapshots")
    suspend fun deleteAll()
}
