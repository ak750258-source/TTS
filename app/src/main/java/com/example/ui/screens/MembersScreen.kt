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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Member
import com.example.ui.components.MemberAvatar
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.BorderLightGreen
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldText
import com.example.ui.theme.LightSageCard
import com.example.ui.theme.MintBackground
import com.example.ui.theme.PineGreenDark
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SoftMintContainer
import com.example.ui.theme.TextPrimaryGreen
import com.example.ui.theme.TextSecondaryGreen

@Composable
fun MembersScreen(
    members: List<Member>,
    isAdminLoggedIn: Boolean,
    onlineCandidateIds: Set<Long> = emptySet(),
    onOpenAddMember: () -> Unit,
    onOpenSelfRegister: () -> Unit,
    onOpenAdminLogin: () -> Unit,
    onDistributeDesignation: (Member) -> Unit,
    onAwardBestPerformer: (Member) -> Unit,
    onUpdatePhoto: (Member) -> Unit = {},
    onSelectForIDCard: (Member) -> Unit,
    onDeleteMember: (memberId: Long, memberName: String) -> Unit,
    onTriggerSync: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedWingFilter by remember { mutableStateOf("सभी (All)") }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    val wings = listOf("सभी (All)", "संरक्षक मंडल", "प्रबंधक बोर्ड", "जुलूस कमेटी", "लंगर व्यवस्था", "वित्तीय लेखा", "युवा विंग")

    val filteredMembers = members.filter { member ->
        val matchesSearch = member.fullName.contains(searchQuery, ignoreCase = true) ||
                member.designation.contains(searchQuery, ignoreCase = true) ||
                member.memberCode.contains(searchQuery, ignoreCase = true) ||
                member.phoneNumber.contains(searchQuery, ignoreCase = true)

        val matchesWing = when (selectedWingFilter) {
            "सभी (All)" -> true
            "संरक्षक मंडल" -> member.committeeWing.contains("संरक्षक")
            "प्रबंधक बोर्ड" -> member.committeeWing.contains("प्रबंधक")
            "जुलूस कमेटी" -> member.committeeWing.contains("जुलूस")
            "लंगर व्यवस्था" -> member.committeeWing.contains("लंगर")
            "वित्तीय लेखा" -> member.committeeWing.contains("वित्तीय") || member.committeeWing.contains("चंदा")
            "युवा विंग" -> member.committeeWing.contains("युवा")
            else -> true
        }
        matchesSearch && matchesWing
    }

    Box(modifier = modifier.fillMaxSize().background(MintBackground)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Header Banner in Hindi
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "12 रबी-उल-अव्वल कमेटी सदस्य रोस्टर",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimaryGreen
                                )
                                Text(
                                    text = "कुल ${members.size} पंजीकृत पदाधिकारी एवं खादिम",
                                    fontSize = 11.sp,
                                    color = TextSecondaryGreen
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = onTriggerSync,
                                    modifier = Modifier.size(32.dp).testTag("members_sync_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "सिंक करें",
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (isAdminLoggedIn) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SoftMintContainer)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "👑 एडमिन मोड",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PineGreenDark
                                        )
                                    }
                                }
                            }
                        }

                        // Self Registration Button Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenSelfRegister,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("self_register_banner_btn")
                            ) {
                                Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("सदस्य स्व-पंजीकरण", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (isAdminLoggedIn) {
                                OutlinedButton(
                                    onClick = onOpenAddMember,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).testTag("admin_add_member_btn")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("सदस्य जोड़ें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("नाम, पद (संस्थापक, कोषाध्यक्ष), आईडी या फोन खोजें...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldGreen) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryGreen)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("member_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Wing Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(wings) { wing ->
                                FilterChip(
                                    selected = selectedWingFilter == wing,
                                    onClick = { selectedWingFilter = wing },
                                    label = { Text(wing, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SoftMintContainer,
                                        selectedLabelColor = PineGreenDark
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Member Cards
            if (filteredMembers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(48.dp), tint = TextSecondaryGreen)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("कोई सदस्य नहीं मिला", fontWeight = FontWeight.Bold, color = TextPrimaryGreen)
                            Text("खोज शब्द बदलें अथवा नया सदस्य जोड़ें", fontSize = 12.sp, color = TextSecondaryGreen)
                        }
                    }
                }
            } else {
                items(filteredMembers, key = { it.id }) { member ->
                    MemberRosterCard(
                        member = member,
                        isAdminLoggedIn = isAdminLoggedIn,
                        isOnline = onlineCandidateIds.contains(member.id),
                        onOpenAdminLogin = onOpenAdminLogin,
                        onDistributeDesignation = { onDistributeDesignation(member) },
                        onAwardBestPerformer = { onAwardBestPerformer(member) },
                        onUpdatePhoto = { onUpdatePhoto(member) },
                        onSelectForIDCard = { onSelectForIDCard(member) },
                        onDeleteClick = { memberToDelete = member }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button to Add Member (Accessible by Admin or open login)
        FloatingActionButton(
            onClick = {
                if (isAdminLoggedIn) {
                    onOpenAddMember()
                } else {
                    onOpenAdminLogin()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_member_fab"),
            containerColor = PrimaryGreen,
            contentColor = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isAdminLoggedIn) "सदस्य जोड़ें" else "एडमिन लॉगिन", fontWeight = FontWeight.Bold)
            }
        }

        // Delete Confirmation Dialog
        if (memberToDelete != null) {
            AlertDialog(
                onDismissRequest = { memberToDelete = null },
                title = { Text("सदस्य हटाएं (Delete Member)", fontWeight = FontWeight.Bold) },
                text = {
                    Text("क्या आप निश्चित रूप से '${memberToDelete!!.fullName}' (${memberToDelete!!.memberCode}) को 12 रबी-उल-अव्वल कमेटी सूची से हटाना चाहते हैं?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = memberToDelete!!.id
                            val name = memberToDelete!!.fullName
                            memberToDelete = null
                            onDeleteMember(id, name)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("हाँ, हटाएं", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { memberToDelete = null }) {
                        Text("रद्द करें")
                    }
                }
            )
        }
    }
}

@Composable
fun MemberRosterCard(
    member: Member,
    isAdminLoggedIn: Boolean,
    isOnline: Boolean = false,
    onOpenAdminLogin: () -> Unit,
    onDistributeDesignation: () -> Unit,
    onAwardBestPerformer: () -> Unit,
    onUpdatePhoto: () -> Unit = {},
    onSelectForIDCard: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (member.isBestPerformer) GoldAccent else BorderLightGreen
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo / Avatar with online indicator and click to update
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clickable { onUpdatePhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        MemberAvatar(
                            name = member.fullName,
                            photoUri = member.photoUri,
                            photoResName = member.photoResName,
                            size = 52.dp,
                            textSize = 18,
                            colorIndex = member.avatarColorIndex,
                            isOnline = isOnline,
                            showOnlineIndicator = true
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = member.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimaryGreen
                            )
                            if (member.isBestPerformer) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.Star, contentDescription = "Best Performer", tint = GoldText, modifier = Modifier.size(16.dp))
                            }
                            if (isOnline) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFDCFCE7))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "● लाइव",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }

                        // Designation Box (e.g. संस्थापक, कोषाध्यक्ष, सदर, सेक्रेटरी)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SoftMintContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = member.designation,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PineGreenDark
                            )
                        }

                        Text(
                            text = "${member.committeeWing} • ${member.memberCode}",
                            fontSize = 10.sp,
                            color = TextSecondaryGreen
                        )
                    }
                }

                // Call Action Icon
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${member.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SoftMintContainer)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                }
            }

            if (member.isBestPerformer && member.bestPerformerBadge != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "⭐ ${member.bestPerformerBadge}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }
            }

            HorizontalDivider(color = BorderLightGreen, thickness = 1.dp)

            // Contact & Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "फोन: ${member.phoneNumber}",
                    fontSize = 11.sp,
                    color = TextPrimaryGreen,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = member.address,
                    fontSize = 10.sp,
                    color = TextSecondaryGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Buttons Toolbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View ID Card Button (with Muhammad Flag & Photo)
                OutlinedButton(
                    onClick = onSelectForIDCard,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                    modifier = Modifier.weight(1f).testTag("view_id_card_${member.id}")
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ID कार्ड देखें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Admin: Distribute Designation Button
                OutlinedButton(
                    onClick = {
                        if (isAdminLoggedIn) {
                            onDistributeDesignation()
                        } else {
                            onOpenAdminLogin()
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("distribute_desig_${member.id}")
                ) {
                    Icon(Icons.Default.Work, contentDescription = null, tint = GoldText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("पद सौंपें", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepForestGreen)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Admin: Award Best Performer
                IconButton(
                    onClick = {
                        if (isAdminLoggedIn) {
                            onAwardBestPerformer()
                        } else {
                            onOpenAdminLogin()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (member.isBestPerformer) Color(0xFFFEF3C7) else LightSageCard)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Award",
                        tint = if (member.isBestPerformer) GoldText else TextSecondaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isAdminLoggedIn) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEE2E2))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
