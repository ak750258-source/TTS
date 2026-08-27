package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import coil.compose.AsyncImage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.Member
import com.example.data.model.OfficialDocument
import com.example.util.ImageUtils
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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

val HindiDesignationOptions = listOf(
    "संस्थापक (Founder)",
    "सदर (President)",
    "नायब सदर (Vice President)",
    "कोषाध्यक्ष (Treasurer)",
    "जनरल सेक्रेटरी (General Secretary)",
    "सह-सचिव (Joint Secretary)",
    "मुख्य व्यवस्थापक (Langar & Juloos)",
    "प्रवक्ता एवं मीडिया प्रभारी",
    "खादिम-ए-कमेटी (Volunteer)"
)

val HindiWingOptions = listOf(
    "मुख्य संरक्षक मंडल",
    "मुख्य प्रबंधक बोर्ड",
    "12 रबी-उल-अव्वल जुलूस कमेटी",
    "लंगर-ए-पाक व्यवस्था समिति",
    "चंदा एवं वित्तीय लेखा समिति",
    "सुरक्षा एवं अनुशासन विंग",
    "युवा विंग एवं सोशल मीडिया"
)

// --- ADMIN LOGIN DIALOG ---
@Composable
fun AdminLoginDialog(
    onDismiss: () -> Unit,
    onLogin: (user: String, pass: String) -> Boolean
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GoldLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PineGreenDark, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "एडमिन लॉगिन (Admin Portal)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimaryGreen
                            )
                            Text(
                                text = "पद वितरण, नोटिस एवं सदस्य नियंत्रण",
                                fontSize = 11.sp,
                                color = TextSecondaryGreen
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftMintContainer)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "🔒 यह केवल अधिकृत कमेटी एडमिनिस्ट्रेटर हेतु सुरक्षित लॉगिन पैनल है।",
                        fontSize = 11.sp,
                        color = PineGreenDark,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("यूज़रनेम (Username)") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth().testTag("admin_username_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("पासवर्ड (Password)") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldGreen) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondaryGreen
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें (Cancel)", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val success = onLogin(username, password)
                            if (!success) {
                                errorMessage = "गलत यूज़रनेम या पासवर्ड! कृपया सही विवरण दर्ज करें।"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("admin_login_submit_btn")
                    ) {
                        Text("लॉगिन करें (Login)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- DISTRIBUTE DESIGNATION DIALOG ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributeDesignationDialog(
    member: Member,
    onDismiss: () -> Unit,
    onConfirm: (newDesignation: String, newWing: String) -> Unit
) {
    var selectedDesignation by remember { mutableStateOf(member.designation) }
    var selectedWing by remember { mutableStateOf(member.committeeWing) }
    var expandedDesig by remember { mutableStateOf(false) }
    var expandedWing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "👑 पद वितरण (Assign Designation)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "${member.fullName} (${member.memberCode})",
                            fontSize = 12.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightSageCard)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "यहाँ से बदला गया पद सीधे सदस्य के 12 रबी-उल-अव्वल डिजिटल पहचान पत्र (ID Card) पर तुरंत दिखेगा।",
                        fontSize = 11.sp,
                        color = PineGreenDark
                    )
                }

                // Designation Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDesig,
                    onExpandedChange = { expandedDesig = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDesignation,
                        onValueChange = { selectedDesignation = it },
                        label = { Text("नया पद / Designation *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDesig) },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = EmeraldGreen) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDesig,
                        onDismissRequest = { expandedDesig = false }
                    ) {
                        HindiDesignationOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedDesignation = opt
                                    expandedDesig = false
                                }
                            )
                        }
                    }
                }

                // Wing Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedWing,
                    onExpandedChange = { expandedWing = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedWing,
                        onValueChange = { selectedWing = it },
                        label = { Text("कमेटी विंग / विभाग *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWing) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldGreen) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedWing,
                        onDismissRequest = { expandedWing = false }
                    ) {
                        HindiWingOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedWing = opt
                                    expandedWing = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedDesignation.isNotBlank()) {
                                onConfirm(selectedDesignation, selectedWing)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("पद सौंपें (Assign)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- AWARD BEST PERFORMER DIALOG ---
@Composable
fun AwardBestPerformerDialog(
    member: Member,
    onDismiss: () -> Unit,
    onConfirm: (isBest: Boolean, badgeTitle: String?) -> Unit
) {
    var isBest by remember { mutableStateOf(member.isBestPerformer) }
    var badgeTitle by remember { mutableStateOf(member.bestPerformerBadge ?: "12 रबी-उल-अव्वल सर्वश्रेष्ठ खादिम सम्मान") }

    val presetBadges = listOf(
        "12 रबी-उल-अव्वल सर्वश्रेष्ठ खादिम सम्मान",
        "जुलूस-ए-मोहम्मदी स्टार वॉलंटियर 1447H",
        "उत्कृष्ट लंगर-ए-पाक सेवा सम्मान",
        "पारदर्शी चंदा व लेखा प्रबंधन सम्मान",
        "लाइफटाइम कमेटी खिदमतगार सम्मान"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⭐ बेस्ट परफ़ॉर्मर सम्मान",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "${member.fullName} (${member.designation})",
                            fontSize = 12.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftMintContainer)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("बेस्ट परफ़ॉर्मर बनाएं", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryGreen)
                        Text("होम स्क्रीन और ID कार्ड पर विशेष सम्मान बैज व फोटो दिखेगा", fontSize = 10.sp, color = TextSecondaryGreen)
                    }
                    Switch(
                        checked = isBest,
                        onCheckedChange = { isBest = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                    )
                }

                if (isBest) {
                    OutlinedTextField(
                        value = badgeTitle,
                        onValueChange = { badgeTitle = it },
                        label = { Text("सम्मान उपाधि / बैज नाम *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("त्वरित बैज चुनें:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondaryGreen)
                    presetBadges.forEach { badge ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(LightSageCard)
                                .clickable { badgeTitle = badge }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("• $badge", fontSize = 11.sp, color = TextPrimaryGreen)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(isBest, if (isBest) badgeTitle else null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("सहेजें (Save)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- ADD / EDIT MEMBER DIALOG IN HINDI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        designation: String,
        committeeWing: String,
        phoneNumber: String,
        email: String,
        bloodGroup: String,
        address: String,
        emergencyContact: String,
        isBestPerformer: Boolean,
        bestBadge: String?,
        photoUri: String?
    ) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf(HindiDesignationOptions[8]) }
    var committeeWing by remember { mutableStateOf(HindiWingOptions[2]) }
    var phoneNumber by remember { mutableStateOf("+91 ") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("+91 ") }
    var isBestPerformer by remember { mutableStateOf(false) }
    var bestBadge by remember { mutableStateOf("12 रबी-उल-अव्वल सर्वश्रेष्ठ खिदमतगार") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val base64 = ImageUtils.uriToBase64(context, uri)
                selectedPhotoUri = base64 ?: uri.toString()
            }
        }
    }

    var expandedDesignation by remember { mutableStateOf(false) }
    var expandedWing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "नया सदस्य जोड़ें (Add Member)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "12 रबी-उल-अव्वल कमेटी में नया खादिम शामिल करें",
                            fontSize = 11.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Member Photo Upload Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SoftMintContainer)
                        .clickable { photoPickerLauncher.launch("image/*") }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.5.dp, EmeraldGreen, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPhotoUri != null) {
                            AsyncImage(
                                model = selectedPhotoUri,
                                contentDescription = "Selected Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Upload Photo", tint = EmeraldGreen, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedPhotoUri != null) "✓ फोटो चुनी गई (Photo Selected)" else "📷 सदस्य की फोटो अपलोड करें (ID कार्ड हेतु)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = if (selectedPhotoUri != null) "बदलने के लिए यहाँ टैप करें" else "गैलरी से सदस्य की तस्वीर चुनें",
                            fontSize = 10.sp,
                            color = TextSecondaryGreen
                        )
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("पूरा नाम (Full Name) *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth().testTag("member_name_input"),
                    singleLine = true
                )

                // Designation Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDesignation,
                    onExpandedChange = { expandedDesignation = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = designation,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("पद / Designation *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDesignation) },
                        leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = EmeraldGreen) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDesignation,
                        onDismissRequest = { expandedDesignation = false }
                    ) {
                        HindiDesignationOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    designation = opt
                                    expandedDesignation = false
                                }
                            )
                        }
                    }
                }

                // Wing Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedWing,
                    onExpandedChange = { expandedWing = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = committeeWing,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("कमेटी विंग / विभाग *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWing) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedWing,
                        onDismissRequest = { expandedWing = false }
                    ) {
                        HindiWingOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    committeeWing = opt
                                    expandedWing = false
                                }
                            )
                        }
                    }
                }

                // Phone
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("मोबाइल नंबर (Phone) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("मोहल्ला / निवासी पता (Address)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Emergency Contact
                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("आपातकालीन फोन नंबर") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Best performer toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("बेस्ट परफ़ॉर्मर के रूप में जोड़ें", fontSize = 13.sp, color = TextPrimaryGreen)
                    Switch(
                        checked = isBestPerformer,
                        onCheckedChange = { isBestPerformer = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.isBlank()) {
                                errorMsg = "कृपया सदस्य का नाम दर्ज करें"
                                return@Button
                            }
                            if (phoneNumber.length < 6) {
                                errorMsg = "कृपया सही मोबाइल नंबर दर्ज करें"
                                return@Button
                            }
                            onConfirm(
                                fullName,
                                designation,
                                committeeWing,
                                phoneNumber,
                                if (email.isBlank()) "member@ttscommittee.org" else email,
                                "",
                                if (address.isBlank()) "मस्जिद रोड, वार्ड 12" else address,
                                if (emergencyContact.isBlank()) phoneNumber else emergencyContact,
                                isBestPerformer,
                                if (isBestPerformer) bestBadge else null,
                                selectedPhotoUri
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("submit_add_member_button")
                    ) {
                        Text("आईडी जारी करें (Save & Issue ID)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- SELF REGISTER MEMBER DIALOG (FOR VOLUNTEERS/MEMBERS) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfRegisterMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        fullName: String,
        phoneNumber: String,
        email: String,
        address: String,
        requestedWing: String,
        emergencyContact: String,
        photoUri: String?
    ) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("+91 ") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var requestedWing by remember { mutableStateOf(HindiWingOptions[0]) }
    var emergencyContact by remember { mutableStateOf("+91 ") }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }
    var expandedWing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val base64 = ImageUtils.uriToBase64(context, uri)
                selectedPhotoUri = base64 ?: uri.toString()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftMintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HowToReg, contentDescription = null, tint = PrimaryGreen)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "सदस्य स्व-पंजीकरण",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimaryGreen
                            )
                            Text(
                                text = "12 रबी-उल-अव्वल डिजिटल पहचान पत्र पाएं",
                                fontSize = 11.sp,
                                color = TextSecondaryGreen
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Photo Upload Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { photoPickerLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftMintContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(2.dp, GoldAccent, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedPhotoUri != null) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Uploaded Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(26.dp))
                                    Text("फोटो", fontSize = 9.sp, color = TextSecondaryGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (selectedPhotoUri != null) "✓ आपकी फोटो चयनित है" else "📷 अपनी फोटो अपलोड करें *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryGreen
                            )
                            Text(
                                text = "यह फोटो आपके 12 रबी-उल-अव्वल ID कार्ड पर प्रिंट होगी।",
                                fontSize = 11.sp,
                                color = TextSecondaryGreen
                            )
                        }
                    }
                }

                // Full Name
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("आपका पूरा नाम (Full Name) *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth().testTag("self_reg_name_input"),
                    singleLine = true
                )

                // Phone
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("मोबाइल नंबर (WhatsApp Phone) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth().testTag("self_reg_phone_input"),
                    singleLine = true
                )

                // Wing Selection
                ExposedDropdownMenuBox(
                    expanded = expandedWing,
                    onExpandedChange = { expandedWing = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = requestedWing,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("आप किस सेवा विंग में शामिल होना चाहते हैं?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedWing) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = EmeraldGreen) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedWing,
                        onDismissRequest = { expandedWing = false }
                    ) {
                        HindiWingOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    requestedWing = opt
                                    expandedWing = false
                                }
                            )
                        }
                    }
                }

                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("मोहल्ला / गली / वार्ड पता (Address)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Emergency Contact
                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("आपातकालीन संपर्क नंबर (Emergency Phone)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.isBlank()) {
                                errorMsg = "कृपया अपना पूरा नाम दर्ज करें"
                                return@Button
                            }
                            if (phoneNumber.length < 6) {
                                errorMsg = "कृपया सही मोबाइल नंबर दर्ज करें"
                                return@Button
                            }
                            onConfirm(
                                fullName,
                                phoneNumber,
                                if (email.isBlank()) "member@ttscommittee.org" else email,
                                if (address.isBlank()) "वार्ड 12, मुख्य मोहल्ला" else address,
                                requestedWing,
                                if (emergencyContact.isBlank()) phoneNumber else emergencyContact,
                                selectedPhotoUri
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.testTag("submit_self_reg_button")
                    ) {
                        Text("आईडी कार्ड बनाएं (Register)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- UPDATE MEMBER PHOTO DIALOG ---
@Composable
fun UpdateMemberPhotoDialog(
    member: Member,
    onDismiss: () -> Unit,
    onConfirm: (photoUri: String) -> Unit
) {
    var photoUri by remember { mutableStateOf(member.photoUri) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val base64 = ImageUtils.uriToBase64(context, uri)
                photoUri = base64 ?: uri.toString()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "फोटो बदलें / लगाएं",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "${member.fullName} (${member.memberCode})",
                            fontSize = 11.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SoftMintContainer)
                        .border(2.5.dp, GoldAccent, RoundedCornerShape(16.dp))
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Member Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("फोटो चुनें", fontSize = 11.sp, color = TextPrimaryGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftMintContainer)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("गैलरी से फोटो चुनें", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!photoUri.isNullOrBlank()) {
                                onConfirm(photoUri!!)
                            }
                        },
                        enabled = !photoUri.isNullOrBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("सहेजें (Save Photo)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- ADD NOTICE DIALOG IN HINDI ---
@Composable
fun AddNoticeDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, category: String, priority: String, issuedBy: String, content: String, isPinned: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("12 रबी-उल-अव्वल") }
    var priority by remember { mutableStateOf("HIGH") }
    var issuedBy by remember { mutableStateOf("सदर / जनरल सेक्रेटरी, TTS कमेटी") }
    var content by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "नया नोटिस जारी करें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "सूचना पट्ट पर आधिकारिक घोषणा",
                            fontSize = 11.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("नोटिस का शीर्षक (Title) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("श्रेणी (उदा. 12 रबी-उल-अव्वल, लंगर, चंदा, जुलूस)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = issuedBy,
                    onValueChange = { issuedBy = it },
                    label = { Text("जारीकर्ता (Issued By) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("नोटिस का पूरा विवरण (Details) *") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 6
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("सूचना पट्ट के शीर्ष पर पिन करें", fontSize = 13.sp, color = TextPrimaryGreen)
                    Switch(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryGreen)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onConfirm(title, category, priority, issuedBy, content, isPinned)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("प्रकाशित करें (Publish)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- RECORD DONATION DIALOG IN HINDI (UPI ak750258@icici) ---
@Composable
fun AddDonationRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (donorName: String, memberCode: String?, amount: Double, purpose: String, paymentMode: String, transactionRef: String, remarks: String?) -> Unit
) {
    var donorName by remember { mutableStateOf("") }
    var memberCode by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("1100") }
    var purpose by remember { mutableStateOf("12 रबी-उल-अव्वल लंगर-ए-पाक व सजावट") }
    var transactionRef by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    val purposeOptions = listOf(
        "12 रबी-उल-अव्वल लंगर-ए-पाक व सजावट",
        "जुलूस-ए-मोहम्मदी स्टेज व लाइट डेकोरेशन",
        "तबर्रुक व शरबत सबील व्यवस्था",
        "झंडे (परचम) व तोरण द्वार व्यवस्था",
        "कमेटी आम कल्याण कोष"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "चंदा / दान दर्ज करें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "आधिकारिक UPI: ak750258@icici लेजर",
                            fontSize = 11.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                OutlinedTextField(
                    value = donorName,
                    onValueChange = { donorName = it },
                    label = { Text("दानदाता का नाम (Donor Name) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = memberCode,
                    onValueChange = { memberCode = it },
                    label = { Text("सदस्य कोड (वैकल्पिक, e.g. TTS-1447-001)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("राशि (₹ Amount) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("दान का उद्देश्य / कारण *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text("UPI संदर्भ / UTR नंबर (वैकल्पिक)") },
                    placeholder = { Text("उदा. UPI/624910284719") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("विशेष टिप्पणी (Remarks)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (donorName.isNotBlank() && amt > 0) {
                                onConfirm(
                                    donorName,
                                    memberCode,
                                    amt,
                                    purpose,
                                    "UPI (ak750258@icici)",
                                    transactionRef,
                                    remarks
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("लेजर में जोड़ें (Save)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- ADD MEETING DIALOG IN HINDI ---
@Composable
fun AddMeetingDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        type: String,
        dateDisplay: String,
        timeDisplay: String,
        venue: String,
        virtualLink: String?,
        chairperson: String,
        agenda: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("12 रबी-उल-अव्वल मुख्य बैठक") }
    var dateDisplay by remember { mutableStateOf("कल शाम (12 रबी-उल-अव्वल तैयारी)") }
    var timeDisplay by remember { mutableStateOf("08:30 PM - 10:00 PM") }
    var venue by remember { mutableStateOf("TTS सेंट्रल हॉल, जामा मस्जिद चौक") }
    var virtualLink by remember { mutableStateOf("https://meet.google.com/tts-milad-1447") }
    var chairperson by remember { mutableStateOf("जनाब गुलाम मुस्तफा (सदर)") }
    var agenda by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "नई बैठक तय करें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "12 रबी-उल-अव्वल आयोजन बैठक सूची",
                            fontSize = 11.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("बैठक का नाम / विषय *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateDisplay,
                    onValueChange = { dateDisplay = it },
                    label = { Text("तारीख व समय (Date & Time) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("स्थान / Venue *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = chairperson,
                    onValueChange = { chairperson = it },
                    label = { Text("अध्यक्षता (Chairperson) *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = agenda,
                    onValueChange = { agenda = it },
                    label = { Text("कार्यसूची / एजेंडा (Agenda)") },
                    modifier = Modifier.fillMaxWidth().height(90.dp),
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    title,
                                    type,
                                    dateDisplay,
                                    timeDisplay,
                                    venue,
                                    virtualLink,
                                    chairperson,
                                    if (agenda.isBlank()) "1. 12 रबी-उल-अव्वल की तैयारियों की समीक्षा।\n2. लंगर व जुलूस अनुशासन।" else agenda
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("बैठक तय करें", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- DOCUMENT VIEWER DIALOG ---
@Composable
fun DocumentViewerDialog(
    doc: OfficialDocument,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Official Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PineGreenDark)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "TTS अधिकृत दस्तावेज़ वॉल्ट",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "कोड: ${doc.refCode} • प्रमाणित डिजिटल प्रति",
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = doc.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryGreen
                            )
                        }

                        Text(
                            text = doc.accessLevel,
                            fontSize = 12.sp,
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = doc.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryGreen,
                        lineHeight = 23.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("जारी तिथि: ${doc.publishedDate}", fontSize = 12.sp, color = TextSecondaryGreen)
                        Text("साइज: ${doc.fileSize}", fontSize = 12.sp, color = TextSecondaryGreen)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(LightSageCard)
                            .border(1.dp, BorderLightGreen, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "मुख्य सारांश (Summary)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = doc.summary,
                                fontSize = 13.sp,
                                color = TextPrimaryGreen,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFAFAFA))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = doc.fullContent,
                            fontSize = 12.sp,
                            color = Color(0xFF1E293B),
                            lineHeight = 18.sp
                        )
                    }
                }

                // Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = LightSageCard,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, "Official Document Ref", "${doc.title} (${doc.refCode})")
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("शेयर करें", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "दस्तावेज़ '${doc.title}' डाउनलोड हो गया", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF डाउनलोड करें", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// --- PROFILE SWITCHER DIALOG ---
@Composable
fun ProfileSwitcherDialog(
    members: List<Member>,
    currentActiveMember: Member?,
    onSelectMember: (Member) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "सदस्य प्रोफाइल बदलें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "चैट व संवाद हेतु अपनी पहचान चुनें",
                            fontSize = 11.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    members.forEach { m ->
                        val isSelected = m.id == currentActiveMember?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectMember(m)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) SoftMintContainer else LightSageCard
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryGreen)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MemberAvatar(
                                    name = m.fullName,
                                    photoUri = m.photoUri,
                                    photoResName = m.photoResName,
                                    size = 36.dp,
                                    textSize = 13,
                                    colorIndex = m.avatarColorIndex,
                                    showOnlineIndicator = false
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(m.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimaryGreen)
                                    Text("${m.designation} • ${m.memberCode}", fontSize = 11.sp, color = TextSecondaryGreen)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ADD DOCUMENT DIALOG IN HINDI (ADMIN ONLY - REAL-TIME CLOUD SYNCED) ---
@Composable
fun AddDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        category: String,
        accessLevel: String,
        summary: String,
        fullContent: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("प्रशासनिक आदेश") }
    var accessLevel by remember { mutableStateOf("सार्वजनिक (Public)") }
    var summary by remember { mutableStateOf("") }
    var fullContent by remember { mutableStateOf("") }

    val categoryOptions = listOf(
        "प्रशासनिक आदेश",
        "जुलूस नियमावली व रूट मैप",
        "सुरक्षा व अनुमति पत्र",
        "लंगर व सबील व्यवस्था",
        "वित्तीय ऑडिट रिपोर्ट",
        "आधिकारिक प्रस्ताव"
    )

    val accessLevelOptions = listOf(
        "सार्वजनिक (Public)",
        "कमेटी सदस्य केवल (Internal)",
        "गोपनीय (Confidential)"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "नया आधिकारिक दस्तावेज़ जोड़ें",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextPrimaryGreen
                        )
                        Text(
                            text = "सभी सदस्यों व जनता के लिए लाइव क्लाउड सिंक",
                            fontSize = 11.sp,
                            color = TextSecondaryGreen
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryGreen)
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("दस्तावेज़ का शीर्षक / नाम *") },
                    placeholder = { Text("उदा. 12 रबी-उल-अव्वल जुलूस सुरक्षा व अनुशासन नियमावली") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "दस्तावेज़ श्रेणी (Category):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryGreen
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoryOptions.take(3).forEach { cat ->
                            val isSel = category == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PrimaryGreen else SoftMintContainer)
                                    .clickable { category = cat }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextPrimaryGreen
                                )
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "पहुँच स्तर (Access Level):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryGreen
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accessLevelOptions.forEach { acc ->
                            val isSel = accessLevel == acc
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PineGreenDark else SoftMintContainer)
                                    .clickable { accessLevel = acc }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = acc,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else TextPrimaryGreen
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("मुख्य सारांश (Short Summary) *") },
                    placeholder = { Text("दस्तावेज़ का 1-2 पंक्तियों में संक्षिप्त विवरण...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = fullContent,
                    onValueChange = { fullContent = it },
                    label = { Text("दस्तावेज़ का पूर्ण विवरण / आदेश (Full Content) *") },
                    placeholder = { Text("दस्तावेज़ के सभी नियम, बिंदु और निर्देश विस्तार से यहाँ लिखें...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 8
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें", color = TextSecondaryGreen)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && summary.isNotBlank()) {
                                onConfirm(
                                    title,
                                    category,
                                    accessLevel,
                                    summary,
                                    if (fullContent.isBlank()) summary else fullContent
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("दस्तावेज़ अपलोड करें (Publish)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

