package com.example.data.repository

import com.example.data.firebase.FirebaseFirestoreService
import com.example.data.local.TTSDao
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TTSRepository(
    private val ttsDao: TTSDao,
    val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService.getInstance()
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val isCloudConnected: StateFlow<Boolean> = firestoreService.isCloudConnected
    val syncStatus: StateFlow<String> = firestoreService.syncStatus
    val onlineCandidateIds: StateFlow<Set<Long>> = firestoreService.onlineCandidateIds

    suspend fun updateCandidatePresence(memberId: Long, memberName: String, isOnline: Boolean) {
        firestoreService.updateCandidatePresence(memberId, memberName, isOnline)
    }

    fun triggerCloudCatchup() {
        firestoreService.fetchCatchupEvents()
    }

    suspend fun syncAllMembersToCloud() {
        val list = ttsDao.getAllMembersList()
        list.forEach { member ->
            firestoreService.syncMemberToCloud(member)
        }
    }

    suspend fun triggerFullCloudSync() {
        firestoreService.fetchCatchupEvents()
        syncAllMembersToCloud()
    }

    init {
        // Start real-time Firestore listeners to sync from cloud to local Room database
        startCloudSync()
    }

    private fun startCloudSync() {
        // Sync Members from Cloud
        firestoreService.listenToMembers(
            onMembersUpdated = { cloudMembers ->
                if (cloudMembers.isNotEmpty()) {
                    repositoryScope.launch {
                        for (cm in cloudMembers) {
                            val existing = ttsDao.getMemberById(cm.id)
                            if (existing != null) {
                                val finalPhoto = if (!cm.photoUri.isNullOrBlank()) cm.photoUri else existing.photoUri
                                ttsDao.updateMember(cm.copy(photoUri = finalPhoto))
                            } else {
                                ttsDao.insertMember(cm)
                            }
                        }
                    }
                }
            },
            onMemberDeleted = { memberId ->
                repositoryScope.launch {
                    ttsDao.deleteMemberById(memberId)
                }
            }
        )

        // Sync Member Photo Updates from Cloud
        firestoreService.listenToMemberPhotoUpdates { memberId, newPhotoUri ->
            repositoryScope.launch {
                ttsDao.updateMemberPhoto(memberId, newPhotoUri.takeIf { it.isNotBlank() })
            }
        }

        // Sync Donations from Cloud
        firestoreService.listenToDonations(
            onDonationsUpdated = { cloudDonations ->
                if (cloudDonations.isNotEmpty()) {
                    repositoryScope.launch {
                        ttsDao.insertDonations(cloudDonations)
                    }
                }
            },
            onDonationDeleted = { donationId ->
                repositoryScope.launch {
                    ttsDao.deleteDonationById(donationId)
                }
            }
        )

        // Sync Meetings from Cloud
        firestoreService.listenToMeetings(
            onMeetingsUpdated = { cloudMeetings ->
                if (cloudMeetings.isNotEmpty()) {
                    repositoryScope.launch {
                        ttsDao.insertMeetings(cloudMeetings)
                    }
                }
            },
            onMeetingDeleted = { meetingId ->
                repositoryScope.launch {
                    ttsDao.deleteMeetingById(meetingId)
                }
            }
        )

        // Sync Notices from Cloud
        firestoreService.listenToNotices(
            onNoticesUpdated = { cloudNotices ->
                if (cloudNotices.isNotEmpty()) {
                    repositoryScope.launch {
                        ttsDao.insertNotices(cloudNotices)
                    }
                }
            },
            onNoticeDeleted = { noticeId ->
                repositoryScope.launch {
                    ttsDao.deleteNoticeById(noticeId)
                }
            }
        )

        // Sync Documents from Cloud
        firestoreService.listenToDocuments(
            onDocsUpdated = { docs ->
                if (docs.isNotEmpty()) {
                    repositoryScope.launch {
                        ttsDao.insertDocuments(docs)
                    }
                }
            },
            onDocDeleted = { docId ->
                repositoryScope.launch {
                    ttsDao.deleteDocumentById(docId)
                }
            }
        )

        // Sync Expenses from Cloud
        firestoreService.listenToExpenses(
            onExpensesUpdated = { cloudExpenses ->
                if (cloudExpenses.isNotEmpty()) {
                    repositoryScope.launch {
                        ttsDao.insertExpenses(cloudExpenses)
                    }
                }
            },
            onExpenseDeleted = { expenseId ->
                repositoryScope.launch {
                    ttsDao.deleteExpenseById(expenseId)
                }
            }
        )

        // Sync Chat from Cloud across all channels and global stream immediately
        listOf("general", "rabi_ul_awwal", "donations", "announcements", "duas", "management", "all").forEach { ch ->
            firestoreService.listenToChat(ch) { messages ->
                if (messages.isNotEmpty()) {
                    repositoryScope.launch {
                        ttsDao.insertChatMessages(messages)
                    }
                }
            }
        }

        // Real-time Chat message deletion across all devices
        firestoreService.listenToChatDelete { messageId ->
            repositoryScope.launch {
                ttsDao.deleteChatMessage(messageId)
            }
        }

        // Real-time Chat message SEEN (Double Tick) across devices
        firestoreService.listenToChatSeen { messageId, _ ->
            repositoryScope.launch {
                ttsDao.markMessageSeen(messageId)
            }
        }

        // Real-time Chat clear across all devices
        firestoreService.listenToChatClear {
            repositoryScope.launch {
                ttsDao.clearAllChatMessages()
            }
        }

        // Listen for sync request from any newly joined device and sync all database records
        firestoreService.listenToSyncRequest {
            repositoryScope.launch {
                val docs = ttsDao.getAllDocumentsList()
                docs.forEach { d ->
                    firestoreService.syncDocumentToCloud(d)
                }

                val members = ttsDao.getAllMembersList()
                members.forEach { m ->
                    firestoreService.syncMemberToCloud(m)
                }

                val notices = ttsDao.getAllNoticesList()
                notices.forEach { n ->
                    firestoreService.syncNoticeToCloud(n)
                }

                val donations = ttsDao.getAllDonationsList()
                donations.forEach { don ->
                    firestoreService.syncDonationToCloud(don)
                }

                val meetings = ttsDao.getAllMeetingsList()
                meetings.forEach { mtg ->
                    firestoreService.syncMeetingToCloud(mtg)
                }

                val expenses = ttsDao.getAllExpensesList()
                expenses.forEach { exp ->
                    firestoreService.syncExpenseToCloud(exp)
                }
            }
        }

        // Global data wipe listener
        firestoreService.listenToClearAll {
            repositoryScope.launch {
                ttsDao.clearAllChatMessages()
                ttsDao.clearAllDonations()
                ttsDao.clearAllExpenses()
                ttsDao.clearAllNotices()
                ttsDao.clearAllMeetings()
                ttsDao.clearAllDocuments()
                ttsDao.clearAllMembers()
            }
        }
    }

    fun listenToChannelChat(channelId: String) {
        firestoreService.listenToChat(channelId) { messages ->
            if (messages.isNotEmpty()) {
                repositoryScope.launch {
                    ttsDao.insertChatMessages(messages)
                }
            }
        }
    }

    // Members
    val allMembers: Flow<List<Member>> = ttsDao.getAllMembers()
    val bestPerformers: Flow<List<Member>> = ttsDao.getBestPerformers()
    suspend fun getMemberById(id: Long): Member? = ttsDao.getMemberById(id)
    
    suspend fun insertMember(member: Member): Long {
        val id = ttsDao.insertMember(member)
        val saved = member.copy(id = id)
        firestoreService.syncMemberToCloud(saved)
        return id
    }

    suspend fun updateMember(member: Member) {
        ttsDao.updateMember(member)
        firestoreService.syncMemberToCloud(member)
    }

    suspend fun updateMemberDesignation(id: Long, designation: String, wing: String) {
        ttsDao.updateMemberDesignation(id, designation, wing)
        ttsDao.getMemberById(id)?.let { updated ->
            firestoreService.syncMemberToCloud(updated)
        }
    }

    suspend fun setBestPerformer(id: Long, isBest: Boolean, badge: String?) {
        ttsDao.setBestPerformer(id, isBest, badge)
        ttsDao.getMemberById(id)?.let { updated ->
            firestoreService.syncMemberToCloud(updated)
        }
    }

    suspend fun updateMemberPhoto(id: Long, photoUri: String?) {
        ttsDao.updateMemberPhoto(id, photoUri)
        firestoreService.syncMemberPhotoToCloud(id, photoUri)
        ttsDao.getMemberById(id)?.let { updated ->
            firestoreService.syncMemberToCloud(updated)
        }
    }

    suspend fun deleteMember(id: Long) {
        ttsDao.deleteMemberById(id)
        firestoreService.deleteMemberFromCloud(id)
    }

    // Meetings
    val allMeetings: Flow<List<Meeting>> = ttsDao.getAllMeetings()
    suspend fun insertMeeting(meeting: Meeting): Long {
        val uniqueId = if (meeting.id > 0) meeting.id else (System.currentTimeMillis() + (1..999).random().toLong())
        val withId = meeting.copy(id = uniqueId)
        val id = ttsDao.insertMeeting(withId)
        val saved = withId.copy(id = id)
        firestoreService.syncMeetingToCloud(saved)
        return id
    }
    suspend fun updateMeeting(meeting: Meeting) {
        ttsDao.updateMeeting(meeting)
        firestoreService.syncMeetingToCloud(meeting)
    }
    suspend fun deleteMeeting(id: Long) {
        ttsDao.deleteMeetingById(id)
        firestoreService.deleteMeetingFromCloud(id)
    }

    // Documents
    val allDocuments: Flow<List<OfficialDocument>> = ttsDao.getAllDocuments()
    suspend fun insertDocument(doc: OfficialDocument): Long {
        val uniqueId = if (doc.id > 0) doc.id else (System.currentTimeMillis() + (1..999).random().toLong())
        val withId = doc.copy(id = uniqueId)
        val id = ttsDao.insertDocument(withId)
        val saved = withId.copy(id = id)
        firestoreService.syncDocumentToCloud(saved)
        return id
    }
    suspend fun deleteDocument(id: Long) {
        ttsDao.deleteDocumentById(id)
        firestoreService.deleteDocumentFromCloud(id)
    }

    // Notices
    val allNotices: Flow<List<Notice>> = ttsDao.getAllNotices()
    suspend fun insertNotice(notice: Notice): Long {
        val uniqueId = if (notice.id > 0) notice.id else (System.currentTimeMillis() + (1..999).random().toLong())
        val withId = notice.copy(id = uniqueId)
        val id = ttsDao.insertNotice(withId)
        val saved = withId.copy(id = id)
        firestoreService.syncNoticeToCloud(saved)
        return id
    }
    suspend fun deleteNotice(id: Long) {
        ttsDao.deleteNoticeById(id)
        firestoreService.deleteNoticeFromCloud(id)
    }

    // Donations
    val allDonations: Flow<List<Donation>> = ttsDao.getAllDonations()
    val approvedDonations: Flow<List<Donation>> = ttsDao.getApprovedDonations()
    val pendingDonations: Flow<List<Donation>> = ttsDao.getPendingDonations()
    val totalDonationsSum: Flow<Double?> = ttsDao.getTotalDonationsSum()
    val totalApprovedDonationsSum: Flow<Double?> = ttsDao.getTotalApprovedDonationsSum()
    
    suspend fun insertDonation(donation: Donation): Long {
        val uniqueId = if (donation.id > 0) donation.id else (System.currentTimeMillis() + (1..999).random().toLong())
        val withId = donation.copy(id = uniqueId)
        val id = ttsDao.insertDonation(withId)
        val saved = withId.copy(id = id)
        firestoreService.syncDonationToCloud(saved)
        return id
    }

    suspend fun updateDonation(donation: Donation) {
        ttsDao.updateDonation(donation)
        firestoreService.syncDonationToCloud(donation)
    }

    suspend fun updateDonationVerification(id: Long, verified: Boolean) {
        ttsDao.updateDonationVerification(id, verified)
        val donations = ttsDao.getAllDonationsList()
        donations.find { it.id == id }?.let { updated ->
            firestoreService.syncDonationToCloud(updated.copy(verified = verified))
        }
    }

    suspend fun approveDonation(donation: Donation, isApproved: Boolean) {
        val updated = donation.copy(verified = isApproved)
        ttsDao.updateDonation(updated)
        firestoreService.syncDonationToCloud(updated)
    }

    suspend fun deleteDonation(id: Long) {
        ttsDao.deleteDonationById(id)
        firestoreService.deleteDonationFromCloud(id)
    }

    suspend fun clearAllDonations() {
        ttsDao.clearAllDonations()
        firestoreService.clearAllDonationsFromCloud()
    }

    // Expenses (खर्च विवरण)
    val allExpenses: Flow<List<Expense>> = ttsDao.getAllExpenses()
    val totalExpensesSum: Flow<Double?> = ttsDao.getTotalExpensesSum()

    suspend fun insertExpense(expense: Expense): Long {
        val uniqueId = if (expense.id > 0) expense.id else (System.currentTimeMillis() + (1..999).random().toLong())
        val withId = expense.copy(id = uniqueId)
        val id = ttsDao.insertExpense(withId)
        val saved = withId.copy(id = id)
        firestoreService.syncExpenseToCloud(saved)
        return id
    }

    suspend fun updateExpense(expense: Expense) {
        ttsDao.updateExpense(expense)
        firestoreService.syncExpenseToCloud(expense)
    }

    suspend fun deleteExpense(id: Long) {
        ttsDao.deleteExpenseById(id)
        firestoreService.deleteExpenseFromCloud(id)
    }

    suspend fun clearAllExpenses() {
        ttsDao.clearAllExpenses()
        firestoreService.clearAllExpensesFromCloud()
    }

    // Chat
    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessage>> {
        listenToChannelChat(channelId)
        return ttsDao.getMessagesForChannel(channelId)
    }

    suspend fun sendChatMessage(message: ChatMessage): Long {
        val id = ttsDao.insertChatMessage(message)
        val saved = message.copy(id = id)
        firestoreService.syncChatMessageToCloud(saved)
        return id
    }

    suspend fun markChatMessageSeen(id: Long, channelId: String = "general") {
        ttsDao.markMessageSeen(id)
        firestoreService.broadcastChatSeen(id, channelId)
    }

    suspend fun deleteChatMessage(id: Long, channelId: String = "general") {
        ttsDao.deleteChatMessage(id)
        firestoreService.deleteChatMessageFromCloud(id, channelId)
    }

    suspend fun clearAllChatMessages() {
        ttsDao.clearAllChatMessages()
        firestoreService.clearAllChatFromCloud()
    }

    suspend fun clearAllNotices() {
        ttsDao.clearAllNotices()
        firestoreService.clearAllNoticesFromCloud()
    }

    suspend fun clearAllMeetings() {
        ttsDao.clearAllMeetings()
        firestoreService.clearAllMeetingsFromCloud()
    }

    suspend fun clearAllDocuments() {
        ttsDao.clearAllDocuments()
        firestoreService.clearAllDocumentsFromCloud()
    }

    suspend fun clearAllMembers() {
        ttsDao.clearAllMembers()
        firestoreService.clearAllMembersFromCloud()
    }

    suspend fun clearAllApplicationData() {
        ttsDao.clearAllChatMessages()
        ttsDao.clearAllDonations()
        ttsDao.clearAllExpenses()
        ttsDao.clearAllNotices()
        ttsDao.clearAllMeetings()
        ttsDao.clearAllDocuments()
        ttsDao.clearAllMembers()
        firestoreService.clearAllAppCloudData()
    }
}
