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
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

class SocketService : Service() {
    @Volatile private var pcIp: String? = null
    private val DISCOVERY_PORT = 5000
    private val DISCOVERY_PAYLOAD = "DISCOVER_TOUCHPAD".toByteArray(Charsets.UTF_8)
    private val DISCOVERY_RESPONSE = "TOUCHPAD_OK" // or whatever PC replies

    private var socket: DatagramSocket? = null
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
        Log.d("SocketService", "onCreate")
        startDiscoveryAsync()
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
        val targetIp = pcIp
        Log.d("SocketService", "sendPacket: ${data.joinToString("-") { "%02X".format(it) }} to $targetIp")
        socket?.let { ds ->
            Thread {
                try {
                    val packet = DatagramPacket(
                        data,
                        data.size,
                        InetAddress.getByName(targetIp),
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
    private fun startDiscoveryAsync() {
        Log.d("SocketService", "startDiscoveryAsync")

        Thread {
            val found = discoverPcIp()
            if (found != null) {
                pcIp = found.hostAddress
                Log.i("SocketService", "Discovered PC IP = $pcIp")
            } else {
                Log.w("SocketService", "Discovery failed")
            }
        }.start()
    }
    private fun discoverPcIp(timeoutMs: Int = 800, retries: Int = 3): InetAddress? {
        Log.d("SocketService", "discoverPcIp")

        // build list of candidate broadcast addresses
        val broadcasts = mutableSetOf<InetAddress>()

        try {
            val nets = NetworkInterface.getNetworkInterfaces()
            for (ni in nets) {
                if (!ni.isUp || ni.isLoopback) continue
                for (ia in ni.interfaceAddresses) {
                    val b = ia.broadcast
                    val addr = ia.address
                    if (b != null && addr is Inet4Address) {
                        broadcasts.add(b)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("SocketService", "Error enumerating interfaces: ${e.message}")
        }

        // fallback: global broadcast
        broadcasts.add(InetAddress.getByName("255.255.255.255"))

        // optional optimization: if you know tether subnet (192.168.244.0/24)
        // you can add 192.168.244.255 explicitly:
        try {
            broadcasts.add(InetAddress.getByName("192.168.244.255"))
        } catch (_: Exception) {}

        repeat(retries) { attempt ->
            for (bcast in broadcasts) {
                Log.d("SocketService", "Discovery try ${attempt+1} -> $bcast")
                val resp = sendDiscoveryAndWait(bcast, DISCOVERY_PORT, timeoutMs)
                if (resp != null) return resp
            }
        }
        return null
    }
    private fun sendDiscoveryAndWait(broadcast: InetAddress, port: Int, timeoutMs: Int): InetAddress? {
        DatagramSocket().use { sock ->
            try {
                sock.broadcast = true
                sock.soTimeout = timeoutMs
                val packet = DatagramPacket(DISCOVERY_PAYLOAD, DISCOVERY_PAYLOAD.size, broadcast, port)
                sock.send(packet)

                // chờ reply
                val buf = ByteArray(256)
                val resp = DatagramPacket(buf,buf.size)
                sock.receive(resp) // blocking until timeout
                val s = String(resp.data, 0, resp.length, Charsets.UTF_8)
                Log.d("SocketService", "Discovery recv: '$s' from ${resp.address.hostAddress}")
                if (s.startsWith(DISCOVERY_RESPONSE)) {
                    return resp.address
                }
            } catch (e: Exception) {
                // timeout hoặc error
                // Log.d("SocketService", "Discovery error: ${e.message}")
            }
        }
        return null
    }
}
