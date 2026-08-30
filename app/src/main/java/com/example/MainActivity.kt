package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Donation
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.OfficialDocument
import com.example.service.TTSBackgroundSyncService
import com.example.ui.components.AddDocumentDialog
import com.example.ui.components.AddDonationRecordDialog
import com.example.ui.components.AddMeetingDialog
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.AddNoticeDialog
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.AwardBestPerformerDialog
import com.example.ui.components.DistributeDesignationDialog
import com.example.ui.components.DocumentViewerDialog
import com.example.ui.components.EditDonationRecordDialog
import com.example.ui.components.EditMeetingLinkDialog
import com.example.ui.components.EditMemberDialog
import com.example.ui.components.ProfileSwitcherDialog
import com.example.ui.components.SelfRegisterMemberDialog
import com.example.ui.components.TTSAppHeader
import com.example.ui.components.TTSBottomNavigationBar
import com.example.ui.components.UpdateMemberPhotoDialog
import com.example.ui.screens.DonationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IDCardScreen
import com.example.ui.screens.MeetingsScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.NoticesScreen
import com.example.ui.theme.MintBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.TTSViewModel
import com.example.util.TTSNotificationHelper

class MainActivity : ComponentActivity() {

    private var initialTargetTab: String? = null
    private var initialTargetChannel: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            // Create notification channels
            TTSNotificationHelper.createNotificationChannels(this)
            TTSBackgroundSyncService.startService(this)
        } catch (e: Exception) {
            // Safe initialization
        }

        initialTargetTab = intent?.getStringExtra("TARGET_TAB")
        initialTargetChannel = intent?.getStringExtra("TARGET_CHANNEL")

        setContent {
            MyApplicationTheme {
                TTSMainApp(
                    initialTabStr = initialTargetTab,
                    initialChannelStr = initialTargetChannel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val tab = intent.getStringExtra("TARGET_TAB")
        val channel = intent.getStringExtra("TARGET_CHANNEL")
        initialTargetTab = tab
        initialTargetChannel = channel
    }
}

@Composable
fun TTSMainApp(
    viewModel: TTSViewModel = viewModel(),
    initialTabStr: String? = null,
    initialChannelStr: String? = null
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val activeMember by viewModel.currentActiveMember.collectAsState()

    // Request Notification Permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            TTSBackgroundSyncService.startService(context)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        TTSBackgroundSyncService.startService(context)

        // Handle navigation from notification tap
        when (initialTabStr) {
            "donations" -> viewModel.setTab(AppTab.DONATIONS)
            "meetings" -> {
                viewModel.setTab(AppTab.MEETINGS)
                if (!initialChannelStr.isNullOrBlank()) {
                    viewModel.selectChatChannel(initialChannelStr)
                }
            }
            "notices" -> viewModel.setTab(AppTab.NOTICES)
            "members" -> viewModel.setTab(AppTab.MEMBERS)
        }
    }
    val members by viewModel.members.collectAsState()
    val bestPerformers by viewModel.bestPerformers.collectAsState()
    val meetings by viewModel.meetings.collectAsState()
    val notices by viewModel.notices.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val approvedDonations by viewModel.approvedDonations.collectAsState()
    val pendingDonations by viewModel.pendingDonations.collectAsState()
    val totalApprovedDonations by viewModel.totalApprovedDonationsSum.collectAsState()
    val chatMessages by viewModel.currentChannelMessages.collectAsState()
    val selectedChannel by viewModel.selectedChatChannel.collectAsState()
    val previewIDCardMember by viewModel.selectedMemberForIDCard.collectAsState()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val syncStatusText by viewModel.syncStatus.collectAsState()
    val onlineCandidateIds by viewModel.onlineCandidateIds.collectAsState()

    // Dialog States
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var showProfileSwitcher by remember { mutableStateOf(false) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var showSelfRegisterDialog by remember { mutableStateOf(false) }
    var showAddMeetingDialog by remember { mutableStateOf(false) }
    var showAddNoticeDialog by remember { mutableStateOf(false) }
    var showAddDonationDialog by remember { mutableStateOf(false) }
    var showAddDocumentDialog by remember { mutableStateOf(false) }
    var donationForEdit by remember { mutableStateOf<Donation?>(null) }
    var memberForDesignation by remember { mutableStateOf<Member?>(null) }
    var memberForBestPerformer by remember { mutableStateOf<Member?>(null) }
    var memberForPhotoUpdate by remember { mutableStateOf<Member?>(null) }
    var memberForEdit by remember { mutableStateOf<Member?>(null) }
    var meetingForEditLink by remember { mutableStateOf<Meeting?>(null) }
    var viewedDocument by remember { mutableStateOf<OfficialDocument?>(null) }

    val anyDialogVisible = showAdminLoginDialog || showProfileSwitcher || showAddMemberDialog ||
            showSelfRegisterDialog || showAddMeetingDialog || showAddNoticeDialog ||
            showAddDonationDialog || showAddDocumentDialog || donationForEdit != null ||
            memberForDesignation != null || memberForBestPerformer != null ||
            memberForPhotoUpdate != null || memberForEdit != null || meetingForEditLink != null || viewedDocument != null

    BackHandler(enabled = anyDialogVisible || currentTab != AppTab.HOME) {
        when {
            showAdminLoginDialog -> showAdminLoginDialog = false
            showProfileSwitcher -> showProfileSwitcher = false
            showAddMemberDialog -> showAddMemberDialog = false
            showSelfRegisterDialog -> showSelfRegisterDialog = false
            showAddMeetingDialog -> showAddMeetingDialog = false
            showAddNoticeDialog -> showAddNoticeDialog = false
            showAddDonationDialog -> showAddDonationDialog = false
            showAddDocumentDialog -> showAddDocumentDialog = false
            donationForEdit != null -> donationForEdit = null
            memberForDesignation != null -> memberForDesignation = null
            memberForBestPerformer != null -> memberForBestPerformer = null
            memberForPhotoUpdate != null -> memberForPhotoUpdate = null
            memberForEdit != null -> memberForEdit = null
            meetingForEditLink != null -> meetingForEditLink = null
            viewedDocument != null -> viewedDocument = null
            currentTab != AppTab.HOME -> viewModel.setTab(AppTab.HOME)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MintBackground),
        topBar = {
            TTSAppHeader(
                currentTab = currentTab,
                activeMember = activeMember,
                allMembers = members,
                isAdminLoggedIn = isAdminLoggedIn,
                isCloudConnected = isCloudConnected,
                syncStatusText = syncStatusText,
                onOpenAdminLogin = { showAdminLoginDialog = true },
                onOpenProfileSwitcher = { showProfileSwitcher = true },
                onSelectActiveMember = { viewModel.setActiveMember(it) },
                onNavigateToTab = { viewModel.setTab(it) }
            )
        },
        bottomBar = {
            TTSBottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MintBackground)
        ) {
            when (currentTab) {
                AppTab.HOME -> {
                    HomeScreen(
                        members = members,
                        bestPerformers = bestPerformers,
                        meetings = meetings,
                        notices = notices,
                        documents = documents,
                        donations = approvedDonations,
                        totalDonations = totalApprovedDonations ?: 0.0,
                        activeMember = activeMember,
                        isAdminLoggedIn = isAdminLoggedIn,
                        onlineCandidateIds = onlineCandidateIds,
                        onOpenAdminLogin = { showAdminLoginDialog = true },
                        onLogoutAdmin = { viewModel.logoutAdmin() },
                        onNavigateToTab = { viewModel.setTab(it) },
                        onOpenAddMember = {
                            if (isAdminLoggedIn) showAddMemberDialog = true else showAdminLoginDialog = true
                        },
                        onOpenAddMeeting = {
                            if (isAdminLoggedIn) showAddMeetingDialog = true else showAdminLoginDialog = true
                        },
                        onOpenAddDonation = { showAddDonationDialog = true },
                        onOpenAddNotice = {
                            if (isAdminLoggedIn) showAddNoticeDialog = true else showAdminLoginDialog = true
                        },
                        onOpenAddDocument = {
                            if (isAdminLoggedIn) showAddDocumentDialog = true else showAdminLoginDialog = true
                        },
                        onSelectMemberForID = { member ->
                            viewModel.selectMemberForIDCard(member)
                            viewModel.setTab(AppTab.ID_CARD)
                        },
                        onSelectDocument = { viewedDocument = it },
                        onSelectMeeting = { viewModel.setTab(AppTab.MEETINGS) },
                        onClearEntireData = { viewModel.clearEntireApplicationData() }
                    )
                }

                AppTab.MEMBERS -> {
                    MembersScreen(
                        members = members,
                        isAdminLoggedIn = isAdminLoggedIn,
                        onlineCandidateIds = onlineCandidateIds,
                        onOpenAddMember = { showAddMemberDialog = true },
                        onOpenSelfRegister = { showSelfRegisterDialog = true },
                        onOpenAdminLogin = { showAdminLoginDialog = true },
                        onDistributeDesignation = { member -> memberForDesignation = member },
                        onAwardBestPerformer = { member -> memberForBestPerformer = member },
                        onUpdatePhoto = { member -> memberForPhotoUpdate = member },
                        onEditMember = { member -> memberForEdit = member },
                        onSelectForIDCard = { member ->
                            viewModel.selectMemberForIDCard(member)
                            viewModel.setTab(AppTab.ID_CARD)
                        },
                        onDeleteMember = { memberId, name ->
                            viewModel.deleteMember(memberId, name)
                        },
                        onTriggerSync = { viewModel.triggerCloudSync() }
                    )
                }

                AppTab.ID_CARD -> {
                    IDCardScreen(
                        members = members,
                        selectedMember = previewIDCardMember ?: activeMember,
                        onSelectMember = { viewModel.selectMemberForIDCard(it) },
                        onNavigateToMembers = { viewModel.setTab(AppTab.MEMBERS) },
                        onUpdatePhoto = { memberId, photoUri ->
                            viewModel.updateMemberPhoto(memberId, photoUri)
                        }
                    )
                }

                AppTab.MEETINGS -> {
                    MeetingsScreen(
                        meetings = meetings,
                        chatMessages = chatMessages,
                        selectedChannel = selectedChannel,
                        activeMember = activeMember,
                        allMembers = members,
                        isAdminLoggedIn = isAdminLoggedIn,
                        onlineCandidateIds = onlineCandidateIds,
                        onSelectChannel = { viewModel.selectChatChannel(it) },
                        onSendMessage = { msg -> viewModel.sendChatMessage(msg) },
                        onDeleteChatMessage = { id, channel -> viewModel.deleteChatMessage(id, channel) },
                        onOpenAddMeeting = { showAddMeetingDialog = true },
                        onOpenAdminLogin = { showAdminLoginDialog = true },
                        onDeleteMeeting = { id -> viewModel.deleteMeeting(id) },
                        onUpdateMeetingLink = { meeting -> meetingForEditLink = meeting },
                        onOpenProfileSwitcher = { showProfileSwitcher = true },
                        onClearChat = { viewModel.clearAllChatMessages() }
                    )
                }

                AppTab.NOTICES -> {
                    NoticesScreen(
                        notices = notices,
                        documents = documents,
                        isAdminLoggedIn = isAdminLoggedIn,
                        onAddNoticeClick = { showAddNoticeDialog = true },
                        onDeleteNotice = { id -> viewModel.deleteNotice(id) },
                        onAddDocumentClick = {
                            if (isAdminLoggedIn) showAddDocumentDialog = true else showAdminLoginDialog = true
                        },
                        onDeleteDocument = { id -> viewModel.deleteDocument(id) },
                        onViewDocument = { viewedDocument = it },
                        onOpenAdminLogin = { showAdminLoginDialog = true }
                    )
                }

                AppTab.DONATIONS -> {
                    DonationScreen(
                        approvedDonations = approvedDonations,
                        pendingDonations = pendingDonations,
                        totalDonations = totalApprovedDonations ?: 0.0,
                        isAdminLoggedIn = isAdminLoggedIn,
                        onOpenAdminLogin = { showAdminLoginDialog = true },
                        onOpenAddDonationModal = { showAddDonationDialog = true },
                        onApproveDonation = { donation -> viewModel.approveDonation(donation) },
                        onRejectDonation = { donation -> viewModel.rejectDonation(donation) },
                        onVerifyDonation = { id, verified -> viewModel.updateDonationVerification(id, verified) },
                        onEditDonation = { donationForEdit = it },
                        onDeleteDonation = { id -> viewModel.deleteDonation(id) },
                        onClearOldDonations = { viewModel.clearAllOldDonations() }
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // Admin Login Dialog
    if (showAdminLoginDialog) {
        AdminLoginDialog(
            onDismiss = { showAdminLoginDialog = false },
            onLogin = { u, p ->
                val success = viewModel.loginAdmin(u, p)
                if (success) {
                    showAdminLoginDialog = false
                    Toast.makeText(context, "एडमिन लॉगिन सफल!", Toast.LENGTH_SHORT).show()
                }
                success
            }
        )
    }

    // Distribute Designation Dialog (Admin)
    if (memberForDesignation != null) {
        val targetMember = memberForDesignation!!
        DistributeDesignationDialog(
            member = targetMember,
            onDismiss = { memberForDesignation = null },
            onConfirm = { newDesig, newWing ->
                viewModel.distributeDesignation(targetMember.id, newDesig, newWing)
                memberForDesignation = null
            }
        )
    }

    // Award Best Performer Dialog (Admin)
    if (memberForBestPerformer != null) {
        val targetMember = memberForBestPerformer!!
        AwardBestPerformerDialog(
            member = targetMember,
            onDismiss = { memberForBestPerformer = null },
            onConfirm = { isAward, badge ->
                viewModel.toggleBestPerformerStatus(targetMember.id, isAward, badge)
                memberForBestPerformer = null
            }
        )
    }

    // Update Photo Dialog
    if (memberForPhotoUpdate != null) {
        val targetMember = memberForPhotoUpdate!!
        UpdateMemberPhotoDialog(
            member = targetMember,
            onDismiss = { memberForPhotoUpdate = null },
            onConfirm = { newPhotoUri ->
                viewModel.updateMemberPhoto(targetMember.id, newPhotoUri)
                memberForPhotoUpdate = null
            }
        )
    }

    // Edit Member Details Dialog
    if (memberForEdit != null) {
        val targetMember = memberForEdit!!
        EditMemberDialog(
            member = targetMember,
            onDismiss = { memberForEdit = null },
            onConfirm = { updatedMember ->
                viewModel.updateMember(updatedMember)
                memberForEdit = null
            },
            onDelete = {
                viewModel.deleteMember(targetMember.id, targetMember.fullName)
                memberForEdit = null
            }
        )
    }

    // Persona Switcher Dialog
    if (showProfileSwitcher) {
        ProfileSwitcherDialog(
            members = members,
            currentActiveMember = activeMember,
            onSelectMember = { viewModel.setActiveMember(it) },
            onDismiss = { showProfileSwitcher = false }
        )
    }

    // Self Register Member Dialog
    if (showSelfRegisterDialog) {
        SelfRegisterMemberDialog(
            onDismiss = { showSelfRegisterDialog = false },
            onConfirm = { name, phone, email, addr, wing, emergency, photoUri ->
                viewModel.selfRegisterMember(
                    fullName = name,
                    phoneNumber = phone,
                    email = email,
                    address = addr,
                    requestedWing = wing,
                    emergencyContact = emergency,
                    photoUri = photoUri
                )
                showSelfRegisterDialog = false
            }
        )
    }

    // Add Member Dialog (Admin)
    if (showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { showAddMemberDialog = false },
            onConfirm = { name, desig, wing, phone, email, blood, addr, emContact, isBest, bestBadge, photoUri ->
                viewModel.addMember(
                    fullName = name,
                    designation = desig,
                    committeeWing = wing,
                    phoneNumber = phone,
                    email = email,
                    bloodGroup = blood,
                    address = addr,
                    emergencyContact = emContact,
                    isBestPerformer = isBest,
                    bestBadge = bestBadge,
                    photoUri = photoUri
                )
                showAddMemberDialog = false
            }
        )
    }

    // Add Meeting Dialog
    if (showAddMeetingDialog) {
        AddMeetingDialog(
            onDismiss = { showAddMeetingDialog = false },
            onConfirm = { title, type, date, time, venue, link, chair, agenda ->
                viewModel.addMeeting(
                    title = title,
                    type = type,
                    dateDisplay = date,
                    timeDisplay = time,
                    venue = venue,
                    virtualLink = link,
                    chairperson = chair,
                    agenda = agenda
                )
                showAddMeetingDialog = false
            }
        )
    }

    // Add Notice Dialog
    if (showAddNoticeDialog) {
        AddNoticeDialog(
            onDismiss = { showAddNoticeDialog = false },
            onConfirm = { title, category, priority, issuedBy, content, isPinned ->
                viewModel.addNotice(
                    title = title,
                    category = category,
                    priority = priority,
                    issuedBy = issuedBy,
                    content = content,
                    isPinned = isPinned
                )
                showAddNoticeDialog = false
            }
        )
    }

    // Add Donation Record Dialog
    if (showAddDonationDialog) {
        AddDonationRecordDialog(
            isAdmin = isAdminLoggedIn,
            onDismiss = { showAddDonationDialog = false },
            onConfirm = { donor, code, amt, purpose, mode, ref, rem, proof ->
                viewModel.addDonation(
                    donorName = donor,
                    donorMemberCode = code,
                    amount = amt,
                    purpose = purpose,
                    paymentMode = mode,
                    transactionRef = ref,
                    remarks = rem,
                    isApproved = isAdminLoggedIn,
                    paymentProofUri = proof
                )
                showAddDonationDialog = false
            }
        )
    }

    // Add Document Dialog (Admin Only)
    if (showAddDocumentDialog) {
        AddDocumentDialog(
            onDismiss = { showAddDocumentDialog = false },
            onConfirm = { title, category, accessLevel, summary, fullContent, attachmentUri, attachmentName ->
                viewModel.addDocument(
                    title = title,
                    category = category,
                    accessLevel = accessLevel,
                    summary = summary,
                    fullContent = fullContent,
                    attachmentUri = attachmentUri,
                    attachmentName = attachmentName
                )
                showAddDocumentDialog = false
            }
        )
    }

    // View Document Dialog
    if (viewedDocument != null) {
        DocumentViewerDialog(
            doc = viewedDocument!!,
            onDismiss = { viewedDocument = null }
        )
    }

    // Edit / Modify Donation Dialog (Admin Only)
    if (donationForEdit != null) {
        EditDonationRecordDialog(
            donation = donationForEdit!!,
            onDismiss = { donationForEdit = null },
            onConfirm = { updatedDonation ->
                viewModel.updateDonation(updatedDonation)
                donationForEdit = null
            }
        )
    }

    // Edit Google Meet Link Dialog (Admin Only)
    if (meetingForEditLink != null) {
        val targetMeeting = meetingForEditLink!!
        EditMeetingLinkDialog(
            meeting = targetMeeting,
            onDismiss = { meetingForEditLink = null },
            onConfirm = { meetingId, newLink ->
                viewModel.updateMeetingLink(meetingId, newLink)
                meetingForEditLink = null
            }
        )
    }
}
