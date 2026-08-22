package com.example.data.firebase

import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirebaseFirestoreService {

    companion object {
        private const val TAG = "FirebaseFirestoreService"
        private const val COLLECTION_MEMBERS = "tts_members"
        private const val COLLECTION_DONATIONS = "tts_donations"
        private const val COLLECTION_MEETINGS = "tts_meetings"
        private const val COLLECTION_NOTICES = "tts_notices"
        private const val COLLECTION_DOCUMENTS = "tts_documents"
        private const val COLLECTION_CHAT = "tts_chat_messages"
        private const val COLLECTION_PRESENCE = "tts_presence"
    }

    private var db: FirebaseFirestore? = null
    private var isInitialized = false

    private val _isCloudConnected = MutableStateFlow(false)
    val isCloudConnected: StateFlow<Boolean> = _isCloudConnected.asStateFlow()

    private val _syncStatus = MutableStateFlow("Firebase Firestore सक्रिय")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _onlineCandidateIds = MutableStateFlow<Set<Long>>(emptySet())
    val onlineCandidateIds: StateFlow<Set<Long>> = _onlineCandidateIds.asStateFlow()

    private val listeners = mutableListOf<ListenerRegistration>()

    init {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
            db = firestore
            isInitialized = true
            _isCloudConnected.value = true
            _syncStatus.value = "🟢 Firebase Firestore लाइव सिंक सक्रिय"
            Log.d(TAG, "Firebase Firestore successfully initialized")
            startPresenceListener()
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization notice: ${e.message}")
            isInitialized = false
            _isCloudConnected.value = false
            _syncStatus.value = "🟡 Firebase स्टैंडबाय (ऑफलाइन सुरक्षा सक्रिय)"
        }
    }

    // --- CANDIDATE ONLINE PRESENCE TRACKING ---
    private fun startPresenceListener() {
        val firestore = db ?: return
        try {
            val registration = firestore.collection(COLLECTION_PRESENCE)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val activeIds = mutableSetOf<Long>()
                        val now = System.currentTimeMillis()
                        // 10 minutes timeout window for live candidates
                        for (doc in snapshot.documents) {
                            val isOnline = doc.getBoolean("isOnline") ?: true
                            val lastSeen = (doc.get("lastSeen") as? Number)?.toLong() ?: 0L
                            val memberId = (doc.get("memberId") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                            if (memberId > 0 && isOnline && (now - lastSeen < 15 * 60 * 1000)) {
                                activeIds.add(memberId)
                            }
                        }
                        _onlineCandidateIds.value = activeIds
                    }
                }
            listeners.add(registration)
        } catch (e: Exception) {
            Log.w(TAG, "Presence listener error: ${e.message}")
        }
    }

    suspend fun updateCandidatePresence(memberId: Long, memberName: String, isOnline: Boolean) {
        val firestore = db ?: return
        if (memberId <= 0) return
        try {
            val docRef = firestore.collection(COLLECTION_PRESENCE).document(memberId.toString())
            val data = hashMapOf(
                "memberId" to memberId,
                "memberName" to memberName,
                "isOnline" to isOnline,
                "lastSeen" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Presence update error: ${e.message}")
        }
    }

    // --- MEMBERS SYNC ---
    fun listenToMembers(onMembersUpdated: (List<Member>) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            val registration = firestore.collection(COLLECTION_MEMBERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error listening to members: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val members = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = (doc.get("id") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                                Member(
                                    id = id,
                                    memberCode = doc.getString("memberCode") ?: "TTS-${id}",
                                    fullName = doc.getString("fullName") ?: "",
                                    designation = doc.getString("designation") ?: "खादिम (Volunteer)",
                                    committeeWing = doc.getString("committeeWing") ?: "12 रबी-उल-अव्वल जुलूस कमेटी",
                                    phoneNumber = doc.getString("phoneNumber") ?: "",
                                    email = doc.getString("email") ?: "",
                                    bloodGroup = doc.getString("bloodGroup") ?: "",
                                    joinDate = doc.getString("joinDate") ?: "12 रबी-उल-अव्वल 1447H",
                                    address = doc.getString("address") ?: "",
                                    emergencyContact = doc.getString("emergencyContact") ?: "",
                                    avatarColorIndex = (doc.get("avatarColorIndex") as? Number)?.toInt() ?: 0,
                                    isActive = doc.getBoolean("isActive") ?: true,
                                    isBestPerformer = doc.getBoolean("isBestPerformer") ?: false,
                                    bestPerformerBadge = doc.getString("bestPerformerBadge"),
                                    photoResName = doc.getString("photoResName"),
                                    photoUri = doc.getString("photoUri")
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing member doc: ${e.message}")
                                null
                            }
                        }
                        _isCloudConnected.value = true
                        onMembersUpdated(members)
                    }
                }
            listeners.add(registration)
            registration
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach member listener: ${e.message}")
            null
        }
    }

    suspend fun syncMemberToCloud(member: Member) {
        val firestore = db ?: return
        try {
            val docRef = firestore.collection(COLLECTION_MEMBERS).document(member.id.toString())
            val data = hashMapOf(
                "id" to member.id,
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
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
            _isCloudConnected.value = true
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync member error: ${e.message}")
        }
    }

    suspend fun deleteMemberFromCloud(memberId: Long) {
        val firestore = db ?: return
        try {
            firestore.collection(COLLECTION_MEMBERS).document(memberId.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Cloud delete member error: ${e.message}")
        }
    }

    // --- DONATIONS SYNC ---
    fun listenToDonations(onDonationsUpdated: (List<Donation>) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            val registration = firestore.collection(COLLECTION_DONATIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error listening to donations: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val donations = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = (doc.get("id") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                                Donation(
                                    id = id,
                                    donorName = doc.getString("donorName") ?: "",
                                    donorMemberCode = doc.getString("donorMemberCode"),
                                    amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
                                    purpose = doc.getString("purpose") ?: "12 रबी-उल-अव्वल जलसा व सजावट",
                                    paymentMode = doc.getString("paymentMode") ?: "UPI (ak750258@icici)",
                                    transactionRef = doc.getString("transactionRef") ?: "TTS-${id}",
                                    date = doc.getString("date") ?: "",
                                    timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis(),
                                    verified = doc.getBoolean("verified") ?: true,
                                    remarks = doc.getString("remarks")
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing donation doc: ${e.message}")
                                null
                            }
                        }
                        _isCloudConnected.value = true
                        onDonationsUpdated(donations)
                    }
                }
            listeners.add(registration)
            registration
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach donation listener: ${e.message}")
            null
        }
    }

    suspend fun syncDonationToCloud(donation: Donation) {
        val firestore = db ?: return
        try {
            val docRef = firestore.collection(COLLECTION_DONATIONS).document(donation.id.toString())
            val data = hashMapOf(
                "id" to donation.id,
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
                "updatedAt" to System.currentTimeMillis()
            )
            docRef.set(data, SetOptions.merge()).await()
            _isCloudConnected.value = true
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync donation error: ${e.message}")
        }
    }

    suspend fun deleteDonationFromCloud(donationId: Long) {
        val firestore = db ?: return
        try {
            firestore.collection(COLLECTION_DONATIONS).document(donationId.toString()).delete().await()
        } catch (e: Exception) {
            Log.w(TAG, "Cloud delete donation error: ${e.message}")
        }
    }

    suspend fun clearAllDonationsFromCloud() {
        val firestore = db ?: return
        try {
            val snapshot = firestore.collection(COLLECTION_DONATIONS).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud clear donations error: ${e.message}")
        }
    }

    // --- MEETINGS SYNC ---
    fun listenToMeetings(onMeetingsUpdated: (List<Meeting>) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            val registration = firestore.collection(COLLECTION_MEETINGS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val meetings = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = (doc.get("id") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                                Meeting(
                                    id = id,
                                    title = doc.getString("title") ?: "",
                                    type = doc.getString("type") ?: "12 रबी-उल-अव्वल मुख्य बैठक",
                                    dateDisplay = doc.getString("dateDisplay") ?: "",
                                    timeDisplay = doc.getString("timeDisplay") ?: "",
                                    dateTimeMillis = (doc.get("dateTimeMillis") as? Number)?.toLong() ?: 0L,
                                    venue = doc.getString("venue") ?: "",
                                    virtualLink = doc.getString("virtualLink"),
                                    chairperson = doc.getString("chairperson") ?: "",
                                    agenda = doc.getString("agenda") ?: "",
                                    status = doc.getString("status") ?: "Upcoming",
                                    notesOrMinutes = doc.getString("notesOrMinutes")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onMeetingsUpdated(meetings)
                    }
                }
            listeners.add(registration)
            registration
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncMeetingToCloud(meeting: Meeting) {
        val firestore = db ?: return
        try {
            val docRef = firestore.collection(COLLECTION_MEETINGS).document(meeting.id.toString())
            val data = hashMapOf(
                "id" to meeting.id,
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
                "notesOrMinutes" to (meeting.notesOrMinutes ?: "")
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync meeting error: ${e.message}")
        }
    }

    // --- NOTICES SYNC ---
    fun listenToNotices(onNoticesUpdated: (List<Notice>) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            val registration = firestore.collection(COLLECTION_NOTICES)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val notices = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = (doc.get("id") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                                Notice(
                                    id = id,
                                    title = doc.getString("title") ?: "",
                                    category = doc.getString("category") ?: "12 रबी-उल-अव्वल",
                                    priority = doc.getString("priority") ?: "NORMAL",
                                    issuedBy = doc.getString("issuedBy") ?: "",
                                    date = doc.getString("date") ?: "",
                                    content = doc.getString("content") ?: "",
                                    isPinned = doc.getBoolean("isPinned") ?: false
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onNoticesUpdated(notices)
                    }
                }
            listeners.add(registration)
            registration
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncNoticeToCloud(notice: Notice) {
        val firestore = db ?: return
        try {
            val docRef = firestore.collection(COLLECTION_NOTICES).document(notice.id.toString())
            val data = hashMapOf(
                "id" to notice.id,
                "title" to notice.title,
                "category" to notice.category,
                "priority" to notice.priority,
                "issuedBy" to notice.issuedBy,
                "date" to notice.date,
                "content" to notice.content,
                "isPinned" to notice.isPinned
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync notice error: ${e.message}")
        }
    }

    // --- CHAT MESSAGES SYNC ---
    fun listenToChat(channelId: String, onMessagesUpdated: (List<ChatMessage>) -> Unit): ListenerRegistration? {
        val firestore = db ?: return null
        return try {
            val registration = firestore.collection(COLLECTION_CHAT)
                .whereEqualTo("channelId", channelId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val messages = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = (doc.get("id") as? Number)?.toLong() ?: doc.id.toLongOrNull() ?: 0L
                                ChatMessage(
                                    id = id,
                                    channelId = doc.getString("channelId") ?: channelId,
                                    senderName = doc.getString("senderName") ?: "",
                                    senderRole = doc.getString("senderRole") ?: "खादिम",
                                    senderAvatarIndex = (doc.get("senderAvatarIndex") as? Number)?.toInt() ?: 0,
                                    messageText = doc.getString("messageText") ?: "",
                                    timestamp = (doc.get("timestamp") as? Number)?.toLong() ?: System.currentTimeMillis(),
                                    timeDisplay = doc.getString("timeDisplay") ?: "",
                                    isAnnouncement = doc.getBoolean("isAnnouncement") ?: false
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onMessagesUpdated(messages.sortedBy { it.timestamp })
                    }
                }
            listeners.add(registration)
            registration
        } catch (e: Exception) {
            null
        }
    }

    suspend fun syncChatMessageToCloud(message: ChatMessage) {
        val firestore = db ?: return
        try {
            val docId = if (message.id > 0) message.id.toString() else "${message.timestamp}_${message.senderName.hashCode()}"
            val docRef = firestore.collection(COLLECTION_CHAT).document(docId)
            val data = hashMapOf(
                "id" to (if (message.id > 0) message.id else System.currentTimeMillis()),
                "channelId" to message.channelId,
                "senderName" to message.senderName,
                "senderRole" to message.senderRole,
                "senderAvatarIndex" to message.senderAvatarIndex,
                "messageText" to message.messageText,
                "timestamp" to message.timestamp,
                "timeDisplay" to message.timeDisplay,
                "isAnnouncement" to message.isAnnouncement
            )
            docRef.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.w(TAG, "Cloud sync chat error: ${e.message}")
        }
    }

    suspend fun clearAllChatFromCloud() {
        val firestore = db ?: return
        try {
            val snapshot = firestore.collection(COLLECTION_CHAT).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud clear chat error: ${e.message}")
        }
    }

    suspend fun clearAllMembersFromCloud() {
        val firestore = db ?: return
        try {
            val snapshot = firestore.collection(COLLECTION_MEMBERS).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud clear members error: ${e.message}")
        }
    }

    suspend fun clearAllMeetingsFromCloud() {
        val firestore = db ?: return
        try {
            val snapshot = firestore.collection(COLLECTION_MEETINGS).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud clear meetings error: ${e.message}")
        }
    }

    suspend fun clearAllNoticesFromCloud() {
        val firestore = db ?: return
        try {
            val snapshot = firestore.collection(COLLECTION_NOTICES).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud clear notices error: ${e.message}")
        }
    }

    suspend fun clearAllDocumentsFromCloud() {
        val firestore = db ?: return
        try {
            val snapshot = firestore.collection(COLLECTION_DOCUMENTS).get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud clear documents error: ${e.message}")
        }
    }

    suspend fun clearAllAppCloudData() {
        clearAllChatFromCloud()
        clearAllDonationsFromCloud()
        clearAllNoticesFromCloud()
        clearAllMeetingsFromCloud()
        clearAllDocumentsFromCloud()
        clearAllMembersFromCloud()
    }

    fun clearListeners() {
        for (reg in listeners) {
            try {
                reg.remove()
            } catch (e: Exception) {
                // ignore
            }
        }
        listeners.clear()
    }
}
