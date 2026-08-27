package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.example.util.ImageUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.theme.PineGreenDark
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SoftMintContainer
import com.example.ui.theme.TextPrimaryGreen
import com.example.ui.theme.TextSecondaryGreen

@Composable
fun MemberIDCardView(
    member: Member,
    onUpdatePhoto: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isBackSide by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && onUpdatePhoto != null) {
            coroutineScope.launch {
                val base64 = ImageUtils.uriToBase64(context, uri)
                onUpdatePhoto(base64 ?: uri.toString())
                Toast.makeText(context, "फोटो सफलतापूर्वक अपडेट हो गई!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ID Card Flip Card with Flag Watermark Background
        AnimatedContent(
            targetState = isBackSide,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "IDCardFlip"
        ) { back ->
            if (!back) {
                IDCardFrontSide(
                    member = member,
                    onFlip = { isBackSide = true },
                    onPickPhoto = if (onUpdatePhoto != null) { { photoPickerLauncher.launch("image/*") } } else null
                )
            } else {
                IDCardBackSide(member = member, onFlip = { isBackSide = false })
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Toolbar below ID Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { isBackSide = !isBackSide },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryGreen),
                modifier = Modifier.testTag("flip_id_card_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FlipCameraAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isBackSide) "सामने देखें" else "पीछे देखें", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            if (onUpdatePhoto != null) {
                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    modifier = Modifier.testTag("upload_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (!member.photoUri.isNullOrBlank()) "फोटो बदलें" else "फोटो लगाएं", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    Toast.makeText(context, "${member.fullName} का 12 रबी-उल-अव्वल ID कार्ड डाउनलोड हो गया", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier.testTag("download_id_card_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("कार्ड सेव", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IDCardFrontSide(
    member: Member,
    onFlip: () -> Unit,
    onPickPhoto: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .border(2.5.dp, Brush.linearGradient(listOf(GoldAccent, EmeraldGreen, GoldAccent)), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FCF9))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Muhammad Islamic Green Flag Background Watermark (Requested by User)
            Image(
                painter = painterResource(id = R.drawable.img_muhammad_flag_bg),
                contentDescription = "Muhammad Islamic Flag Background",
                modifier = Modifier
                    .fillMaxSize()
                    .matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.18f
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Banner with Islamic Greeting & Lanyard Hole
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(PineGreenDark, DeepForestGreen, Color(0xFF0F5132))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Simulated Lanyard Hole
                        Box(
                            modifier = Modifier
                                .width(36.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.45f))
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent)
                                        .border(1.5.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = PineGreenDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "TTS 12 रबी-उल-अव्वल कमेटी",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "जश्न-ए-विलादत-उन-नबी ﷺ • अधिकृत पहचान पत्र",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA5D6A7)
                                    )
                                }
                            }

                            // 1447H Flag Tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(GoldAccent, Color(0xFFFDE68A), GoldAccent)
                                        )
                                    )
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = PineGreenDark, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "1447H",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PineGreenDark
                                    )
                                }
                            }
                        }
                    }
                }

                // Member Body with Real Photo & Details (NO BLOOD GROUP)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Member Photo Box (Shows photo or avatar frame)
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(2.5.dp, GoldAccent, RoundedCornerShape(14.dp))
                                .shadow(4.dp, RoundedCornerShape(14.dp))
                                .then(if (onPickPhoto != null) Modifier.clickable { onPickPhoto() } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!member.photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = member.photoUri,
                                    contentDescription = "Member Photo - ${member.fullName}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (member.photoResName == "img_best_performer") {
                                Image(
                                    painter = painterResource(id = R.drawable.img_best_performer),
                                    contentDescription = "Member Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                MemberAvatar(
                                    name = member.fullName,
                                    photoUri = member.photoUri,
                                    photoResName = member.photoResName,
                                    size = 78.dp,
                                    textSize = 28,
                                    colorIndex = member.avatarColorIndex,
                                    showOnlineIndicator = false
                                )
                            }

                            // Camera upload icon hint overlay if editable
                            if (onPickPhoto != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(PineGreenDark)
                                        .border(1.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Upload Photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            // Best Performer Badge if present
                            if (member.isBestPerformer) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .border(1.dp, GoldAccent, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "⭐ बेस्ट परफ़ॉर्मर",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                            }

                            Text(
                                text = member.fullName,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TextPrimaryGreen,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))

                            // Highlighted Designation (संस्थापक, कोषाध्यक्ष, सदर, etc.)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SoftMintContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = member.designation,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PineGreenDark
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = member.committeeWing,
                                fontSize = 11.sp,
                                color = TextSecondaryGreen
                            )
                            Text(
                                text = "आईडी: ${member.memberCode}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = EmeraldGreen
                            )
                        }
                    }

                    HorizontalDivider(color = BorderLightGreen, thickness = 1.dp)

                    // Details Row (Blood Group completely removed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("शामिल होने की तिथि", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryGreen)
                            Text(member.joinDate, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = TextPrimaryGreen)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("संपर्क नंबर", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryGreen)
                            Text(member.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimaryGreen)
                        }
                    }

                    // Islamic Green Watermark Footer with Authorized Signatures
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "समारोह: 12 रबी-उल-अव्वल 1447H",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestGreen
                            )
                            Text(
                                text = "UPI: ak750258@icici",
                                fontSize = 9.sp,
                                color = EmeraldGreen,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "जनाब मुंतज़िर खांन",
                                fontFamily = FontFamily.Cursive,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = DeepForestGreen
                            )
                            Box(
                                modifier = Modifier
                                    .width(75.dp)
                                    .height(1.dp)
                                    .background(TextSecondaryGreen)
                            )
                            Text(
                                text = "सदर (President), TTS",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondaryGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IDCardBackSide(
    member: Member,
    onFlip: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .border(2.5.dp, Brush.linearGradient(listOf(EmeraldGreen, GoldAccent, PineGreenDark)), RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FCF9))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Muhammad Flag Watermark on back as well
            Image(
                painter = painterResource(id = R.drawable.img_muhammad_flag_bg),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.14f
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "12 रबी-उल-अव्वल कमेटी पहचान एवं नियम",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = DeepForestGreen
                    )
                    Text(
                        text = member.memberCode,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = GoldText
                    )
                }

                HorizontalDivider(color = BorderLightGreen, thickness = 1.dp)

                // Address & Emergency Info (NO Blood Group)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("निवासी पता (Address):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryGreen)
                    Text(member.address, fontSize = 11.sp, color = TextPrimaryGreen, lineHeight = 15.sp)

                    Spacer(modifier = Modifier.height(2.dp))

                    Text("आपातकालीन संपर्क (Emergency Help):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondaryGreen)
                    Text(member.emergencyContact, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                }

                // QR & UPI Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .border(1.dp, BorderLightGreen, RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "डिजिटल सुरक्षा QR कोड",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "जुलूस-ए-मोहम्मदी एवं लंगर प्रवेश पर स्कैन करें।",
                            fontSize = 9.sp,
                            color = TextSecondaryGreen,
                            lineHeight = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "चंदा UPI: ak750258@icici",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    SimulatedQRCode(
                        dataText = "TTS-1447:${member.memberCode}:${member.fullName}:${member.designation}",
                        size = 64.dp
                    )
                }

                // Official Head Office Address Box requested by user
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE8F5E9))
                        .border(1.dp, BorderLightGreen, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Column {
                        Text(
                            text = "प्रधान कार्यालय (Head Office):",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PineGreenDark
                        )
                        Text(
                            text = "धनुपुरा, बिसौली, बदायूं, उत्तर प्रदेश (पिन कोड: 243632)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryGreen,
                            lineHeight = 14.sp
                        )
                    }
                }

                // Instructions in Hindi
                Text(
                    text = "1. यह पहचान पत्र 12 रबी-उल-अव्वल जश्न-ए-विलादत कमेटी की अधिकृत संपत्ति है।\n2. जुलूस व लंगर-ए-आम के दौरान गले में आईडी कार्ड अवश्य धारण करें।\n3. प्रधान कार्यालय: धनुपुरा, बिसौली, बदायूं, उत्तर प्रदेश, पिन कोड 243632",
                    fontSize = 8.sp,
                    color = TextSecondaryGreen,
                    lineHeight = 11.sp
                )

                HorizontalDivider(color = BorderLightGreen, thickness = 1.dp)

                // Barcode canvas graphic
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                ) {
                    val barWidth = 3f
                    var currentX = 10f
                    val pattern = listOf(2, 1, 3, 1, 2, 4, 1, 2, 3, 1, 1, 4, 2, 1, 3, 2, 1, 4, 1, 2, 3, 1, 2, 4, 2, 1, 3)
                    for (w in pattern) {
                        drawRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(currentX, 0f),
                            size = Size(barWidth * w, size.height)
                        )
                        currentX += (barWidth * w) + (barWidth * 1.5f)
                    }
                }
            }
        }
    }
}
