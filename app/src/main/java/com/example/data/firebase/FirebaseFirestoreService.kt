package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import com.example.util.TTSNotificationHelper
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Universal High-Reliability Google Cloud Firestore Real-Time Synchronization Engine.
 * 
 * Synchronizes 100% of data across all distributed Android devices:
 * - Live Chat Messages across all channels with instant delivery & read receipts
 * - Member Profiles, Designations, Badges & Photos
 * - Notices & Important Announcements
 * - Donation Records & Live Totals (Chanda Vivran)
 * - Expense Records & Remaining Balance (Kharch Vivran)
 * - Meetings & Agendas
 * - Official Documents
 * - Live Candidate Online Indicators (🟢)
 */
class FirebaseFirestoreService(context: Context? = null) {

    companion object {
        private const val TAG = "CloudSyncEngine"
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

    private val firestoreInstance: FirebaseFirestore by lazy {
        val databaseId = appContext?.getString(R.string.firestore_database_id)
        if (!databaseId.isNullOrBlank()) {
            FirebaseFirestore.getInstance(databaseId)
        } else {
            FirebaseFirestore.getInstance()
        }
    }

    private val _isCloudConnected = MutableStateFlow(true)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _syncStatus = MutableStateFlow("🟢 लाइव क्लाउड सिंक सक्रिय (Firebase)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _onlineCandidateIds = MutableStateFlow<Set<Long>>(emptySet())
    val onlineCandidateIds: StateFlow<Set<Long>> = _onlineCandidateIds.asStateFlow()

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

    private val chatListeners = ConcurrentHashMap<String, MutableList<(List<ChatMessage>) -> Unit>>()
    private val chatDeleteListeners = mutableListOf<(Long) -> Unit>()
    private val chatDeliveredListeners = mutableListOf<(Long, String) -> Unit>()
    private val chatSeenListeners = mutableListOf<(Long, String) -> Unit>()
    private val chatClearListeners = mutableListOf<() -> Unit>()
    private val syncRequestListeners = mutableListOf<() -> Unit>()
    private val clearAllListeners = mutableListOf<() -> Unit>()

    private var currentActiveMemberId: Long = 0L
    private var currentActiveMemberName: String = ""

    // Notification suppression
    private val serviceStartTime = System.currentTimeMillis()
    private val notifiedMessageIds = ConcurrentHashMap.newKeySet<Long>()
    private val notifiedNoticeIds = ConcurrentHashMap.newKeySet<Long>()
    private val notifiedDonationIds = ConcurrentHashMap.newKeySet<Long>()
    private val notifiedDocumentIds = ConcurrentHashMap.newKeySet<Long>()

    // Active Firestore listener registrations
    private val activeRegistrations = mutableListOf<ListenerRegistration>()

    init {
        Log.d(TAG, "Initializing Real-Time Cloud Firestore Sync Engine with Device ID: $DEVICE_ID")
        startFirestoreListeners()
        startPeriodicPresenceHeartbeat()
    }

    fun setContext(context: Context) {
        appContext = context.applicationContext
        startFirestoreListeners()
    }

    private fun startFirestoreListeners() {
        if (appContext == null) return
        if (activeRegistrations.isNotEmpty()) return

        try {
            val db = firestoreInstance

            // 1. Members Snapshot Listener
            val memberReg = db.collection("members").addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Members snapshot listener note: ${error.message}")
                    _isCloudConnected.value = false
                    _syncStatus.value = "🟡 क्लाउड कनेक्ट हो रहा है..."
                    return@addSnapshotListener
                }
                _isCloudConnected.value = true
                _syncStatus.value = "🟢 लाइव क्लाउड सिंक सक्रिय (Firebase)"

                if (snapshot != null) {
                    val updatedMembers = mutableListOf<Member>()
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val member = parseMemberDoc(doc.data)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                if (member != null) {
                                    updatedMembers.add(member)
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val id = doc.id.toLongOrNull() ?: member?.id ?: 0L
                                if (id > 0) {
                                    synchronized(memberDeleteListeners) {
                                        memberDeleteListeners.forEach { it(id) }
                                    }
                                }
                            }
                        }
                    }
                    if (updatedMembers.isNotEmpty()) {
                        synchronized(memberUpsertListeners) {
                            memberUpsertListeners.forEach { it(updatedMembers) }
                        }
                    }
                }
            }
            activeRegistrations.add(memberReg)

