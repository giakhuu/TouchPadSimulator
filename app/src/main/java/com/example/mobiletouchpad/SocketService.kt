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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SocketService : Service() {
    private var socket: DatagramSocket? = null
    private val PC_IP = "192.168.244.129"
    private val PC_PORT = 5000
    private val CHANNEL_ID = "TouchpadService"

    // Binder để Activity bind vào
    inner class LocalBinder : Binder() {
        fun getService(): SocketService = this@SocketService
    }
    private val binder = LocalBinder()

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        // Khởi DatagramSocket
        socket = DatagramSocket().apply {
            // Không broadcast
            broadcast = false
        }

        createNotificationChannel()
        startForeground(1, buildNotification())
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MobileTouchPad")
            .setContentText("Touchpad đang kết nối PC qua UDP")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "MobileTouchPad Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(this)
            }
        }
    }

    /**
     * Gửi gói UDP 3-byte: [type, dx, dy]
     */
    fun sendPacket(data: ByteArray) {
        Log.d("SocketService", "sendPacket: ${data.joinToString("-") { "%02X".format(it) }}")
        socket?.let { ds ->
            Thread {
                try {
                    val packet = DatagramPacket(
                        data,
                        data.size,
                        InetAddress.getByName(PC_IP),
                        PC_PORT
                    )
                    ds.send(packet)
                } catch (e: Exception) {
                    Log.e("SocketService", "UDP send error", e)
                }
            }.start()
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        socket?.close()
    }
}
