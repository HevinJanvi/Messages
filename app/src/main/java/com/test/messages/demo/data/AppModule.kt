package com.test.messages.demo.data

import android.content.Context
import com.test.messages.demo.repository.MessageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun MessageRepository(@ApplicationContext context:Context): MessageRepository{
        return com.test.messages.demo.repository.MessageRepository(context)
    }
//    @Provides
//    @Singleton
//    fun ConversationRepository(@ApplicationContext context:Context): ConversationRepository{
//        return com.test.messages.demo.repository.ConversationRepository(context)
//    }

}