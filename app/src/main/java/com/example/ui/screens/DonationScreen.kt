package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.ui.components.MemberAvatar
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
import com.example.util.ImageUtils

const val OFFICIAL_UPI_ID = "ak750258@icici"

@Composable
fun DonationScreen(
    approvedDonations: List<Donation>,
    pendingDonations: List<Donation>,
    totalDonations: Double,
    expenses: List<Expense> = emptyList(),
    totalExpenses: Double = 0.0,
    remainingBalance: Double = 0.0,
    donationGoal: Double = 250000.0,
    isAdminLoggedIn: Boolean = false,
    onOpenAdminLogin: () -> Unit = {},
    onOpenEditGoal: () -> Unit = {},
    onOpenAddDonationModal: () -> Unit,
    onOpenAddExpenseModal: () -> Unit = {},
    onApproveDonation: (donation: Donation) -> Unit = {},
    onRejectDonation: (donation: Donation) -> Unit = {},
    onVerifyDonation: (donationId: Long, isVerified: Boolean) -> Unit = { _, _ -> },
    onEditDonation: (donation: Donation) -> Unit = {},
    onDeleteDonation: (donationId: Long) -> Unit = {},
    onClearOldDonations: () -> Unit = {},
    onEditExpense: (expense: Expense) -> Unit = {},
    onDeleteExpense: (expenseId: Long) -> Unit = {},
    onClearAllExpenses: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: चंदा संग्रह (Donations), 1: खर्च विवरण (Expenses)
    var selectedAmount by remember { mutableDoubleStateOf(1100.0) }
    var searchQuery by remember { mutableStateOf("") }
    var expenseSearchQuery by remember { mutableStateOf("") }
    var selectedPurposeFilter by remember { mutableStateOf("सभी मद (All)") }
    var selectedExpenseCategoryFilter by remember { mutableStateOf("सभी श्रेणियां (All)") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showClearExpensesConfirmDialog by remember { mutableStateOf(false) }
    var donationToDelete by remember { mutableStateOf<Donation?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var donationToReject by remember { mutableStateOf<Donation?>(null) }
    var selectedProofDonation by remember { mutableStateOf<Donation?>(null) }
    var selectedProofExpense by remember { mutableStateOf<Expense?>(null) }

    val safeGoal = if (donationGoal > 0) donationGoal else 250000.0
    val progress = (totalDonations / safeGoal).toFloat().coerceIn(0f, 1f)

    val quickAmounts = listOf(500.0, 1100.0, 2100.0, 5100.0, 11000.0)
    val purposeFilters = listOf("सभी मद (All)", "12 रबी-उल-अव्वल जुलूस", "आम लंगर-ए-गौसिया", "सजावट व रौशनी", "तबर्रुक व रसद")
    val expenseCategoryFilters = listOf("सभी श्रेणियां (All)", "लंगर व रसद", "लाइटिंग व डेकोरेशन", "माइक, टेंट व स्टेज", "परचम व तोरण द्वार", "प्रशासनिक व विविध")

    val filteredApprovedDonations = approvedDonations.filter { d ->
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

    val filteredExpenses = expenses.filter { e ->
        val matchesSearch = e.title.contains(expenseSearchQuery, ignoreCase = true) ||
                e.spentBy.contains(expenseSearchQuery, ignoreCase = true) ||
                (e.receiptRef?.contains(expenseSearchQuery, ignoreCase = true) == true) ||
                (e.remarks?.contains(expenseSearchQuery, ignoreCase = true) == true)

        val matchesCategory = when (selectedExpenseCategoryFilter) {
            "सभी श्रेणियां (All)" -> true
            else -> e.category.contains(selectedExpenseCategoryFilter) || selectedExpenseCategoryFilter.contains(e.category)
        }

        matchesSearch && matchesCategory
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MintBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "12 रबी-उल-अव्वल आय-व्यय व चंदा",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = TextPrimaryGreen
                    )
                    Text(
                        text = "पारदर्शी बहीखाता • चंदा संग्रह एवं खर्च लेजर",
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

        // Comprehensive Financial Overview Card (चंदा + खर्च + शुद्ध शेष बचत)
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
                    Text(
                        text = "📊 कमेटी बजट सारांश (Financial Summary)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryGreen
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total Approved Donation Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftMintContainer)
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("कुल चंदा", fontSize = 10.sp, color = TextSecondaryGreen, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "₹${String.format("%,.0f", totalDonations)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PineGreenDark
                                )
                                Text("${approvedDonations.size} रसीदें", fontSize = 9.sp, color = TextSecondaryGreen)
                            }
                        }

                        // Total Expenses Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF2F2))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("कुल खर्च", fontSize = 10.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "₹${String.format("%,.0f", totalExpenses)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFDC2626)
                                )
                                Text("${expenses.size} प्रविष्टियां", fontSize = 9.sp, color = Color(0xFF991B1B))
                            }
                        }

                        // Net Balance Box
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (remainingBalance >= 0) Color(0xFFECFDF5) else Color(0xFFFFF1F2))
                                .border(1.dp, if (remainingBalance >= 0) EmeraldGreen else Color(0xFFDC2626), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = if (remainingBalance >= 0) EmeraldGreen else Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("शेष बैलेंस", fontSize = 10.sp, color = if (remainingBalance >= 0) PineGreenDark else Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "₹${String.format("%,.0f", remainingBalance)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (remainingBalance >= 0) EmeraldGreen else Color(0xFFDC2626)
                                )
                                Text("शुद्ध बचत", fontSize = 9.sp, color = if (remainingBalance >= 0) TextSecondaryGreen else Color(0xFFDC2626))
                            }
                        }
                    }

                    // Progress towards Goal with Admin-Secure Edit Button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAF8))
                            .border(1.dp, BorderLightGreen, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🎯 चंदा संग्रह लक्ष्य: ₹${String.format(java.util.Locale.getDefault(), "%,.0f", safeGoal)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
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
                                            text = "लाइव सिंक",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PineGreenDark
                                        )
                                    }
                                }
                                Text(
                                    text = "प्राप्त: ₹${String.format(java.util.Locale.getDefault(), "%,.0f", totalDonations)} • ${(progress * 100).toInt()}% पूर्ण",
                                    fontSize = 10.sp,
                                    color = TextSecondaryGreen
                                )
                            }

                            // Admin Target Edit Action Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isAdminLoggedIn) PrimaryGreen else Color.White,
                                border = if (isAdminLoggedIn) null else BorderStroke(1.dp, BorderLightGreen),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isAdminLoggedIn) onOpenEditGoal() else onOpenAdminLogin()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isAdminLoggedIn) Icons.Default.Edit else Icons.Default.Lock,
                                        contentDescription = "लक्ष्य बदलें",
                                        tint = if (isAdminLoggedIn) Color.White else PrimaryGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAdminLoggedIn) "लक्ष्य बदलें" else "लक्ष्य (Admin)",
                                        fontSize = 10.sp,
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
                    }
                }
            }
        }

        // SUB TABS SELECTOR (चंदा संग्रह vs खर्च विवरण)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BorderLightGreen)
                )
            ) {
                TabRow(
                    selectedTabIndex = activeSubTab,
                    containerColor = Color.White,
                    contentColor = PrimaryGreen,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                            color = PrimaryGreen,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = activeSubTab == 0,
                        onClick = { activeSubTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "चंदा सूची (${approvedDonations.size})",
                                    fontWeight = if (activeSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )

                    Tab(
                        selected = activeSubTab == 1,
                        onClick = { activeSubTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "खर्च विवरण (${expenses.size})",
                                    fontWeight = if (activeSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                }
            }
        }

        // =========================================================================
        // TAB 1: DONATIONS (चंदा संग्रह एवं UPI)
        // =========================================================================
        if (activeSubTab == 0) {
            // UPI Payment & Chanda Entry Banner
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "12 रबी-उल-अव्वल सहयोग राशि (Direct UPI)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimaryGreen
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(OFFICIAL_UPI_ID, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = PineGreenDark)
                                IconButton(
                                    onClick = { copyToClipboard(context, "UPI ID", OFFICIAL_UPI_ID) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

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
                                        .border(1.dp, if (isSelected) PrimaryGreen else BorderLightGreen, RoundedCornerShape(10.dp))
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

            // Pending Donations Section (if any)
            if (pendingDonations.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF59E0B))
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "स्वीकृति हेतु लंबित चंदा (${pendingDonations.size})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = if (isAdminLoggedIn) "एडमिन स्वीकृति (Approve) के बाद ही मुख्य सूची व कुल योग में जुड़ेगा।"
                                            else "सदस्यों द्वारा दर्ज चंदा • व्यवस्थापक सत्यापन के पश्चात सार्वजनिक होगा।",
                                            fontSize = 10.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }

                                if (!isAdminLoggedIn) {
                                    TextButton(onClick = onOpenAdminLogin) {
                                        Text("एडमिन लॉगिन", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFFDE68A), thickness = 1.dp)

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                pendingDonations.forEach { pDonation ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        border = CardDefaults.outlinedCardBorder().copy(
                                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A))
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    MemberAvatar(name = pDonation.donorName, size = 32.dp, textSize = 13, colorIndex = 1)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = pDonation.donorName,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextPrimaryGreen
                                                        )
                                                        Text(
                                                            text = "${pDonation.purpose} • ${pDonation.date}",
                                                            fontSize = 10.sp,
                                                            color = TextSecondaryGreen
                                                        )
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "₹${String.format("%,.0f", pDonation.amount)}",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 16.sp,
                                                        color = Color(0xFFD97706)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color(0xFFFEF3C7))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("⏳ लंबित (Pending)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "रसीद/UTR: ${pDonation.transactionRef}",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = TextSecondaryGreen
                                                )

                                                if (!pDonation.paymentProofUri.isNullOrBlank()) {
                                                    TextButton(
                                                        onClick = { selectedProofDonation = pDonation },
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryGreen)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("स्क्रीनशॉट देखें", fontSize = 10.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }

                                            if (isAdminLoggedIn) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(
                                                        onClick = { donationToReject = pDonation },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                                                    ) {
                                                        Text("अस्वीकार (Reject)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Button(
                                                        onClick = { onApproveDonation(pDonation) },
                                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                                        modifier = Modifier.height(34.dp)
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("स्वीकार करें (Approve)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Publish official list banner
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
                            text = "कमेटी द्वारा संकलित स्वीकृत चंदा सूची को सार्वजनिक रूप से व्हाट्सएप अथवा सूचना-पट्ट पर साझा करें।",
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
                                        appendLine("📋 *12 रबी-उल-अव्वल TTS कमेटी - आधिकारिक स्वीकृत चंदा सूची*")
                                        appendLine("आधिकारिक UPI: ak750258@icici")
                                        appendLine("कुल संग्रह: ₹${String.format("%,.0f", totalDonations)} (${approvedDonations.size} दानकर्ता)")
                                        appendLine("कुल खर्च: ₹${String.format("%,.0f", totalExpenses)}")
                                        appendLine("शुद्ध शेष बचत: ₹${String.format("%,.0f", remainingBalance)}")
                                        appendLine("----------------------------------")
                                        approvedDonations.forEachIndexed { index, d ->
                                            appendLine("${index + 1}. ${d.donorName} - ₹${d.amount.toInt()} (${d.purpose})")
                                        }
                                        appendLine("----------------------------------")
                                        appendLine("जज़ाकल्लाह ख़ैर • 12 रबी-उल-अव्वल कमेटी")
                                    }
                                    copyToClipboard(context, "Official Chanda List", listText)
                                    Toast.makeText(context, "स्वीकृत चंदा सूची कॉपी हो गई! सदस्यों में साझा करें।", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("publish_chanda_list_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("चंदा सूची साझा करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            }
                        }
                    }
                }
            }

            // Ledger Search & Filter (Approved Donations)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "स्वीकृत दानदाता सूची एवं रसीदें (Approved Ledger)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimaryGreen
                        )
                        Text("${filteredApprovedDonations.size} प्रविष्टियां", fontSize = 11.sp, color = TextSecondaryGreen)
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("नाम, रसीद संख्या, अथवा मद से खोजें...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryGreen) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryGreen)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Purpose Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(purposeFilters) { filter ->
                            val isSelected = selectedPurposeFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedPurposeFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Donations List Items
            if (filteredApprovedDonations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = TextSecondaryGreen, modifier = Modifier.size(40.dp))
                            Text("कोई स्वीकृत चंदा रिकॉर्ड उपलब्ध नहीं है", fontWeight = FontWeight.Bold, color = TextPrimaryGreen)
                            Text("नया चंदा दर्ज करने के लिए ऊपर दिए बटन पर क्लिक करें।", fontSize = 11.sp, color = TextSecondaryGreen)
                        }
                    }
                }
            } else {
                items(filteredApprovedDonations, key = { it.id }) { donation ->
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(Icons.Default.Verified, contentDescription = "सत्यापित", tint = EmeraldGreen, modifier = Modifier.size(14.dp))
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
                                    if (!donation.paymentProofUri.isNullOrBlank()) {
                                        TextButton(
                                            onClick = { selectedProofDonation = donation },
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(13.dp), tint = PrimaryGreen)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("पर्ची", fontSize = 10.sp, color = PrimaryGreen)
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            copyToClipboard(
                                                context,
                                                "Donation Receipt",
                                                "12 रबी-उल-अव्वल TTS कमेटी चंदा रसीद:\nदानदाता: ${donation.donorName}\nराशि: ₹${donation.amount}\nमद: ${donation.purpose}\nरसीद संख्या: ${donation.transactionRef}\nतारीख: ${donation.date}\nसत्यापन: आधिकारिक रूप से सत्यापित ✓\nUPI: ak750258@icici"
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
        }

        // =========================================================================
        // TAB 2: EXPENSES (खर्च विवरण एवं व्यय लेजर)
        // =========================================================================
        if (activeSubTab == 1) {
            // Action Banner to Add Expense
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "12 रबी-उल-अव्वल खर्च प्रविष्टि (Expense Entry)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "लंगर, टेंट, डेकोरेशन व माइक आदि के खर्च का पारदर्शी विवरण",
                                        fontSize = 10.sp,
                                        color = TextSecondaryGreen
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenAddExpenseModal,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("add_expense_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("नया खर्च जोड़ें (+ Expense)", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val expenseReport = buildString {
                                        appendLine("📊 *12 रबी-उल-अव्वल TTS कमेटी - आधिकारिक खर्च विवरण*")
                                        appendLine("कुल प्राप्त चंदा: ₹${String.format("%,.0f", totalDonations)}")
                                        appendLine("कुल व्यय (खर्च): ₹${String.format("%,.0f", totalExpenses)}")
                                        appendLine("शुद्ध शेष बचत: ₹${String.format("%,.0f", remainingBalance)}")
                                        appendLine("----------------------------------")
                                        expenses.forEachIndexed { index, exp ->
                                            appendLine("${index + 1}. ${exp.title} - ₹${exp.amount.toInt()} [${exp.category}] (व्ययकर्ता: ${exp.spentBy})")
                                        }
                                        appendLine("----------------------------------")
                                        appendLine("12 रबी-उल-अव्वल कमेटी • पारदर्शी बहीखाता")
                                    }
                                    copyToClipboard(context, "Official Expense Report", expenseReport)
                                    Toast.makeText(context, "खर्च रिपोर्ट कॉपी हो गई! साझा करें।", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("खर्च रिपोर्ट साझा करें", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isAdminLoggedIn && expenses.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { showClearExpensesConfirmDialog = true },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("सभी खर्च रिकॉर्ड साफ़ करें", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Expense Search & Filter
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "खर्च लेजर प्रविष्टियां (Expense Ledger)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimaryGreen
                        )
                        Text("${filteredExpenses.size} खर्च प्रविष्टियां", fontSize = 11.sp, color = TextSecondaryGreen)
                    }

                    OutlinedTextField(
                        value = expenseSearchQuery,
                        onValueChange = { expenseSearchQuery = it },
                        placeholder = { Text("मद, खर्चकर्ता, अथवा वाउचर संख्या से खोजें...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryGreen) },
                        trailingIcon = {
                            if (expenseSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { expenseSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondaryGreen)
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(expenseCategoryFilters) { filter ->
                            val isSelected = selectedExpenseCategoryFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedExpenseCategoryFilter = filter },
                                label = { Text(filter, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDC2626),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Expense List Items
            if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextSecondaryGreen, modifier = Modifier.size(40.dp))
                            Text("अभी कोई खर्च प्रविष्टि दर्ज नहीं की गई है", fontWeight = FontWeight.Bold, color = TextPrimaryGreen)
                            Text("नया खर्च दर्ज करने के लिए ऊपर 'नया खर्च जोड़ें' पर क्लिक करें।", fontSize = 11.sp, color = TextSecondaryGreen)
                        }
                    }
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                            .clip(CircleShape)
                                            .background(Color(0xFFFEE2E2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = expense.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimaryGreen
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SoftMintContainer)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(expense.category, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PineGreenDark)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "व्ययकर्ता: ${expense.spentBy}",
                                                fontSize = 10.sp,
                                                color = TextSecondaryGreen
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${String.format("%,.0f", expense.amount)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                    Text(expense.date, fontSize = 10.sp, color = TextSecondaryGreen)
                                }
                            }

                            if (!expense.remarks.isNullOrBlank()) {
                                Text(
                                    text = "टिप्पणी: ${expense.remarks}",
                                    fontSize = 10.sp,
                                    color = TextSecondaryGreen
                                )
                            }

                            HorizontalDivider(color = Color(0xFFFEE2E2), thickness = 0.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "वाउचर/बिल #: ${expense.receiptRef ?: "EXP-${expense.id}"}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondaryGreen
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!expense.attachmentUri.isNullOrBlank()) {
                                        TextButton(
                                            onClick = { selectedProofExpense = expense },
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(13.dp), tint = PrimaryGreen)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("बिल फोटो", fontSize = 10.sp, color = PrimaryGreen)
                                        }
                                    }

                                    TextButton(
                                        onClick = {
                                            copyToClipboard(
                                                context,
                                                "Expense Voucher",
                                                "12 रबी-उल-अव्वल TTS कमेटी खर्च वाउचर:\nमद: ${expense.title}\nश्रेणी: ${expense.category}\nराशि: ₹${expense.amount}\nव्ययकर्ता: ${expense.spentBy}\nवाउचर #: ${expense.receiptRef ?: "N/A"}\nतारीख: ${expense.date}"
                                            )
                                        }
                                    ) {
                                        Text("वाउचर साझा करें", fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                                    }

                                    if (isAdminLoggedIn) {
                                        IconButton(
                                            onClick = { onEditExpense(expense) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { expenseToDelete = expense },
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
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Payment Proof Preview Dialog (Donations)
    if (selectedProofDonation != null) {
        val pDon = selectedProofDonation!!
        Dialog(onDismissRequest = { selectedProofDonation = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("भुगतान का प्रमाण (ScreenShot)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryGreen)
                    Text("दानदाता: ${pDon.donorName} • ₹${pDon.amount.toInt()}", fontSize = 11.sp, color = TextSecondaryGreen)

                    val bitmap: Bitmap? = remember(pDon.paymentProofUri) {
                        ImageUtils.getBitmapFromPhotoUri(pDon.paymentProofUri)
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Proof",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(SoftMintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("प्रमाण पूर्वावलोकन उपलब्ध नहीं", color = TextSecondaryGreen, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { selectedProofDonation = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("बंद करें (Close)", color = Color.White)
                    }
                }
            }
        }
    }

    // Expense Proof / Bill Preview Dialog
    if (selectedProofExpense != null) {
        val pExp = selectedProofExpense!!
        Dialog(onDismissRequest = { selectedProofExpense = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("खर्च / बिल पर्ची की फोटो", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimaryGreen)
                    Text("मद: ${pExp.title} • ₹${pExp.amount.toInt()}", fontSize = 11.sp, color = TextSecondaryGreen)

                    val expBitmap: Bitmap? = remember(pExp.attachmentUri) {
                        ImageUtils.getBitmapFromPhotoUri(pExp.attachmentUri)
                    }

                    if (expBitmap != null) {
                        Image(
                            bitmap = expBitmap.asImageBitmap(),
                            contentDescription = "Bill Photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(SoftMintContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("बिल पर्ची उपलब्ध नहीं", color = TextSecondaryGreen, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { selectedProofExpense = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("बंद करें (Close)", color = Color.White)
                    }
                }
            }
        }
    }

    // Expense Delete Confirmation
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("खर्च रिकॉर्ड हटाएं (Delete Expense)", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = {
                Text("क्या आप निश्चित रूप से '${expenseToDelete?.title}' का ₹${expenseToDelete?.amount?.toInt()} का खर्च रिकॉर्ड हटाना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = expenseToDelete?.id
                        expenseToDelete = null
                        if (id != null) {
                            onDeleteExpense(id)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("हाँ, हटाएं", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Clear All Expenses Dialog
    if (showClearExpensesConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearExpensesConfirmDialog = false },
            title = { Text("सभी खर्च रिकॉर्ड हटाएं (Clear All Expenses)", fontWeight = FontWeight.Bold) },
            text = {
                Text("क्या आप निश्चित रूप से सभी खर्च रिकॉर्ड्स हटाकर नया व्यय बहीखाता आरंभ करना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearExpensesConfirmDialog = false
                        onClearAllExpenses()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("हाँ, साफ़ करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearExpensesConfirmDialog = false }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Decline / Reject Confirmation Dialog (Admin)
    if (donationToReject != null) {
        AlertDialog(
            onDismissRequest = { donationToReject = null },
            title = { Text("चंदा अस्वीकार करें (Decline Donation)", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = {
                Text("क्या आप निश्चित रूप से '${donationToReject?.donorName}' का ₹${donationToReject?.amount?.toInt()} का लंबित चंदा अस्वीकार (Decline) कर हटाना चाहते हैं?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = donationToReject
                        donationToReject = null
                        if (d != null) {
                            onRejectDonation(d)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("हाँ, अस्वीकार करें", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { donationToReject = null }) {
                    Text("रद्द करें")
                }
            }
        )
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
