package com.example.mobiletouchpad

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.io.OutputStream
import java.net.Socket
class SocketService : Service() {
    private var socket: Socket? = null
    private var out: OutputStream? = null
    private val PC_IP = "192.168.244.220"
    private val PC_PORT = 5000
    private val CHANNEL_ID = "TouchpadService"

    inner class LocalBinder : Binder() {
        fun getService(): SocketService = this@SocketService
    }
    private val binder = LocalBinder()

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(1, buildNotification())

        Thread {
            try {
                socket = Socket(PC_IP, PC_PORT)
                out = socket?.getOutputStream()
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }.start()
    }

    private fun buildNotification(): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MobileTouchPad")
            .setContentText("Touchpad đang kết nối PC")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID, "MobileTouchPad", NotificationManager.IMPORTANCE_LOW
            ).apply {
                getSystemService(NotificationManager::class.java).createNotificationChannel(this)
            }
        }
    }

    fun sendPacket(data: ByteArray) {
        Log.d("Socket", "send data")
        out?.let { os ->
            Thread {
                try {
                    os.write(data)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        // 2. Trả binder cho client
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        out?.close()
        socket?.close()
    }
}
