package com.test.messages.demo.data.Model

import android.content.Context
import com.test.messages.demo.data.repository.MessageRepository
import com.test.messages.demo.data.reciever.SmsReceiver
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
    fun MessageRepository(@ApplicationContext context:Context): MessageRepository {
        return com.test.messages.demo.data.repository.MessageRepository(context)
    }


}