package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.foundation.Image
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Member
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
import com.example.ui.theme.SageSurfaceVariant
import com.example.ui.theme.SoftMintContainer
import com.example.ui.theme.TextMutedGreen
import com.example.ui.theme.TextPrimaryGreen
import com.example.ui.theme.TextSecondaryGreen
import com.example.ui.theme.UrgentRed
import com.example.ui.theme.UrgentRedBg
import com.example.ui.viewmodel.AppTab

val AvatarColors = listOf(
    Color(0xFF1B5E20), // Forest Green
    Color(0xFF0D47A1), // Navy Blue
    Color(0xFFB71C1C), // Deep Red
    Color(0xFF4A148C), // Royal Purple
    Color(0xFFE65100), // Amber Orange
    Color(0xFF004D40)  // Deep Teal
)

@Composable
fun MemberAvatar(
    name: String,
    modifier: Modifier = Modifier,
    photoUri: String? = null,
    photoResName: String? = null,
    colorIndex: Int = 0,
    size: Dp = 44.dp,
    textSize: Int = 16,
    isOnline: Boolean = false,
    showOnlineIndicator: Boolean = true
) {
    val bgColor = AvatarColors.getOrElse(colorIndex % AvatarColors.size) { AvatarColors[0] }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bgColor)
                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUri.isNullOrBlank()) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = name,
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop
                )
            } else if (photoResName == "img_best_performer") {
                Image(
                    painter = painterResource(id = R.drawable.img_best_performer),
                    contentDescription = name,
                    modifier = Modifier.size(size),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Profile icon instead of Hindi letter initials
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = name,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.58f)
                )
            }
        }

        // Live Online Green Presence Indicator on Avatar
        if (showOnlineIndicator && isOnline) {
            val dotSize = (size * 0.28f).coerceIn(8.dp, 14.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
                    .border(1.5.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun TTSAppHeader(
    currentTab: AppTab,
    activeMember: Member?,
    allMembers: List<Member>,
    isAdminLoggedIn: Boolean,
    isCloudConnected: Boolean = true,
    syncStatusText: String = "Firebase लाइव सिंक",
    onOpenAdminLogin: () -> Unit,
    onOpenProfileSwitcher: () -> Unit,
    onSelectActiveMember: (Member) -> Unit,
    onNavigateToTab: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MintBackground,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = BorderLightGreen)
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SoftMintContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "TTS Emblem",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TTS कमेटी",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SoftMintContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "12 रबी-उल-अव्वल",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCloudConnected) EmeraldGreen else GoldAccent)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Firestore लाइव क्लाउड सिंक",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCloudConnected) EmeraldGreen else TextSecondaryGreen
                            )
                        }
                    }
                }

                // Persona / Admin Chip
                val currentName = activeMember?.fullName ?: allMembers.firstOrNull()?.fullName ?: "सदस्य"
                val currentRole = activeMember?.designation ?: allMembers.firstOrNull()?.designation ?: "खादिम"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isAdminLoggedIn) SoftMintContainer else SageSurfaceVariant)
                        .border(1.dp, if (isAdminLoggedIn) EmeraldGreen else BorderLightGreen, RoundedCornerShape(18.dp))
                        .clickable { onOpenProfileSwitcher() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MemberAvatar(
                            name = currentName,
                            photoUri = activeMember?.photoUri,
                            photoResName = activeMember?.photoResName,
                            size = 26.dp,
                            textSize = 10,
                            colorIndex = activeMember?.avatarColorIndex ?: 0,
                            isOnline = true,
                            showOnlineIndicator = true
                        )
                        Column {
                            Text(
                                text = currentName.split(" ").firstOrNull() ?: "सदस्य",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isAdminLoggedIn) "👑 $currentRole" else currentRole,
                                fontSize = 9.sp,
                                color = PrimaryGreen,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TTSBottomNavigationBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        NavigationBar(
            modifier = Modifier.border(1.dp, BorderLightGreen),
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            val items = listOf(
                Triple(AppTab.HOME, Icons.Default.Home, "मुख्य"),
                Triple(AppTab.MEMBERS, Icons.Default.Groups, "सदस्य"),
                Triple(AppTab.ID_CARD, Icons.Default.Badge, "ID कार्ड"),
                Triple(AppTab.MEETINGS, Icons.Default.Chat, "बैठक व चैट"),
                Triple(AppTab.NOTICES, Icons.Default.Campaign, "सूचनाएं"),
                Triple(AppTab.DONATIONS, Icons.Default.VolunteerActivism, "चंदा")
            )

            items.forEach { (tab, icon, label) ->
                val selected = currentTab == tab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryGreen,
                        selectedTextColor = PrimaryGreen,
                        unselectedIconColor = TextMutedGreen,
                        unselectedTextColor = TextMutedGreen,
                        indicatorColor = SoftMintContainer
                    ),
                    modifier = Modifier.testTag("nav_${tab.route}")
                )
            }
        }
    }
}

@Composable
fun SimulatedQRCode(
    dataText: String,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    foregroundColor: Color = PineGreenDark,
    backgroundColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size - 12.dp)) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val gridSize = 11
            val cellSize = canvasWidth / gridSize

            val hash = (dataText.hashCode().toLong() and 0xFFFFFFFL)

            // Draw QR finder corners
            fun drawFinderCorner(x: Float, y: Float, size: Float) {
                drawRect(
                    color = foregroundColor,
                    topLeft = Offset(x, y),
                    size = Size(size, size)
                )
                drawRect(
                    color = backgroundColor,
                    topLeft = Offset(x + size * 0.15f, y + size * 0.15f),
                    size = Size(size * 0.7f, size * 0.7f)
                )
                drawRect(
                    color = foregroundColor,
                    topLeft = Offset(x + size * 0.35f, y + size * 0.35f),
                    size = Size(size * 0.3f, size * 0.3f)
                )
            }

            val cornerSize = cellSize * 3f
            drawFinderCorner(0f, 0f, cornerSize)
            drawFinderCorner(canvasWidth - cornerSize, 0f, cornerSize)
            drawFinderCorner(0f, canvasHeight - cornerSize, cornerSize)

            // Fill pseudo-random cells
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if ((r < 3 && c < 3) || (r < 3 && c >= gridSize - 3) || (r >= gridSize - 3 && c < 3)) {
                        continue
                    }
                    val bitIndex = (r * gridSize + c) % 31
                    val isBlack = ((hash shr bitIndex) and 1L) == 1L || ((r + c) % 3 == 0)
                    if (isBlack) {
                        drawRect(
                            color = foregroundColor,
                            topLeft = Offset(c * cellSize, r * cellSize),
                            size = Size(cellSize * 0.9f, cellSize * 0.9f)
                        )
                    }
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "विवरण क्लिपबोर्ड पर कॉपी हो गया", Toast.LENGTH_SHORT).show()
}

fun openUPIIntent(
    context: Context,
    upiId: String,
    payeeName: String,
    amount: Double,
    note: String
) {
    val uri = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", upiId)
        .appendQueryParameter("pn", payeeName)
        .appendQueryParameter("am", String.format("%.2f", amount))
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", note)
        .build()

    val intent = Intent(Intent.ACTION_VIEW, uri)
    val chooser = Intent.createChooser(intent, "Google Pay / PhonePe / Paytm से चंदा दें")
    try {
        context.startActivity(chooser)
    } catch (e: Exception) {
        copyToClipboard(context, "UPI ID", upiId)
        Toast.makeText(context, "UPI ID ($upiId) कॉपी हो गई है", Toast.LENGTH_LONG).show()
    }
}
