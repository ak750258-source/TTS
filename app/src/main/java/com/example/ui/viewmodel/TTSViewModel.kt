package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.TTSDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import com.example.data.repository.TTSRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppTab(val titleHindi: String, val titleEnglish: String, val route: String) {
    HOME("मुख्य पृष्ठ", "Overview", "home"),
    MEMBERS("सदस्य सूची", "Members", "members"),
    ID_CARD("पहचान पत्र", "ID Card", "id_card"),
    MEETINGS("बैठकें व चैट", "Meetings & Chat", "meetings"),
    NOTICES("सूचना पट्ट", "Notices & Docs", "notices"),
    DONATIONS("चंदा व UPI", "Donations", "donations")
}

class TTSViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TTSRepository

    init {
        val database = TTSDatabase.getDatabase(application, viewModelScope)
        repository = TTSRepository(database.ttsDao())
    }

    // Navigation State
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun setTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // Admin State & Authentication (username: admin, password: admin)
    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _showAdminLoginDialog = MutableStateFlow(false)
    val showAdminLoginDialog: StateFlow<Boolean> = _showAdminLoginDialog.asStateFlow()

    fun openAdminLogin() {
        _showAdminLoginDialog.value = true
    }

    fun closeAdminLogin() {
        _showAdminLoginDialog.value = false
    }

    fun loginAdmin(usernameInput: String, passwordInput: String): Boolean {
        return if (usernameInput.trim() == "admin" && passwordInput.trim() == "admin") {
            _isAdminLoggedIn.value = true
            _showAdminLoginDialog.value = false
            showSnackbar("सफलतापूर्वक एडमिन लॉगिन हो गया! अब आप पद वितरण, नोटिस व सदस्य संपादित कर सकते हैं।")
            true
        } else {
            showSnackbar("गलत यूज़रनेम या पासवर्ड! केवल अधिकृत एडमिन (admin / admin) अनुमति प्राप्त है।")
            false
        }
    }

    fun logoutAdmin() {
        _isAdminLoggedIn.value = false
        showSnackbar("एडमिन लॉगआउट हो गया।")
    }

    // Online Status / Live Network Connectivity
    private val _isOnlineMode = MutableStateFlow(true)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    fun setOnlineMode(online: Boolean) {
        _isOnlineMode.value = online
        showSnackbar(if (online) "● ऑनलाइन लाइव नेटवर्क सक्रिय (Online Live Sync)" else "ऑफलाइन मोड सक्रिय")
    }

    // Active User Persona for committee interactions
    private val _currentActiveMember = MutableStateFlow<Member?>(null)
    val currentActiveMember: StateFlow<Member?> = _currentActiveMember.asStateFlow()

    // Snackbar notifications
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    // Selected items for modal/dialog viewing
    private val _selectedMemberForIDCard = MutableStateFlow<Member?>(null)
    val selectedMemberForIDCard: StateFlow<Member?> = _selectedMemberForIDCard.asStateFlow()

    private val _selectedDocForView = MutableStateFlow<OfficialDocument?>(null)
    val selectedDocForView: StateFlow<OfficialDocument?> = _selectedDocForView.asStateFlow()

    private val _selectedMeetingForDetail = MutableStateFlow<Meeting?>(null)
    val selectedMeetingForDetail: StateFlow<Meeting?> = _selectedMeetingForDetail.asStateFlow()

    // Member to edit / change designation
    private val _memberToEdit = MutableStateFlow<Member?>(null)
    val memberToEdit: StateFlow<Member?> = _memberToEdit.asStateFlow()

    fun selectMemberToEdit(member: Member?) {
        _memberToEdit.value = member
    }

    // Selected Chat Channel
    private val _selectedChatChannel = MutableStateFlow("general")
    val selectedChatChannel: StateFlow<String> = _selectedChatChannel.asStateFlow()

    fun selectChatChannel(channelId: String) {
        _selectedChatChannel.value = channelId
    }

    // Data streams from Room
    val members: StateFlow<List<Member>> = repository.allMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val bestPerformers: StateFlow<List<Member>> = repository.bestPerformers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val meetings: StateFlow<List<Meeting>> = repository.allMeetings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val documents: StateFlow<List<OfficialDocument>> = repository.allDocuments.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notices: StateFlow<List<Notice>> = repository.allNotices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val donations: StateFlow<List<Donation>> = repository.allDonations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalDonationsSum: StateFlow<Double> = repository.totalDonationsSum
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentChannelMessages: StateFlow<List<ChatMessage>> = _selectedChatChannel
        .flatMapLatest { channelId ->
            repository.getMessagesForChannel(channelId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Member Actions
    fun addMember(
        fullName: String,
        designation: String,
        committeeWing: String,
        phoneNumber: String,
        email: String,
        bloodGroup: String = "",
        address: String,
        emergencyContact: String,
        isBestPerformer: Boolean = false,
        bestBadge: String? = null
    ) {
        viewModelScope.launch {
            val count = members.value.size + 1
            val code = "TTS-1447-${String.format(Locale.getDefault(), "%03d", count)}"
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val today = dateFormat.format(Date())

            val newMember = Member(
                memberCode = code,
                fullName = fullName.trim(),
                designation = designation.trim(),
                committeeWing = committeeWing.trim(),
                phoneNumber = phoneNumber.trim(),
                email = email.trim(),
                bloodGroup = bloodGroup.trim(),
                joinDate = "12 रबी-उल-अव्वल 1447H ($today)",
                address = address.trim(),
                emergencyContact = emergencyContact.trim(),
                avatarColorIndex = (count % 6),
                isBestPerformer = isBestPerformer,
                bestPerformerBadge = if (isBestPerformer) (bestBadge ?: "विशेष खिदमतगार सम्मान") else null,
                photoResName = if (isBestPerformer) "img_best_performer" else null
            )
            repository.insertMember(newMember)
            showSnackbar("नया सदस्य '${newMember.fullName}' सफलतापूर्वक जोड़ा गया! (आईडी: $code)")
        }
    }

    fun updateMember(member: Member) {
        viewModelScope.launch {
            repository.updateMember(member)
            if (_selectedMemberForIDCard.value?.id == member.id) {
                _selectedMemberForIDCard.value = member
            }
            showSnackbar("सदस्य '${member.fullName}' का विवरण व पद सफलतापूर्वक अपडेट किया गया")
        }
    }

    fun distributeDesignation(memberId: Long, newDesignation: String, newWing: String) {
        viewModelScope.launch {
            repository.updateMemberDesignation(memberId, newDesignation.trim(), newWing.trim())
            val current = members.value.find { it.id == memberId }
            if (current != null && _selectedMemberForIDCard.value?.id == memberId) {
                _selectedMemberForIDCard.value = current.copy(designation = newDesignation.trim(), committeeWing = newWing.trim())
            }
            showSnackbar("पद वितरण सफल! नया पद: $newDesignation (ID कार्ड पर तुरंत अपडेट हो गया)")
        }
    }

    fun toggleBestPerformerStatus(memberId: Long, isBest: Boolean, badgeTitle: String?) {
        viewModelScope.launch {
            repository.setBestPerformer(memberId, isBest, if (isBest) (badgeTitle ?: "12 रबी-उल-अव्वल सर्वश्रेष्ठ खिदमतगार") else null)
            val name = members.value.find { it.id == memberId }?.fullName ?: "सदस्य"
            showSnackbar(if (isBest) "🏆 $name को 'बेस्ट परफ़ॉर्मर' सम्मान प्रदान किया गया!" else "$name का बेस्ट परफ़ॉर्मर स्टेटस हटाया गया")
        }
    }

    fun deleteMember(memberId: Long, memberName: String) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
            if (_selectedMemberForIDCard.value?.id == memberId) {
                _selectedMemberForIDCard.value = null
            }
            showSnackbar("सदस्य $memberName को कमेटी सूची से हटाया गया")
        }
    }

    fun selectMemberForIDCard(member: Member?) {
        _selectedMemberForIDCard.value = member
    }

    fun selectDocumentForView(doc: OfficialDocument?) {
        _selectedDocForView.value = doc
    }

    fun selectMeetingForDetail(meeting: Meeting?) {
        _selectedMeetingForDetail.value = meeting
    }

    // Meeting Actions
    fun addMeeting(
        title: String,
        type: String,
        dateDisplay: String,
        timeDisplay: String,
        venue: String,
        virtualLink: String?,
        chairperson: String,
        agenda: String
    ) {
        viewModelScope.launch {
            val meeting = Meeting(
                title = title.trim(),
                type = type.trim(),
                dateDisplay = dateDisplay.trim(),
                timeDisplay = timeDisplay.trim(),
                dateTimeMillis = System.currentTimeMillis() + (7L * 24 * 3600 * 1000),
                venue = venue.trim(),
                virtualLink = if (virtualLink.isNullOrBlank()) null else virtualLink.trim(),
                chairperson = chairperson.trim(),
                agenda = agenda.trim(),
                status = "Upcoming"
            )
            repository.insertMeeting(meeting)
            showSnackbar("नई बैठक तय की गई: ${meeting.title}")
        }
    }

    fun deleteMeeting(meetingId: Long) {
        viewModelScope.launch {
            repository.deleteMeeting(meetingId)
            showSnackbar("बैठक का कार्यक्रम हटा दिया गया")
        }
    }

    // Notice Actions
    fun addNotice(
        title: String,
        category: String,
        priority: String,
        issuedBy: String,
        content: String,
        isPinned: Boolean
    ) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val notice = Notice(
                title = title.trim(),
                category = category.trim(),
                priority = priority.trim(),
                issuedBy = issuedBy.trim(),
                date = dateFormat.format(Date()),
                content = content.trim(),
                isPinned = isPinned
            )
            repository.insertNotice(notice)
            showSnackbar("आधिकारिक सूचना सूचना-पट्ट पर जारी की गई")
        }
    }

    fun deleteNotice(noticeId: Long) {
        viewModelScope.launch {
            repository.deleteNotice(noticeId)
            showSnackbar("सूचना पट्ट से नोटिस हटा दिया गया")
        }
    }

    // Document Actions
    fun addDocument(
        title: String,
        category: String,
        accessLevel: String,
        summary: String,
        fullContent: String
    ) {
        viewModelScope.launch {
            val count = documents.value.size + 1
            val ref = "TTS/DOC/1447/${String.format(Locale.getDefault(), "%03d", count)}"
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val doc = OfficialDocument(
                title = title.trim(),
                category = category.trim(),
                refCode = ref,
                publishedDate = dateFormat.format(Date()),
                fileSize = "1.5 MB PDF",
                accessLevel = accessLevel.trim(),
                summary = summary.trim(),
                fullContent = fullContent.trim()
            )
            repository.insertDocument(doc)
            showSnackbar("दस्तावेज़ '$title' सफलतापूर्वक सहेजा गया")
        }
    }

    fun deleteDocument(docId: Long) {
        viewModelScope.launch {
            repository.deleteDocument(docId)
            if (_selectedDocForView.value?.id == docId) {
                _selectedDocForView.value = null
            }
            showSnackbar("दस्तावेज़ हटा दिया गया")
        }
    }

    // Donation Actions (UPI ak750258@icici)
    fun addDonation(
        donorName: String,
        donorMemberCode: String?,
        amount: Double,
        purpose: String,
        paymentMode: String = "UPI (ak750258@icici)",
        transactionRef: String,
        remarks: String?
    ) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val donation = Donation(
                donorName = donorName.trim(),
                donorMemberCode = if (donorMemberCode.isNullOrBlank()) null else donorMemberCode.trim(),
                amount = amount,
                purpose = purpose.trim(),
                paymentMode = paymentMode.trim(),
                transactionRef = transactionRef.trim().ifEmpty {
                    "UPI/${System.currentTimeMillis().toString().takeLast(10)}"
                },
                date = dateFormat.format(Date()),
                timestamp = System.currentTimeMillis(),
                verified = true,
                remarks = if (remarks.isNullOrBlank()) null else remarks.trim()
            )
            repository.insertDonation(donation)
            showSnackbar("₹$amount का चंदा ($donorName) पारदर्शी सार्वजनिक लेजर में दर्ज हो गया!")
        }
    }

    // Chat Message Actions (Online live communication)
    fun sendChatMessage(messageText: String) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            val sender = _currentActiveMember.value ?: members.value.firstOrNull()
            val senderName = sender?.fullName ?: "कमेटी खादिम"
            val senderRole = sender?.designation ?: "सदस्य"
            val avatarIdx = sender?.avatarColorIndex ?: 0

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val chatMsg = ChatMessage(
                channelId = _selectedChatChannel.value,
                senderName = senderName,
                senderRole = senderRole,
                senderAvatarIndex = avatarIdx,
                messageText = messageText.trim(),
                timestamp = System.currentTimeMillis(),
                timeDisplay = timeFormat.format(Date()),
                isAnnouncement = _isAdminLoggedIn.value
            )
            repository.sendChatMessage(chatMsg)
        }
    }

    fun setActiveMember(member: Member?) {
        _currentActiveMember.value = member
    }
}
