package com.example.bitchat

import android.app.Application
import android.content.Context

class AppGlobals : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        lateinit var appContext: Context
            private set
    }
}
