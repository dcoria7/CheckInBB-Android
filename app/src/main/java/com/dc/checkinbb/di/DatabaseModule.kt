package com.dc.checkinbb.di

import android.content.Context
import com.dc.checkinbb.data.local.AppDatabase
import com.dc.checkinbb.data.local.FeedingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideFeedingDao(db: AppDatabase): FeedingDao = db.feedingDao()
}