            // 2. Notices Snapshot Listener
            val noticeReg = db.collection("notices").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val updatedNotices = mutableListOf<Notice>()
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val notice = parseNoticeDoc(doc.data)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                if (notice != null) {
                                    updatedNotices.add(notice)
                                    val senderDev = doc.getString("senderDeviceId") ?: ""
                                    if (senderDev.isNotEmpty() && senderDev != DEVICE_ID) {
                                        val ts = doc.getLong("timestamp") ?: 0L
                                        if (ts > (serviceStartTime - 5000L) && notifiedNoticeIds.add(notice.id)) {
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
                            DocumentChange.Type.REMOVED -> {
                                val id = doc.id.toLongOrNull() ?: notice?.id ?: 0L
                                if (id > 0) {
                                    synchronized(noticeDeleteListeners) {
                                        noticeDeleteListeners.forEach { it(id) }
                                    }
                                }
                            }
                        }
                    }
                    if (updatedNotices.isNotEmpty()) {
                        synchronized(noticeUpsertListeners) {
                            noticeUpsertListeners.forEach { it(updatedNotices) }
                        }
                    }
                }
            }
            activeRegistrations.add(noticeReg)

            // 3. Donations Snapshot Listener (Chanda Vivran)
            val donationReg = db.collection("donations").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val updatedDonations = mutableListOf<Donation>()
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val don = parseDonationDoc(doc.data)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                if (don != null) {
                                    updatedDonations.add(don)
                                    val senderDev = doc.getString("senderDeviceId") ?: ""
                                    if (senderDev.isNotEmpty() && senderDev != DEVICE_ID) {
                                        if (don.timestamp > (serviceStartTime - 5000L) && notifiedDonationIds.add(don.id)) {
                                            appContext?.let { ctx ->
                                                TTSNotificationHelper.showDonationNotification(
                                                    context = ctx,
                                                    donorName = don.donorName,
                                                    amount = don.amount,
                                                    receiptNumber = don.transactionRef,
                                                    note = don.purpose
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val id = doc.id.toLongOrNull() ?: don?.id ?: 0L
                                if (id > 0) {
                                    synchronized(donationDeleteListeners) {
                                        donationDeleteListeners.forEach { it(id) }
                                    }
                                }
                            }
                        }
                    }
                    if (updatedDonations.isNotEmpty()) {
                        synchronized(donationUpsertListeners) {
                            donationUpsertListeners.forEach { it(updatedDonations) }
                        }
                    }
                }
            }
            activeRegistrations.add(donationReg)

            // 4. Expenses Snapshot Listener (Kharch Vivran)
            val expenseReg = db.collection("expenses").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val updatedExpenses = mutableListOf<Expense>()
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val exp = parseExpenseDoc(doc.data)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                if (exp != null) updatedExpenses.add(exp)
                            }
                            DocumentChange.Type.REMOVED -> {
                                val id = doc.id.toLongOrNull() ?: exp?.id ?: 0L
                                if (id > 0) {
                                    synchronized(expenseDeleteListeners) {
                                        expenseDeleteListeners.forEach { it(id) }
                                    }
                                }
                            }
                        }
                    }
                    if (updatedExpenses.isNotEmpty()) {
                        synchronized(expenseUpsertListeners) {
                            expenseUpsertListeners.forEach { it(updatedExpenses) }
                        }
                    }
                }
            }
            activeRegistrations.add(expenseReg)

            // 5. Chat Messages Snapshot Listener (Live Committee Group Chat)
            val chatReg = db.collection("chat_messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Chat snapshot error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val newMessages = mutableListOf<ChatMessage>()
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val msg = parseChatMessageDoc(doc.data)
                            when (change.type) {
                                DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                    if (msg != null) {
                                        newMessages.add(msg)
                                        val senderDev = msg.senderDeviceId
                                        if (senderDev.isNotEmpty() && senderDev != DEVICE_ID) {
                                            // Acknowledge delivered back in background
                                            serviceScope.launch {
                                                try {
                                                    if (msg.status == "SENT") {
                                                        db.collection("chat_messages").document(msg.id.toString())
                                                            .update("status", "DELIVERED")
                                                    }
                                                } catch (_: Exception) {}
                                            }

                                            if (msg.timestamp > (serviceStartTime - 5000L) && notifiedMessageIds.add(msg.id)) {
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
                                DocumentChange.Type.REMOVED -> {
                                    val id = doc.id.toLongOrNull() ?: msg?.id ?: 0L
                                    if (id > 0) {
                                        synchronized(chatDeleteListeners) {
                                            chatDeleteListeners.forEach { it(id) }
                                        }
                                    }
                                }
                            }
                        }

                        if (newMessages.isNotEmpty()) {
                            // Notify all-channel global listeners
                            chatListeners["all"]?.let { allList ->
                                synchronized(allList) {
                                    allList.toList().forEach { it(newMessages) }
                                }
                            }

                            // Group by channel and notify channel listeners
                            val byChannel = newMessages.groupBy { it.channelId }
                            for ((chan, list) in byChannel) {
                                chatListeners[chan]?.let { channelList ->
                                    synchronized(channelList) {
                                        channelList.toList().forEach { it(list) }
                                    }
                                }
                            }
                        }
                    }
                }
            activeRegistrations.add(chatReg)

            // 6. Meetings Snapshot Listener
            val meetingReg = db.collection("meetings").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val updated = mutableListOf<Meeting>()
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val mtg = parseMeetingDoc(doc.data)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                if (mtg != null) updated.add(mtg)
                            }
                            DocumentChange.Type.REMOVED -> {
                                val id = doc.id.toLongOrNull() ?: mtg?.id ?: 0L
                                if (id > 0) {
                                    synchronized(meetingDeleteListeners) {
                                        meetingDeleteListeners.forEach { it(id) }
                                    }
                                }
                            }
                        }
                    }
                    if (updated.isNotEmpty()) {
                        synchronized(meetingUpsertListeners) {
                            meetingUpsertListeners.forEach { it(updated) }
                        }
                    }
                }
            }
            activeRegistrations.add(meetingReg)

            // 7. Documents Snapshot Listener
            val docReg = db.collection("documents").addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val updated = mutableListOf<OfficialDocument>()
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        val officialDoc = parseDocumentDoc(doc.data)
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                if (officialDoc != null) {
                                    updated.add(officialDoc)
                                    val senderDev = doc.getString("senderDeviceId") ?: ""
                                    if (senderDev.isNotEmpty() && senderDev != DEVICE_ID) {
                                        val ts = doc.getLong("timestamp") ?: 0L
                                        if (ts > (serviceStartTime - 5000L) && notifiedDocumentIds.add(officialDoc.id)) {
                                            appContext?.let { ctx ->
                                                TTSNotificationHelper.showDocumentNotification(
                                                    context = ctx,
                                                    title = officialDoc.title,
                                                    category = officialDoc.category,
                                                    summary = officialDoc.summary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            DocumentChange.Type.REMOVED -> {
                                val id = doc.id.toLongOrNull() ?: officialDoc?.id ?: 0L
                                if (id > 0) {
                                    synchronized(documentDeleteListeners) {
                                        documentDeleteListeners.forEach { it(id) }
                                    }
                                }
                            }
                        }
                    }
                    if (updated.isNotEmpty()) {
                        synchronized(documentUpsertListeners) {
                            documentUpsertListeners.forEach { it(updated) }
                        }
                    }
                }
            }
            activeRegistrations.add(docReg)

            // 8. Config / Donation Goal Listener
            val configReg = db.collection("config").document("donation_goal").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val goal = snapshot.getDouble("donationGoal") ?: snapshot.getDouble("goal") ?: 250000.0
                saveDonationGoal(goal)
                synchronized(donationGoalListeners) {
                    donationGoalListeners.forEach { it(goal) }
                }
            }
            activeRegistrations.add(configReg)

            // 9. Real-Time Candidate Presence Listener
            val presReg = db.collection("presence").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val now = System.currentTimeMillis()
                val activeIds = mutableSetOf<Long>()
                for (doc in snapshot.documents) {
                    val memberId = doc.id.toLongOrNull() ?: continue
                    val lastSeen = doc.getLong("lastSeen") ?: 0L
                    val isOnline = doc.getBoolean("isOnline") ?: true
                    if (isOnline && (now - lastSeen < 10 * 60 * 1000)) {
                        activeIds.add(memberId)
                        candidateLastSeenMap[memberId] = lastSeen
                    }
                }
                _onlineCandidateIds.value = activeIds
            }
            activeRegistrations.add(presReg)

        } catch (e: Exception) {
            Log.e(TAG, "Error starting Firestore listeners: ${e.message}", e)
        }
    }

    private fun startPeriodicPresenceHeartbeat() {
        serviceScope.launch {
            while (isActive) {
                try {
                    if (currentActiveMemberId > 0) {
                        updateCandidatePresence(currentActiveMemberId, currentActiveMemberName, true)
                    }
                    // Clean up offline candidates
                    val now = System.currentTimeMillis()
                    val activeIds = mutableSetOf<Long>()
                    for ((id, lastSeen) in candidateLastSeenMap) {
                        if (now - lastSeen < 10 * 60 * 1000) {
                            activeIds.add(id)
                        }
                    }
                    _onlineCandidateIds.value = activeIds
                } catch (_: Exception) {}
                delay(20000)
            }
        }
    }

    suspend fun updateCandidatePresence(memberId: Long, memberName: String, isOnline: Boolean) {
        if (memberId <= 0) return
        currentActiveMemberId = memberId
        currentActiveMemberName = memberName
        val now = System.currentTimeMillis()
        if (isOnline) {
            candidateLastSeenMap[memberId] = now
        } else {
            candidateLastSeenMap.remove(memberId)
        }
        _onlineCandidateIds.value = candidateLastSeenMap.keys().toList().toSet()

        serviceScope.launch {
            try {
                val data = hashMapOf(
                    "memberId" to memberId,
                    "memberName" to memberName,
                    "isOnline" to isOnline,
                    "lastSeen" to now
                )
                firestoreInstance.collection("presence").document(memberId.toString()).set(data)
            } catch (e: Exception) {
                Log.v(TAG, "Presence update note: ${e.message}")
            }
        }
    }

    fun ensureConnection() {
        startFirestoreListeners()
    }

    fun fetchCatchupEvents() {
        startFirestoreListeners()
    }

    fun requestDataSync() {
        startFirestoreListeners()
    }

    // --- MEMBERS CRUD SYNC ---
    suspend fun syncMemberToCloud(member: Member) {
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "memberId" to member.id,
                    "memberCode" to member.memberCode,
                    "fullName" to member.fullName,
                    "designation" to member.designation,
                    "committeeWing" to member.committeeWing,
                    "phoneNumber" to member.phoneNumber,
                    "email" to member.email,
                    "bloodGroup" to member.bloodGroup,
                    "joinDate" to member.joinDate,
                    "address" to member.address,
                    "emergencyContact" to member.emergencyContact,
                    "avatarColorIndex" to member.avatarColorIndex,
                    "isActive" to member.isActive,
                    "isBestPerformer" to member.isBestPerformer,
                    "bestPerformerBadge" to (member.bestPerformerBadge ?: ""),
                    "photoResName" to (member.photoResName ?: ""),
                    "photoUri" to (member.photoUri ?: ""),
                    "senderDeviceId" to DEVICE_ID,
                    "timestamp" to System.currentTimeMillis()
                )
                firestoreInstance.collection("members").document(member.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Member ${member.fullName} synced to Firestore cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync member to cloud: ${e.message}")
            }
        }
    }

    suspend fun syncMemberPhotoToCloud(memberId: Long, photoUri: String?) {
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "photoUri" to (photoUri ?: ""),
                    "timestamp" to System.currentTimeMillis()
                )
                firestoreInstance.collection("members").document(memberId.toString())
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync member photo to cloud: ${e.message}")
            }
        }
    }

    suspend fun deleteMemberFromCloud(memberId: Long) {
        serviceScope.launch {
            try {
                firestoreInstance.collection("members").document(memberId.toString()).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete member from cloud: ${e.message}")
            }
        }
    }

    // --- NOTICES CRUD SYNC ---
    suspend fun syncNoticeToCloud(notice: Notice) {
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "noticeId" to notice.id,
                    "title" to notice.title,
                    "category" to notice.category,
                    "priority" to notice.priority,
                    "issuedBy" to notice.issuedBy,
                    "date" to notice.date,
                    "content" to notice.content,
                    "isPinned" to notice.isPinned,
                    "senderDeviceId" to DEVICE_ID,
                    "timestamp" to System.currentTimeMillis()
                )
                firestoreInstance.collection("notices").document(notice.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Notice ${notice.title} synced to Firestore cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync notice to cloud: ${e.message}")
            }
        }
    }

    suspend fun deleteNoticeFromCloud(noticeId: Long) {
        serviceScope.launch {
            try {
                firestoreInstance.collection("notices").document(noticeId.toString()).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete notice from cloud: ${e.message}")
            }
        }
    }

    // --- DONATIONS CRUD SYNC (Chanda Vivran) ---
    suspend fun syncDonationToCloud(donation: Donation) {
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "donationId" to donation.id,
                    "donorName" to donation.donorName,
                    "donorMemberCode" to (donation.donorMemberCode ?: ""),
                    "amount" to donation.amount,
                    "purpose" to donation.purpose,
                    "paymentMode" to donation.paymentMode,
                    "transactionRef" to donation.transactionRef,
                    "date" to donation.date,
                    "timestamp" to donation.timestamp,
                    "verified" to donation.verified,
                    "remarks" to (donation.remarks ?: ""),
                    "paymentProofUri" to (donation.paymentProofUri ?: ""),
                    "senderDeviceId" to DEVICE_ID
                )
                firestoreInstance.collection("donations").document(donation.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Donation ₹${donation.amount} (${donation.donorName}) synced to Firestore cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync donation to cloud: ${e.message}")
            }
        }
    }

    suspend fun deleteDonationFromCloud(donationId: Long) {
        serviceScope.launch {
            try {
                firestoreInstance.collection("donations").document(donationId.toString()).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete donation from cloud: ${e.message}")
            }
        }
    }

    suspend fun clearAllDonationsFromCloud() {
        serviceScope.launch {
            try {
                val snapshot = firestoreInstance.collection("donations").get().await()
                val batch = firestoreInstance.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear donations: ${e.message}")
            }
        }
    }

    // --- EXPENSES CRUD SYNC (Kharch Vivran) ---
    suspend fun syncExpenseToCloud(expense: Expense) {
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "expenseId" to expense.id,
                    "title" to expense.title,
                    "category" to expense.category,
                    "amount" to expense.amount,
                    "spentBy" to expense.spentBy,
                    "date" to expense.date,
                    "timestamp" to expense.timestamp,
                    "receiptRef" to (expense.receiptRef ?: ""),
                    "attachmentUri" to (expense.attachmentUri ?: ""),
                    "remarks" to (expense.remarks ?: ""),
                    "senderDeviceId" to DEVICE_ID
                )
                firestoreInstance.collection("expenses").document(expense.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Expense ₹${expense.amount} (${expense.title}) synced to Firestore cloud")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync expense to cloud: ${e.message}")
            }
        }
    }

    suspend fun deleteExpenseFromCloud(expenseId: Long) {
        serviceScope.launch {
            try {
                firestoreInstance.collection("expenses").document(expenseId.toString()).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete expense: ${e.message}")
            }
        }
    }

    suspend fun clearAllExpensesFromCloud() {
        serviceScope.launch {
            try {
                val snapshot = firestoreInstance.collection("expenses").get().await()
                val batch = firestoreInstance.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear expenses: ${e.message}")
            }
        }
    }

    // --- CHAT MESSAGES CRUD SYNC ---
    suspend fun syncChatMessageToCloud(message: ChatMessage) {
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "messageId" to message.id,
                    "channelId" to message.channelId,
                    "senderName" to message.senderName,
                    "senderRole" to message.senderRole,
                    "senderAvatarIndex" to message.senderAvatarIndex,
                    "messageText" to message.messageText,
                    "timestamp" to message.timestamp,
                    "timeDisplay" to message.timeDisplay,
                    "isAnnouncement" to message.isAnnouncement,
                    "senderMemberId" to message.senderMemberId,
                    "senderDeviceId" to DEVICE_ID,
                    "status" to message.status,
                    "isSeen" to message.isSeen
                )
                firestoreInstance.collection("chat_messages").document(message.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "ChatMessage synced: ${message.messageText}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync chat message: ${e.message}")
            }
        }
    }

    suspend fun broadcastChatSeen(messageId: Long, channelId: String = "general") {
        serviceScope.launch {
            try {
                firestoreInstance.collection("chat_messages").document(messageId.toString())
                    .update("status", "SEEN", "isSeen", true)
            } catch (_: Exception) {}
        }
    }

    suspend fun deleteChatMessageFromCloud(messageId: Long, channelId: String = "general") {
        serviceScope.launch {
            try {
                firestoreInstance.collection("chat_messages").document(messageId.toString()).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete chat message: ${e.message}")
            }
        }
    }

    suspend fun clearAllChatFromCloud() {
        serviceScope.launch {
            try {
                val snapshot = firestoreInstance.collection("chat_messages").get().await()
                val batch = firestoreInstance.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear chat: ${e.message}")
            }
        }
    }

    // --- MEETINGS CRUD SYNC ---
    suspend fun syncMeetingToCloud(meeting: Meeting) {
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "meetingId" to meeting.id,
                    "title" to meeting.title,
                    "type" to meeting.type,
                    "dateDisplay" to meeting.dateDisplay,
                    "timeDisplay" to meeting.timeDisplay,
                    "dateTimeMillis" to meeting.dateTimeMillis,
                    "venue" to meeting.venue,
                    "virtualLink" to (meeting.virtualLink ?: ""),
                    "chairperson" to meeting.chairperson,
                    "agenda" to meeting.agenda,
                    "status" to meeting.status,
                    "notesOrMinutes" to (meeting.notesOrMinutes ?: ""),
                    "senderDeviceId" to DEVICE_ID,
                    "timestamp" to System.currentTimeMillis()
                )
                firestoreInstance.collection("meetings").document(meeting.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync meeting: ${e.message}")
            }
        }
    }

    suspend fun deleteMeetingFromCloud(meetingId: Long) {
        serviceScope.launch {
            try {
                firestoreInstance.collection("meetings").document(meetingId.toString()).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete meeting: ${e.message}")
            }
        }
    }

    suspend fun clearAllMeetingsFromCloud() {
        serviceScope.launch {
            try {
                val snapshot = firestoreInstance.collection("meetings").get().await()
                val batch = firestoreInstance.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (_: Exception) {}
        }
    }

    // --- DOCUMENTS CRUD SYNC ---
    suspend fun syncDocumentToCloud(doc: OfficialDocument) {
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "documentId" to doc.id,
                    "title" to doc.title,
                    "category" to doc.category,
                    "refCode" to doc.refCode,
                    "publishedDate" to doc.publishedDate,
                    "fileSize" to doc.fileSize,
                    "accessLevel" to doc.accessLevel,
                    "summary" to doc.summary,
                    "fullContent" to doc.fullContent,
                    "attachmentUri" to (doc.attachmentUri ?: ""),
                    "attachmentName" to (doc.attachmentName ?: ""),
                    "senderDeviceId" to DEVICE_ID,
                    "timestamp" to System.currentTimeMillis()
                )
                firestoreInstance.collection("documents").document(doc.id.toString())
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync document: ${e.message}")
            }
        }
    }

    suspend fun deleteDocumentFromCloud(docId: Long) {
        serviceScope.launch {
            try {
                firestoreInstance.collection("documents").document(docId.toString()).delete().await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete document: ${e.message}")
            }
        }
    }

    suspend fun clearAllDocumentsFromCloud() {
        serviceScope.launch {
            try {
                val snapshot = firestoreInstance.collection("documents").get().await()
                val batch = firestoreInstance.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (_: Exception) {}
        }
    }

    suspend fun clearAllMembersFromCloud() {
        serviceScope.launch {
            try {
                val snapshot = firestoreInstance.collection("members").get().await()
                val batch = firestoreInstance.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (_: Exception) {}
        }
    }

    suspend fun clearAllNoticesFromCloud() {
        serviceScope.launch {
            try {
                val snapshot = firestoreInstance.collection("notices").get().await()
                val batch = firestoreInstance.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().await()
            } catch (_: Exception) {}
        }
    }

    suspend fun clearAllAppCloudData() {
        clearAllMembersFromCloud()
        clearAllNoticesFromCloud()
        clearAllDonationsFromCloud()
        clearAllExpensesFromCloud()
        clearAllMeetingsFromCloud()
        clearAllDocumentsFromCloud()
        clearAllChatFromCloud()
    }

    // --- DONATION GOAL ---
    suspend fun syncDonationGoalToCloud(goal: Double) {
        saveDonationGoal(goal)
        serviceScope.launch {
            try {
                val data = hashMapOf<String, Any?>(
                    "donationGoal" to goal,
                    "goal" to goal,
                    "updatedAt" to System.currentTimeMillis()
                )
                firestoreInstance.collection("config").document("donation_goal")
                    .set(data, SetOptions.merge())
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync donation goal: ${e.message}")
            }
        }
    }

    fun getSavedDonationGoal(): Double {
        return try {
            appContext?.let { ctx ->
                val sp = ctx.getSharedPreferences("tts_sync_prefs", Context.MODE_PRIVATE)
                sp.getFloat("donation_goal_val", 250000f).toDouble()
            } ?: 250000.0
        } catch (_: Exception) {
            250000.0
        }
    }

    private fun saveDonationGoal(goal: Double) {
        try {
            appContext?.let { ctx ->
                ctx.getSharedPreferences("tts_sync_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putFloat("donation_goal_val", goal.toFloat())
                    .apply()
            }
        } catch (_: Exception) {}
    }

    // --- LISTENER REGISTRATIONS ---
    fun listenToMembers(onMembersUpdated: (List<Member>) -> Unit, onMemberDeleted: (Long) -> Unit) {
        synchronized(memberUpsertListeners) { memberUpsertListeners.add(onMembersUpdated) }
        synchronized(memberDeleteListeners) { memberDeleteListeners.add(onMemberDeleted) }
    }

    fun listenToMemberPhotoUpdates(listener: (Long, String) -> Unit) {
        synchronized(memberPhotoUpdateListeners) { memberPhotoUpdateListeners.add(listener) }
    }

    fun listenToDonations(onDonationsUpdated: (List<Donation>) -> Unit, onDonationDeleted: (Long) -> Unit) {
        synchronized(donationUpsertListeners) { donationUpsertListeners.add(onDonationsUpdated) }
        synchronized(donationDeleteListeners) { donationDeleteListeners.add(onDonationDeleted) }
    }

    fun listenToExpenses(onExpensesUpdated: (List<Expense>) -> Unit, onExpenseDeleted: (Long) -> Unit) {
        synchronized(expenseUpsertListeners) { expenseUpsertListeners.add(onExpensesUpdated) }
        synchronized(expenseDeleteListeners) { expenseDeleteListeners.add(onExpenseDeleted) }
    }

    fun listenToNotices(onNoticesUpdated: (List<Notice>) -> Unit, onNoticeDeleted: (Long) -> Unit) {
        synchronized(noticeUpsertListeners) { noticeUpsertListeners.add(onNoticesUpdated) }
        synchronized(noticeDeleteListeners) { noticeDeleteListeners.add(onNoticeDeleted) }
    }

    fun listenToMeetings(onMeetingsUpdated: (List<Meeting>) -> Unit, onMeetingDeleted: (Long) -> Unit) {
        synchronized(meetingUpsertListeners) { meetingUpsertListeners.add(onMeetingsUpdated) }
        synchronized(meetingDeleteListeners) { meetingDeleteListeners.add(onMeetingDeleted) }
    }

    fun listenToDocuments(onDocsUpdated: (List<OfficialDocument>) -> Unit, onDocDeleted: (Long) -> Unit) {
        synchronized(documentUpsertListeners) { documentUpsertListeners.add(onDocsUpdated) }
        synchronized(documentDeleteListeners) { documentDeleteListeners.add(onDocDeleted) }
    }

    fun listenToChat(channelId: String, listener: (List<ChatMessage>) -> Unit) {
        val list = chatListeners.getOrPut(channelId) { mutableListOf() }
        synchronized(list) { list.add(listener) }
    }

    fun listenToChatDelete(listener: (Long) -> Unit) {
        synchronized(chatDeleteListeners) { chatDeleteListeners.add(listener) }
    }

    fun listenToChatDelivered(listener: (Long, String) -> Unit) {
        synchronized(chatDeliveredListeners) { chatDeliveredListeners.add(listener) }
    }

    fun listenToChatSeen(listener: (Long, String) -> Unit) {
        synchronized(chatSeenListeners) { chatSeenListeners.add(listener) }
    }

    fun listenToChatClear(listener: () -> Unit) {
        synchronized(chatClearListeners) { chatClearListeners.add(listener) }
    }

    fun listenToClearExpenses(listener: () -> Unit) {
        synchronized(expenseClearListeners) { expenseClearListeners.add(listener) }
    }

    fun listenToClearDonations(listener: () -> Unit) {
        synchronized(donationClearListeners) { donationClearListeners.add(listener) }
    }

    fun listenToSyncRequest(listener: () -> Unit) {
        synchronized(syncRequestListeners) { syncRequestListeners.add(listener) }
    }

    fun listenToClearAll(listener: () -> Unit) {
        synchronized(clearAllListeners) { clearAllListeners.add(listener) }
    }

    fun listenToDonationGoal(onGoalUpdated: (Double) -> Unit) {
        synchronized(donationGoalListeners) { donationGoalListeners.add(onGoalUpdated) }
    }

    // --- FIRESTORE MAP PARSERS ---
    private fun parseMemberDoc(data: Map<String, Any?>): Member? {
        return try {
            val id = (data["memberId"] as? Number)?.toLong() ?: 0L
            val fullName = (data["fullName"] as? String) ?: return null
            val memberCode = (data["memberCode"] as? String) ?: "TTS-${id}"
            val desig = (data["designation"] as? String) ?: "खादिम"
            val wing = (data["committeeWing"] as? String) ?: "12 रबी-उल-अव्वल जुलूस कमेटी"
            val phone = (data["phoneNumber"] as? String) ?: ""
            val email = (data["email"] as? String) ?: ""
            val blood = (data["bloodGroup"] as? String) ?: ""
            val joinDate = (data["joinDate"] as? String) ?: ""
            val address = (data["address"] as? String) ?: ""
            val emergency = (data["emergencyContact"] as? String) ?: ""
            val avatarIdx = (data["avatarColorIndex"] as? Number)?.toInt() ?: 0
            val isActive = (data["isActive"] as? Boolean) ?: true
            val isBest = (data["isBestPerformer"] as? Boolean) ?: false
            val badge = (data["bestPerformerBadge"] as? String)?.takeIf { it.isNotBlank() }
            val photoRes = (data["photoResName"] as? String)?.takeIf { it.isNotBlank() }
            val photoUri = (data["photoUri"] as? String)?.takeIf { it.isNotBlank() }

            Member(
                id = id,
                memberCode = memberCode,
                fullName = fullName,
                designation = desig,
                committeeWing = wing,
                phoneNumber = phone,
                email = email,
                bloodGroup = blood,
                joinDate = joinDate,
                address = address,
                emergencyContact = emergency,
                avatarColorIndex = avatarIdx,
                isActive = isActive,
                isBestPerformer = isBest,
                bestPerformerBadge = badge,
                photoResName = photoRes,
                photoUri = photoUri
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNoticeDoc(data: Map<String, Any?>): Notice? {
        return try {
            val id = (data["noticeId"] as? Number)?.toLong() ?: 0L
            val title = (data["title"] as? String) ?: return null
            val category = (data["category"] as? String) ?: "12 रबी-उल-अव्वल"
            val priority = (data["priority"] as? String) ?: "NORMAL"
            val issuedBy = (data["issuedBy"] as? String) ?: "कमेटी"
            val date = (data["date"] as? String) ?: ""
            val content = (data["content"] as? String) ?: ""
            val isPinned = (data["isPinned"] as? Boolean) ?: false

            Notice(
                id = id,
                title = title,
                category = category,
                priority = priority,
                issuedBy = issuedBy,
                date = date,
                content = content,
                isPinned = isPinned
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDonationDoc(data: Map<String, Any?>): Donation? {
        return try {
            val id = (data["donationId"] as? Number)?.toLong() ?: 0L
            val donorName = (data["donorName"] as? String) ?: return null
            val donorCode = (data["donorMemberCode"] as? String)?.takeIf { it.isNotBlank() }
            val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
            val purpose = (data["purpose"] as? String) ?: "12 रबी-उल-अव्वल चंदा"
            val mode = (data["paymentMode"] as? String) ?: "UPI (ak750258@icici)"
            val txRef = (data["transactionRef"] as? String) ?: ""
            val date = (data["date"] as? String) ?: ""
            val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val verified = (data["verified"] as? Boolean) ?: true
            val remarks = (data["remarks"] as? String)?.takeIf { it.isNotBlank() }
            val proofUri = (data["paymentProofUri"] as? String)?.takeIf { it.isNotBlank() }

            Donation(
                id = id,
                donorName = donorName,
                donorMemberCode = donorCode,
                amount = amount,
                purpose = purpose,
                paymentMode = mode,
                transactionRef = txRef,
                date = date,
                timestamp = timestamp,
                verified = verified,
                remarks = remarks,
                paymentProofUri = proofUri
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseExpenseDoc(data: Map<String, Any?>): Expense? {
        return try {
            val id = (data["expenseId"] as? Number)?.toLong() ?: 0L
            val title = (data["title"] as? String) ?: return null
            val category = (data["category"] as? String) ?: "विविध"
            val amount = (data["amount"] as? Number)?.toDouble() ?: 0.0
            val spentBy = (data["spentBy"] as? String) ?: "कमेटी व्यवस्थापक"
            val date = (data["date"] as? String) ?: ""
            val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val ref = (data["receiptRef"] as? String)?.takeIf { it.isNotBlank() }
            val attach = (data["attachmentUri"] as? String)?.takeIf { it.isNotBlank() }
            val remarks = (data["remarks"] as? String)?.takeIf { it.isNotBlank() }

            Expense(
                id = id,
                title = title,
                category = category,
                amount = amount,
                spentBy = spentBy,
                date = date,
                timestamp = timestamp,
                receiptRef = ref,
                attachmentUri = attach,
                remarks = remarks
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseChatMessageDoc(data: Map<String, Any?>): ChatMessage? {
        return try {
            val id = (data["messageId"] as? Number)?.toLong() ?: 0L
            val channel = (data["channelId"] as? String) ?: "general"
            val senderName = (data["senderName"] as? String) ?: "कमेटी खादिम"
            val senderRole = (data["senderRole"] as? String) ?: "सदस्य"
            val avatarIdx = (data["senderAvatarIndex"] as? Number)?.toInt() ?: 0
            val messageText = (data["messageText"] as? String) ?: return null
            val timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val timeDisplay = (data["timeDisplay"] as? String) ?: ""
            val isAnnouncement = (data["isAnnouncement"] as? Boolean) ?: false
            val senderMemberId = (data["senderMemberId"] as? Number)?.toLong() ?: 0L
            val senderDeviceId = (data["senderDeviceId"] as? String) ?: ""
            val status = (data["status"] as? String) ?: "SENT"
            val isSeen = (data["isSeen"] as? Boolean) ?: false

            ChatMessage(
                id = id,
                channelId = channel,
                senderName = senderName,
                senderRole = senderRole,
                senderAvatarIndex = avatarIdx,
                messageText = messageText,
                timestamp = timestamp,
                timeDisplay = timeDisplay,
                isAnnouncement = isAnnouncement,
                senderMemberId = senderMemberId,
                senderDeviceId = senderDeviceId,
                status = status,
                isSeen = isSeen
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMeetingDoc(data: Map<String, Any?>): Meeting? {
        return try {
            val id = (data["meetingId"] as? Number)?.toLong() ?: 0L
            val title = (data["title"] as? String) ?: return null
            val type = (data["type"] as? String) ?: "मुख्य बैठक"
            val dateDisplay = (data["dateDisplay"] as? String) ?: ""
            val timeDisplay = (data["timeDisplay"] as? String) ?: ""
            val dateTimeMillis = (data["dateTimeMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
            val venue = (data["venue"] as? String) ?: ""
            val virtualLink = (data["virtualLink"] as? String)?.takeIf { it.isNotBlank() }
            val chair = (data["chairperson"] as? String) ?: ""
            val agenda = (data["agenda"] as? String) ?: ""
            val status = (data["status"] as? String) ?: "Upcoming"
            val notes = (data["notesOrMinutes"] as? String)?.takeIf { it.isNotBlank() }

            Meeting(
                id = id,
                title = title,
                type = type,
                dateDisplay = dateDisplay,
                timeDisplay = timeDisplay,
                dateTimeMillis = dateTimeMillis,
                venue = venue,
                virtualLink = virtualLink,
                chairperson = chair,
                agenda = agenda,
                status = status,
                notesOrMinutes = notes
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDocumentDoc(data: Map<String, Any?>): OfficialDocument? {
        return try {
            val id = (data["documentId"] as? Number)?.toLong() ?: 0L
            val title = (data["title"] as? String) ?: return null
            val category = (data["category"] as? String) ?: "कमेटी नियमावली"
            val refCode = (data["refCode"] as? String) ?: "TTS/DOC"
            val publishedDate = (data["publishedDate"] as? String) ?: ""
            val fileSize = (data["fileSize"] as? String) ?: "1.5 MB"
            val accessLevel = (data["accessLevel"] as? String) ?: "All Members"
            val summary = (data["summary"] as? String) ?: ""
            val fullContent = (data["fullContent"] as? String) ?: ""
            val attachUri = (data["attachmentUri"] as? String)?.takeIf { it.isNotBlank() }
            val attachName = (data["attachmentName"] as? String)?.takeIf { it.isNotBlank() }

            OfficialDocument(
                id = id,
                title = title,
                category = category,
                refCode = refCode,
                publishedDate = publishedDate,
                fileSize = fileSize,
                accessLevel = accessLevel,
                summary = summary,
                fullContent = fullContent,
                attachmentUri = attachUri,
                attachmentName = attachName
            )
        } catch (_: Exception) {
            null
        }
    }
}
