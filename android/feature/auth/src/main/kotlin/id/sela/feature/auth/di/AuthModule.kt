package id.sela.feature.auth.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import id.sela.feature.auth.GoogleSignInClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideGoogleSignInClient(): GoogleSignInClient = GoogleSignInClient()
}
