package com.dc.checkinbb.di

import android.content.Context
import com.dc.checkinbb.sync.NearbyConnectionsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideNearbyConnectionsManager(
        @ApplicationContext context: Context
    ): NearbyConnectionsManager = NearbyConnectionsManager(context)
}
