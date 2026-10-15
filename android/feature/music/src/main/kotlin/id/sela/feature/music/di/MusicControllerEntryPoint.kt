package id.sela.feature.music.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.sela.feature.music.data.MusicDao
import id.sela.feature.music.playback.SelaMusicController

/**
 * EntryPoint agar MainActivity (app module) bisa mengakses
 * SelaMusicController singleton dari :feature:music.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MusicControllerEntryPoint {
    fun musicController(): SelaMusicController
    fun musicDao(): MusicDao
}
