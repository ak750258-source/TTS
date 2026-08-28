package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberCode: String,
    val fullName: String,
    val designation: String, // e.g. "संस्थापक (Founder)", "कोषाध्यक्ष (Treasurer)", "सदर (President)", "नायब सदर (Vice President)", "जनरल सेक्रेटरी (General Secretary)", "व्यवस्थापक (Organizer)", "खादिम (Volunteer)"
    val committeeWing: String, // e.g. "मुख्य प्रबंधक बोर्ड", "12 रबी-उल-अव्वल जुलूस कमेटी", "लंगर-ए-पाक व्यवस्था", "चंदा एवं वित्तीय समिति", "युवा विंग"
    val phoneNumber: String,
    val email: String,
    val bloodGroup: String = "", // Kept in model for legacy, removed from ID card rendering
    val joinDate: String, // e.g. "12 रबी-उल-अव्वल 1447 हिजरी / 2026"
    val address: String,
    val emergencyContact: String,
    val avatarColorIndex: Int = 0,
    val isActive: Boolean = true,
    val isBestPerformer: Boolean = false,
    val bestPerformerBadge: String? = null, // e.g. "सर्वश्रेष्ठ खिदमतगार 1447H", "जुलूस-ए-मोहम्मदी स्टार वॉलंटियर", "उत्कृष्ट लंगर सेवा"
    val photoResName: String? = null, // e.g. "img_best_performer"
    val photoUri: String? = null // Device gallery image URI or content path
)

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String, // "12 रबी-उल-अव्वल मुख्य बैठक", "जुलूस प्रबंध सभा", "चंदा व हिसाब बैठक", "आपातकालीन सत्र"
    val dateDisplay: String,
    val timeDisplay: String,
    val dateTimeMillis: Long,
    val venue: String,
    val virtualLink: String? = null,
    val chairperson: String,
    val agenda: String,
    val status: String = "Upcoming", // "Upcoming", "In Progress", "Completed", "Postponed"
    val notesOrMinutes: String? = null
)

@Entity(tableName = "documents")
data class OfficialDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "कमेटी नियमावली", "जुलूस मार्ग एवं अनुमति", "लेखा व चंदा ऑडिट", "प्रस्ताव व फैसले"
    val refCode: String,
    val publishedDate: String,
    val fileSize: String,
    val accessLevel: String, // "All Members", "Executive Only", "Confidential"
    val summary: String,
    val fullContent: String,
    val attachmentUri: String? = null, // Document attachment (scanned PDF/image/base64)
    val attachmentName: String? = null
)

@Entity(tableName = "notices")
data class Notice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String, // "अति आवश्यक", "12 रबी-उल-अव्वल", "जुलूस-ए-मोहम्मदी", "लंगर-ए-आम", "चंदा सूचना"
    val priority: String, // "HIGH", "NORMAL"
    val issuedBy: String,
    val date: String,
    val content: String,
    val isPinned: Boolean = false
)

@Entity(tableName = "donations")
data class Donation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val donorName: String,
    val donorMemberCode: String? = null,
    val amount: Double,
    val purpose: String, // "12 रबी-उल-अव्वल जलसा व सजावट", "लंगर-ए-पाक (भोजन व्यवस्था)", "जुलूस-ए-मोहम्मदी इंतजाम", "कमेटी कल्याण कोष"
    val paymentMode: String = "UPI (ak750258@icici)",
    val transactionRef: String,
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val verified: Boolean = true, // true = Approved & Published, false = Pending Admin Approval
    val remarks: String? = null,
    val paymentProofUri: String? = null // Attachment proof of UPI payment
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // खर्च का नाम / मद (e.g. लंगर राशन, साउंड व स्टेज, झंडे व तोरण, लाइट व्यवस्था)
    val category: String, // "लंगर-ए-पाक", "स्टेज व साउंड", "डेकोरेशन व रोशनी", "जुलूस इंतजाम", "प्रशासनिक व विविध"
    val amount: Double, // खर्च की राशि
    val spentBy: String, // खर्चकर्ता / जिम्मेदार पदाधिकारी
    val date: String, // खर्च की तारीख
    val timestamp: Long = System.currentTimeMillis(),
    val receiptRef: String? = null, // बिल / वाउचर संख्या
    val attachmentUri: String? = null, // बिल / पर्ची की फोटो (Base64 / URI)
    val remarks: String? = null
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val channelId: String = "general", // "general", "rabi_ul_awwal", "donations"
    val senderName: String,
    val senderRole: String,
    val senderAvatarIndex: Int = 0,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeDisplay: String,
    val isAnnouncement: Boolean = false
)
