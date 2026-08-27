package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.example.data.model.Donation
import com.example.ui.components.MemberAvatar
import com.example.ui.components.SimulatedQRCode
import com.example.ui.components.copyToClipboard
import com.example.ui.components.openUPIIntent
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

const val OFFICIAL_UPI_ID = "ak750258@icici"

@Composable
fun DonationScreen(
    donations: List<Donation>,
    totalDonations: Double,
    isAdminLoggedIn: Boolean = false,
    onOpenAdminLogin: () -> Unit = {},
    onOpenAddDonationModal: () -> Unit,
    onVerifyDonation: (donationId: Long, isVerified: Boolean) -> Unit = { _, _ -> },
    onEditDonation: (donation: Donation) -> Unit = {},
    onDeleteDonation: (donationId: Long) -> Unit = {},
    onClearOldDonations: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedAmount by remember { mutableDoubleStateOf(1100.0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPurposeFilter by remember { mutableStateOf("सभी मद (All)") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var donationToDelete by remember { mutableStateOf<Donation?>(null) }

    val donationGoal = 250000.0
    val progress = (totalDonations / donationGoal).toFloat().coerceIn(0f, 1f)

    val quickAmounts = listOf(500.0, 1100.0, 2100.0, 5100.0, 11000.0)
    val purposeFilters = listOf("सभी मद (All)", "12 रबी-उल-अव्वल जुलूस", "आम लंगर-ए-गौसिया", "सजावट व रौशनी", "तबर्रुक व रसद")

    val filteredDonations = donations.filter { d ->
        val matchesSearch = d.donorName.contains(searchQuery, ignoreCase = true) ||
                (d.donorMemberCode?.contains(searchQuery, ignoreCase = true) == true) ||
                d.transactionRef.contains(searchQuery, ignoreCase = true) ||
                d.purpose.contains(searchQuery, ignoreCase = true)

        val matchesPurpose = when (selectedPurposeFilter) {
            "सभी मद (All)" -> true
            "12 रबी-उल-अव्वल जुलूस" -> d.purpose.contains("जुलूस")
            "आम लंगर-ए-गौसिया" -> d.purpose.contains("लंगर")
            "सजावट व रौशनी" -> d.purpose.contains("सजावट") || d.purpose.contains("रौशनी")
            "तबर्रुक व रसद" -> d.purpose.contains("तबर्रुक") || d.purpose.contains("रसद")
            else -> true
        }

        matchesSearch && matchesPurpose
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MintBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        text = "12 रबी-उल-अव्वल चंदा संग्रह रिपोर्ट",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = TextPrimaryGreen
                    )
                    Text(
                        text = "पारदर्शी बहीखाता • प्रत्येक सदस्य हेतु उपलब्ध",
                        fontSize = 11.sp,
                        color = TextSecondaryGreen
                    )
                }

                if (isAdminLoggedIn) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftMintContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("एडमिन अधिकार", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PineGreenDark)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SoftMintContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("100% पारदर्शी", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PineGreenDark)
                        }
                    }
                }
            }
        }

        // Admin Authority Banner (Issue official list, clear old records)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isAdminLoggedIn) SoftMintContainer else Color.White),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isAdminLoggedIn) EmeraldGreen else BorderLightGreen)
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Publish, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "आधिकारिक चंदा सूची जारी करें (Publish Official List)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimaryGreen
                            )
                        }
                    }

                    Text(
                        text = "कमेटी द्वारा संकलित पूर्ण चंदा सूची को सार्वजनिक रूप से व्हाट्सएप, सूचना-पट्ट अथवा सदस्यों में साझा करें।",
                        fontSize = 10.sp,
                        color = TextSecondaryGreen
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val listText = buildString {
                                    appendLine("📋 *12 रबी-उल-अव्वल TTS कमेटी - आधिकारिक चंदा सूची*")
                                    appendLine("आधिकारिक UPI: ak750258@icici")
                                    appendLine("कुल संग्रह: ₹${String.format("%,.0f", totalDonations)} (${donations.size} दानकर्ता)")
                                    appendLine("----------------------------------")
                                    donations.forEachIndexed { index, d ->
                                        appendLine("${index + 1}. ${d.donorName} - ₹${d.amount.toInt()} (${d.purpose})")
                                    }
                                    appendLine("----------------------------------")
                                    appendLine("जज़ाकल्लाह ख़ैर • 12 रबी-उल-अव्वल कमेटी")
                                }
                                copyToClipboard(context, "Official Chanda List", listText)
                                Toast.makeText(context, "आधिकारिक चंदा सूची कॉपी हो गई! सदस्यों में साझा करें।", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("publish_chanda_list_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("चंदा सूची जारी / साझा करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (isAdminLoggedIn) {
                            OutlinedButton(
                                onClick = { showClearConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("clear_old_records_btn")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("पुराना रिकॉर्ड हटाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onOpenAdminLogin,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("एडमिन लॉगिन", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Total Collection Summary Card with Progress
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("12 रबी-उल-अव्वल कुल चंदा संग्रह", fontSize = 12.sp, color = TextSecondaryGreen)
                            Text(
                                text = "₹${String.format("%,.0f", totalDonations)}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimaryGreen
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftMintContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("कुल रसीदें", fontSize = 10.sp, color = TextSecondaryGreen)
                                Text("${donations.size} दानकर्ता", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PineGreenDark)
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("लक्ष्य: ₹2,50,000", fontSize = 11.sp, color = TextSecondaryGreen)
                            Text("${(progress * 100).toInt()}% पूर्ण", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
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
                    }

                    // UPI ID Information Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightSageCard)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("आधिकारिक UPI आईडी:", fontSize = 10.sp, color = TextSecondaryGreen)
                                Text(
                                    text = OFFICIAL_UPI_ID,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = PineGreenDark
                                )
                            }

                            IconButton(
                                onClick = {
                                    copyToClipboard(context, "TTS UPI ID", OFFICIAL_UPI_ID)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy UPI", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Quick Pay / Donate via UPI Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
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
                    Text(
                        text = "12 रबी-उल-अव्वल सहयोग राशि चुनें (Direct UPI)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimaryGreen
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickAmounts) { amt ->
                            val isSelected = selectedAmount == amt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryGreen else Color.White)
                                    .clickable { selectedAmount = amt }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "₹${amt.toInt()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else TextPrimaryGreen
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                openUPIIntent(
                                    context = context,
                                    upiId = OFFICIAL_UPI_ID,
                                    payeeName = "TTS 12 Rabi Ul Awwal Committee",
                                    amount = selectedAmount,
                                    note = "12 Rabi Ul Awwal Chanda"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier.weight(1f).testTag("upi_pay_button")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UPI से ₹${selectedAmount.toInt()} दें", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenAddDonationModal,
                            modifier = Modifier.weight(1f).testTag("record_donation_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("चंदा दर्ज करें", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }
                    }
                }
            }
        }

        // Ledger & Filter Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "दानदाता सूची एवं रसीदें (Donation Ledger)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimaryGreen
                    )
                    Text("${filteredDonations.size} प्रविष्टियां", fontSize = 11.sp, color = TextSecondaryGreen)
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("दानदाता नाम, मद या रसीद संदर्भ खोजें...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldGreen) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Purpose Filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(purposeFilters) { p ->
                        FilterChip(
                            selected = selectedPurposeFilter == p,
                            onClick = { selectedPurposeFilter = p },
                            label = { Text(p, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftMintContainer,
                                selectedLabelColor = PineGreenDark
                            )
                        )
                    }
                }
            }
        }

        // Donation Entries List
        if (filteredDonations.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("कोई चंदा प्रविष्टि नहीं मिली", color = TextSecondaryGreen)
                }
            }
        } else {
            items(filteredDonations, key = { it.id }) { donation ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MemberAvatar(name = donation.donorName, size = 36.dp, textSize = 14, colorIndex = 0)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = donation.donorName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimaryGreen
                                        )
                                        if (donation.verified) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Verified, contentDescription = "सत्यापित", tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Text(
                                        text = "${donation.purpose} • ${donation.paymentMode}",
                                        fontSize = 10.sp,
                                        color = TextSecondaryGreen
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${String.format("%,.0f", donation.amount)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = EmeraldGreen
                                )
                                Text(donation.date, fontSize = 10.sp, color = TextSecondaryGreen)
                            }
                        }

                        HorizontalDivider(color = BorderLightGreen, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "रसीद #: ${donation.transactionRef}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondaryGreen
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = {
                                        copyToClipboard(
                                            context,
                                            "Donation Receipt",
                                            "12 रबी-उल-अव्वल TTS कमेटी चंदा रसीद:\nदानदाता: ${donation.donorName}\nराशि: ₹${donation.amount}\nमद: ${donation.purpose}\nरसीद संख्या: ${donation.transactionRef}\nतारीख: ${donation.date}\nसत्यापन: ${if (donation.verified) "आधिकारिक रूप से सत्यापित" else "लंबित"}\nUPI: ak750258@icici"
                                        )
                                    }
                                ) {
                                    Text("रसीद साझा करें", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                }

                                if (isAdminLoggedIn) {
                                    IconButton(
                                        onClick = { onEditDonation(donation) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { donationToDelete = donation },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    }
                                }
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

    // Single item delete confirmation dialog
    if (donationToDelete != null) {
        AlertDialog(
            onDismissRequest = { donationToDelete = null },
            title = { Text("चंदा प्रविष्टि हटाएं (Delete Donation)", fontWeight = FontWeight.Bold) },
            text = {
                Text("क्या आप निश्चित रूप से '${donationToDelete?.donorName}' का ₹${donationToDelete?.amount?.toInt()} का चंदा रिकॉर्ड हटाना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = donationToDelete?.id
                        donationToDelete = null
                        if (id != null) {
                            onDeleteDonation(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("हाँ, हटाएं", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { donationToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("पुराना चंदा रिकॉर्ड हटाएं (Clear Records)", fontWeight = FontWeight.Bold) },
            text = {
                Text("क्या आप निश्चित रूप से पुराने चंदा रिकॉर्ड्स हटाकर नया बहीखाता आरंभ करना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        onClearOldDonations()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("हाँ, साफ़ करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }
}
