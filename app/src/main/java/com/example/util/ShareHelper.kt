package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import java.net.URLEncoder

object ShareHelper {

    fun shareText(context: Context, title: String, text: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val chooser = Intent.createChooser(sendIntent, title).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "शेयर करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareToWhatsApp(context: Context, text: String, phoneNumber: String? = null) {
        try {
            val cleanPhone = phoneNumber?.replace("[^0-9]".toRegex(), "")
            if (!cleanPhone.isNullOrBlank()) {
                val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
                val encodedText = URLEncoder.encode(text, "UTF-8")
                val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedText")
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }

            // Target WhatsApp app specifically if possible, else fallback to standard chooser
            val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                `package` = "com.whatsapp"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            // If WhatsApp not directly installed or failed, fallback to general share chooser
            shareText(context, "WhatsApp / अन्य माध्यम से शेयर करें", text)
        }
    }

    fun formatDonationReceipt(donation: Donation): String {
        return buildString {
            appendLine("📜 *12 रबी-उल-अव्वल TTS कमेटी • आधिकारिक चंदा रसीद*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("👤 *दानदाता:* ${donation.donorName}")
            appendLine("💰 *राशि:* ₹${donation.amount.toInt()} (₹${String.format(java.util.Locale.ENGLISH, "%,.2f", donation.amount)})")
            appendLine("🏷️ *मद / उद्देश्य:* ${donation.purpose}")
            appendLine("🧾 *रसीद संख्या:* ${donation.transactionRef}")
            appendLine("📅 *दिनांक:* ${donation.date}")
            appendLine("💳 *भुगतान माध्यम:* ${donation.paymentMode}")
            appendLine("✅ *सत्यापन:* आधिकारिक रूप से सत्यापित ✓")
            appendLine("🏦 *कमेटी UPI ID:* ak750258@icici")
            if (!donation.remarks.isNullOrBlank()) {
                appendLine("📝 *विशेष टिप्पणी:* ${donation.remarks}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🤲 *दुआ:* अल्लाह तआला आपके इस तआवुन व सदक़े को अपनी बारगाह में क़बूल फ़रमाए। आमीन!")
            appendLine("🌐 *तहफ्फुज़-ए-तहज़ीब वेलफेयर सोसाइटी (TTS)*")
        }
    }

    fun shareDonationReceipt(context: Context, donation: Donation, toWhatsApp: Boolean = false) {
        val text = formatDonationReceipt(donation)
        if (toWhatsApp) {
            shareToWhatsApp(context, text)
        } else {
            shareText(context, "12 रबी-उल-अव्वल चंदा रसीद - ${donation.donorName}", text)
        }
    }

    fun formatMemberIDCard(member: Member): String {
        return buildString {
            appendLine("🆔 *12 रबी-उल-अव्वल TTS कमेटी • डिजिटल पहचान पत्र*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("👤 *नाम:* ${member.fullName}")
            appendLine("🎖️ *अधिकृत पद:* ${member.designation}")
            appendLine("🏛️ *विभाग/विंग:* ${member.committeeWing}")
            appendLine("🆔 *सदस्य कोड:* ${member.memberCode}")
            appendLine("📞 *मोबाइल नंबर:* ${member.phoneNumber}")
            if (member.emergencyContact.isNotBlank()) {
                appendLine("🚨 *आपातकालीन संपर्क:* ${member.emergencyContact}")
            }
            if (member.address.isNotBlank()) {
                appendLine("📍 *पता/इलाका:* ${member.address}")
            }
            appendLine("📅 *सत्र:* 12 रबी-उल-अव्वल 1447H")
            appendLine("💳 *चंदा UPI:* ak750258@icici")
            if (member.isBestPerformer) {
                appendLine("⭐ *विशेष सम्मान:* ${member.bestPerformerBadge ?: "विशेष खिदमतगार"}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("✅ *तहफ्फुज़-ए-तहज़ीब वेलफेयर सोसाइटी (TTS) द्वारा अधिकृत*")
        }
    }

    fun shareMemberIDCard(context: Context, member: Member, toWhatsApp: Boolean = false) {
        val text = formatMemberIDCard(member)
        if (toWhatsApp) {
            shareToWhatsApp(context, text, member.phoneNumber)
        } else {
            shareText(context, "TTS डिजिटल आईडी कार्ड - ${member.fullName}", text)
        }
    }

    fun shareNotice(context: Context, notice: Notice, toWhatsApp: Boolean = false) {
        val text = buildString {
            appendLine("📢 *12 रबी-उल-अव्वल TTS कमेटी • आधिकारिक सूचना*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📌 *शीर्षक:* ${notice.title}")
            appendLine("🏷️ *श्रेणी:* ${notice.category} | प्राथमिकता: ${notice.priority}")
            appendLine("📅 *दिनांक:* ${notice.date} | जारीकर्ता: ${notice.issuedBy}")
            appendLine()
            appendLine(notice.content)
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("🌐 *तहफ्फुज़-ए-तहज़ीब वेलफेयर सोसाइटी (TTS)*")
        }
        if (toWhatsApp) {
            shareToWhatsApp(context, text)
        } else {
            shareText(context, "कमेटी सूचना - ${notice.title}", text)
        }
    }

    fun shareMeeting(context: Context, meeting: Meeting, toWhatsApp: Boolean = false) {
        val text = buildString {
            appendLine("🤝 *12 रबी-उल-अव्वल TTS कमेटी • बैठक निमंत्रण*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("📌 *विषय:* ${meeting.title}")
            appendLine("📅 *दिनांक व समय:* ${meeting.dateDisplay} (${meeting.timeDisplay})")
            appendLine("📍 *स्थान / वेन्यू:* ${meeting.venue}")
            appendLine("👤 *अध्यक्षता:* ${meeting.chairperson}")
            if (meeting.agenda.isNotBlank()) {
                appendLine("📋 *एजेंडा:* ${meeting.agenda}")
            }
            if (!meeting.virtualLink.isNullOrBlank()) {
                appendLine("🔗 *ऑनलाइन लिंक:* ${meeting.virtualLink}")
            }
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("तमाम कमेटी अहबाब व जिम्मेदारान से समय पर तशरीफ़ लाने की गुजारिश है।")
            appendLine("🌐 *तहफ्फुज़-ए-तहज़ीब वेलफेयर सोसाइटी (TTS)*")
        }
        if (toWhatsApp) {
            shareToWhatsApp(context, text)
        } else {
            shareText(context, "बैठक सूचना - ${meeting.title}", text)
        }
    }

    fun shareBudgetReport(
        context: Context,
        totalDonations: Double,
        totalExpenses: Double,
        donorCount: Int,
        expenseCount: Int,
        toWhatsApp: Boolean = false
    ) {
        val balance = totalDonations - totalExpenses
        val text = buildString {
            appendLine("📊 *12 रबी-उल-अव्वल TTS कमेटी • आय-व्यय व बजट रिपोर्ट*")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("💰 *कुल प्राप्त चंदा:* ₹${String.format(java.util.Locale.ENGLISH, "%,.2f", totalDonations)} (${donorCount} दानदाता)")
            appendLine("💸 *कुल खर्च:* ₹${String.format(java.util.Locale.ENGLISH, "%,.2f", totalExpenses)} (${expenseCount} मदें)")
            appendLine("⚖️ *शेष बैलेंस:* ₹${String.format(java.util.Locale.ENGLISH, "%,.2f", balance)}")
            appendLine("💳 *आधिकारिक UPI ID:* ak750258@icici")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("पारदर्शी हिसाब • तहफ्फुज़-ए-तहज़ीब वेलफेयर सोसाइटी (TTS)")
        }
        if (toWhatsApp) {
            shareToWhatsApp(context, text)
        } else {
            shareText(context, "12 रबी-उल-अव्वल बजट रिपोर्ट", text)
        }
    }
}
