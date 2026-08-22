package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.OfficialDocument
import com.example.ui.components.AddDonationRecordDialog
import com.example.ui.components.AddMeetingDialog
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.AddNoticeDialog
import com.example.ui.components.AdminLoginDialog
import com.example.ui.components.AwardBestPerformerDialog
import com.example.ui.components.DistributeDesignationDialog
import com.example.ui.components.DocumentViewerDialog
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TTSMainApp()
            }
        }
    }
}

@Composable
fun TTSMainApp(viewModel: TTSViewModel = viewModel()) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val activeMember by viewModel.currentActiveMember.collectAsState()
    val members by viewModel.members.collectAsState()
    val bestPerformers by viewModel.bestPerformers.collectAsState()
    val meetings by viewModel.meetings.collectAsState()
    val notices by viewModel.notices.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val donations by viewModel.donations.collectAsState()
    val totalDonations by viewModel.totalDonationsSum.collectAsState()
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
    var memberForDesignation by remember { mutableStateOf<Member?>(null) }
    var memberForBestPerformer by remember { mutableStateOf<Member?>(null) }
    var memberForPhotoUpdate by remember { mutableStateOf<Member?>(null) }
    var viewedDocument by remember { mutableStateOf<OfficialDocument?>(null) }

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
                        donations = donations,
                        totalDonations = totalDonations,
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
                        onSelectForIDCard = { member ->
                            viewModel.selectMemberForIDCard(member)
                            viewModel.setTab(AppTab.ID_CARD)
                        },
                        onDeleteMember = { memberId, name ->
                            viewModel.deleteMember(memberId, name)
                        }
                    )
                }

                AppTab.ID_CARD -> {
                    IDCardScreen(
                        members = members,
                        selectedMember = previewIDCardMember ?: activeMember,
                        onSelectMember = { viewModel.selectMemberForIDCard(it) },
                        onNavigateToMembers = { viewModel.setTab(AppTab.MEMBERS) }
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
                        onOpenAddMeeting = { showAddMeetingDialog = true },
                        onOpenAdminLogin = { showAdminLoginDialog = true },
                        onDeleteMeeting = { id -> viewModel.deleteMeeting(id) },
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
                            viewModel.addDocument(
                                title = "12 रबी-उल-अव्वल जुलूस व सुरक्षा आधिकारिक नियमावली",
                                category = "प्रशासनिक आदेश",
                                accessLevel = "Confidential",
                                summary = "12 रबी-उल-अव्वल यौम-ए-पैदाइश हज़रत मुहम्मद ﷺ समारोह एवं जुलूस-ए-मोहम्मदी का पूर्ण दिशानिर्देश।",
                                fullContent = "TTS 12 रबी-उल-अव्वल कमेटी आधिकारिक आदेश:\n\n1. जुलूस-ए-मोहम्मदी का आरंभ सुबह 9:00 बजे शाही जामा मस्जिद चौक से होगा।\n2. सभी वालंटियर्स अधिकृत पहचान पत्र (ID Card) अवश्य पहनें।\n3. लंगर-ए-पाक वितरण में स्वच्छता एवं कतार का पूर्ण ध्यान रखें।\n4. चंदा केवल अधिकृत UPI आईडी ak750258@icici पर ही जमा कराएं।"
                            )
                        },
                        onDeleteDocument = { id -> viewModel.deleteDocument(id) },
                        onViewDocument = { viewedDocument = it },
                        onOpenAdminLogin = { showAdminLoginDialog = true }
                    )
                }

                AppTab.DONATIONS -> {
                    DonationScreen(
                        donations = donations,
                        totalDonations = totalDonations,
                        isAdminLoggedIn = isAdminLoggedIn,
                        onOpenAdminLogin = { showAdminLoginDialog = true },
                        onOpenAddDonationModal = { showAddDonationDialog = true },
                        onVerifyDonation = { id, verified -> viewModel.updateDonationVerification(id, verified) },
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
            onDismiss = { showAddDonationDialog = false },
            onConfirm = { donor, code, amt, purpose, mode, ref, rem ->
                viewModel.addDonation(
                    donorName = donor,
                    donorMemberCode = code,
                    amount = amt,
                    purpose = purpose,
                    paymentMode = mode,
                    transactionRef = ref,
                    remarks = rem
                )
                showAddDonationDialog = false
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
}
