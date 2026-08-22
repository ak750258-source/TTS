package com.example.data.repository

import com.example.data.local.TTSDao
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import kotlinx.coroutines.flow.Flow

class TTSRepository(private val ttsDao: TTSDao) {

    // Members
    val allMembers: Flow<List<Member>> = ttsDao.getAllMembers()
    val bestPerformers: Flow<List<Member>> = ttsDao.getBestPerformers()
    suspend fun getMemberById(id: Long): Member? = ttsDao.getMemberById(id)
    suspend fun insertMember(member: Member): Long = ttsDao.insertMember(member)
    suspend fun updateMember(member: Member) = ttsDao.updateMember(member)
    suspend fun updateMemberDesignation(id: Long, designation: String, wing: String) =
        ttsDao.updateMemberDesignation(id, designation, wing)
    suspend fun setBestPerformer(id: Long, isBest: Boolean, badge: String?) =
        ttsDao.setBestPerformer(id, isBest, badge)
    suspend fun deleteMember(id: Long) = ttsDao.deleteMemberById(id)

    // Meetings
    val allMeetings: Flow<List<Meeting>> = ttsDao.getAllMeetings()
    suspend fun insertMeeting(meeting: Meeting): Long = ttsDao.insertMeeting(meeting)
    suspend fun deleteMeeting(id: Long) = ttsDao.deleteMeetingById(id)

    // Documents
    val allDocuments: Flow<List<OfficialDocument>> = ttsDao.getAllDocuments()
    suspend fun insertDocument(doc: OfficialDocument): Long = ttsDao.insertDocument(doc)
    suspend fun deleteDocument(id: Long) = ttsDao.deleteDocumentById(id)

    // Notices
    val allNotices: Flow<List<Notice>> = ttsDao.getAllNotices()
    suspend fun insertNotice(notice: Notice): Long = ttsDao.insertNotice(notice)
    suspend fun deleteNotice(id: Long) = ttsDao.deleteNoticeById(id)

    // Donations
    val allDonations: Flow<List<Donation>> = ttsDao.getAllDonations()
    val totalDonationsSum: Flow<Double?> = ttsDao.getTotalDonationsSum()
    suspend fun insertDonation(donation: Donation): Long = ttsDao.insertDonation(donation)

    // Chat
    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessage>> =
        ttsDao.getMessagesForChannel(channelId)

    suspend fun sendChatMessage(message: ChatMessage): Long = ttsDao.insertChatMessage(message)
    suspend fun deleteChatMessage(id: Long) = ttsDao.deleteChatMessage(id)
}
