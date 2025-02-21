package com.test.messages.demo.data

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApp : Application() {

//    lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
//        db = Room.databaseBuilder(
//            applicationContext,
//            AppDatabase::class.java, "app-database"
//        ).build()
    }
}