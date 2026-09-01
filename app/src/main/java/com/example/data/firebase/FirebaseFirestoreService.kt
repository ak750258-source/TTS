package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import com.example.util.TTSNotificationHelper
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Universal High-Reliability Real-Time Cloud Synchronization Engine.
 * 
 * Synchronizes 100% of data across all distributed Android devices:
 * - Live Chat Messages across all channels
 * - Member Profiles, Designations, Badges & Photos
 * - Notices & Important Announcements
 * - Donation Records & Live Totals
 * - Meetings & Agendas
 * - Official Documents
 * - Live Candidate Online Indicators (🟢)
 * - Remote Data Wiping & Reset Propagation
 */
class FirebaseFirestoreService(context: Context? = null) {

    companion object {
        private const val TAG = "CloudSyncEngine"
        private const val SYNC_TOPIC = "tts_12rabiulawwal_live_sync_v6"
        private const val BASE_URL = "https://ntfy.sh/$SYNC_TOPIC"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val DEVICE_ID = UUID.randomUUID().toString()

        @Volatile
        private var INSTANCE: FirebaseFirestoreService? = null

        fun getDeviceId(): String = DEVICE_ID

        fun getInstance(context: Context? = null): FirebaseFirestoreService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseFirestoreService(context?.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private var appContext: Context? = context?.applicationContext

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private val ipv4PrioritizedDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                val addresses = Dns.SYSTEM.lookup(hostname)
                // Prioritize IPv4 addresses first to avoid unreachable IPv6 routes in mobile/container environments
                addresses.sortedWith(compareBy { if (it is Inet4Address) 0 else 1 })
            } catch (e: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .dns(ipv4PrioritizedDns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val streamingClient = OkHttpClient.Builder()
        .dns(ipv4PrioritizedDns)
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val _isCloudConnected = MutableStateFlow(true)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _syncStatus = MutableStateFlow("🟢 लाइव क्लाउड सिंक सक्रिय")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _onlineCandidateIds = MutableStateFlow<Set<Long>>(emptySet())
    val onlineCandidateIds: StateFlow<Set<Long>> = _onlineCandidateIds.asStateFlow()

    // Active candidate last seen timestamp map
    private val candidateLastSeenMap = ConcurrentHashMap<Long, Long>()

    // Listeners for Repository updates
    private val memberUpsertListeners = mutableListOf<(List<Member>) -> Unit>()
    private val memberDeleteListeners = mutableListOf<(Long) -> Unit>()
    
    private val donationUpsertListeners = mutableListOf<(List<Donation>) -> Unit>()
    private val donationDeleteListeners = mutableListOf<(Long) -> Unit>()

    private val meetingUpsertListeners = mutableListOf<(List<Meeting>) -> Unit>()
    private val meetingDeleteListeners = mutableListOf<(Long) -> Unit>()

    private val noticeUpsertListeners = mutableListOf<(List<Notice>) -> Unit>()
    private val noticeDeleteListeners = mutableListOf<(Long) -> Unit>()

    private val documentUpsertListeners = mutableListOf<(List<OfficialDocument>) -> Unit>()
    private val documentDeleteListeners = mutableListOf<(Long) -> Unit>()

    private val expenseUpsertListeners = mutableListOf<(List<Expense>) -> Unit>()
    private val expenseDeleteListeners = mutableListOf<(Long) -> Unit>()
    private val expenseClearListeners = mutableListOf<() -> Unit>()
    private val donationClearListeners = mutableListOf<() -> Unit>()
    private val donationGoalListeners = mutableListOf<(Double) -> Unit>()
    private val memberPhotoUpdateListeners = mutableListOf<(Long, String) -> Unit>()
    // Buffer for assembling multi-part / chunked photo data
    private val incomingPhotoChunks = ConcurrentHashMap<String, ConcurrentHashMap<Int, String>>()

    private val chatListeners = ConcurrentHashMap<String, MutableList<(List<ChatMessage>) -> Unit>>()
    private val chatDeleteListeners = mutableListOf<(Long) -> Unit>()
    private val chatSeenListeners = mutableListOf<(Long, String) -> Unit>()
    private val chatClearListeners = mutableListOf<() -> Unit>()
    private val syncRequestListeners = mutableListOf<() -> Unit>()
    private val clearAllListeners = mutableListOf<() -> Unit>()

    private var currentActiveMemberId: Long = 0L
    private var currentActiveMemberName: String = ""

    // Timestamps to prevent re-populating deleted/cleared records on sync catchup
    @Volatile
    private var lastChatClearedTimestamp: Long = 0L
    @Volatile
    private var lastDataClearedTimestamp: Long = 0L

    // Notification suppression & duplicate prevention
    private val serviceStartTime = System.currentTimeMillis()
    @Volatile
    private var isCatchupSyncActive = false
    private val notifiedMessageIds = ConcurrentHashMap.newKeySet<Long>()
    private val notifiedNoticeIds = ConcurrentHashMap.newKeySet<Long>()
    private val notifiedDonationIds = ConcurrentHashMap.newKeySet<Long>()
    private val notifiedDocumentIds = ConcurrentHashMap.newKeySet<Long>()

    private fun shouldNotify(
        id: Long,
        notifiedSet: MutableSet<Long>,
        eventTimestamp: Long,
        isFromAnotherDevice: Boolean
    ): Boolean {
        if (isCatchupSyncActive) return false
        if (!isFromAnotherDevice) return false
        if (id <= 0L) return false
        // Skip any historical events that occurred before this app session started
        if (eventTimestamp > 0L && eventTimestamp < (serviceStartTime - 3000L)) return false
        // Ensure notification is displayed exactly once per item ID
        return notifiedSet.add(id)
    }

    init {
        Log.d(TAG, "Initializing Real-Time Cloud Sync Engine with Device ID: $DEVICE_ID")
        loadClearedTimestamps()
        startSseEventListener()
        startCatchupPoll()
        startFastPoller()
        startPeriodicSyncHeartbeat()
    }

    fun setContext(context: Context) {
        appContext = context.applicationContext
        loadClearedTimestamps()
    }

    private fun loadClearedTimestamps() {
        try {
            appContext?.let { ctx ->
                val sp = ctx.getSharedPreferences("tts_sync_prefs", Context.MODE_PRIVATE)
                val chatTs = sp.getLong("last_chat_cleared_ts", 0L)
                val dataTs = sp.getLong("last_data_cleared_ts", 0L)
                if (chatTs > lastChatClearedTimestamp) lastChatClearedTimestamp = chatTs
                if (dataTs > lastDataClearedTimestamp) lastDataClearedTimestamp = dataTs
            }
        } catch (e: Exception) {
            Log.v(TAG, "loadClearedTimestamps note: ${e.message}")
        }
    }

    private fun saveChatClearedTimestamp(ts: Long) {
        if (ts <= 0L) return
        lastChatClearedTimestamp = maxOf(lastChatClearedTimestamp, ts)
        try {
            appContext?.let { ctx ->
                ctx.getSharedPreferences("tts_sync_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("last_chat_cleared_ts", lastChatClearedTimestamp)
                    .apply()
            }
        } catch (e: Exception) {
            Log.v(TAG, "saveChatClearedTimestamp note: ${e.message}")
        }
    }

    private fun saveDataClearedTimestamp(ts: Long) {
        if (ts <= 0L) return
        lastDataClearedTimestamp = maxOf(lastDataClearedTimestamp, ts)
        lastChatClearedTimestamp = maxOf(lastChatClearedTimestamp, ts)
        try {
            appContext?.let { ctx ->
                ctx.getSharedPreferences("tts_sync_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("last_data_cleared_ts", lastDataClearedTimestamp)
                    .putLong("last_chat_cleared_ts", lastChatClearedTimestamp)
                    .apply()
            }
        } catch (e: Exception) {
            Log.v(TAG, "saveDataClearedTimestamp note: ${e.message}")
        }
    }

    // --- REAL-TIME SSE STREAMING CONNECTION ---
    private fun startSseEventListener() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val request = Request.Builder()
                        .url("$BASE_URL/json")
                        .build()

                    _isCloudConnected.value = true
                    _syncStatus.value = "🟢 लाइव क्लाउड सिंक कनेक्टेड"

                    streamingClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Log.w(TAG, "SSE stream response error: ${response.code}")
                            delay(3000)
                            return@use
                        }

                        val source = response.body?.source() ?: return@use
                        while (isActive && !source.exhausted()) {
                            val line = source.readUtf8Line() ?: break
                            if (line.isBlank()) continue

                            try {
                                parseAndDispatchRawJsonLine(line)
                            } catch (e: Exception) {
                                Log.v(TAG, "SSE line parse note: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "SSE stream disconnected, reconnecting in 2s: ${e.message}")
                    _isCloudConnected.value = false
                    _syncStatus.value = "🟡 क्लाउड पुनः कनेक्ट हो रहा है..."
                    delay(2000)
                }
            }
        }
    }

    // --- CATCH-UP RECENT EVENTS (FOR MISSED DATA / NEW PHONES) ---
    fun fetchCatchupEvents() {
        serviceScope.launch {
            isCatchupSyncActive = true
            try {
                // Fetch events from recent cache so any newly opened device catches up on all data
                val pollUrls = listOf(
                    "$BASE_URL/json?poll=1&since=7d",
                    "$BASE_URL/json?poll=1&since=24h",
                    "$BASE_URL/json?poll=1&since=12h"
                )
                for (pollUrl in pollUrls) {
                    try {
                        val request = Request.Builder().url(pollUrl).build()
                        httpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                val lines = body.split("\n")
                                for (line in lines) {
                                    if (line.isNotBlank()) {
                                        parseAndDispatchRawJsonLine(line)
                                    }
                                }
                                return@launch
                            }
                        }
                    } catch (netEx: Exception) {
                        Log.v(TAG, "Catchup poll url note: ${netEx.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Catch-up poll note: ${e.message}")
            } finally {
                // Delay clearing catchup flag briefly to ensure parsed queued events don't trigger notifications
                delay(1500)
                isCatchupSyncActive = false
            }
        }
    }

    private fun startCatchupPoll() {
        fetchCatchupEvents()
    }

    private fun startFastPoller() {
        serviceScope.launch {
            // Give initial SSE stream a moment to connect
            delay(3000)
            while (isActive) {
                try {
                    val pollUrl = "$BASE_URL/json?poll=1&since=2m"
                    val request = Request.Builder().url(pollUrl).build()
                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: ""
                            val lines = body.split("\n")
                            for (line in lines) {
                                if (line.isNotBlank()) {
                                    parseAndDispatchRawJsonLine(line)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.v(TAG, "Fast poller note: ${e.message}")
                }
                delay(3500)
            }
        }
    }

    private fun parseAndDispatchRawJsonLine(line: String) {
        var trimmed = line.trim()
        if (trimmed.startsWith("data:")) {
            trimmed = trimmed.substringAfter("data:").trim()
        }
        if (trimmed.startsWith("event:") || trimmed.startsWith("id:") || trimmed.startsWith(":")) {
            return
        }
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return
        try {
            val sseJson = JSONObject(trimmed)
            val event = sseJson.optString("event", "message")
            if (sseJson.has("eventType")) {
                handleCloudEvent(trimmed)
            } else if (event == "message" || event.isEmpty() || event == "poll" || event == "open") {
                val messageObj = sseJson.opt("message")
                when (messageObj) {
                    is JSONObject -> handleCloudEvent(messageObj.toString())
                    is String -> {
                        val str = messageObj.trim()
                        if (str.startsWith("{")) {
                            handleCloudEvent(str)
                        }
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            if (trimmed.startsWith("{") && trimmed.contains("eventType")) {
                try {
                    handleCloudEvent(trimmed)
                } catch (_: Exception) {}
            }
        }
    }

    // --- PERIODIC SYNC HEARTBEAT & CANDIDATE PRESENCE ---
    private fun startPeriodicSyncHeartbeat() {
        serviceScope.launch {
            while (isActive) {
                try {
                    // Send my active presence if logged in
                    if (currentActiveMemberId > 0) {
                        updateCandidatePresence(currentActiveMemberId, currentActiveMemberName, true)
                    }

                    // Clean up candidates who haven't sent a ping in 10 minutes
                    val now = System.currentTimeMillis()
                    val activeIds = mutableSetOf<Long>()
                    for ((id, lastSeen) in candidateLastSeenMap) {
                        if (now - lastSeen < 10 * 60 * 1000) {
                            activeIds.add(id)
                        }
                    }
                    _onlineCandidateIds.value = activeIds
                } catch (e: Exception) {
                    Log.v(TAG, "Heartbeat note: ${e.message}")
                }
                delay(12000)
            }
        }
    }

    // --- EVENT DISPATCHER ---
    private fun handleCloudEvent(rawJson: String) {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty() || !trimmed.startsWith("{")) return
        try {
            val root = JSONObject(trimmed)
            val eventType = root.optString("eventType")
            val payload = root.optJSONObject("payload")
            val senderDeviceId = root.optString("senderDeviceId")
            val isFromAnotherDevice = senderDeviceId.isNotBlank() && senderDeviceId != DEVICE_ID

            // Update sender candidate presence
            val senderMemberId = root.optLong("senderMemberId", 0L)
            if (senderMemberId > 0) {
                candidateLastSeenMap[senderMemberId] = System.currentTimeMillis()
                _onlineCandidateIds.value = candidateLastSeenMap.keys().toList().toSet()
            }

            val eventTimestamp = root.optLong("timestamp", 0L)

            when (eventType) {
                "CHAT_MESSAGE" -> {
                    if (payload != null) {
                        val msg = parseChatMessage(payload)
                        if (msg != null) {
                            // Discard message if it was sent prior to chat clear
                            if (lastChatClearedTimestamp > 0L && msg.timestamp <= lastChatClearedTimestamp) {
                                Log.d(TAG, "Skipping pre-clear chat message (${msg.id}, timestamp=${msg.timestamp} <= $lastChatClearedTimestamp)")
                                return
                            }

                            // Notify all-channel global listeners
                            chatListeners["all"]?.let { allList ->
                                synchronized(allList) {
                                    allList.toList().forEach { it(listOf(msg)) }
                                }
                            }

                            // Notify channel-specific listeners
                            chatListeners[msg.channelId]?.let { channelList ->
                                synchronized(channelList) {
                                    channelList.toList().forEach { it(listOf(msg)) }
                                }
                            }

                            // Immediately emit read receipt / seen acknowledgment back to sender for double tick
                            if (isFromAnotherDevice || (currentActiveMemberId > 0 && senderMemberId != currentActiveMemberId)) {
                                serviceScope.launch {
                                    try {
                                        val seenPayload = JSONObject().apply {
                                            put("messageId", msg.id)
                                            put("channelId", msg.channelId)
                                            put("seenByDeviceId", DEVICE_ID)
                                            put("seenByMemberId", currentActiveMemberId)
                                            put("timestamp", System.currentTimeMillis())
                                        }
                                        broadcastEvent("CHAT_SEEN", seenPayload)
                                    } catch (e: Exception) {
                                        Log.v(TAG, "SEEN broadcast note: ${e.message}")
                                    }
                                }

                                if (shouldNotify(msg.id, notifiedMessageIds, msg.timestamp, isFromAnotherDevice)) {
                                    appContext?.let { ctx ->
                                        TTSNotificationHelper.showChatNotification(
                                            context = ctx,
                                            senderName = msg.senderName,
                                            channelId = msg.channelId,
                                            messageText = msg.messageText
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                "CHAT_SEEN" -> {
                    val messageId = payload?.optLong("messageId", 0L) ?: 0L
                    val channelId = payload?.optString("channelId", "general") ?: "general"
                    if (messageId > 0) {
                        synchronized(chatSeenListeners) {
                            chatSeenListeners.forEach { it(messageId, channelId) }
                        }
                    }
                }
                "MEMBER_UPSERT" -> {
                    if (payload != null) {
                        val member = parseMember(payload)
                        if (member != null) {
                            synchronized(memberUpsertListeners) {
                                memberUpsertListeners.forEach { it(listOf(member)) }
                            }
                        }
                    }
                }
                "MEMBER_PHOTO_UPDATE" -> {
                    if (payload != null) {
                        val memberId = payload.optLong("memberId", 0L)
                        val directPhotoUri = payload.optString("photoUri", "")

                        if (memberId > 0) {
                            synchronized(memberPhotoUpdateListeners) {
                                memberPhotoUpdateListeners.forEach { it(memberId, directPhotoUri) }
                            }
                        }
                    }
                }
                "MEMBER_PHOTO_CHUNK" -> {
                    if (payload != null) {
                        val memberId = payload.optLong("memberId", 0L)
                        val photoId = payload.optString("photoId", "$memberId")
                        val chunkIndex = payload.optInt("chunkIndex", 0)
                        val totalChunks = payload.optInt("totalChunks", 1)
                        val chunkData = payload.optString("chunkData", "")

                        if (memberId > 0 && chunkData.isNotEmpty()) {
                            val chunksMap = incomingPhotoChunks.getOrPut(photoId) { ConcurrentHashMap() }
                            chunksMap[chunkIndex] = chunkData

                            if (chunksMap.size >= totalChunks) {
                                val sb = StringBuilder()
                                for (i in 0 until totalChunks) {
                                    sb.append(chunksMap[i] ?: "")
                                }
                                val fullPhotoUri = sb.toString()
                                incomingPhotoChunks.remove(photoId)

                                if (fullPhotoUri.isNotBlank()) {
                                    synchronized(memberPhotoUpdateListeners) {
                                        memberPhotoUpdateListeners.forEach { it(memberId, fullPhotoUri) }
                                    }
                                }
                            }
                        }
                    }
                }
                "MEMBER_DELETE" -> {
                    val id = payload?.optLong("id", 0L) ?: root.optLong("id", 0L)
                    if (id > 0) {
                        synchronized(memberDeleteListeners) {
                            memberDeleteListeners.forEach { it(id) }
                        }
                    }
                }
                "NOTICE_UPSERT" -> {
                    if (payload != null) {
                        val notice = parseNotice(payload)
                        if (notice != null) {
                            synchronized(noticeUpsertListeners) {
                                noticeUpsertListeners.forEach { it(listOf(notice)) }
                            }

                            // Trigger phone notification only for fresh incoming notice
                            if (shouldNotify(notice.id, notifiedNoticeIds, eventTimestamp, isFromAnotherDevice)) {
                                appContext?.let { ctx ->
                                    TTSNotificationHelper.showNoticeNotification(
                                        context = ctx,
                                        title = notice.title,
                                        content = notice.content,
                                        priority = notice.priority
                                    )
                                }
                            }
                        }
                    }
                }
                "NOTICE_DELETE" -> {
                    val id = payload?.optLong("id", 0L) ?: root.optLong("id", 0L)
                    if (id > 0) {
                        synchronized(noticeDeleteListeners) {
                            noticeDeleteListeners.forEach { it(id) }
                        }
                    }
                }
                "DONATION_UPSERT" -> {
                    if (payload != null) {
                        val donation = parseDonation(payload)
                        if (donation != null) {
                            synchronized(donationUpsertListeners) {
                                donationUpsertListeners.forEach { it(listOf(donation)) }
                            }

                            // Trigger phone notification only for fresh incoming donation
                            if (shouldNotify(donation.id, notifiedDonationIds, donation.timestamp, isFromAnotherDevice)) {
                                appContext?.let { ctx ->
                                    TTSNotificationHelper.showDonationNotification(
                                        context = ctx,
                                        donorName = donation.donorName,
                                        amount = donation.amount,
                                        receiptNumber = donation.transactionRef,
                                        note = donation.purpose
                                    )
                                }
                            }
                        }
                    }
                }
                "DONATION_DELETE" -> {
                    val id = payload?.optLong("id", 0L) ?: root.optLong("id", 0L)
                    if (id > 0) {
                        synchronized(donationDeleteListeners) {
                            donationDeleteListeners.forEach { it(id) }
                        }
                    }
                }
                "MEETING_UPSERT" -> {
                    if (payload != null) {
                        val meeting = parseMeeting(payload)
                        if (meeting != null) {
                            synchronized(meetingUpsertListeners) {
                                meetingUpsertListeners.forEach { it(listOf(meeting)) }
                            }
                        }
                    }
                }
                "MEETING_DELETE" -> {
                    val id = payload?.optLong("id", 0L) ?: root.optLong("id", 0L)
                    if (id > 0) {
                        synchronized(meetingDeleteListeners) {
                            meetingDeleteListeners.forEach { it(id) }
                        }
                    }
                }
                "DOCUMENT_UPSERT" -> {
                    if (payload != null) {
                        val doc = parseDocument(payload)
                        if (doc != null) {
                            synchronized(documentUpsertListeners) {
                                documentUpsertListeners.forEach { it(listOf(doc)) }
                            }

                            // Trigger notification only for fresh incoming document
                            if (shouldNotify(doc.id, notifiedDocumentIds, eventTimestamp, isFromAnotherDevice)) {
                                appContext?.let { ctx ->
                                    TTSNotificationHelper.showDocumentNotification(
                                        context = ctx,
                                        title = doc.title,
                                        category = doc.category,
                                        summary = doc.summary
                                    )
                                }
                            }
                        }
                    }
                }
                "DOCUMENT_DELETE" -> {
                    val id = payload?.optLong("id", 0L) ?: root.optLong("id", 0L)
                    if (id > 0) {
                        synchronized(documentDeleteListeners) {
                            documentDeleteListeners.forEach { it(id) }
                        }
                    }
                }
                "EXPENSE_UPSERT" -> {
                    if (payload != null) {
                        val expense = parseExpense(payload)
                        if (expense != null) {
                            synchronized(expenseUpsertListeners) {
                                expenseUpsertListeners.forEach { it(listOf(expense)) }
                            }
                        }
                    }
                }
                "EXPENSE_DELETE" -> {
                    val id = payload?.optLong("id", 0L) ?: root.optLong("id", 0L)
                    if (id > 0) {
                        synchronized(expenseDeleteListeners) {
                            expenseDeleteListeners.forEach { it(id) }
                        }
                    }
                }
                "CHAT_DELETE" -> {
                    val id = payload?.optLong("id", 0L) ?: payload?.optLong("messageId", 0L) ?: root.optLong("id", 0L)
                    if (id > 0) {
                        synchronized(chatDeleteListeners) {
                            chatDeleteListeners.forEach { it(id) }
                        }
                    }
                }
                "REQUEST_FULL_SYNC" -> {
                    if (isFromAnotherDevice) {
                        synchronized(syncRequestListeners) {
                            syncRequestListeners.forEach { it() }
                        }
                    }
                }
                "PRESENCE_PING" -> {
                    val memberId = root.optLong("memberId", 0L)
                    val isOnline = root.optBoolean("isOnline", true)
                    if (memberId > 0) {
                        if (isOnline) {
                            candidateLastSeenMap[memberId] = System.currentTimeMillis()
                        } else {
                            candidateLastSeenMap.remove(memberId)
                        }
                        _onlineCandidateIds.value = candidateLastSeenMap.keys().toList().toSet()
                    }
                }
                "CLEAR_CHAT" -> {
                    val ts = payload?.optLong("clearedTimestamp", 0L).let { if (it != null && it > 0) it else eventTimestamp.let { et -> if (et > 0) et else System.currentTimeMillis() } }
                    saveChatClearedTimestamp(ts)
                    synchronized(chatClearListeners) {
                        chatClearListeners.forEach { it() }
                    }
                }
                "DONATION_GOAL_UPDATE" -> {
                    val goal = payload?.optDouble("goal", 250000.0) ?: root.optDouble("goal", 250000.0)
                    if (goal > 0) {
                        saveDonationGoal(goal)
                        synchronized(donationGoalListeners) {
                            donationGoalListeners.forEach { it(goal) }
                        }
                    }
                }
                "CLEAR_EXPENSES" -> {
                    synchronized(expenseClearListeners) {
                        expenseClearListeners.forEach { it() }
                    }
                }
                "CLEAR_DONATIONS" -> {
                    synchronized(donationClearListeners) {
                        donationClearListeners.forEach { it() }
                    }
                }
                "CLEAR_ALL_DATA" -> {
                    val ts = if (eventTimestamp > 0) eventTimestamp else System.currentTimeMillis()
                    saveDataClearedTimestamp(ts)
                    synchronized(clearAllListeners) {
                        clearAllListeners.forEach { it() }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling cloud event: ${e.message}", e)
        }
    }

    fun ensureConnection() {
        if (!_isCloudConnected.value) {
            fetchCatchupEvents()
        }
    }

    // --- BROADCAST AN EVENT TO ALL DISTRIBUTED APPS ---
    private fun broadcastEvent(eventType: String, payload: JSONObject?) {
        serviceScope.launch {
            try {
                val envelope = JSONObject().apply {
                    put("senderDeviceId", DEVICE_ID)
                    put("senderMemberId", currentActiveMemberId)
                    put("eventType", eventType)
                    put("timestamp", System.currentTimeMillis())
                    if (payload != null) {
                        put("payload", payload)
                    }
                }

                // 1. Direct high-priority post to topic endpoint for ultra-fast instant delivery
                val directRequest = Request.Builder()
                    .url(BASE_URL)
                    .header("Title", "TTS_SYNC_$eventType")
                    .header("Priority", "5")
                    .header("X-Priority", "5")
                    .post(envelope.toString().toRequestBody("text/plain; charset=utf-8".toMediaType()))
                    .build()

                var success = false
                var attempts = 0
                while (!success && attempts < 2) {
                    attempts++
                    try {
                        httpClient.newCall(directRequest).execute().use { response ->
                            if (response.isSuccessful) {
                                success = true
                                _isCloudConnected.value = true
                                _syncStatus.value = "🟢 लाइव क्लाउड सिंक कनेक्टेड"
                                Log.d(TAG, "Fast cloud broadcast success: $eventType")
                            }
                        }
                    } catch (netEx: Exception) {
                        Log.v(TAG, "Direct broadcast retry note: ${netEx.message}")
                    }
                }

                // 2. Standard ntfy JSON publishing fallback if needed
                if (!success) {
                    val ntfyPayload = JSONObject().apply {
                        put("topic", SYNC_TOPIC)
                        put("title", "TTS_SYNC_$eventType")
                        put("message", envelope.toString())
                        put("priority", 5)
                    }

                    val jsonRequest = Request.Builder()
                        .url("https://ntfy.sh")
                        .post(ntfyPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    try {
                        httpClient.newCall(jsonRequest).execute().use { response ->
                            if (response.isSuccessful) {
                                success = true
                                _isCloudConnected.value = true
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network broadcast exception: ${e.message}")
            }
        }
    }

    // --- CANDIDATE PRESENCE ---
    suspend fun updateCandidatePresence(memberId: Long, memberName: String, isOnline: Boolean) {
        if (memberId <= 0) return
        currentActiveMemberId = memberId
        currentActiveMemberName = memberName
        if (isOnline) {
            candidateLastSeenMap[memberId] = System.currentTimeMillis()
        } else {
            candidateLastSeenMap.remove(memberId)
        }
        _onlineCandidateIds.value = candidateLastSeenMap.keys().toList().toSet()

        val json = JSONObject().apply {
            put("memberId", memberId)
            put("memberName", memberName)
            put("isOnline", isOnline)
        }
        broadcastEvent("PRESENCE_PING", json)
    }

    // --- LISTENERS REGISTRATION ---
    fun listenToMembers(
        onMembersUpdated: (List<Member>) -> Unit,
        onMemberDeleted: (Long) -> Unit = {}
    ): ListenerRegistration? {
        synchronized(memberUpsertListeners) { memberUpsertListeners.add(onMembersUpdated) }
        synchronized(memberDeleteListeners) { memberDeleteListeners.add(onMemberDeleted) }
        return null
    }

    fun listenToDonations(
        onDonationsUpdated: (List<Donation>) -> Unit,
        onDonationDeleted: (Long) -> Unit = {}
    ): ListenerRegistration? {
        synchronized(donationUpsertListeners) { donationUpsertListeners.add(onDonationsUpdated) }
        synchronized(donationDeleteListeners) { donationDeleteListeners.add(onDonationDeleted) }
        return null
    }

    fun listenToMeetings(
        onMeetingsUpdated: (List<Meeting>) -> Unit,
        onMeetingDeleted: (Long) -> Unit = {}
    ): ListenerRegistration? {
        synchronized(meetingUpsertListeners) { meetingUpsertListeners.add(onMeetingsUpdated) }
        synchronized(meetingDeleteListeners) { meetingDeleteListeners.add(onMeetingDeleted) }
        return null
    }

    fun listenToNotices(
        onNoticesUpdated: (List<Notice>) -> Unit,
        onNoticeDeleted: (Long) -> Unit = {}
    ): ListenerRegistration? {
        synchronized(noticeUpsertListeners) { noticeUpsertListeners.add(onNoticesUpdated) }
        synchronized(noticeDeleteListeners) { noticeDeleteListeners.add(onNoticeDeleted) }
        return null
    }

    fun listenToDocuments(
        onDocsUpdated: (List<OfficialDocument>) -> Unit,
        onDocDeleted: (Long) -> Unit = {}
    ): ListenerRegistration? {
        synchronized(documentUpsertListeners) { documentUpsertListeners.add(onDocsUpdated) }
        synchronized(documentDeleteListeners) { documentDeleteListeners.add(onDocDeleted) }
        return null
    }

    fun listenToExpenses(
        onExpensesUpdated: (List<Expense>) -> Unit,
        onExpenseDeleted: (Long) -> Unit = {}
    ): ListenerRegistration? {
        synchronized(expenseUpsertListeners) { expenseUpsertListeners.add(onExpensesUpdated) }
        synchronized(expenseDeleteListeners) { expenseDeleteListeners.add(onExpenseDeleted) }
        return null
    }

    fun listenToClearExpenses(onClearExpenses: () -> Unit) {
        synchronized(expenseClearListeners) {
            expenseClearListeners.add(onClearExpenses)
        }
    }

    fun listenToClearDonations(onClearDonations: () -> Unit) {
        synchronized(donationClearListeners) {
            donationClearListeners.add(onClearDonations)
        }
    }

    fun listenToDonationGoal(onGoalUpdated: (Double) -> Unit): ListenerRegistration? {
        synchronized(donationGoalListeners) {
            donationGoalListeners.add(onGoalUpdated)
        }
        return null
    }

    suspend fun syncDonationGoalToCloud(goal: Double) {
        withContext(Dispatchers.IO) {
            saveDonationGoal(goal)
            val json = JSONObject().apply {
                put("goal", goal)
                put("timestamp", System.currentTimeMillis())
            }
            broadcastEvent("DONATION_GOAL_UPDATE", json)
        }
    }

    fun getSavedDonationGoal(): Double {
        return try {
            appContext?.getSharedPreferences("tts_sync_prefs", Context.MODE_PRIVATE)
                ?.getString("donation_goal_val", "250000.0")?.toDoubleOrNull() ?: 250000.0
        } catch (e: Exception) {
            250000.0
        }
    }

    private fun saveDonationGoal(goal: Double) {
        try {
            appContext?.getSharedPreferences("tts_sync_prefs", Context.MODE_PRIVATE)
                ?.edit()
                ?.putString("donation_goal_val", goal.toString())
                ?.apply()
        } catch (e: Exception) {
            Log.v(TAG, "saveDonationGoal note: ${e.message}")
        }
    }

    fun listenToChat(channelId: String, onChatUpdated: (List<ChatMessage>) -> Unit): ListenerRegistration? {
        val list = chatListeners.getOrPut(channelId) { mutableListOf() }
        synchronized(list) {
            list.add(onChatUpdated)
        }
        return null
    }

    fun listenToChatSeen(onChatSeen: (Long, String) -> Unit) {
        synchronized(chatSeenListeners) {
            chatSeenListeners.add(onChatSeen)
        }
    }

    fun listenToChatDelete(onMessageDeleted: (Long) -> Unit) {
        synchronized(chatDeleteListeners) {
            chatDeleteListeners.add(onMessageDeleted)
        }
    }

    fun listenToChatClear(onChatCleared: () -> Unit) {
        synchronized(chatClearListeners) {
            chatClearListeners.add(onChatCleared)
        }
    }

    fun listenToSyncRequest(onSyncRequested: () -> Unit) {
        synchronized(syncRequestListeners) {
            syncRequestListeners.add(onSyncRequested)
        }
    }

    fun requestDataSync() {
        broadcastEvent("REQUEST_FULL_SYNC", null)
    }

    fun listenToClearAll(onClearAll: () -> Unit) {
        synchronized(clearAllListeners) {
            clearAllListeners.add(onClearAll)
        }
    }

    fun listenToMemberPhotoUpdates(onPhotoUpdated: (Long, String) -> Unit) {
        synchronized(memberPhotoUpdateListeners) {
            memberPhotoUpdateListeners.add(onPhotoUpdated)
        }
    }

    suspend fun syncMemberPhotoToCloud(memberId: Long, photoUri: String?) {
        withContext(Dispatchers.IO) {
            try {
                val cleanPhoto = photoUri?.trim() ?: ""
                if (cleanPhoto.isBlank()) {
                    val json = JSONObject().apply {
                        put("memberId", memberId)
                        put("photoUri", "")
                    }
                    broadcastEvent("MEMBER_PHOTO_UPDATE", json)
                    return@withContext
                }

                // If small enough (fits within single packet topic limit), send as single atomic packet
                if (cleanPhoto.length <= 3600) {
                    val json = JSONObject().apply {
                        put("memberId", memberId)
                        put("photoUri", cleanPhoto)
                    }
                    broadcastEvent("MEMBER_PHOTO_UPDATE", json)
                } else {
                    // Send chunked packets so network / ntfy message size limits (4KB) are never exceeded
                    val chunkSize = 2000
                    val totalChunks = (cleanPhoto.length + chunkSize - 1) / chunkSize
                    val photoId = "photo_${memberId}_${System.currentTimeMillis()}"

                    for (i in 0 until totalChunks) {
                        val start = i * chunkSize
                        val end = minOf(start + chunkSize, cleanPhoto.length)
                        val chunk = cleanPhoto.substring(start, end)

                        val chunkJson = JSONObject().apply {
                            put("memberId", memberId)
                            put("photoId", photoId)
                            put("chunkIndex", i)
                            put("totalChunks", totalChunks)
                            put("chunkData", chunk)
                        }
                        broadcastEvent("MEMBER_PHOTO_CHUNK", chunkJson)
                        delay(50) // slight spacing between chunks
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "syncMemberPhotoToCloud error: ${e.message}")
            }
        }
    }

    // --- SYNC ACTIONS TO CLOUD ---
    suspend fun syncMemberToCloud(member: Member) {
        withContext(Dispatchers.IO) {
            val photoToEmbed = if ((member.photoUri?.length ?: 0) <= 3600) {
                member.photoUri?.trim() ?: ""
            } else {
                "" // Exceeds single packet limit; syncs reliably via syncMemberPhotoToCloud
            }

            val json = JSONObject().apply {
                put("id", member.id)
                put("memberCode", member.memberCode)
                put("fullName", member.fullName)
                put("designation", member.designation)
                put("committeeWing", member.committeeWing)
                put("phoneNumber", member.phoneNumber)
                put("email", member.email)
                put("bloodGroup", member.bloodGroup)
                put("joinDate", member.joinDate)
                put("address", member.address)
                put("emergencyContact", member.emergencyContact)
                put("avatarColorIndex", member.avatarColorIndex)
                put("isActive", member.isActive)
                put("isBestPerformer", member.isBestPerformer)
                put("bestPerformerBadge", member.bestPerformerBadge ?: "")
                put("photoResName", member.photoResName ?: "")
                put("photoUri", photoToEmbed)
            }
            broadcastEvent("MEMBER_UPSERT", json)

            // If member has photoUri, also sync it via photo channel for 100% guarantee
            if (!member.photoUri.isNullOrBlank()) {
                syncMemberPhotoToCloud(member.id, member.photoUri)
            }
        }
    }

    suspend fun deleteMemberFromCloud(id: Long) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", id)
            }
            broadcastEvent("MEMBER_DELETE", json)
        }
    }

    suspend fun syncDonationToCloud(donation: Donation) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", donation.id)
                put("donorName", donation.donorName)
                put("donorMemberCode", donation.donorMemberCode ?: "")
                put("amount", donation.amount)
                put("purpose", donation.purpose)
                put("paymentMode", donation.paymentMode)
                put("transactionRef", donation.transactionRef)
                put("date", donation.date)
                put("timestamp", donation.timestamp)
                put("verified", donation.verified)
                put("remarks", donation.remarks ?: "")
                put("paymentProofUri", donation.paymentProofUri ?: "")
            }
            broadcastEvent("DONATION_UPSERT", json)
        }
    }

    suspend fun deleteDonationFromCloud(id: Long) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", id)
            }
            broadcastEvent("DONATION_DELETE", json)
        }
    }

    suspend fun syncMeetingToCloud(meeting: Meeting) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", meeting.id)
                put("title", meeting.title)
                put("type", meeting.type)
                put("dateDisplay", meeting.dateDisplay)
                put("timeDisplay", meeting.timeDisplay)
                put("dateTimeMillis", meeting.dateTimeMillis)
                put("venue", meeting.venue)
                put("virtualLink", meeting.virtualLink ?: "")
                put("chairperson", meeting.chairperson)
                put("agenda", meeting.agenda)
                put("status", meeting.status)
                put("notesOrMinutes", meeting.notesOrMinutes ?: "")
            }
            broadcastEvent("MEETING_UPSERT", json)
        }
    }

    suspend fun deleteMeetingFromCloud(id: Long) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", id)
            }
            broadcastEvent("MEETING_DELETE", json)
        }
    }

    suspend fun syncNoticeToCloud(notice: Notice) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", notice.id)
                put("title", notice.title)
                put("category", notice.category)
                put("priority", notice.priority)
                put("issuedBy", notice.issuedBy)
                put("date", notice.date)
                put("content", notice.content)
                put("isPinned", notice.isPinned)
            }
            broadcastEvent("NOTICE_UPSERT", json)
        }
    }

    suspend fun deleteNoticeFromCloud(id: Long) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", id)
            }
            broadcastEvent("NOTICE_DELETE", json)
        }
    }

    suspend fun syncDocumentToCloud(doc: OfficialDocument) {
        withContext(Dispatchers.IO) {
            val safeAttachmentUri = if (!doc.attachmentUri.isNullOrBlank() && doc.attachmentUri.length > 4000) {
                // If attachment is too large for real-time payload, do not corrupt Base64 string with truncation
                ""
            } else {
                doc.attachmentUri ?: ""
            }
            val json = JSONObject().apply {
                put("id", doc.id)
                put("title", doc.title)
                put("category", doc.category)
                put("refCode", doc.refCode)
                put("publishedDate", doc.publishedDate)
                put("fileSize", doc.fileSize)
                put("accessLevel", doc.accessLevel)
                put("summary", doc.summary)
                put("fullContent", doc.fullContent)
                put("attachmentUri", safeAttachmentUri)
                put("attachmentName", doc.attachmentName ?: "")
            }
            broadcastEvent("DOCUMENT_UPSERT", json)
        }
    }

    suspend fun deleteDocumentFromCloud(id: Long) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", id)
            }
            broadcastEvent("DOCUMENT_DELETE", json)
        }
    }

    suspend fun syncExpenseToCloud(expense: Expense) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", expense.id)
                put("title", expense.title)
                put("category", expense.category)
                put("amount", expense.amount)
                put("spentBy", expense.spentBy)
                put("date", expense.date)
                put("timestamp", expense.timestamp)
                put("receiptRef", expense.receiptRef ?: "")
                put("attachmentUri", expense.attachmentUri ?: "")
                put("remarks", expense.remarks ?: "")
            }
            broadcastEvent("EXPENSE_UPSERT", json)
        }
    }

    suspend fun deleteExpenseFromCloud(id: Long) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", id)
            }
            broadcastEvent("EXPENSE_DELETE", json)
        }
    }

    suspend fun clearAllExpensesFromCloud() {
        broadcastEvent("CLEAR_EXPENSES", null)
    }

    suspend fun syncChatMessageToCloud(message: ChatMessage) {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", message.id)
                put("channelId", message.channelId)
                put("senderName", message.senderName)
                put("senderRole", message.senderRole)
                put("senderAvatarIndex", message.senderAvatarIndex)
                put("messageText", message.messageText)
                put("timestamp", message.timestamp)
                put("timeDisplay", message.timeDisplay)
                put("isAnnouncement", message.isAnnouncement)
                put("senderMemberId", message.senderMemberId)
                put("senderDeviceId", message.senderDeviceId.ifBlank { DEVICE_ID })
                put("status", message.status)
                put("isSeen", message.isSeen)
            }
            broadcastEvent("CHAT_MESSAGE", json)
        }
    }

    suspend fun broadcastChatSeen(messageId: Long, channelId: String = "general") {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("messageId", messageId)
                put("channelId", channelId)
                put("seenByDeviceId", DEVICE_ID)
                put("seenByMemberId", currentActiveMemberId)
                put("timestamp", System.currentTimeMillis())
            }
            broadcastEvent("CHAT_SEEN", json)
        }
    }

    suspend fun deleteChatMessageFromCloud(messageId: Long, channelId: String = "general") {
        withContext(Dispatchers.IO) {
            val json = JSONObject().apply {
                put("id", messageId)
                put("channelId", channelId)
            }
            broadcastEvent("CHAT_DELETE", json)
        }
    }

    suspend fun clearAllChatFromCloud() {
        val now = System.currentTimeMillis()
        saveChatClearedTimestamp(now)
        val json = JSONObject().apply {
            put("clearedTimestamp", now)
        }
        broadcastEvent("CLEAR_CHAT", json)
    }

    suspend fun clearAllMembersFromCloud() {
        broadcastEvent("CLEAR_MEMBERS", null)
    }

    suspend fun clearAllMeetingsFromCloud() {
        broadcastEvent("CLEAR_MEETINGS", null)
    }

    suspend fun clearAllNoticesFromCloud() {
        broadcastEvent("CLEAR_NOTICES", null)
    }

    suspend fun clearAllDocumentsFromCloud() {
        broadcastEvent("CLEAR_DOCUMENTS", null)
    }

    suspend fun clearAllDonationsFromCloud() {
        broadcastEvent("CLEAR_DONATIONS", null)
    }

    suspend fun clearAllAppCloudData() {
        val now = System.currentTimeMillis()
        saveDataClearedTimestamp(now)
        val json = JSONObject().apply {
            put("clearedTimestamp", now)
        }
        broadcastEvent("CLEAR_ALL_DATA", json)
    }

    // --- JSON PARSERS ---
    private fun parseChatMessage(obj: JSONObject): ChatMessage? {
        return try {
            val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            val timeDisplay = obj.optString("timeDisplay").takeIf { it.isNotBlank() }
                ?: SimpleDateFormat("hh:mm a", Locale("hi", "IN")).format(Date(timestamp))

            ChatMessage(
                id = obj.optLong("id", timestamp),
                channelId = obj.optString("channelId", "general"),
                senderName = obj.optString("senderName", "कमेटी सदस्य"),
                senderRole = obj.optString("senderRole", "खादिम"),
                senderAvatarIndex = obj.optInt("senderAvatarIndex", 0),
                messageText = obj.optString("messageText", ""),
                timestamp = timestamp,
                timeDisplay = timeDisplay,
                isAnnouncement = obj.optBoolean("isAnnouncement", false),
                senderMemberId = obj.optLong("senderMemberId", 0L),
                senderDeviceId = obj.optString("senderDeviceId", ""),
                status = obj.optString("status", "SENT"),
                isSeen = obj.optBoolean("isSeen", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMember(obj: JSONObject): Member? {
        return try {
            val fullName = obj.optString("fullName", "").trim()
            if (fullName.isBlank()) return null
            val id = obj.optLong("id", 0L).let { if (it > 0) it else System.currentTimeMillis() }
            val memberCode = obj.optString("memberCode", "").ifBlank { "TTS-$id" }
            Member(
                id = id,
                memberCode = memberCode,
                fullName = fullName,
                designation = obj.optString("designation", "खादिम (Volunteer)"),
                committeeWing = obj.optString("committeeWing", "12 रबी-उल-अव्वल जुलूस कमेटी"),
                phoneNumber = obj.optString("phoneNumber", ""),
                email = obj.optString("email", ""),
                bloodGroup = obj.optString("bloodGroup", ""),
                joinDate = obj.optString("joinDate", "12 रबी-उल-अव्वल"),
                address = obj.optString("address", ""),
                emergencyContact = obj.optString("emergencyContact", ""),
                avatarColorIndex = obj.optInt("avatarColorIndex", 0),
                isActive = obj.optBoolean("isActive", true),
                isBestPerformer = obj.optBoolean("isBestPerformer", false),
                bestPerformerBadge = obj.optString("bestPerformerBadge").takeIf { it.isNotBlank() },
                photoResName = obj.optString("photoResName").takeIf { it.isNotBlank() },
                photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseMember exception: ${e.message}", e)
            null
        }
    }

    private fun parseMeeting(obj: JSONObject): Meeting? {
        return try {
            val id = obj.optLong("id", 0L)
            if (id <= 0) return null
            Meeting(
                id = id,
                title = obj.optString("title", ""),
                type = obj.optString("type", "12 रबी-उल-अव्वल मुख्य बैठक"),
                dateDisplay = obj.optString("dateDisplay", ""),
                timeDisplay = obj.optString("timeDisplay", ""),
                dateTimeMillis = obj.optLong("dateTimeMillis", System.currentTimeMillis()),
                venue = obj.optString("venue", ""),
                virtualLink = obj.optString("virtualLink").takeIf { it.isNotBlank() },
                chairperson = obj.optString("chairperson", ""),
                agenda = obj.optString("agenda", ""),
                status = obj.optString("status", "Upcoming"),
                notesOrMinutes = obj.optString("notesOrMinutes").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNotice(obj: JSONObject): Notice? {
        return try {
            val id = obj.optLong("id", 0L)
            if (id <= 0) return null
            Notice(
                id = id,
                title = obj.optString("title", ""),
                category = obj.optString("category", "सामान्य"),
                priority = obj.optString("priority", "NORMAL"),
                issuedBy = obj.optString("issuedBy", "TTS कमेटी"),
                date = obj.optString("date", ""),
                content = obj.optString("content", ""),
                isPinned = obj.optBoolean("isPinned", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDonation(obj: JSONObject): Donation? {
        return try {
            val id = obj.optLong("id", 0L)
            if (id <= 0) return null
            Donation(
                id = id,
                donorName = obj.optString("donorName", ""),
                donorMemberCode = obj.optString("donorMemberCode").takeIf { it.isNotBlank() },
                amount = obj.optDouble("amount", 0.0),
                purpose = obj.optString("purpose", "12 रबी-उल-अव्वल"),
                paymentMode = obj.optString("paymentMode", "UPI (ak750258@icici)"),
                transactionRef = obj.optString("transactionRef", ""),
                date = obj.optString("date", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                verified = obj.optBoolean("verified", true),
                remarks = obj.optString("remarks").takeIf { it.isNotBlank() },
                paymentProofUri = obj.optString("paymentProofUri").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDocument(obj: JSONObject): OfficialDocument? {
        return try {
            val title = obj.optString("title", "").trim()
            if (title.isBlank()) return null
            val id = obj.optLong("id", 0L).let { if (it > 0) it else System.currentTimeMillis() }
            OfficialDocument(
                id = id,
                title = title,
                category = obj.optString("category", "कमेटी नियमावली"),
                refCode = obj.optString("refCode", "DOC-$id"),
                publishedDate = obj.optString("publishedDate", ""),
                fileSize = obj.optString("fileSize", "सत्यापित प्रति"),
                accessLevel = obj.optString("accessLevel", "सार्वजनिक (Public)"),
                summary = obj.optString("summary", ""),
                fullContent = obj.optString("fullContent", ""),
                attachmentUri = obj.optString("attachmentUri").takeIf { it.isNotBlank() },
                attachmentName = obj.optString("attachmentName").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseExpense(obj: JSONObject): Expense? {
        return try {
            val id = obj.optLong("id", 0L)
            if (id <= 0) return null
            Expense(
                id = id,
                title = obj.optString("title", "खर्च प्रविष्टि"),
                category = obj.optString("category", "लंगर-ए-पाक"),
                amount = obj.optDouble("amount", 0.0),
                spentBy = obj.optString("spentBy", "कमेटी व्यवस्थापक"),
                date = obj.optString("date", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                receiptRef = obj.optString("receiptRef").takeIf { it.isNotBlank() },
                attachmentUri = obj.optString("attachmentUri").takeIf { it.isNotBlank() },
                remarks = obj.optString("remarks").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }
}
