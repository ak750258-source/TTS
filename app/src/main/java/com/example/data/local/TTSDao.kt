package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import kotlinx.coroutines.flow.Flow

@Dao
interface TTSDao {

    // --- MEMBERS ---
    @Query("SELECT * FROM members ORDER BY id ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members ORDER BY id ASC")
    suspend fun getAllMembersList(): List<Member>

    @Query("SELECT * FROM members WHERE isBestPerformer = 1 ORDER BY id ASC")
    fun getBestPerformers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberById(id: Long): Member?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Member>)

    @Update
    suspend fun updateMember(member: Member)

    @Query("UPDATE members SET designation = :designation, committeeWing = :wing WHERE id = :id")
    suspend fun updateMemberDesignation(id: Long, designation: String, wing: String)

    @Query("UPDATE members SET isBestPerformer = :isBest, bestPerformerBadge = :badge WHERE id = :id")
    suspend fun setBestPerformer(id: Long, isBest: Boolean, badge: String?)

    @Query("UPDATE members SET photoUri = :photoUri WHERE id = :id")
    suspend fun updateMemberPhoto(id: Long, photoUri: String?)

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMemberById(id: Long)

    @Query("DELETE FROM members")
    suspend fun clearAllMembers()

    @Query("SELECT COUNT(*) FROM members")
    suspend fun getMembersCount(): Int

    // --- MEETINGS ---
    @Query("SELECT * FROM meetings ORDER BY dateTimeMillis ASC")
    fun getAllMeetings(): Flow<List<Meeting>>

    @Query("SELECT * FROM meetings ORDER BY dateTimeMillis ASC")
    suspend fun getAllMeetingsList(): List<Meeting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: Meeting): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeetings(meetings: List<Meeting>)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun deleteMeetingById(id: Long)

    @Query("DELETE FROM meetings")
    suspend fun clearAllMeetings()

    // --- DOCUMENTS ---
    @Query("SELECT * FROM documents ORDER BY id DESC")
    fun getAllDocuments(): Flow<List<OfficialDocument>>

    @Query("SELECT * FROM documents ORDER BY id DESC")
    suspend fun getAllDocumentsList(): List<OfficialDocument>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: OfficialDocument): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(docs: List<OfficialDocument>)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocumentById(id: Long)

    @Query("DELETE FROM documents")
    suspend fun clearAllDocuments()

    // --- NOTICES ---
    @Query("SELECT * FROM notices ORDER BY isPinned DESC, id DESC")
    fun getAllNotices(): Flow<List<Notice>>

    @Query("SELECT * FROM notices ORDER BY isPinned DESC, id DESC")
    suspend fun getAllNoticesList(): List<Notice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: Notice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<Notice>)

    @Query("DELETE FROM notices WHERE id = :id")
    suspend fun deleteNoticeById(id: Long)

    @Query("DELETE FROM notices")
    suspend fun clearAllNotices()

    // --- DONATIONS ---
    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    fun getAllDonations(): Flow<List<Donation>>

    @Query("SELECT * FROM donations ORDER BY timestamp DESC")
    suspend fun getAllDonationsList(): List<Donation>

    @Query("SELECT * FROM donations WHERE verified = 1 ORDER BY timestamp DESC")
    fun getApprovedDonations(): Flow<List<Donation>>

    @Query("SELECT * FROM donations WHERE verified = 0 ORDER BY timestamp DESC")
    fun getPendingDonations(): Flow<List<Donation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonation(donation: Donation): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonations(donations: List<Donation>)

    @Update
    suspend fun updateDonation(donation: Donation)

    @Query("SELECT SUM(amount) FROM donations")
    fun getTotalDonationsSum(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM donations WHERE verified = 1")
    fun getTotalApprovedDonationsSum(): Flow<Double?>

    @Query("UPDATE donations SET verified = :verified WHERE id = :id")
    suspend fun updateDonationVerification(id: Long, verified: Boolean)

    @Query("DELETE FROM donations WHERE id = :id")
    suspend fun deleteDonationById(id: Long)

    @Query("DELETE FROM donations")
    suspend fun clearAllDonations()

    // --- EXPENSES (खर्च विवरण) ---
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesList(): List<Expense>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpensesSum(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    // --- CHAT MESSAGES ---
    @Query("SELECT * FROM chat_messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(messages: List<ChatMessage>)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteChatMessage(id: Long)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllChatMessages()

    @Query("DELETE FROM chat_messages WHERE timestamp <= :timestamp")
    suspend fun clearChatMessagesOlderThan(timestamp: Long)
}
