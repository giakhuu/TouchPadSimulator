package com.example.mobiletouchpad

import android.app.Application

class MyApp : Application() {

    companion object {
        lateinit var instance: MyApp
            private set
    }

    var socketService: SocketService? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}