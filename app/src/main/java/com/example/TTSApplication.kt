package com.example

import android.app.Application
import android.util.Log
import com.example.data.firebase.FirebaseFirestoreService
import com.example.service.TTSBackgroundSyncService
import com.example.util.TTSNotificationHelper

class TTSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("TTSApplication", "Application started - Initializing 24/7 Live Cloud Push Engine")

        try {
            // 1. Create Notification Channels for Chanda, Chat & Notices
            TTSNotificationHelper.createNotificationChannels(this)

            // 2. Initialize Real-Time Cloud Service with Context
            FirebaseFirestoreService.getInstance(this).setContext(this)

            // 3. Start Background Sync Service safely
            TTSBackgroundSyncService.startService(this)
        } catch (e: Exception) {
            Log.e("TTSApplication", "Startup initialization warning: ${e.message}")
        }
    }
}
