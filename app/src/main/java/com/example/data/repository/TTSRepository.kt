package com.example.data.repository

import com.example.data.firebase.FirebaseFirestoreService
import com.example.data.local.TTSDao
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
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
    val firestoreService: FirebaseFirestoreService = FirebaseFirestoreService()
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    val isCloudConnected: StateFlow<Boolean> = firestoreService.isCloudConnected
    val syncStatus: StateFlow<String> = firestoreService.syncStatus

    init {
        // Start real-time Firestore listeners to sync from cloud to local Room database
        startCloudSync()
    }

    private fun startCloudSync() {
        // Sync Members from Cloud
        firestoreService.listenToMembers { cloudMembers ->
            if (cloudMembers.isNotEmpty()) {
                repositoryScope.launch {
                    ttsDao.insertMembers(cloudMembers)
                }
            }
        }

        // Sync Donations from Cloud
        firestoreService.listenToDonations { cloudDonations ->
            if (cloudDonations.isNotEmpty()) {
                repositoryScope.launch {
                    ttsDao.insertDonations(cloudDonations)
                }
            }
        }

        // Sync Meetings from Cloud
        firestoreService.listenToMeetings { cloudMeetings ->
            if (cloudMeetings.isNotEmpty()) {
                repositoryScope.launch {
                    ttsDao.insertMeetings(cloudMeetings)
                }
            }
        }

        // Sync Notices from Cloud
        firestoreService.listenToNotices { cloudNotices ->
            if (cloudNotices.isNotEmpty()) {
                repositoryScope.launch {
                    ttsDao.insertNotices(cloudNotices)
                }
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
        val id = ttsDao.insertMeeting(meeting)
        val saved = meeting.copy(id = id)
        firestoreService.syncMeetingToCloud(saved)
        return id
    }
    suspend fun deleteMeeting(id: Long) = ttsDao.deleteMeetingById(id)

    // Documents
    val allDocuments: Flow<List<OfficialDocument>> = ttsDao.getAllDocuments()
    suspend fun insertDocument(doc: OfficialDocument): Long = ttsDao.insertDocument(doc)
    suspend fun deleteDocument(id: Long) = ttsDao.deleteDocumentById(id)

    // Notices
    val allNotices: Flow<List<Notice>> = ttsDao.getAllNotices()
    suspend fun insertNotice(notice: Notice): Long {
        val id = ttsDao.insertNotice(notice)
        val saved = notice.copy(id = id)
        firestoreService.syncNoticeToCloud(saved)
        return id
    }
    suspend fun deleteNotice(id: Long) = ttsDao.deleteNoticeById(id)

    // Donations
    val allDonations: Flow<List<Donation>> = ttsDao.getAllDonations()
    val totalDonationsSum: Flow<Double?> = ttsDao.getTotalDonationsSum()
    
    suspend fun insertDonation(donation: Donation): Long {
        val id = ttsDao.insertDonation(donation)
        val saved = donation.copy(id = id)
        firestoreService.syncDonationToCloud(saved)
        return id
    }

    suspend fun updateDonationVerification(id: Long, verified: Boolean) {
        ttsDao.updateDonationVerification(id, verified)
    }

    suspend fun deleteDonation(id: Long) {
        ttsDao.deleteDonationById(id)
        firestoreService.deleteDonationFromCloud(id)
    }

    suspend fun clearAllDonations() {
        ttsDao.clearAllDonations()
        firestoreService.clearAllDonationsFromCloud()
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

    suspend fun deleteChatMessage(id: Long) = ttsDao.deleteChatMessage(id)
}

