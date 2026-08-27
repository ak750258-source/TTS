package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
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
import com.example.ui.theme.UrgentRed
import com.example.ui.theme.UrgentRedBg

@Composable
fun NoticesScreen(
    notices: List<Notice>,
    documents: List<OfficialDocument>,
    isAdminLoggedIn: Boolean,
    onAddNoticeClick: () -> Unit,
    onDeleteNotice: (Long) -> Unit,
    onAddDocumentClick: () -> Unit,
    onDeleteDocument: (Long) -> Unit,
    onViewDocument: (OfficialDocument) -> Unit,
    onOpenAdminLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MintBackground)
    ) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = Color.White,
            contentColor = PrimaryGreen
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("सूचना पट्ट (Notices)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("दस्तावेज़ वॉल्ट (Docs)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        }

        if (selectedSubTab == 0) {
            // NOTICES TAB
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                text = "12 रबी-उल-अव्वल आधिकारिक सूचना पट्ट",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimaryGreen
                            )
                            Text(
                                text = "कमेटी द्वारा जारी निर्देश एवं ऐलान",
                                fontSize = 11.sp,
                                color = TextSecondaryGreen
                            )
                        }

                        Button(
                            onClick = {
                                if (isAdminLoggedIn) onAddNoticeClick() else onOpenAdminLogin()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier.testTag("post_notice_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isAdminLoggedIn) "नोटिस जारी करें" else "एडमिन पोस्ट", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }

                items(notices, key = { it.id }) { notice ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (notice.priority == "HIGH") Color(0xFFFCA5A5) else BorderLightGreen
                            )
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (notice.isPinned) {
                                        Icon(Icons.Default.PushPin, contentDescription = "Pinned", tint = GoldText, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (notice.priority == "HIGH") UrgentRedBg else SoftMintContainer)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = notice.category,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (notice.priority == "HIGH") UrgentRed else PineGreenDark
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(notice.date, fontSize = 11.sp, color = TextSecondaryGreen)
                                    if (isAdminLoggedIn) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { onDeleteNotice(notice.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            Text(
                                text = notice.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimaryGreen,
                                lineHeight = 20.sp
                            )

                            Text(
                                text = notice.content,
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                lineHeight = 18.sp
                            )

                            HorizontalDivider(color = BorderLightGreen, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "जारीकर्ता: ${notice.issuedBy}",
                                    fontSize = 10.sp,
                                    color = TextSecondaryGreen,
                                    fontWeight = FontWeight.Medium
                                )

                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, "TTS Notice", "${notice.title}\n\n${notice.content}\n\n- ${notice.issuedBy}")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        } else {
            // DOCUMENTS VAULT TAB
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                    text = "कमेटी नियमावली एवं प्रशासनिक दस्तावेज़",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextPrimaryGreen
                                )
                                Text(
                                    text = "आधिकारिक डिजिटल आदेश व नियमावली सूची",
                                    fontSize = 11.sp,
                                    color = TextSecondaryGreen
                                )
                            }

                            if (isAdminLoggedIn) {
                                Button(
                                    onClick = onAddDocumentClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("दस्तावेज़ जोड़ें", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(documents, key = { it.id }) { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onViewDocument(doc) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SoftMintContainer)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(doc.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PineGreenDark)
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(doc.refCode, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = TextSecondaryGreen)
                                        if (isAdminLoggedIn) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = { onDeleteDocument(doc.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = doc.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimaryGreen,
                                    lineHeight = 19.sp
                                )

                                Text(
                                    text = doc.summary,
                                    fontSize = 11.sp,
                                    color = TextSecondaryGreen,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("जारी: ${doc.publishedDate}", fontSize = 10.sp, color = TextSecondaryGreen)
                                    Text("दस्तावेज़ खोलें →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}
