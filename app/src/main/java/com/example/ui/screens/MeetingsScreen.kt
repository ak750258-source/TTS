package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.ui.components.MemberAvatar
import com.example.ui.theme.BorderLightGreen
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldText
import com.example.ui.theme.LightSageCard
import com.example.ui.theme.MintBackground
import com.example.ui.theme.PineGreenDark
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SoftMintContainer
import com.example.ui.theme.TextPrimaryGreen
import com.example.ui.theme.TextSecondaryGreen

@Composable
fun MeetingsScreen(
    meetings: List<Meeting>,
    chatMessages: List<ChatMessage>,
    selectedChannel: String,
    activeMember: Member?,
    allMembers: List<Member>,
    isAdminLoggedIn: Boolean,
    onSelectChannel: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenAddMeeting: () -> Unit,
    onOpenAdminLogin: () -> Unit,
    onDeleteMeeting: (Long) -> Unit,
    onOpenProfileSwitcher: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("💬 लाइव सदस्य चैट (Live Chat)", "📅 बैठकें व सभाएं (Meetings)")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MintBackground)
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = PrimaryGreen
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (selectedTabIndex == index) PrimaryGreen else TextSecondaryGreen
                        )
                    }
                )
            }
        }

        if (selectedTabIndex == 0) {
            // LIVE CHAT TAB (Every member can chat, online active sync)
            LiveChatSection(
                chatMessages = chatMessages,
                selectedChannel = selectedChannel,
                activeMember = activeMember ?: allMembers.firstOrNull(),
                isAdminLoggedIn = isAdminLoggedIn,
                onSelectChannel = onSelectChannel,
                onSendMessage = onSendMessage,
                onOpenProfileSwitcher = onOpenProfileSwitcher
            )
        } else {
            // MEETINGS SCHEDULE TAB
            MeetingsScheduleSection(
                meetings = meetings,
                isAdminLoggedIn = isAdminLoggedIn,
                onOpenAddMeeting = onOpenAddMeeting,
                onOpenAdminLogin = onOpenAdminLogin,
                onDeleteMeeting = onDeleteMeeting
            )
        }
    }
}

@Composable
fun LiveChatSection(
    chatMessages: List<ChatMessage>,
    selectedChannel: String,
    activeMember: Member?,
    isAdminLoggedIn: Boolean,
    onSelectChannel: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenProfileSwitcher: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val channels = listOf(
        Pair("general", "आम चर्चा (General)"),
        Pair("rabi_ul_awwal", "12 रबी-उल-अव्वल तैयारी"),
        Pair("donations", "चंदा व लंगर सहयोग")
    )

    val quickGreetings = listOf(
        "अस्सलाम वालेकुम",
        "सुभानअल्लाह",
        "माशाअल्लाह",
        "12 रबी-उल-अव्वल मुबारक!",
        "चंदा जमा हो गया",
        "लंगर व्यवस्था तैयार है"
    )

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Channel Bar & Active Persona Bar
        Surface(
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Online Sync Indicator & Profile Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "● ऑनलाइन लाइव नेटवर्क सक्रिय",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }

                    // Speaking As Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftMintContainer)
                            .clickable { onOpenProfileSwitcher() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SwitchAccount, contentDescription = null, tint = PineGreenDark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${activeMember?.fullName?.split(" ")?.firstOrNull() ?: "सदस्य"} (${activeMember?.designation ?: "खादिम"})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PineGreenDark
                            )
                        }
                    }
                }

                // Channels Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(channels) { (id, label) ->
                        FilterChip(
                            selected = selectedChannel == id,
                            onClick = { onSelectChannel(id) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftMintContainer,
                                selectedLabelColor = PineGreenDark
                            )
                        )
                    }
                }
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages, key = { it.id }) { msg ->
                val isMe = msg.senderName.startsWith(activeMember?.fullName?.take(6) ?: "XYZ")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    if (!isMe) {
                        MemberAvatar(
                            name = msg.senderName,
                            size = 32.dp,
                            textSize = 12,
                            colorIndex = msg.senderAvatarIndex
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Card(
                        modifier = Modifier.widthIn(max = 280.dp),
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (isMe) 14.dp else 2.dp,
                            bottomEnd = if (isMe) 2.dp else 14.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) PrimaryGreen else Color.White
                        ),
                        border = if (!isMe) CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
                        ) else null
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (!isMe) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg.senderName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimaryGreen
                                    )
                                    Text(
                                        text = msg.senderRole,
                                        fontSize = 9.sp,
                                        color = EmeraldGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            Text(
                                text = msg.messageText,
                                fontSize = 13.sp,
                                color = if (isMe) Color.White else TextPrimaryGreen,
                                lineHeight = 17.sp
                            )

                            Text(
                                text = msg.timeDisplay,
                                fontSize = 9.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.75f) else TextSecondaryGreen,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        // Quick Islamic Greeting Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickGreetings) { greet ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightSageCard)
                        .clickable { onSendMessage(greet) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(greet, fontSize = 10.sp, color = DeepForestGreen, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Input Field
        Surface(
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("12 रबी-उल-अव्वल चर्चा या संदेश लिखें...", fontSize = 12.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen)
                        .testTag("chat_send_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun MeetingsScheduleSection(
    meetings: List<Meeting>,
    isAdminLoggedIn: Boolean,
    onOpenAddMeeting: () -> Unit,
    onOpenAdminLogin: () -> Unit,
    onDeleteMeeting: (Long) -> Unit
) {
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "12 रबी-उल-अव्वल आधिकारिक बैठकें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "जुलूस, लंगर व चंदा प्रबंधन सभा कार्यक्रम",
                            fontSize = 11.sp,
                            color = TextSecondaryGreen
                        )
                    }

                    if (isAdminLoggedIn) {
                        Button(
                            onClick = onOpenAddMeeting,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("बैठक तय करें", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            items(meetings, key = { it.id }) { meeting ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SoftMintContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(meeting.type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PineGreenDark)
                            }

                            if (isAdminLoggedIn) {
                                IconButton(
                                    onClick = { onDeleteMeeting(meeting.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(
                            text = meeting.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimaryGreen,
                            lineHeight = 20.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("📅 ${meeting.dateDisplay}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            Text("⏰ ${meeting.timeDisplay}", fontSize = 11.sp, color = TextSecondaryGreen)
                        }

                        Text("📍 स्थान: ${meeting.venue}", fontSize = 11.sp, color = TextPrimaryGreen)
                        Text("👤 अध्यक्षता: ${meeting.chairperson}", fontSize = 11.sp, color = TextSecondaryGreen)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LightSageCard)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("कार्यसूची (Agenda):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                                Text(meeting.agenda, fontSize = 11.sp, color = TextPrimaryGreen, lineHeight = 16.sp)
                            }
                        }

                        if (meeting.virtualLink != null) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(meeting.virtualLink))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ऑनलाइन वीडियो मीटिंग में जुड़ें (Google Meet)", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }
}
