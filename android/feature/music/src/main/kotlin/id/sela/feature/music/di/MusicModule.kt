package id.sela.feature.music.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import id.sela.feature.music.data.MusicDao
import id.sela.feature.music.data.MusicDatabase
import id.sela.feature.music.data.MusicRepository
import id.sela.feature.music.playback.SelaMusicController
import id.sela.feature.music.playback.StreamResolver
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MusicModule {

    @Provides
    @Singleton
    fun provideMusicDatabase(@ApplicationContext context: Context): MusicDatabase =
        Room.databaseBuilder(context, MusicDatabase::class.java, "sela_music.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideMusicDao(db: MusicDatabase): MusicDao = db.musicDao()

    @Provides
    @Singleton
    fun provideMusicRepository(): MusicRepository = MusicRepository()

    @Provides
    @Singleton
    fun provideStreamResolver(): StreamResolver = StreamResolver()

    @Provides
    @Singleton
    fun provideMusicController(
        @ApplicationContext context: Context,
        streamResolver: StreamResolver,
        musicDao: MusicDao,
        musicRepository: MusicRepository,
    ): SelaMusicController = SelaMusicController(context, streamResolver, musicDao, musicRepository)
}
