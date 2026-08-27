package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.concurrent.atomic.AtomicInteger

object TTSNotificationHelper {

    const val CHANNEL_DONATION_ID = "tts_donations_channel"
    const val CHANNEL_CHAT_ID = "tts_chat_channel"
    const val CHANNEL_NOTICE_ID = "tts_notices_channel"
    const val CHANNEL_SERVICE_ID = "tts_sync_service_channel"

    private val notificationIdGenerator = AtomicInteger(1000)

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Donation Channel (High Priority)
            val donationChannel = NotificationChannel(
                CHANNEL_DONATION_ID,
                "चंदा अपडेट्स (Donations)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "12 रबी-उल-अव्वल नया चंदा प्राप्ति की लाइव सूचनाएं"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setShowBadge(true)
            }

            // 2. Chat Channel (High Priority with Heads-Up Pop)
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT_ID,
                "लाइव कमेटी चैट (Chat Messages)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "कमेटी सदस्यों के नए चैट व संदेश"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 150, 200)
                setShowBadge(true)
            }

            // 3. Notice & Document Channel (High Priority)
            val noticeChannel = NotificationChannel(
                CHANNEL_NOTICE_ID,
                "कमेटी नोटिस व आदेश (Notices & Docs)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "महत्वपूर्ण प्रशासनिक नोटिस व आधिकारिक दस्तावेज़"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
                setShowBadge(true)
            }

            // 4. Background Sync Service Channel
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "बैकग्राउंड लाइव सेवा (Live Sync Service)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "24/7 लाइव क्लाउड सिंक व नोटिफिकेशन सर्विस"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannels(
                listOf(donationChannel, chatChannel, noticeChannel, serviceChannel)
            )
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Show notification when Chanda / Donation is added
     */
    fun showDonationNotification(
        context: Context,
        donorName: String,
        amount: Double,
        receiptNumber: String,
        note: String = ""
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_TAB", "donations")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdGenerator.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val formattedAmount = String.format("₹%,.0f", amount)

        val notification = NotificationCompat.Builder(context, CHANNEL_DONATION_ID)
            .setSmallIcon(R.drawable.ic_tts_logo)
            .setContentTitle("💰 नया चंदा प्राप्त हुआ: $formattedAmount")
            .setContentText("$donorName द्वारा $formattedAmount का चंदा दर्ज हुआ (रसीद: $receiptNumber)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("दानदाता: $donorName\nराशि: $formattedAmount\nरसीद क्र.: $receiptNumber" + if (note.isNotBlank()) "\nविवरण: $note" else "")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdGenerator.incrementAndGet(), notification)
    }

    /**
     * Show notification when Chat message is received (especially when app is closed / in background)
     */
    fun showChatNotification(
        context: Context,
        senderName: String,
        channelId: String,
        messageText: String
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_TAB", "meetings")
            putExtra("TARGET_CHANNEL", channelId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdGenerator.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channelTitle = when (channelId) {
            "general" -> "आम चर्चा"
            "announcements" -> "अहम घोषणाएं"
            "duas" -> "दुआ व नात"
            "management" -> "प्रबंधन"
            else -> channelId
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_CHAT_ID)
            .setSmallIcon(R.drawable.ic_tts_logo)
            .setContentTitle("💬 $senderName ($channelTitle)")
            .setContentText(messageText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("💬 $senderName ($channelTitle)")
                    .bigText(messageText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 150, 200))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdGenerator.incrementAndGet(), notification)
    }

    /**
     * Show notification when a Notice is published
     */
    fun showNoticeNotification(
        context: Context,
        title: String,
        content: String,
        priority: String = "सामान्य"
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_TAB", "notices")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdGenerator.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val priorityTag = if (priority.contains("अति आवश्यक") || priority.contains("Urgent")) "🚨 [अति आवश्यक] " else "📢 "

        val notification = NotificationCompat.Builder(context, CHANNEL_NOTICE_ID)
            .setSmallIcon(R.drawable.ic_tts_logo)
            .setContentTitle("$priorityTag नया नोटिस: $title")
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("$priorityTag $title")
                    .bigText(content)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 400, 200, 400))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdGenerator.incrementAndGet(), notification)
    }

    /**
     * Show notification when an Official Document is uploaded
     */
    fun showDocumentNotification(
        context: Context,
        title: String,
        category: String,
        summary: String
    ) {
        if (!hasNotificationPermission(context)) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_TAB", "notices")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationIdGenerator.incrementAndGet(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_NOTICE_ID)
            .setSmallIcon(R.drawable.ic_tts_logo)
            .setContentTitle("📄 नया आधिकारिक दस्तावेज़: $title")
            .setContentText("$category • $summary")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("📄 $title")
                    .bigText("श्रेणी: $category\n\n$summary")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSound(soundUri)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationIdGenerator.incrementAndGet(), notification)
    }
}
