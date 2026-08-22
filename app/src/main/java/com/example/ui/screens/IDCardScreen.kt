package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Member
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MemberIDCardView
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.BorderLightGreen
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LightSageCard
import com.example.ui.theme.MintBackground
import com.example.ui.theme.PineGreenDark
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SoftMintContainer
import com.example.ui.theme.TextPrimaryGreen
import com.example.ui.theme.TextSecondaryGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDCardScreen(
    members: List<Member>,
    selectedMember: Member?,
    onSelectMember: (Member) -> Unit,
    onNavigateToMembers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentMember = selectedMember ?: members.firstOrNull()
    var expandedDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MintBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "12 रबी-उल-अव्वल डिजिटल पहचान पत्र",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = TextPrimaryGreen
                    )
                    Text(
                        text = "पवित्र परचम वाटरमार्क एवं फोटो युक्त अधिकृत ID कार्ड",
                        fontSize = 11.sp,
                        color = TextSecondaryGreen
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SoftMintContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("1447H", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PineGreenDark)
                    }
                }
            }
        }

        // Member Selector Dropdown & Quick Carousel
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "सदस्य चुनें (Select Member for ID):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimaryGreen
                    )

                    if (members.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = "${currentMember?.fullName ?: ""} • ${currentMember?.designation ?: ""} (${currentMember?.memberCode ?: ""})",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("id_card_member_dropdown"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                members.forEach { member ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (member.photoResName == "img_best_performer") {
                                                    Image(
                                                        painter = painterResource(id = R.drawable.img_best_performer),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp).clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    MemberAvatar(name = member.fullName, size = 24.dp, textSize = 9, colorIndex = member.avatarColorIndex)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(member.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("${member.designation} • ${member.memberCode}", fontSize = 11.sp, color = TextSecondaryGreen)
                                                }
                                            }
                                        },
                                        onClick = {
                                            onSelectMember(member)
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick Avatar Strip
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(members) { m ->
                                val isSelected = m.id == currentMember?.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) SoftMintContainer else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) PrimaryGreen else BorderLightGreen,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectMember(m) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (m.photoResName == "img_best_performer") {
                                            Image(
                                                painter = painterResource(id = R.drawable.img_best_performer),
                                                contentDescription = null,
                                                modifier = Modifier.size(22.dp).clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            MemberAvatar(name = m.fullName, size = 22.dp, textSize = 8, colorIndex = m.avatarColorIndex)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = m.fullName.split(" ").firstOrNull() ?: m.fullName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) TextPrimaryGreen else TextSecondaryGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live ID Card Component (With Muhammad flag background & photo, NO blood group)
        if (currentMember != null) {
            item {
                MemberIDCardView(member = currentMember)
            }

            // Quick Info & Share Actions
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "कार्ड विवरण एवं प्रमाणीकरण",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimaryGreen
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("सदस्य कोड:", fontSize = 12.sp, color = TextSecondaryGreen)
                            Text(currentMember.memberCode, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextPrimaryGreen)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("अधिकृत पद:", fontSize = 12.sp, color = TextSecondaryGreen)
                            Text(currentMember.designation, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PineGreenDark)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("विभाग / विंग:", fontSize = 12.sp, color = TextSecondaryGreen)
                            Text(currentMember.committeeWing, fontSize = 12.sp, color = TextPrimaryGreen)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("चंदा UPI आईडी:", fontSize = 12.sp, color = TextSecondaryGreen)
                            Text("ak750258@icici", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = EmeraldGreen)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    copyToClipboard(
                                        context,
                                        "12 Rabi Ul Awwal ID Card",
                                        "TTS 12 रबी-उल-अव्वल कमेटी पहचान पत्र:\nनाम: ${currentMember.fullName}\nपद: ${currentMember.designation}\nआईडी: ${currentMember.memberCode}\nफोन: ${currentMember.phoneNumber}\nचंदा UPI: ak750258@icici"
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("विवरण कॉपी करें", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "${currentMember.fullName} का ID कार्ड प्रिंट हेतु तैयार है", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("प्रिंट / PDF", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
