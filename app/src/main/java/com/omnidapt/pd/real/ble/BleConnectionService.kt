package com.omnidapt.pd.real.ble

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.omnidapt.pd.R

/**
 * Keeps the research BLE link alive while the app moves between doctor/patient
 * monitor pages. The actual client remains application-scoped and is stopped
 * explicitly on logout/disconnect.
 */
class BleConnectionService : Service() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "科研模拟设备连接",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.omnidapt_launcher)
                .setContentTitle("Ominidapt PD")
                .setContentText("正在保持与非临床模拟设备的BLE连接")
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build(),
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "omnidapt_ble_connection"
        private const val NOTIFICATION_ID = 1002
    }
}
