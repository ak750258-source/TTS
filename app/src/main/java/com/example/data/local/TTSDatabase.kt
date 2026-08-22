package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ChatMessage
import com.example.data.model.Donation
import com.example.data.model.Meeting
import com.example.data.model.Member
import com.example.data.model.Notice
import com.example.data.model.OfficialDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Member::class,
        Meeting::class,
        OfficialDocument::class,
        Notice::class,
        Donation::class,
        ChatMessage::class
    ],
    version = 2,
    exportSchema = false
)
abstract class TTSDatabase : RoomDatabase() {
    abstract fun ttsDao(): TTSDao

    companion object {
        @Volatile
        private var INSTANCE: TTSDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): TTSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TTSDatabase::class.java,
                    "tts_committee_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(TTSDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class TTSDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.ttsDao())
                }
            }
        }

        private suspend fun populateInitialData(dao: TTSDao) {
            // Initial Members for 12 Rabi-Ul-Awwal Committee
            val initialMembers = listOf(
                Member(
                    id = 1,
                    memberCode = "TTS-1447-001",
                    fullName = "हाजी मोहम्मद अशरफ",
                    designation = "संस्थापक (Founder)",
                    committeeWing = "मुख्य संरक्षक मंडल",
                    phoneNumber = "+91 98450 11223",
                    email = "founder@ttscommittee.org",
                    bloodGroup = "O+",
                    joinDate = "12 रबी-उल-अव्वल 1440 हिजरी",
                    address = "मस्जिद रोड, सेंट्रल चौक, वार्ड 12",
                    emergencyContact = "+91 98450 99887",
                    avatarColorIndex = 0,
                    isBestPerformer = true,
                    bestPerformerBadge = "मुख्य संस्थापक एवं लाइफटाइम खिदमतगार सम्मान",
                    photoResName = "img_best_performer"
                ),
                Member(
                    id = 2,
                    memberCode = "TTS-1447-002",
                    fullName = "जनाब गुलाम मुस्तफा",
                    designation = "सदर (President)",
                    committeeWing = "मुख्य प्रबंधक बोर्ड",
                    phoneNumber = "+91 94432 55667",
                    email = "president@ttscommittee.org",
                    bloodGroup = "A+",
                    joinDate = "12 रबी-उल-अव्वल 1442 हिजरी",
                    address = "नूरी नगर, मदीना मस्जिद लेन",
                    emergencyContact = "+91 94432 11223",
                    avatarColorIndex = 1,
                    isBestPerformer = true,
                    bestPerformerBadge = "जुलूस-ए-मोहम्मदी मुख्य संचालक 1447H",
                    photoResName = "img_best_performer"
                ),
                Member(
                    id = 3,
                    memberCode = "TTS-1447-003",
                    fullName = "हाफिज मोहम्मद ताहिर",
                    designation = "कोषाध्यक्ष (Treasurer)",
                    committeeWing = "चंदा एवं वित्तीय लेखा समिति",
                    phoneNumber = "+91 98840 77889",
                    email = "treasury@ttscommittee.org",
                    bloodGroup = "B+",
                    joinDate = "01 मुहर्रम 1443 हिजरी",
                    address = "मार्केट कॉम्प्लेक्स, ब्लॉक B-402",
                    emergencyContact = "+91 98840 33445",
                    avatarColorIndex = 2,
                    isBestPerformer = true,
                    bestPerformerBadge = "पारदर्शी चंदा प्रबंधन एवं वित्तीय सेवा सम्मान",
                    photoResName = "img_best_performer"
                ),
                Member(
                    id = 4,
                    memberCode = "TTS-1447-004",
                    fullName = "मौलाना अख्तर रज़ा कादरी",
                    designation = "जनरल सेक्रेटरी (General Secretary)",
                    committeeWing = "धार्मिक एवं जलसा संचालन समिति",
                    phoneNumber = "+91 97100 44332",
                    email = "secretary@ttscommittee.org",
                    bloodGroup = "AB+",
                    joinDate = "15 सफर 1444 हिजरी",
                    address = "गुलशन-ए-रज़ा कॉलोनी, हाउस 18",
                    emergencyContact = "+91 97100 88990",
                    avatarColorIndex = 3,
                    isBestPerformer = false,
                    bestPerformerBadge = null,
                    photoResName = null
                ),
                Member(
                    id = 5,
                    memberCode = "TTS-1447-005",
                    fullName = "सैय्यद इरफान अली",
                    designation = "नायब सदर (Vice President)",
                    committeeWing = "सुरक्षा व जुलूस व्यवस्था विंग",
                    phoneNumber = "+91 98401 22998",
                    email = "irfan.ali@ttscommittee.org",
                    bloodGroup = "O-",
                    joinDate = "10 रजब 1445 हिजरी",
                    address = "अहले सुन्नत नगर, स्ट्रीट 5",
                    emergencyContact = "+91 98401 66554",
                    avatarColorIndex = 4,
                    isBestPerformer = true,
                    bestPerformerBadge = "सर्वश्रेष्ठ व्यवस्थापक (12 रबी-उल-अव्वल)",
                    photoResName = "img_best_performer"
                ),
                Member(
                    id = 6,
                    memberCode = "TTS-1447-006",
                    fullName = "मोहम्मद तारिक अनवर",
                    designation = "मुख्य व्यवस्थापक (Langar Lead)",
                    committeeWing = "लंगर-ए-पाक व्यवस्था समिति",
                    phoneNumber = "+91 99201 88442",
                    email = "tariq.anwar@ttscommittee.org",
                    bloodGroup = "B-",
                    joinDate = "01 शाबान 1446 हिजरी",
                    address = "अल-नूर मंजिल, गुलजार स्ट्रीट",
                    emergencyContact = "+91 99201 33221",
                    avatarColorIndex = 5,
                    isBestPerformer = false,
                    bestPerformerBadge = null,
                    photoResName = null
                ),
                Member(
                    id = 7,
                    memberCode = "TTS-1447-007",
                    fullName = "मोहम्मद साकिब रज़ा",
                    designation = "खादिम-ए-कमेटी (Youth Volunteer)",
                    committeeWing = "युवा विंग एवं सोशल मीडिया",
                    phoneNumber = "+91 98230 44119",
                    email = "sakib.raza@ttscommittee.org",
                    bloodGroup = "A+",
                    joinDate = "10 मुहर्रम 1447 हिजरी",
                    address = "रज़ा चौक, नियर बड़ी मस्जिद",
                    emergencyContact = "+91 98230 77881",
                    avatarColorIndex = 0,
                    isBestPerformer = true,
                    bestPerformerBadge = "स्टार युवा खिदमतगार सम्मान",
                    photoResName = "img_best_performer"
                )
            )
            dao.insertMembers(initialMembers)

            // Initial Meetings in Hindi
            val initialMeetings = listOf(
                Meeting(
                    id = 1,
                    title = "12 रबी-उल-अव्वल जश्न-ए-विलादत मुख्य तैयारी बैठक",
                    type = "12 रबी-उल-अव्वल मुख्य बैठक",
                    dateDisplay = "12 रबी-उल-अव्वल तैयारी (कल शाम)",
                    timeDisplay = "08:00 PM - 10:30 PM",
                    dateTimeMillis = System.currentTimeMillis() + (1L * 24 * 3600 * 1000),
                    venue = "TTS सेंट्रल हॉल, जामा मस्जिद कॉम्प्लेक्स",
                    virtualLink = "https://meet.google.com/tts-milad-1447",
                    chairperson = "जनाब गुलाम मुस्तफा (सदर)",
                    agenda = "1. जुलूस-ए-मोहम्मदी का तयशुदा रूट व परमिशन समीक्षा।\n2. लंगर-ए-आम (भोजन वितरण) की व्यवस्था व वॉलिंटियर्स ड्यूटी।\n3. शहर में रोशनी, झंडे (परचम) व डेकोरेशन का मुआयना।\n4. चंदा वसूली की पारदर्शी रिपोर्ट कोषाध्यक्ष द्वारा पेश करना।",
                    status = "Upcoming"
                ),
                Meeting(
                    id = 2,
                    title = "लंगर-ए-पाक व तबर्रुक वितरण प्रबंध उप-समिति सभा",
                    type = "लंगर प्रबंध सभा",
                    dateDisplay = "26 Aug 2026",
                    timeDisplay = "05:00 PM - 07:00 PM",
                    dateTimeMillis = System.currentTimeMillis() + (4L * 24 * 3600 * 1000),
                    venue = "लंगर खाना हॉल, दरगाह रोड",
                    virtualLink = null,
                    chairperson = "मोहम्मद तारिक अनवर (व्यवस्थापक)",
                    agenda = "1. देगों व राशन सामग्री की खरीद व स्टॉक सत्यापन।\n2. सभी मोहल्लों में समय पर तबर्रुक पहुँचाने की व्यवस्था।\n3. सफाई व स्वच्छता टीम की तैनाती।",
                    status = "Upcoming"
                ),
                Meeting(
                    id = 3,
                    title = "चंदा एवं वित्तीय हिसाब-किताब सत्यापन बैठक",
                    type = "चंदा व हिसाब बैठक",
                    dateDisplay = "15 Aug 2026",
                    timeDisplay = "04:30 PM - 06:30 PM",
                    dateTimeMillis = System.currentTimeMillis() - (6L * 24 * 3600 * 1000),
                    venue = "कमेटी कार्यालय",
                    virtualLink = null,
                    chairperson = "हाफिज मोहम्मद ताहिर (कोषाध्यक्ष)",
                    agenda = "1. UPI आईडी (ak750258@icici) पर प्राप्त सभी दानों का मिलान।\n2. रसीद बुक व डिजिटल लेजर का पूर्ण ऑडिट।",
                    status = "Completed",
                    notesOrMinutes = "कोषाध्यक्ष द्वारा प्रस्तुत समस्त हिसाब-किताब सर्वसम्मति से पारित हुआ। कुल ₹1,85,000 की चंदा राशि पारदर्शी लेजर में दर्ज पाई गई।"
                )
            )
            dao.insertMeetings(initialMeetings)

            // Initial Documents in Hindi
            val initialDocs = listOf(
                OfficialDocument(
                    id = 1,
                    title = "TTS 12 रबी-उल-अव्वल कमेटी संविधान एवं नियमावली (1447H)",
                    category = "कमेटी नियमावली",
                    refCode = "TTS/RABI/1447/001",
                    publishedDate = "01 सफर 1447 हिजरी",
                    fileSize = "2.8 MB PDF",
                    accessLevel = "सभी सदस्य (All Members)",
                    summary = "12 रबी-उल-अव्वल (जश्न-ए-विलादत-उन-नबी ﷺ) के पावन अवसर पर शांतिपूर्ण, अनुशासित व भव्य कार्यक्रम संचालन हेतु अधिकृत नियम व दिशा-निर्देश।",
                    fullContent = """
                        बिसमिल्लाहिर्रहमानिर्रहीम
                        TTS कमेटी (12 रबी-उल-अव्वल जश्न-ए-विलादत समिति)
                        दस्तावेज़ कोड: TTS/RABI/1447/001
                        
                        1. उद्देश्य:
                        हज़रत मुहम्मद सल्लल्लाहु अलैहि वसल्लम के यौम-ए-पैदाइश (12 रबी-उल-अव्वल) को अकीदत, एहतराम, अमन व भाईचारे के साथ मनाना।
                        
                        2. जुलूस-ए-मोहम्मदी अनुशासन:
                        - जुलूस में पूरी शालीनता और अनुशासन का पालन अनिवार्य होगा।
                        - डीजे व तेज आवाज वाले अनुचित लाउडस्पीकर पूर्णतः प्रतिबंधित रहेंगे, केवल नात-ए-पाक और दुरूद-ओ-सलाम पढ़ा जाएगा।
                        - ट्रैफिक व आम जनता को किसी प्रकार की असुविधा न हो।
                        
                        3. चंदा एवं वित्तीय पारदर्शिता:
                        - सभी चंदा केवल अधिकृत UPI आईडी: ak750258@icici अथवा अधिकृत कोषाध्यक्ष रसीद के माध्यम से ही स्वीकार्य होगा।
                        - पाई-पाई का हिसाब सार्वजनिक डिजिटल लेजर में हर सदस्य के लिए खुला रहेगा।
                        
                        4. पहचान पत्र (Digital ID):
                        - प्रत्येक अधिकृत खादिम व पदाधिकारी के पास पवित्र परचम बैकग्राउंड वाला डिजिटल ID कार्ड होना अनिवार्य है।
                    """.trimIndent()
                ),
                OfficialDocument(
                    id = 2,
                    title = "जुलूस-ए-मोहम्मदी अधिकृत रूट मैप व पुलिस अनुमति पत्र",
                    category = "जुलूस मार्ग एवं अनुमति",
                    refCode = "TTS/ROUTE/1447/02",
                    publishedDate = "10 सफर 1447 हिजरी",
                    fileSize = "1.5 MB PDF",
                    accessLevel = "सभी सदस्य (All Members)",
                    summary = "प्रशासन व पुलिस से अधिकृत जुलूस मार्ग: मदीना मस्जिद चौक से प्रारंभ होकर सेंट्रल जामा मस्जिद तक।",
                    fullContent = """
                        जुलूस-ए-मोहम्मदी (12 रबी-उल-अव्वल) अधिकृत मार्ग निर्देश:
                        
                        रूट विवरण:
                        1. प्रारंभ: मदीना मस्जिद चौक (प्रातः 09:00 बजे)
                        2. पड़ाव 1: नूरी चौक (सलाम व शरबत वितरण)
                        3. पड़ाव 2: रज़ा तिराहा (नात ख्वानी व परचम सलामी)
                        4. समापन: सेंट्रल जामा मस्जिद ग्राउंड (दोपहर 01:30 बजे, लंगर-ए-आम व दुआ)
                        
                        स्वयंसेवकों के लिए निर्देश:
                        - सभी वॉलिंटियर ग्रीन बेल्ट व डिजिटल ID कार्ड धारण करें।
                        - आपातकालीन एम्बुलेंस हेतु रास्ता सदैव खुला रखें।
                    """.trimIndent()
                ),
                OfficialDocument(
                    id = 3,
                    title = "वार्षिक चंदा व व्यय ऑडिट रिपोर्ट (पारदर्शी लेखा)",
                    category = "लेखा व चंदा ऑडिट",
                    refCode = "TTS/AUDIT/1447/03",
                    publishedDate = "15 सफर 1447 हिजरी",
                    fileSize = "3.4 MB PDF",
                    accessLevel = "सभी सदस्य (All Members)",
                    summary = "कोषाध्यक्ष हाफिज मोहम्मद ताहिर द्वारा सत्यापित संपूर्ण चंदा संग्रह एवं लंगर व्यय का पूर्ण विवरण।",
                    fullContent = """
                        TTS कमेटी - चंदा एवं आय-व्यय ऑडिट विवरण:
                        
                        कुल प्राप्त चंदा (UPI: ak750258@icici + नकद): ₹1,85,000
                        - लंगर-ए-आम व तबर्रुक व्यय: ₹82,500
                        - लाइट डेकोरेशन व स्टेज: ₹38,000
                        - साउंड सिस्टम व नात ख्वानी मंच: ₹24,500
                        - बैनर, परचम व स्वागत गेट: ₹16,000
                        - आपातकालीन आरक्षित कोष: ₹24,000
                        
                        संपूर्ण बैंक रसीदें व UPI संदर्भ संख्या मिलान पूर्ण।
                    """.trimIndent()
                )
            )
            dao.insertDocuments(initialDocs)

            // Initial Hindi Notices for 12 Rabi-Ul-Awwal
            val initialNotices = listOf(
                Notice(
                    id = 1,
                    title = "📢 जश्न-ए-विलादत-उन-नबी ﷺ (12 रबी-उल-अव्वल) मुबारक! विशेष दिशा-निर्देश",
                    category = "12 रबी-उल-अव्वल",
                    priority = "HIGH",
                    issuedBy = "सदर व संस्थापक, TTS कमेटी",
                    date = "22 Aug 2026",
                    content = "तमाम अहले वतन व कमेटी सदस्यों को 12 रबी-उल-अव्वल (हज़रत मुहम्मद सल्लल्लाहु अलैहि वसल्लम का यौम-ए-पैदाइश) की दिली मुबारकबाद! जुलूस-ए-मोहम्मदी में सभी सदस्य अनुशासन, दुरूद-ओ-सलाम और अमन-शांति के साथ शामिल हों। अपने-अपने डिजिटल ID कार्ड मोबाइल में सुरक्षित रखें।",
                    isPinned = true
                ),
                Notice(
                    id = 2,
                    title = "💰 चंदा संग्रह चालू है - केवल अधिकृत UPI आईडी (ak750258@icici) पर दान करें",
                    category = "चंदा सूचना",
                    priority = "HIGH",
                    issuedBy = "हाफिज मोहम्मद ताहिर (कोषाध्यक्ष)",
                    date = "20 Aug 2026",
                    content = "12 रबी-उल-अव्वल के लंगर-ए-पाक, रोशनी और जलसे के इंतजामात हेतु अपनी नेकी का हिस्सा दें। चंदा भेजने की आधिकारिक UPI ID: ak750258@icici है। दान करने के उपरांत रसीद हेतु ऐप में 'चंदा दर्ज करें' बटन का उपयोग करें।",
                    isPinned = true
                ),
                Notice(
                    id = 3,
                    title = "🍲 लंगर-ए-पाक व तबर्रुक वितरण समय व स्थान तय",
                    category = "लंगर-ए-आम",
                    priority = "NORMAL",
                    issuedBy = "मोहम्मद तारिक अनवर (व्यवस्थापक)",
                    date = "18 Aug 2026",
                    content = "जुलूस समाप्ति के पश्चात सेंट्रल जामा मस्जिद प्रांगण में विशाल लंगर-ए-आम का आयोजन होगा। 5000+ अकीदतमंदों हेतु भोजन व्यवस्था रहेगी। सभी वॉलिंटियर्स प्रातः 11 बजे लंगर हॉल में रिपोर्ट करें।",
                    isPinned = false
                ),
                Notice(
                    id = 4,
                    title = "🏆 सर्वश्रेष्ठ परफ़ॉर्मर व खादिम सम्मान 1447H की घोषणा",
                    category = "अति आवश्यक",
                    priority = "NORMAL",
                    issuedBy = "एडमिन पैनल, TTS कमेटी",
                    date = "16 Aug 2026",
                    content = "12 रबी-उल-अव्वल के इंतजामात में दिन-रात खिदमत करने वाले उत्कृष्ट वॉलिंटियर्स को 'सर्वश्रेष्ठ खादिम' सम्मान दिया जा रहा है। होम स्क्रीन पर बेस्ट परफ़ॉर्मर्स की फोटो व उपलब्धियाँ देखें।",
                    isPinned = false
                )
            )
            dao.insertNotices(initialNotices)

            // Initial Donations Report (Visible to every member, with UPI ak750258@icici)
            val initialDonations = listOf(
                Donation(
                    id = 1,
                    donorName = "हाजी मोहम्मद अशरफ (संस्थापक)",
                    donorMemberCode = "TTS-1447-001",
                    amount = 51000.0,
                    purpose = "12 रबी-उल-अव्वल लंगर-ए-पाक व सजावट",
                    paymentMode = "UPI (ak750258@icici)",
                    transactionRef = "UPI/623819024810",
                    date = "21 Aug 2026",
                    timestamp = System.currentTimeMillis() - (1L * 24 * 3600 * 1000),
                    verified = true,
                    remarks = "संस्थापक की ओर से लंगर-ए-आम योगदान"
                ),
                Donation(
                    id = 2,
                    donorName = "जनाब गुलाम मुस्तफा (सदर)",
                    donorMemberCode = "TTS-1447-002",
                    amount = 25000.0,
                    purpose = "जुलूस-ए-मोहम्मदी स्टेज व लाइट डेकोरेशन",
                    paymentMode = "UPI (ak750258@icici)",
                    transactionRef = "UPI/623789011244",
                    date = "20 Aug 2026",
                    timestamp = System.currentTimeMillis() - (2L * 24 * 3600 * 1000),
                    verified = true,
                    remarks = "मुख्य स्वागत गेट एवं रोशनी व्यवस्था"
                ),
                Donation(
                    id = 3,
                    donorName = "हाफिज मोहम्मद ताहिर (कोषाध्यक्ष)",
                    donorMemberCode = "TTS-1447-003",
                    amount = 21000.0,
                    purpose = "तबर्रुक व शरबत सबील व्यवस्था",
                    paymentMode = "UPI (ak750258@icici)",
                    transactionRef = "UPI/623655489012",
                    date = "19 Aug 2026",
                    timestamp = System.currentTimeMillis() - (3L * 24 * 3600 * 1000),
                    verified = true,
                    remarks = "सबील-ए-हुसैन व शरबत वितरण"
                ),
                Donation(
                    id = 4,
                    donorName = "सैय्यद इरफान अली (नायब सदर)",
                    donorMemberCode = "TTS-1447-005",
                    amount = 15000.0,
                    purpose = "जुलूस-ए-मोहम्मदी इंतजाम",
                    paymentMode = "UPI (ak750258@icici)",
                    transactionRef = "UPI/623544198273",
                    date = "18 Aug 2026",
                    timestamp = System.currentTimeMillis() - (4L * 24 * 3600 * 1000),
                    verified = true,
                    remarks = "सुरक्षा व वॉलिंटियर किट सहयोग"
                ),
                Donation(
                    id = 5,
                    donorName = "मोहम्मद तारिक अनवर",
                    donorMemberCode = "TTS-1447-006",
                    amount = 11000.0,
                    purpose = "12 रबी-उल-अव्वल लंगर-ए-पाक",
                    paymentMode = "UPI (ak750258@icici)",
                    transactionRef = "UPI/623412098451",
                    date = "16 Aug 2026",
                    timestamp = System.currentTimeMillis() - (6L * 24 * 3600 * 1000),
                    verified = true,
                    remarks = "देग राशन सामग्री"
                ),
                Donation(
                    id = 6,
                    donorName = "मोहम्मद साकिब रज़ा",
                    donorMemberCode = "TTS-1447-007",
                    amount = 5100.0,
                    purpose = "झंडे (परचम) व बैनर व्यवस्था",
                    paymentMode = "UPI (ak750258@icici)",
                    transactionRef = "UPI/623309871120",
                    date = "15 Aug 2026",
                    timestamp = System.currentTimeMillis() - (7L * 24 * 3600 * 1000),
                    verified = true,
                    remarks = "युवा विंग परचम"
                )
            )
            dao.insertDonations(initialDonations)

            // Initial Hindi Chat Messages for Online Collaboration
            val initialChats = listOf(
                ChatMessage(
                    id = 1,
                    channelId = "general",
                    senderName = "हाजी मोहम्मद अशरफ (संस्थापक)",
                    senderRole = "संस्थापक",
                    senderAvatarIndex = 0,
                    messageText = "अस्सलाम वालेकुम व रहमतुल्लाहि व बरकातुहु! 12 रबी-उल-अव्वल की सभी भाइयों को मुबारकबाद। सभी तैयारियों का जायजा लें।",
                    timestamp = System.currentTimeMillis() - 7200000,
                    timeDisplay = "10:15 AM",
                    isAnnouncement = true
                ),
                ChatMessage(
                    id = 2,
                    channelId = "general",
                    senderName = "जनाब गुलाम मुस्तफा (सदर)",
                    senderRole = "सदर",
                    senderAvatarIndex = 1,
                    messageText = "वालेकुम अस्सलाम हाजी साहब! जुलूस का रूट तय हो चुका है और प्रशासन से विधिवत अनुमति प्राप्त हो गई है।",
                    timestamp = System.currentTimeMillis() - 5400000,
                    timeDisplay = "10:45 AM"
                ),
                ChatMessage(
                    id = 3,
                    channelId = "general",
                    senderName = "हाफिज मोहम्मद ताहिर (कोषाध्यक्ष)",
                    senderRole = "कोषाध्यक्ष",
                    senderAvatarIndex = 2,
                    messageText = "माशाअल्लाह! UPI ID (ak750258@icici) पर अब तक कुल ₹1,28,100 चंदा आ चुका है। पूरा विवरण ऐप के चंदा लेजर में लाइव दिख रहा है।",
                    timestamp = System.currentTimeMillis() - 3600000,
                    timeDisplay = "11:15 AM"
                ),
                ChatMessage(
                    id = 4,
                    channelId = "rabi_ul_awwal",
                    senderName = "मोहम्मद तारिक अनवर",
                    senderRole = "मुख्य व्यवस्थापक",
                    senderAvatarIndex = 5,
                    messageText = "लंगर-ए-पाक के लिए सभी राशन सामग्री व देगों का ऑर्डर हो चुका है। कुल 20 देग बिरयानी व शीर कोरमा तैयार किया जाएगा।",
                    timestamp = System.currentTimeMillis() - 1800000,
                    timeDisplay = "11:45 AM"
                ),
                ChatMessage(
                    id = 5,
                    channelId = "general",
                    senderName = "मोहम्मद साकिब रज़ा",
                    senderRole = "खादिम",
                    senderAvatarIndex = 0,
                    messageText = "युवा विंग ने पूरे मार्ग पर 1000+ हरे परचम (झंडे) और स्वागत तोरण द्वार लगा दिए हैं। सुभानअल्लाह!",
                    timestamp = System.currentTimeMillis() - 600000,
                    timeDisplay = "12:10 PM"
                )
            )
            dao.insertChatMessages(initialChats)
        }
    }
}
