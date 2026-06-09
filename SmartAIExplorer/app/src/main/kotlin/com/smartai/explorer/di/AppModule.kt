package com.smartai.explorer.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.smartai.explorer.BuildConfig
import com.smartai.explorer.data.local.SmartAIDatabase
import com.smartai.explorer.data.remote.SmartAIApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "smartai_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)   // SSE streams can be long
        .writeTimeout(60,  TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): SmartAIApiService =
        retrofit.create(SmartAIApiService::class.java)

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): SmartAIDatabase =
        Room.databaseBuilder(ctx, SmartAIDatabase::class.java, "smartai.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideDocumentDao(db: SmartAIDatabase)    = db.documentDao()
    @Provides fun provideChatMessageDao(db: SmartAIDatabase) = db.chatMessageDao()
    @Provides fun provideSummaryDao(db: SmartAIDatabase)     = db.summaryDao()
    @Provides fun provideFlashcardDao(db: SmartAIDatabase)   = db.flashcardDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.dataStore
}
