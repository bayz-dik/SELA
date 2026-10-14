package id.sela.network

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.sela.database.SnapshotDao
import id.sela.database.TransitDao
import id.sela.database.TransitDatabase
import id.sela.model.TransitRepository
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TransitScope

@Module
@InstallIn(SingletonComponent::class)
object TransitModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): TransitDatabase =
        Room.databaseBuilder(context, TransitDatabase::class.java, "sela-transit.db")
            .addMigrations(
                object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `trip_history` (`historyId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routeId` TEXT NOT NULL, `routeName` TEXT NOT NULL, `stopId` TEXT, `stopName` TEXT, `timestamp` INTEGER NOT NULL)")
                    }
                },
                object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `notification_history` (`notifId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `kind` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
                    }
                },
                object : androidx.room.migration.Migration(3, 4) {
                    override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS `vehicle_snapshots` (`snapshotId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `vehicleId` TEXT NOT NULL, `routeId` TEXT NOT NULL, `lat` REAL NOT NULL, `lon` REAL NOT NULL, `timestamp` INTEGER NOT NULL)")
                    }
                },
            )
            .build()
    @Provides fun dao(database: TransitDatabase): TransitDao = database.transitDao()
    @Provides fun snapshotDao(database: TransitDatabase): SnapshotDao = database.snapshotDao()
    @Provides @Singleton fun gson(): Gson = Gson()
    @Provides @Singleton @TransitScope fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Provides @Singleton fun repository(repository: StandaloneTransitRepository): TransitRepository = repository
}
