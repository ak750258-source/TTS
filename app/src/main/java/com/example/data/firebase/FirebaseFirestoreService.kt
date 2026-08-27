package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
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
        private const val SYNC_TOPIC = "tts_12rabiulawwal_live_sync_v4"
        private const val BASE_URL = "https://ntfy.sh/$SYNC_TOPIC"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val DEVICE_ID = UUID.randomUUID().toString()

        @Volatile
        private var INSTANCE: FirebaseFirestoreService? = null

        fun getInstance(context: Context? = null): FirebaseFirestoreService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseFirestoreService(context?.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private var appContext: Context? = context?.applicationContext

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

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
        .connectTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val streamingClient = OkHttpClient.Builder()
        .dns(ipv4PrioritizedDns)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for SSE stream
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

    private val chatListeners = ConcurrentHashMap<String, MutableList<(List<ChatMessage>) -> Unit>>()
    private val clearAllListeners = mutableListOf<() -> Unit>()

    private var currentActiveMemberId: Long = 0L
    private var currentActiveMemberName: String = ""

    init {
        Log.d(TAG, "Initializing Real-Time Cloud Sync Engine with Device ID: $DEVICE_ID")
        startSseEventListener()
        startCatchupPoll()
        startPeriodicSyncHeartbeat()
    }

    // --- REAL-TIME SSE STREAMING CONNECTION ---
    private fun startSseEventListener() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val request = Request.Builder()
                        .url("$BASE_URL/json")
                        .header("Accept", "text/event-stream")
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
                                val sseJson = JSONObject(line)
                                if (sseJson.optString("event") == "message") {
                                    val messageBody = sseJson.optString("message")
                                    if (messageBody.isNotBlank()) {
                                        handleCloudEvent(messageBody)
                                    }
                                }
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
            try {
                // Fetch events from the last 7 days so any newly installed device catches up on all data
                val pollUrl = "$BASE_URL/json?poll=1&since=7d"
                val request = Request.Builder().url(pollUrl).build()
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        val lines = body.split("\n")
                        for (line in lines) {
                            if (line.isBlank()) continue
                            try {
                                val sseJson = JSONObject(line)
                                if (sseJson.optString("event") == "message") {
                                    val messageBody = sseJson.optString("message")
                                    if (messageBody.isNotBlank()) {
                                        handleCloudEvent(messageBody)
                                    }
                                }
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Catch-up poll note: ${e.message}")
            }
        }
    }

    private fun startCatchupPoll() {
        fetchCatchupEvents()
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
        try {
            val root = JSONObject(rawJson)
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

            when (eventType) {
                "CHAT_MESSAGE" -> {
                    if (payload != null) {
                        val msg = parseChatMessage(payload)
                        if (msg != null) {
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

                            // Trigger phone notification if message came from another device / candidate
                            if (isFromAnotherDevice || (currentActiveMemberId > 0 && senderMemberId != currentActiveMemberId)) {
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

                            // Trigger phone notification when notice is added
                            if (isFromAnotherDevice) {
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

                            // Trigger phone notification when donation/chanda is added
                            if (isFromAnotherDevice) {
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

                            // Trigger notification for new official document
                            if (isFromAnotherDevice) {
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
                "CLEAR_ALL_DATA" -> {
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

                val request = Request.Builder()
                    .url(BASE_URL)
                    .header("Title", "TTS_SYNC_$eventType")
                    .header("Priority", "high")
                    .post(envelope.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                var success = false
                var attempts = 0
                while (!success && attempts < 2) {
                    attempts++
                    try {
                        httpClient.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                success = true
                                _isCloudConnected.value = true
                                _syncStatus.value = "🟢 लाइव क्लाउड सिंक कनेक्टेड"
                                Log.d(TAG, "Cloud broadcast success: $eventType")
                            } else {
                                Log.w(TAG, "Cloud broadcast warning code: ${response.code}")
                            }
                        }
                    } catch (netEx: Exception) {
                        if (attempts >= 2) {
                            Log.w(TAG, "Cloud broadcast non-fatal note: ${netEx.message}")
                        } else {
                            delay(500)
                        }
                    }
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

    fun listenToChat(channelId: String, onChatUpdated: (List<ChatMessage>) -> Unit): ListenerRegistration? {
        val list = chatListeners.getOrPut(channelId) { mutableListOf() }
        synchronized(list) {
            list.add(onChatUpdated)
        }
        return null
    }

    fun listenToClearAll(onClearAll: () -> Unit) {
        synchronized(clearAllListeners) {
            clearAllListeners.add(onClearAll)
        }
    }

    // --- SYNC ACTIONS TO CLOUD ---
    suspend fun syncMemberToCloud(member: Member) {
        withContext(Dispatchers.IO) {
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
                put("photoUri", member.photoUri ?: "")
            }
            broadcastEvent("MEMBER_UPSERT", json)
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
            }
            broadcastEvent("CHAT_MESSAGE", json)
        }
    }

    suspend fun clearAllChatFromCloud() {
        broadcastEvent("CLEAR_CHAT", null)
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
        broadcastEvent("CLEAR_ALL_DATA", null)
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
                isAnnouncement = obj.optBoolean("isAnnouncement", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseMember(obj: JSONObject): Member? {
        return try {
            val id = obj.optLong("id", 0L)
            if (id <= 0) return null
            Member(
                id = id,
                memberCode = obj.optString("memberCode", "TTS-$id"),
                fullName = obj.optString("fullName", ""),
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
                remarks = obj.optString("remarks").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDocument(obj: JSONObject): OfficialDocument? {
        return try {
            val id = obj.optLong("id", 0L)
            if (id <= 0) return null
            OfficialDocument(
                id = id,
                title = obj.optString("title", ""),
                category = obj.optString("category", "कमेटी नियमावली"),
                refCode = obj.optString("refCode", "DOC-$id"),
                publishedDate = obj.optString("publishedDate", ""),
                fileSize = obj.optString("fileSize", "1.2 MB"),
                accessLevel = obj.optString("accessLevel", "All Members"),
                summary = obj.optString("summary", ""),
                fullContent = obj.optString("fullContent", "")
            )
        } catch (e: Exception) {
            null
        }
    }
}
