package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.firebase.FirebaseFirestoreService
import com.example.util.TTSNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-Reliability Background Service that keeps the Real-Time Sync and Push Notification Engine
 * alive 24/7 even when the application is closed or minimized by the user.
 */
class TTSBackgroundSyncService : Service() {

    companion object {
        private const val TAG = "TTSBgSyncService"
        private const val SERVICE_NOTIFICATION_ID = 9999

        fun startService(context: Context) {
            try {
                val intent = Intent(context, TTSBackgroundSyncService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting TTSBackgroundSyncService: ${e.message}")
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var firestoreService: FirebaseFirestoreService

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TTSBackgroundSyncService created")
        TTSNotificationHelper.createNotificationChannels(this)
        firestoreService = FirebaseFirestoreService.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(SERVICE_NOTIFICATION_ID, createForegroundNotification())

        // Ensure cloud connection is active
        serviceScope.launch {
            while (isActive) {
                try {
                    firestoreService.ensureConnection()
                } catch (e: Exception) {
                    Log.v(TAG, "Service sync check: ${e.message}")
                }
                delay(30000)
            }
        }

        return START_STICKY
    }

    private fun createForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, TTSNotificationHelper.CHANNEL_SERVICE_ID)
            .setSmallIcon(R.drawable.ic_tts_logo)
            .setContentTitle("🕌 12 रबी-उल-अव्वल कमेटी लाइव सेवा")
            .setContentText("चंदा, लाइव चैट और नोटिस की तुरंत सूचनाएं सक्रिय हैं")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "TTSBackgroundSyncService destroyed")
    }
}
