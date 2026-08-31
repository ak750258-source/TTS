package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Donation
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import com.example.ui.components.MemberAvatar
import com.example.ui.components.copyToClipboard
import com.example.ui.components.openUPIIntent
import com.example.ui.theme.ActiveSage
import com.example.ui.theme.BorderContainerGreen
import com.example.ui.theme.BorderLightGreen
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldText
import com.example.ui.theme.LightSageCard
import com.example.ui.theme.MintBackground
import com.example.ui.theme.NoticeMintContainer
import com.example.ui.theme.PineGreenDark
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SoftMintContainer
import com.example.ui.theme.TextPrimaryGreen
import com.example.ui.theme.TextSecondaryGreen
import com.example.ui.theme.UrgentRed
import com.example.ui.theme.UrgentRedBg
import com.example.ui.viewmodel.AppTab

@Composable
fun HomeScreen(
    members: List<Member>,
    bestPerformers: List<Member>,
    meetings: List<Meeting>,
    notices: List<Notice>,
    documents: List<OfficialDocument>,
    donations: List<Donation>,
    totalDonations: Double,
    expenses: List<com.example.data.model.Expense> = emptyList(),
    totalExpenses: Double = 0.0,
    remainingBalance: Double = 0.0,
    donationGoal: Double = 250000.0,
    activeMember: Member?,
    isAdminLoggedIn: Boolean,
    onlineCandidateIds: Set<Long> = emptySet(),
    onOpenAdminLogin: () -> Unit,
    onLogoutAdmin: () -> Unit,
    onOpenEditGoal: () -> Unit = {},
    onNavigateToTab: (AppTab) -> Unit,
    onOpenAddMember: () -> Unit,
    onOpenAddMeeting: () -> Unit,
    onOpenAddDonation: () -> Unit,
    onOpenAddExpense: () -> Unit = {},
    onOpenAddNotice: () -> Unit,
    onOpenAddDocument: () -> Unit = {},
    onSelectMemberForID: (Member) -> Unit,
    onSelectDocument: (OfficialDocument) -> Unit,
    onSelectMeeting: (Meeting) -> Unit,
    onClearEntireData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }
    val upcomingMeeting = meetings.firstOrNull { it.status == "Upcoming" }
    val urgentNotice = notices.firstOrNull { it.priority == "HIGH" || it.isPinned } ?: notices.firstOrNull()

    val safeGoal = if (donationGoal > 0) donationGoal else 250000.0
    val progress = (totalDonations / safeGoal).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MintBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 12 Rabi Ul Awwal Hero Banner with Image
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = PineGreenDark)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_milad_banner),
                        contentDescription = "12 Rabi Ul Awwal Celebration Banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Gradient Overlay for contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.3f),
                                        PineGreenDark.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldAccent)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "12 रबी-उल-अव्वल 1447 हिजरी",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PineGreenDark
                                )
                            }

                            // Online Live Status Chip
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4ADE80))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Firebase लाइव क्लाउड",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Column {
                            Text(
                                text = "जश्न-ए-विलादत-उन-नबी ﷺ मुबारक!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "हज़रत मुहम्मद सल्लल्लाहु अलैहि वसल्लम यौम-ए-पैदाइश समारोह • TTS कमेटी",
                                fontSize = 11.sp,
                                color = Color(0xFFA5D6A7),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Admin Portal / Control Dashboard Section
        item {
            if (isAdminLoggedIn) {
                // Expanded Full Admin Control Dashboard
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = SoftMintContainer),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EmeraldGreen)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Header: Admin Status Badge & Logout Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "🔐 मुख्य एडमिन कंट्रोल पैनल",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryGreen
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(EmeraldGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("सक्रिय (Active)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DeepForestGreen)
                                        }
                                    }
                                    Text(
                                        text = "दस्तावेज़, सदस्य, नोटिस व डेटा प्रबंधन सक्षम है",
                                        fontSize = 10.sp,
                                        color = TextSecondaryGreen
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = onLogoutAdmin,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("admin_logout_btn")
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFDC2626))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("लॉगआउट", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = BorderLightGreen.copy(alpha = 0.6f))

                        Text(
                            text = "⚡ त्वरित एडमिन प्रबंधन (Admin Actions):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryGreen
                        )

                        // Action Row 1: Document Upload & Notice Post
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenAddDocument,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).testTag("admin_add_doc_btn")
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("दस्तावेज़ अपलोड", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onOpenAddNotice,
                                colors = ButtonDefaults.buttonColors(containerColor = PineGreenDark),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).testTag("admin_add_notice_btn")
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("नोटिस जारी करें", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Action Row 2: Add Member & Schedule Meeting
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenAddMember,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = PrimaryGreen),
                                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).testTag("admin_add_member_btn")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp), tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("सदस्य जोड़ें", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }

                            OutlinedButton(
                                onClick = onOpenAddMeeting,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = PrimaryGreen),
                                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).testTag("admin_add_meeting_btn")
                            ) {
                                Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(15.dp), tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("मीटिंग तय करें", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }

                        // Danger Action: Clear All Data (Retained & Prominently Styled)
                        OutlinedButton(
                            onClick = { showClearAllConfirmDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFEF2F2),
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("admin_clear_all_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🗑️ सभी डेटा साफ़ करें (Wipe All App Data)", fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Admin Login Prompt Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(GoldAccent)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = PineGreenDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🔐 एडमिन सेक्शन (Admin Portal)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryGreen
                                )
                                Text(
                                    text = "पद वितरण, नोटिस, दस्तावेज़ व डेटा प्रबंधन हेतु लॉगिन करें",
                                    fontSize = 10.sp,
                                    color = TextSecondaryGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenAdminLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier.testTag("admin_login_btn")
                        ) {
                            Text("लॉगिन", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ⭐ BEST PERFORMER PHOTO SHOWCASE (User Requirement)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⭐ बेस्ट परफ़ॉर्मर / स्टार खिदमतगार (Top Performers)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimaryGreen
                        )
                    }

                    Text(
                        text = "12 रबी-उल-अव्वल",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreen
                    )
                }

                val showcaseMembers = if (bestPerformers.isNotEmpty()) bestPerformers else members.take(4)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(showcaseMembers) { performer ->
                        Card(
                            modifier = Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectMemberForID(performer) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (performer.isBestPerformer) GoldAccent else BorderLightGreen
                                )
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Photo Frame with Gold Ribbon for Best Performers
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, GoldAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MemberAvatar(
                                        name = performer.fullName,
                                        photoUri = performer.photoUri,
                                        photoResName = performer.photoResName,
                                        size = 64.dp,
                                        textSize = 22,
                                        colorIndex = performer.avatarColorIndex,
                                        isOnline = onlineCandidateIds.contains(performer.id),
                                        showOnlineIndicator = true
                                    )
                                }

                                Text(
                                    text = performer.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextPrimaryGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SoftMintContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = performer.designation,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PineGreenDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (performer.isBestPerformer) {
                                    Text(
                                        text = performer.bestPerformerBadge ?: "⭐ विशेष सम्मान",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Urgent Hindi Noticeboard Card
        if (urgentNotice != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToTab(AppTab.NOTICES) },
                    colors = CardDefaults.cardColors(containerColor = NoticeMintContainer),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BorderContainerGreen)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "आधिकारिक सूचना पट्ट (NOTICE)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestGreen,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(UrgentRedBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "12 रबी-उल-अव्वल",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = UrgentRed
                                )
                            }
                        }

                        Text(
                            text = urgentNotice.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryGreen,
                            lineHeight = 19.sp
                        )

                        Text(
                            text = urgentNotice.content,
                            fontSize = 12.sp,
                            color = TextSecondaryGreen,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "जारीकर्ता: ${urgentNotice.issuedBy}",
                                fontSize = 10.sp,
                                color = TextSecondaryGreen
                            )
                            Text(
                                text = "पूरी सूचना पढ़ें →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Quick Action Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "त्वरित सुविधाएँ (Quick Access)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimaryGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickFeatureCard(
                        title = "सदस्य सूची",
                        subtitle = "${members.size} पंजीकृत खादिम",
                        icon = Icons.Default.Groups,
                        iconTint = PineGreenDark,
                        badgeText = "रोस्टर",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppTab.MEMBERS) }
                    )

                    QuickFeatureCard(
                        title = "पहचान पत्र",
                        subtitle = "मुहम्मद ﷺ परचम ID कार्ड",
                        icon = Icons.Default.Badge,
                        iconTint = GoldAccent,
                        badgeText = "डिजिटल",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppTab.ID_CARD) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickFeatureCard(
                        title = "बैठकें व चैट",
                        subtitle = "लाइव संवाद व कार्यक्रम",
                        icon = Icons.Default.Chat,
                        iconTint = EmeraldGreen,
                        badgeText = "लाइव",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppTab.MEETINGS) }
                    )

                    QuickFeatureCard(
                        title = "चंदा व UPI",
                        subtitle = "ak750258@icici",
                        icon = Icons.Default.VolunteerActivism,
                        iconTint = PrimaryGreen,
                        badgeText = "पारदर्शी",
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateToTab(AppTab.DONATIONS) }
                    )
                }
            }
        }

        // 📄 OFFICIAL DOCUMENTS PREVIEW (User Requirement: Dastavej Section)
        if (documents.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "📄 आधिकारिक दस्तावेज़ (${documents.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryGreen
                            )
                        }

                        if (isAdminLoggedIn) {
                            TextButton(onClick = onOpenAddDocument) {
                                Text("+ नया दस्तावेज़", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = { onNavigateToTab(AppTab.NOTICES) }) {
                                Text("सभी देखें →", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    documents.take(2).forEach { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectDocument(doc) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SoftMintContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextPrimaryGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${doc.category} • ${doc.publishedDate}",
                                        fontSize = 10.sp,
                                        color = TextSecondaryGreen
                                    )
                                }
                                Text("देखें →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            }
                        }
                    }
                }
            }
        }

        // Transparent Donation Progress & UPI Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "12 रबी-उल-अव्वल चंदा रिपोर्ट",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryGreen
                            )
                        }

                        Text(
                            text = "UPI: ak750258@icici",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("कुल चंदा संग्रह", fontSize = 11.sp, color = TextSecondaryGreen)
                            Text(
                                text = "₹${String.format("%,.0f", totalDonations)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimaryGreen
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("कुल खर्च", fontSize = 11.sp, color = Color(0xFFDC2626))
                            Text(
                                text = "₹${String.format("%,.0f", totalExpenses)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("शेष बचत (Balance)", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                            Text(
                                text = "₹${String.format("%,.0f", remainingBalance)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldGreen
                            )
                        }
                    }

                    // Goal & Progress Info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎯 लक्ष्य: ₹${String.format(java.util.Locale.getDefault(), "%,.0f", safeGoal)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• ${(progress * 100).toInt()}% पूर्ण",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryGreen
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isAdminLoggedIn) PrimaryGreen else Color.White,
                            border = if (isAdminLoggedIn) null else BorderStroke(1.dp, BorderLightGreen),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (isAdminLoggedIn) onOpenEditGoal() else onOpenAdminLogin()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isAdminLoggedIn) Icons.Default.Edit else Icons.Default.Lock,
                                    contentDescription = "लक्ष्य बदलें",
                                    tint = if (isAdminLoggedIn) Color.White else PrimaryGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isAdminLoggedIn) "लक्ष्य बदलें" else "लक्ष्य (Admin)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAdminLoggedIn) Color.White else PrimaryGreen
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PrimaryGreen,
                        trackColor = BorderLightGreen
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                openUPIIntent(
                                    context = context,
                                    upiId = "ak750258@icici",
                                    payeeName = "TTS 12 Rabi Ul Awwal Committee",
                                    amount = 1100.0,
                                    note = "12 Rabi Ul Awwal Donation"
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("UPI दान", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onOpenAddDonation,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ चंदा", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onOpenAddExpense,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ खर्च", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = {
                Text(
                    text = "⚠️ संपूर्ण पुराना डेटा साफ़ करें? (Wipe All App Data)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = "क्या आप निश्चित रूप से TTS 12 रबी-उल-अव्वल एप्लिकेशन का सभी पुराना डेटा (लोकल कैश व क्लाउड सर्वर सहित) पूर्णतः साफ़ करना चाहते हैं?\n\nइसके पश्चात सदस्य, संदेश, बैठक, नोटिस व चंदा का पुराना डेटा साफ़ हो जाएगा और एप्लिकेशन बिल्कुल नई स्थिति में प्रारंभ होगी।"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirmDialog = false
                        onClearEntireData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("हाँ, पूरा डेटा साफ़ करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun QuickFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftMintContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LightSageCard)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = badgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DeepForestGreen)
                }
            }

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextPrimaryGreen
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = TextSecondaryGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
