package com.example

import android.app.Application
import android.util.Log
import com.example.data.firebase.FirebaseFirestoreService
import com.example.service.TTSBackgroundSyncService
import com.example.util.TTSNotificationHelper

class TTSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("TTSApplication", "Application started")

        try {
            // 1. Create Notification Channels for Chanda, Chat & Notices
            TTSNotificationHelper.createNotificationChannels(this)

            // 2. Initialize Real-Time Cloud Service with Context
            FirebaseFirestoreService.getInstance(this).setContext(this)
        } catch (e: Throwable) {
            Log.e("TTSApplication", "Startup initialization warning: ${e.message}")
        }
    }
}
