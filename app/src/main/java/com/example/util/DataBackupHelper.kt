package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.firebase.FirebaseFirestoreService
import com.example.data.local.TTSDao
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataBackupHelper {

    /**
     * Serializes all application data from SQLite Room to a clean JSON string.
     */
    suspend fun exportAllDataAsJson(ttsDao: TTSDao, donationGoal: Double): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportTime", System.currentTimeMillis())
        root.put("committeeName", "12 Rabi ul Awwal TTS Committee")
        root.put("donationGoal", donationGoal)

        // Members
        val membersList = ttsDao.getAllMembersList()
        val membersArray = JSONArray()
        for (m in membersList) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("memberCode", m.memberCode)
                put("fullName", m.fullName)
                put("designation", m.designation)
                put("committeeWing", m.committeeWing)
                put("phoneNumber", m.phoneNumber)
                put("email", m.email)
                put("bloodGroup", m.bloodGroup)
                put("joinDate", m.joinDate)
                put("address", m.address)
                put("emergencyContact", m.emergencyContact)
                put("avatarColorIndex", m.avatarColorIndex)
                put("isActive", m.isActive)
                put("isBestPerformer", m.isBestPerformer)
                put("bestPerformerBadge", m.bestPerformerBadge ?: "")
                put("photoResName", m.photoResName ?: "")
                put("photoUri", m.photoUri ?: "")
            }
            membersArray.put(obj)
        }
        root.put("members", membersArray)

        // Donations
        val donationsList = ttsDao.getAllDonationsList()
        val donationsArray = JSONArray()
        for (d in donationsList) {
            val obj = JSONObject().apply {
                put("id", d.id)
                put("donorName", d.donorName)
                put("donorMemberCode", d.donorMemberCode ?: "")
                put("amount", d.amount)
                put("purpose", d.purpose)
                put("paymentMode", d.paymentMode)
                put("transactionRef", d.transactionRef)
                put("date", d.date)
                put("timestamp", d.timestamp)
                put("verified", d.verified)
                put("remarks", d.remarks ?: "")
                put("paymentProofUri", d.paymentProofUri ?: "")
            }
            donationsArray.put(obj)
        }
        root.put("donations", donationsArray)

        // Expenses
        val expensesList = ttsDao.getAllExpensesList()
        val expensesArray = JSONArray()
        for (e in expensesList) {
            val obj = JSONObject().apply {
                put("id", e.id)
                put("title", e.title)
                put("category", e.category)
                put("amount", e.amount)
                put("spentBy", e.spentBy)
                put("date", e.date)
                put("timestamp", e.timestamp)
                put("receiptRef", e.receiptRef ?: "")
                put("attachmentUri", e.attachmentUri ?: "")
                put("remarks", e.remarks ?: "")
            }
            expensesArray.put(obj)
        }
        root.put("expenses", expensesArray)

        // Meetings
        val meetingsList = ttsDao.getAllMeetingsList()
        val meetingsArray = JSONArray()
        for (m in meetingsList) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("type", m.type)
                put("dateDisplay", m.dateDisplay)
                put("timeDisplay", m.timeDisplay)
                put("dateTimeMillis", m.dateTimeMillis)
                put("venue", m.venue)
                put("virtualLink", m.virtualLink ?: "")
                put("chairperson", m.chairperson)
                put("agenda", m.agenda)
                put("status", m.status)
                put("notesOrMinutes", m.notesOrMinutes ?: "")
            }
            meetingsArray.put(obj)
        }
        root.put("meetings", meetingsArray)

        // Notices
        val noticesList = ttsDao.getAllNoticesList()
        val noticesArray = JSONArray()
        for (n in noticesList) {
            val obj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("category", n.category)
                put("priority", n.priority)
                put("issuedBy", n.issuedBy)
                put("date", n.date)
                put("content", n.content)
                put("isPinned", n.isPinned)
            }
            noticesArray.put(obj)
        }
        root.put("notices", noticesArray)

        // Documents
        val docsList = ttsDao.getAllDocumentsList()
        val docsArray = JSONArray()
        for (doc in docsList) {
            val obj = JSONObject().apply {
                put("id", doc.id)
                put("title", doc.title)
                put("category", doc.category)
                put("refCode", doc.refCode)
                put("publishedDate", doc.publishedDate)
                put("fileSize", doc.fileSize)
                put("accessLevel", doc.accessLevel)
                put("summary", doc.summary)
                put("fullContent", doc.fullContent)
                put("attachmentUri", doc.attachmentUri ?: "")
                put("attachmentName", doc.attachmentName ?: "")
            }
            docsArray.put(obj)
        }
        root.put("documents", docsArray)

        return@withContext root.toString(2)
    }

    /**
     * Imports and merges JSON backup data into SQLite Room and triggers real-time cloud sync.
     */
    suspend fun importDataFromJson(
        jsonString: String,
        ttsDao: TTSDao,
        firestoreService: FirebaseFirestoreService?
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString.trim())
            var countMembers = 0
            var countDonations = 0
            var countExpenses = 0
            var countNotices = 0
            var countMeetings = 0
            var countDocs = 0

            // 1. Members
            if (root.has("members")) {
                val array = root.getJSONArray("members")
                val members = mutableListOf<Member>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optLong("id", System.currentTimeMillis() + i)
                    val m = Member(
                        id = id,
                        memberCode = obj.optString("memberCode", "TTS-$id"),
                        fullName = obj.optString("fullName", "सदस्य"),
                        designation = obj.optString("designation", "खादिम"),
                        committeeWing = obj.optString("committeeWing", "12 रबी-उल-अव्वल"),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        email = obj.optString("email", ""),
                        bloodGroup = obj.optString("bloodGroup", ""),
                        joinDate = obj.optString("joinDate", "12 रबी-उल-अव्वल"),
                        address = obj.optString("address", ""),
                        emergencyContact = obj.optString("emergencyContact", ""),
                        avatarColorIndex = obj.optInt("avatarColorIndex", 0),
                        isActive = obj.optBoolean("isActive", true),
                        isBestPerformer = obj.optBoolean("isBestPerformer", false),
                        bestPerformerBadge = obj.optString("bestPerformerBadge").takeIf { it.isNotBlank() },
                        photoResName = obj.optString("photoResName").takeIf { it.isNotBlank() },
                        photoUri = obj.optString("photoUri").takeIf { it.isNotBlank() }
                    )
                    members.add(m)
                }
                if (members.isNotEmpty()) {
                    ttsDao.insertMembers(members)
                    countMembers = members.size
                    members.forEach { firestoreService?.syncMemberToCloud(it) }
                }
            }

            // 2. Donations
            if (root.has("donations")) {
                val array = root.getJSONArray("donations")
                val donations = mutableListOf<Donation>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optLong("id", System.currentTimeMillis() + i)
                    val d = Donation(
                        id = id,
                        donorName = obj.optString("donorName", "दाता"),
                        donorMemberCode = obj.optString("donorMemberCode").takeIf { it.isNotBlank() },
                        amount = obj.optDouble("amount", 0.0),
                        purpose = obj.optString("purpose", "12 रबी-उल-अव्वल"),
                        paymentMode = obj.optString("paymentMode", "UPI (ak750258@icici)"),
                        transactionRef = obj.optString("transactionRef", "REC-$id"),
                        date = obj.optString("date", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        verified = obj.optBoolean("verified", true),
                        remarks = obj.optString("remarks").takeIf { it.isNotBlank() },
                        paymentProofUri = obj.optString("paymentProofUri").takeIf { it.isNotBlank() }
                    )
                    donations.add(d)
                }
                if (donations.isNotEmpty()) {
                    ttsDao.insertDonations(donations)
                    countDonations = donations.size
                    donations.forEach { firestoreService?.syncDonationToCloud(it) }
                }
            }

            // 3. Expenses
            if (root.has("expenses")) {
                val array = root.getJSONArray("expenses")
                val expenses = mutableListOf<Expense>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optLong("id", System.currentTimeMillis() + i)
                    val e = Expense(
                        id = id,
                        title = obj.optString("title", "खर्च"),
                        category = obj.optString("category", "लंगर-ए-पाक"),
                        amount = obj.optDouble("amount", 0.0),
                        spentBy = obj.optString("spentBy", "कमेटी व्यवस्थापक"),
                        date = obj.optString("date", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        receiptRef = obj.optString("receiptRef").takeIf { it.isNotBlank() },
                        attachmentUri = obj.optString("attachmentUri").takeIf { it.isNotBlank() },
                        remarks = obj.optString("remarks").takeIf { it.isNotBlank() }
                    )
                    expenses.add(e)
                }
                if (expenses.isNotEmpty()) {
                    ttsDao.insertExpenses(expenses)
                    countExpenses = expenses.size
                    expenses.forEach { firestoreService?.syncExpenseToCloud(it) }
                }
            }

            // 4. Meetings
            if (root.has("meetings")) {
                val array = root.getJSONArray("meetings")
                val meetings = mutableListOf<Meeting>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optLong("id", System.currentTimeMillis() + i)
                    val m = Meeting(
                        id = id,
                        title = obj.optString("title", "बैठक"),
                        type = obj.optString("type", "मुख्य बैठक"),
                        dateDisplay = obj.optString("dateDisplay", ""),
                        timeDisplay = obj.optString("timeDisplay", ""),
                        dateTimeMillis = obj.optLong("dateTimeMillis", System.currentTimeMillis()),
                        venue = obj.optString("venue", "कमेटी कार्यालय"),
                        virtualLink = obj.optString("virtualLink").takeIf { it.isNotBlank() },
                        chairperson = obj.optString("chairperson", "अध्यक्ष"),
                        agenda = obj.optString("agenda", ""),
                        status = obj.optString("status", "Upcoming"),
                        notesOrMinutes = obj.optString("notesOrMinutes").takeIf { it.isNotBlank() }
                    )
                    meetings.add(m)
                }
                if (meetings.isNotEmpty()) {
                    ttsDao.insertMeetings(meetings)
                    countMeetings = meetings.size
                    meetings.forEach { firestoreService?.syncMeetingToCloud(it) }
                }
            }

            // 5. Notices
            if (root.has("notices")) {
                val array = root.getJSONArray("notices")
                val notices = mutableListOf<Notice>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optLong("id", System.currentTimeMillis() + i)
                    val n = Notice(
                        id = id,
                        title = obj.optString("title", "सूचना"),
                        category = obj.optString("category", "सामान्य"),
                        priority = obj.optString("priority", "NORMAL"),
                        issuedBy = obj.optString("issuedBy", "TTS कमेटी"),
                        date = obj.optString("date", ""),
                        content = obj.optString("content", ""),
                        isPinned = obj.optBoolean("isPinned", false)
                    )
                    notices.add(n)
                }
                if (notices.isNotEmpty()) {
                    ttsDao.insertNotices(notices)
                    countNotices = notices.size
                    notices.forEach { firestoreService?.syncNoticeToCloud(it) }
                }
            }

            // 6. Documents
            if (root.has("documents")) {
                val array = root.getJSONArray("documents")
                val docs = mutableListOf<OfficialDocument>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optLong("id", System.currentTimeMillis() + i)
                    val d = OfficialDocument(
                        id = id,
                        title = obj.optString("title", "दस्तावेज़"),
                        category = obj.optString("category", "कमेटी नियमावली"),
                        refCode = obj.optString("refCode", "DOC-$id"),
                        publishedDate = obj.optString("publishedDate", ""),
                        fileSize = obj.optString("fileSize", "सत्यापित प्रति"),
                        accessLevel = obj.optString("accessLevel", "सार्वजनिक (Public)"),
                        summary = obj.optString("summary", ""),
                        fullContent = obj.optString("fullContent", ""),
                        attachmentUri = obj.optString("attachmentUri").takeIf { it.isNotBlank() },
                        attachmentName = obj.optString("attachmentName").takeIf { it.isNotBlank() }
                    )
                    docs.add(d)
                }
                if (docs.isNotEmpty()) {
                    ttsDao.insertDocuments(docs)
                    countDocs = docs.size
                    docs.forEach { firestoreService?.syncDocumentToCloud(it) }
                }
            }

            // 7. Donation Goal
            if (root.has("donationGoal")) {
                val goal = root.optDouble("donationGoal", 250000.0)
                firestoreService?.syncDonationGoalToCloud(goal)
            }

            val summary = "✅ $countMembers सदस्य, $countDonations चंदा रसीदें, $countExpenses खर्च, $countNotices नोटिस और $countMeetings बैठकें सफलतापूर्वक लोड और सिंक हो गए!"
            return@withContext Pair(true, summary)
        } catch (e: Exception) {
            return@withContext Pair(false, "डेटा लोड करने में त्रुटि: ${e.localizedMessage}")
        }
    }

    /**
     * Shares the complete backup JSON file via WhatsApp or Android Share Sheet.
     */
    fun shareBackupFile(context: Context, jsonString: String, toWhatsApp: Boolean = false) {
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val filename = "TTS_12Rabi_Backup_$dateStr.json"
            val backupDir = File(context.cacheDir, "backups")
            backupDir.mkdirs()
            val backupFile = File(backupDir, filename)

            FileOutputStream(backupFile).use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val fileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "12 Rabi-ul-Awwal TTS Committee Full Backup")
                putExtra(
                    Intent.EXTRA_TEXT,
                    """
                        🕋 12 रबी-उल-अव्वल TTS कमेटी • सम्पूर्ण डेटा बैकअप फ़ाइल
                        
                        इस बैकअप फाइल में सदस्य सूची, चंदा रिकॉर्ड, खर्च बहीखाता, नोटिस एवं बैठकें शामिल हैं।
                        दूसरे फोन के TTS ऐप में "डेटा लोड / रीस्टोर" मेनू से इस फाइल को सेलेक्ट करें।
                        
                        कमेटी UPI ID: ak750258@icici
                    """.trimIndent()
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (toWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }

            val chooser = if (toWhatsApp) shareIntent else Intent.createChooser(shareIntent, "कमेटी डेटा बैकअप फाइल शेयर करें")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            if (toWhatsApp) {
                // Fallback to general share
                shareBackupFile(context, jsonString, false)
            } else {
                Toast.makeText(context, "शेयरिंग में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
