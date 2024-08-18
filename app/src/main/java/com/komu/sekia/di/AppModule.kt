package com.komu.sekia.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import komu.seki.data.repository.MessageHandlerImpl
import komu.seki.data.repository.PreferencesDatastore
import komu.seki.data.services.NsdService
import komu.seki.data.services.WebSocketClient
import komu.seki.data.repository.WebSocketRepositoryImpl
import komu.seki.domain.repository.MessageHandler
import komu.seki.domain.repository.PreferencesRepository
import komu.seki.domain.repository.WebSocketRepository
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideAppContext(@ApplicationContext context: Context) = context

    @Provides
    @Singleton
    fun providesPreferencesRepository(
        application: Application
    ): PreferencesRepository = PreferencesDatastore(context = application)


    @Provides
    @Singleton
    fun provideAppCoroutineScope(): AppCoroutineScope {
        return object : AppCoroutineScope {
            override val coroutineContext =
                SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("App")
        }
    }

    @Provides
    @Singleton
    fun providesMessageHandler(
        application: Application
    ): MessageHandler = MessageHandlerImpl(context = application)


    @Provides
    @Singleton
    fun providesWebSocketClient(messageHandler: MessageHandler): WebSocketClient = WebSocketClient(messageHandler)

    @Provides
    @Singleton
    fun providesWebSocketRepository(
        webSocketClient: WebSocketClient
    ): WebSocketRepository = WebSocketRepositoryImpl(webSocketClient)

    @Provides
    @Singleton
    fun providesNsdHelper(
        application: Application
    ): NsdService =
        NsdService(context = application)
}