package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.FirebaseFirestoreService
import com.example.data.local.TTSDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import com.example.data.repository.TTSRepository
import com.example.util.TTSNotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val repository: TTSRepository = TTSRepository(
        TTSDatabase.getDatabase(application, viewModelScope).ttsDao()
    )

    // Firebase Firestore Live Cloud Sync Status
    val isCloudConnected: StateFlow<Boolean> = repository.isCloudConnected
    val syncStatus: StateFlow<String> = repository.syncStatus
    val onlineCandidateIds: StateFlow<Set<Long>> = repository.onlineCandidateIds

    fun triggerCloudSync() {
        viewModelScope.launch {
            repository.triggerFullCloudSync()
            showSnackbar("🟢 सभी डिवाइस पर लाइव क्लाउड सिंक सफलतापूर्वक पूरा हुआ")
        }
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
            showSnackbar("गलत यूज़रनेम या पासवर्ड! केवल अधिकृत एडमिन को अनुमति प्राप्त है।")
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

    val approvedDonations: StateFlow<List<Donation>> = repository.approvedDonations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pendingDonations: StateFlow<List<Donation>> = repository.pendingDonations.stateIn(
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

    val totalApprovedDonationsSum: StateFlow<Double> = repository.totalApprovedDonationsSum
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Expenses (खर्च विवरण) streams
    val expenses: StateFlow<List<Expense>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalExpensesSum: StateFlow<Double> = repository.totalExpensesSum
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    // Live real-time Remaining Balance (कुल शुद्ध शेष बचत = कुल प्राप्त चंदा - कुल खर्च)
    val remainingBalance: StateFlow<Double> = combine(totalApprovedDonationsSum, totalExpensesSum) { approvedTotal, expensesTotal ->
        approvedTotal - expensesTotal
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Live real-time Donation Goal (चंदा संग्रह लक्ष्य - Admin Controlled & Synced across all devices)
    private val _donationGoal = MutableStateFlow(repository.getSavedDonationGoal())
    val donationGoal: StateFlow<Double> = _donationGoal.asStateFlow()

    fun updateDonationGoal(newGoal: Double) {
        if (newGoal <= 0) return
        viewModelScope.launch {
            _donationGoal.value = newGoal
            repository.syncDonationGoal(newGoal)
            showSnackbar("🎯 चंदा संग्रह लक्ष्य ₹${String.format(Locale.getDefault(), "%,.0f", newGoal)} सफलतापूर्वक सुरक्षित किया गया और सभी डिवाइस पर लाइव अपडेट हो गया!")
        }
    }

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

    val onlineMembers: StateFlow<List<Member>> = combine(members, onlineCandidateIds) { allMems, onlineIds ->
        allMems.filter { onlineIds.contains(it.id) }
    }.stateIn(
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
        bestBadge: String? = null,
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val count = members.value.size + 1
            val uniqueId = System.currentTimeMillis() + (1..999).random().toLong()
            val code = "TTS-1447-${String.format(Locale.getDefault(), "%03d", (uniqueId % 1000).toInt())}"
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val today = dateFormat.format(Date())

            val newMember = Member(
                id = uniqueId,
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
                photoResName = if (photoUri.isNullOrBlank() && isBestPerformer) "img_best_performer" else null,
                photoUri = photoUri?.trim()
            )
            val newId = repository.insertMember(newMember)
            repository.updateCandidatePresence(newMember.id, newMember.fullName, true)
            showSnackbar("नया सदस्य '${newMember.fullName}' सफलतापूर्वक जोड़ा गया और सभी डिवाइस पर सिंक हो गया! (आईडी: $code)")
        }
    }

    fun selfRegisterMember(
        fullName: String,
        phoneNumber: String,
        email: String,
        address: String,
        requestedWing: String,
        emergencyContact: String,
        photoUri: String? = null
    ) {
        viewModelScope.launch {
            val count = members.value.size + 1
            val uniqueId = System.currentTimeMillis() + (1..999).random().toLong()
            val code = "TTS-1447-${String.format(Locale.getDefault(), "%03d", (uniqueId % 1000).toInt())}"
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val today = dateFormat.format(Date())

            val newMember = Member(
                id = uniqueId,
                memberCode = code,
                fullName = fullName.trim(),
                designation = "खादिम-ए-कमेटी (Volunteer)",
                committeeWing = requestedWing.trim().ifEmpty { "12 रबी-उल-अव्वल जुलूस कमेटी" },
                phoneNumber = phoneNumber.trim(),
                email = email.trim(),
                bloodGroup = "",
                joinDate = "12 रबी-उल-अव्वल 1447H ($today)",
                address = address.trim(),
                emergencyContact = emergencyContact.trim(),
                avatarColorIndex = (count % 6),
                isBestPerformer = false,
                bestPerformerBadge = null,
                photoResName = null,
                photoUri = photoUri?.trim()
            )
            val insertedId = repository.insertMember(newMember)
            val memberWithId = newMember.copy(id = insertedId)
            _currentActiveMember.value = memberWithId
            _selectedMemberForIDCard.value = memberWithId
            _currentTab.value = AppTab.ID_CARD
            repository.updateCandidatePresence(memberWithId.id, memberWithId.fullName, true)
            showSnackbar("मुबारक! आपका 12 रबी-उल-अव्वल डिजिटल पहचान पत्र (ID Card) तैयार हो गया और सभी डिवाइस पर सिंक हो गया!")
        }
    }

    fun updateMemberPhoto(memberId: Long, photoUri: String?) {
        viewModelScope.launch {
            repository.updateMemberPhoto(memberId, photoUri)
            val current = members.value.find { it.id == memberId }
            if (current != null) {
                val updated = current.copy(photoUri = photoUri)
                if (_selectedMemberForIDCard.value?.id == memberId) {
                    _selectedMemberForIDCard.value = updated
                }
                if (_currentActiveMember.value?.id == memberId) {
                    _currentActiveMember.value = updated
                }
            }
            showSnackbar("सदस्य की फोटो सफलतापूर्वक अपडेट हो गई!")
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

    fun updateMeetingLink(meetingId: Long, newLink: String) {
        viewModelScope.launch {
            val meeting = meetings.value.find { it.id == meetingId }
            if (meeting != null) {
                val cleanedLink = newLink.trim().ifEmpty { null }
                val updated = meeting.copy(virtualLink = cleanedLink)
                repository.updateMeeting(updated)
                showSnackbar("वीडियो मीटिंग लिंक अपडेट किया गया")
            }
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
        fullContent: String,
        attachmentUri: String? = null,
        attachmentName: String? = null
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
                fileSize = if (attachmentUri != null) "सत्यापित संलग्नक" else "1.5 MB PDF",
                accessLevel = accessLevel.trim(),
                summary = summary.trim(),
                fullContent = fullContent.trim(),
                attachmentUri = attachmentUri,
                attachmentName = attachmentName
            )
            repository.insertDocument(doc)
            showSnackbar("दस्तावेज़ '$title' सफलतापूर्वक सहेजा गया और सभी डिवाइस पर सिंक हो गया")
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
        remarks: String?,
        isApproved: Boolean = true, // Default to true so all donations show live immediately across devices
        paymentProofUri: String? = null
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
                verified = isApproved,
                remarks = if (remarks.isNullOrBlank()) null else remarks.trim(),
                paymentProofUri = paymentProofUri
            )
            repository.insertDonation(donation)
            showSnackbar("₹$amount का चंदा ($donorName) सफलतापूर्वक दर्ज हुआ और सभी डिवाइस पर लाइव दिखेगा!")
        }
    }

    fun approveDonation(donation: Donation) {
        viewModelScope.launch {
            repository.approveDonation(donation, true)
            TTSNotificationHelper.showDonationNotification(
                context = getApplication(),
                donorName = donation.donorName,
                amount = donation.amount,
                receiptNumber = donation.transactionRef,
                note = "एडमिन द्वारा स्वीकृत: " + donation.purpose
            )
            showSnackbar("चंदा (${donation.donorName} - ₹${donation.amount.toInt()}) स्वीकृत हो गया और सभी डिवाइस पर लाइव अपडेट हो गया!")
        }
    }

    fun rejectDonation(donation: Donation) {
        viewModelScope.launch {
            repository.deleteDonation(donation.id)
            showSnackbar("चंदा प्रविष्टि (${donation.donorName}) अस्वीकृत कर दी गई।")
        }
    }

    // Expense Actions (खर्च विवरण - Kharch Vivran)
    fun addExpense(
        title: String,
        category: String,
        amount: Double,
        spentBy: String,
        receiptRef: String? = null,
        attachmentUri: String? = null,
        remarks: String? = null
    ) {
        viewModelScope.launch {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val count = expenses.value.size + 1
            val expense = Expense(
                title = title.trim(),
                category = category.trim(),
                amount = amount,
                spentBy = spentBy.trim().ifEmpty { "कमेटी व्यवस्थापक" },
                date = dateFormat.format(Date()),
                timestamp = System.currentTimeMillis(),
                receiptRef = receiptRef?.trim()?.ifEmpty { "EXP/TTS-${String.format("%03d", count)}" },
                attachmentUri = attachmentUri,
                remarks = remarks?.trim()
            )
            repository.insertExpense(expense)
            showSnackbar("खर्च विवरण '₹${amount.toInt()} - $title' सफलतापूर्वक दर्ज हो गया। शेष बचत स्वतः अपडेट हुई।")
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            showSnackbar("खर्च विवरण (${expense.title} - ₹${expense.amount.toInt()}) संशोधित हो गया!")
        }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            repository.deleteExpense(expenseId)
            showSnackbar("खर्च प्रविष्टि हटा दी गई। शेष बचत पुनः कैल्क्युलेट हो गई।")
        }
    }

    fun clearAllExpenses() {
        viewModelScope.launch {
            repository.clearAllExpenses()
            showSnackbar("सभी खर्च रिकॉर्ड्स साफ़ कर दिए गए!")
        }
    }

    fun updateDonation(donation: Donation) {
        viewModelScope.launch {
            repository.updateDonation(donation)
            showSnackbar("चंदा प्रविष्टि (${donation.donorName} - ₹${donation.amount.toInt()}) सफलतापूर्वक संशोधित की गई")
        }
    }

    fun updateDonationVerification(donationId: Long, isVerified: Boolean) {
        viewModelScope.launch {
            repository.updateDonationVerification(donationId, isVerified)
            showSnackbar(if (isVerified) "चंदा रसीद आधिकारिक रूप से सत्यापित की गई (Verified)" else "चंदा रसीद सत्यापन लंबित किया गया")
        }
    }

    fun deleteDonation(donationId: Long) {
        viewModelScope.launch {
            repository.deleteDonation(donationId)
            showSnackbar("चंदा प्रविष्टि हटा दी गई")
        }
    }

    fun clearAllOldDonations() {
        viewModelScope.launch {
            repository.clearAllDonations()
            showSnackbar("पुराने चंदा रिकॉर्ड्स हटा दिए गए हैं! नया बहीखाता आरंभ हो गया।")
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
            val senderMemberId = sender?.id ?: 0L

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val chatMsg = ChatMessage(
                id = System.currentTimeMillis() + (1..999).random().toLong(),
                channelId = _selectedChatChannel.value,
                senderName = senderName,
                senderRole = senderRole,
                senderAvatarIndex = avatarIdx,
                messageText = messageText.trim(),
                timestamp = System.currentTimeMillis(),
                timeDisplay = timeFormat.format(Date()),
                isAnnouncement = _isAdminLoggedIn.value,
                senderMemberId = senderMemberId,
                senderDeviceId = FirebaseFirestoreService.getDeviceId(),
                status = "SENT",
                isSeen = false
            )
            repository.sendChatMessage(chatMsg)
        }
    }

    fun markChatMessageSeen(messageId: Long, channelId: String = _selectedChatChannel.value) {
        viewModelScope.launch {
            repository.markChatMessageSeen(messageId, channelId)
        }
    }

    fun deleteChatMessage(messageId: Long, channelId: String = _selectedChatChannel.value) {
        viewModelScope.launch {
            repository.deleteChatMessage(messageId, channelId)
            showSnackbar("संदेश सभी डिवाइस से सफलतापूर्वक हटा दिया गया")
        }
    }

    fun clearAllChatMessages() {
        viewModelScope.launch {
            repository.clearAllChatMessages()
            showSnackbar("कमेटी चैट के सभी पुराने संदेश साफ़ कर दिए गए!")
        }
    }

    // Complete Application Data Wipe / Reset ("Old data delete karne par poori application saaf ho jaaye")
    fun clearEntireApplicationData() {
        viewModelScope.launch {
            repository.clearAllApplicationData()
            _currentActiveMember.value = null
            val prefs = getApplication<Application>().getSharedPreferences("tts_device_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            showSnackbar("संपूर्ण एप्लिकेशन डेटा (लोकल एवं क्लाउड) पूर्णतः साफ़ कर दिया गया!")
        }
    }

    fun clearAllMeetings() {
        viewModelScope.launch {
            repository.clearAllMeetings()
            showSnackbar("सभी बैठक रिकॉर्ड्स हटा दिए गए!")
        }
    }

    fun clearAllNotices() {
        viewModelScope.launch {
            repository.clearAllNotices()
            showSnackbar("सभी नोटिस रिकॉर्ड्स हटा दिए गए!")
        }
    }

    fun clearAllDocuments() {
        viewModelScope.launch {
            repository.clearAllDocuments()
            showSnackbar("सभी दस्तावेज़ रिकॉर्ड्स हटा दिए गए!")
        }
    }

    fun setActiveMember(member: Member?) {
        _currentActiveMember.value = member
        val prefs = getApplication<Application>().getSharedPreferences("tts_device_prefs", Context.MODE_PRIVATE)
        if (member != null) {
            prefs.edit().putLong("my_locked_member_id", member.id).apply()
            viewModelScope.launch {
                repository.updateCandidatePresence(member.id, member.fullName, true)
            }
        } else {
            prefs.edit().remove("my_locked_member_id").apply()
        }
    }

    fun lockDeviceProfile(member: Member) {
        setActiveMember(member)
        showSnackbar("🔒 आपकी स्थायी प्रोफ़ाइल '${member.fullName}' (${member.designation}) के रूप में इस डिवाइस पर सेट हो गई है।")
    }

    init {
        // Listen for real-time donation goal changes synced from other devices/admin
        repository.listenToDonationGoal { cloudGoal ->
            if (cloudGoal > 0) {
                _donationGoal.value = cloudGoal
            }
        }

        // Automatically load and lock the saved profile for this device
        viewModelScope.launch {
            try {
                val prefs = getApplication<Application>().getSharedPreferences("tts_device_prefs", Context.MODE_PRIVATE)
                val savedMemberId = prefs.getLong("my_locked_member_id", -1L)
                members.collect { memberList ->
                    if (savedMemberId > 0 && _currentActiveMember.value == null) {
                        val found = memberList.find { it.id == savedMemberId }
                        if (found != null) {
                            _currentActiveMember.value = found
                            repository.updateCandidatePresence(found.id, found.fullName, true)
                        }
                    } else if (_currentActiveMember.value != null) {
                        val updated = memberList.find { it.id == _currentActiveMember.value?.id }
                        if (updated != null) {
                            _currentActiveMember.value = updated
                        }
                    }
                }
            } catch (e: Exception) {
                // Graceful fallback
            }
        }
    }
}
