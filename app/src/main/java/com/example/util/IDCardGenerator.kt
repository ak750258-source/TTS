package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.provider.MediaStore
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Member
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object IDCardGenerator {

    /**
     * Generates a high-resolution, beautifully styled Bitmap of the ID Card.
     */
    fun createIDCardBitmap(context: Context, member: Member): Bitmap {
        val width = 1000
        val height = 1500
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#F4FBF4")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Outer Border
        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1B5E20")
            style = Paint.Style.STROKE
            strokeWidth = 14f
        }
        canvas.drawRoundRect(RectF(15f, 15f, width - 15f, height - 15f), 30f, 30f, borderPaint)

        // Gold Inner Border
        val goldBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(RectF(25f, 25f, width - 25f, height - 25f), 24f, 24f, goldBorderPaint)

        // Top Header Banner
        val headerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F5132")
            style = Paint.Style.FILL
        }
        canvas.drawRect(28f, 28f, width - 28f, 240f, headerPaint)

        // Header Text: Bismillah / Naat
        val arabicPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#FEF08A")
            textSize = 32f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("ﷺ نعت و جشنِ ولادتِ مصطفیٰ ﷺ", width / 2f, 85f, arabicPaint)

        // Header Title: 12 Rabi ul Awwal
        val headerTitlePaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("12 रबी-उल-अव्वल जश्न-ए-विलादत कमेटी", width / 2f, 150f, headerTitlePaint)

        val subHeaderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#BBF7D0")
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        canvas.drawText("तहफ्फुज़-ए-तहज़ीब वेलफेयर सोसाइटी (TTS)", width / 2f, 205f, subHeaderPaint)

        // Member Photo or Placeholder
        val photoSize = 240
        val photoLeft = (width - photoSize) / 2
        val photoTop = 280
        val photoRect = RectF(photoLeft.toFloat(), photoTop.toFloat(), (photoLeft + photoSize).toFloat(), (photoTop + photoSize).toFloat())

        val photoBgPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(photoRect, 20f, 20f, photoBgPaint)

        var memberBitmap: Bitmap? = null
        try {
            if (!member.photoUri.isNullOrBlank()) {
                memberBitmap = ImageUtils.getBitmapFromPhotoUri(member.photoUri)
            }
        } catch (e: Exception) {
            // ignore
        }

        if (memberBitmap != null) {
            val srcRect = Rect(0, 0, memberBitmap.width, memberBitmap.height)
            canvas.drawBitmap(memberBitmap, srcRect, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))
        } else {
            val placeholderPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#1B5E20")
                textSize = 36f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("फोटो", photoRect.centerX(), photoRect.centerY() + 12f, placeholderPaint)
        }

        // Photo Frame Border
        val photoFramePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#D4AF37")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRoundRect(photoRect, 20f, 20f, photoFramePaint)

        // Member Name
        val namePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F5132")
            textSize = 48f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(member.fullName, width / 2f, 590f, namePaint)

        // Designation Pill Box
        val desigBoxPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#D1E7DD")
            style = Paint.Style.FILL
        }
        val desigBoxRect = RectF(120f, 620f, width - 120f, 690f)
        canvas.drawRoundRect(desigBoxRect, 16f, 16f, desigBoxPaint)

        val desigTextPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F5132")
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(member.designation, width / 2f, 668f, desigTextPaint)

        // Details Table
        val labelPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#475569")
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        var startY = 750f
        val lineGap = 55f

        fun drawField(label: String, value: String) {
            canvas.drawText(label, 70f, startY, labelPaint)
            canvas.drawText(value, 360f, startY, valPaint)
            startY += lineGap
        }

        drawField("सदस्य कोड:", member.memberCode)
        drawField("विभाग / विंग:", member.committeeWing)
        drawField("मोबाइल नंबर:", member.phoneNumber)
        if (member.emergencyContact.isNotBlank()) {
            drawField("आपातकालीन:", member.emergencyContact)
        }
        if (member.address.isNotBlank()) {
            val displayAddr = if (member.address.length > 28) member.address.take(28) + "..." else member.address
            drawField("निवासी इलाका:", displayAddr)
        }
        drawField("चंदा UPI ID:", "ak750258@icici")

        // Head office address box
        val officeBoxPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E8F5E9")
            style = Paint.Style.FILL
        }
        val officeRect = RectF(50f, 1080f, width - 50f, 1200f)
        canvas.drawRoundRect(officeRect, 14f, 14f, officeBoxPaint)

        val officeBorderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#A7F3D0")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(officeRect, 14f, 14f, officeBorderPaint)

        val officeLabelPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#065F46")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("प्रधान कार्यालय (Head Office):", 75f, 1125f, officeLabelPaint)

        val officeValPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#1B4332")
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("धनुपुरा, बिसौली, बदायूं, उत्तर प्रदेश (243632)", 75f, 1170f, officeValPaint)

        // Footer Banner
        val footerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#0F5132")
            style = Paint.Style.FILL
        }
        canvas.drawRect(28f, height - 120f, width - 28f, height - 28f, footerPaint)

        val footerTextPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("सत्र 12 रबी-उल-अव्वल 1447H • आधिकारिक पहचान पत्र", width / 2f, height - 60f, footerTextPaint)

        return bitmap
    }

    /**
     * Saves the generated ID card bitmap to the device Gallery/Downloads and returns the Uri.
     */
    fun saveIDCardToGallery(context: Context, member: Member): Uri? {
        val bitmap = createIDCardBitmap(context, member)
        val filename = "TTS_ID_${member.memberCode}_${member.fullName.replace(" ", "_")}.png"

        var fos: OutputStream? = null
        var imageUri: Uri? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TTS_ID_Cards")
                }
                val resolver = context.contentResolver
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (imageUri != null) {
                    fos = resolver.openOutputStream(imageUri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/TTS_ID_Cards"
                val fileDir = File(imagesDir)
                if (!fileDir.exists()) fileDir.mkdirs()
                val imageFile = File(fileDir, filename)
                fos = FileOutputStream(imageFile)
                imageUri = Uri.fromFile(imageFile)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
            }

            Toast.makeText(context, "✅ ${member.fullName} का ID कार्ड डाउनलोड हो गया! (गैलरी/Pictures में सेव है)", Toast.LENGTH_LONG).show()
            return imageUri
        } catch (e: Exception) {
            // Fallback: save to app cache directory and provide FileProvider Uri
            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, filename)
                val out = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.close()
                val fallbackUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                Toast.makeText(context, "✅ ID कार्ड इमेज तैयार है", Toast.LENGTH_SHORT).show()
                return fallbackUri
            } catch (ex: Exception) {
                Toast.makeText(context, "डाउनलोड त्रुटि: ${ex.message}", Toast.LENGTH_SHORT).show()
                return null
            }
        }
    }

    /**
     * Generates an HTML document of the ID card and triggers the Android Print/PDF manager dialog.
     */
    fun printOrExportPDF(context: Context, member: Member) {
        try {
            val webView = WebView(context)
            val htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <style>
                        @page { size: A4 portrait; margin: 20mm; }
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f1f5f9; display: flex; justify-content: center; }
                        .card-container { width: 340px; background: #ffffff; border: 3px solid #15803d; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #14532d, #15803d); color: white; text-align: center; padding: 14px 10px; }
                        .bismillah { font-size: 13px; color: #fef08a; font-weight: bold; margin-bottom: 4px; }
                        .title { font-size: 15px; font-weight: 800; margin: 0; line-height: 1.2; }
                        .subtitle { font-size: 11px; color: #bbf7d0; margin-top: 4px; }
                        .body { padding: 16px; text-align: center; }
                        .photo-box { width: 90px; height: 90px; border-radius: 12px; border: 2px solid #ca8a04; margin: 0 auto 10px auto; background: #e2e8f0; display: flex; align-items: center; justify-content: center; overflow: hidden; }
                        .photo-box img { width: 100%; height: 100%; object-fit: cover; }
                        .name { font-size: 18px; font-weight: bold; color: #14532d; margin: 0 0 4px 0; }
                        .designation { display: inline-block; background: #dcfce7; color: #166534; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; margin-bottom: 12px; }
                        .table-details { width: 100%; text-align: left; font-size: 11px; border-collapse: collapse; margin-bottom: 12px; }
                        .table-details td { padding: 4px 2px; }
                        .label { color: #64748b; font-weight: 600; width: 45%; }
                        .val { color: #0f172a; font-weight: bold; }
                        .upi-box { background: #f0fdf4; border: 1px dashed #86efac; padding: 6px; border-radius: 8px; font-size: 10px; color: #15803d; font-weight: bold; margin-bottom: 10px; }
                        .office-box { background: #e8f5e9; border: 1px solid #c8e6c9; padding: 6px; border-radius: 8px; font-size: 9px; color: #2e7d32; text-align: left; line-height: 1.3; }
                        .footer { background: #14532d; color: white; text-align: center; padding: 8px; font-size: 10px; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="card-container">
                        <div class="header">
                            <div class="bismillah">ﷺ نعت و جشنِ ولادتِ مصطفیٰ ﷺ</div>
                            <div class="title">12 रबी-उल-अव्वल जश्न-ए-विलादत कमेटी</div>
                            <div class="subtitle">तहफ्फुज़-ए-तहज़ीब वेलफेयर सोसाइटी (TTS)</div>
                        </div>
                        <div class="body">
                            <div class="photo-box">
                                ${if (!member.photoUri.isNullOrBlank() && member.photoUri.startsWith("data:")) "<img src='${member.photoUri}' />" else "<span style='font-size:12px;color:#166534;font-weight:bold;'>ID PHOTO</span>"}
                            </div>
                            <div class="name">${member.fullName}</div>
                            <div class="designation">${member.designation}</div>
                            <table class="table-details">
                                <tr><td class="label">सदस्य कोड:</td><td class="val">${member.memberCode}</td></tr>
                                <tr><td class="label">विभाग/विंग:</td><td class="val">${member.committeeWing}</td></tr>
                                <tr><td class="label">मोबाइल:</td><td class="val">${member.phoneNumber}</td></tr>
                                ${if (member.emergencyContact.isNotBlank()) "<tr><td class='label'>आपातकालीन:</td><td class='val'>${member.emergencyContact}</td></tr>" else ""}
                                ${if (member.address.isNotBlank()) "<tr><td class='label'>पता:</td><td class='val'>${member.address}</td></tr>" else ""}
                            </table>
                            <div class="upi-box">कमेटी चंदा UPI: ak750258@icici</div>
                            <div class="office-box">
                                <b>प्रधान कार्यालय:</b> धनुपुरा, बिसौली, बदायूं, उत्तर प्रदेश (243632)
                            </div>
                        </div>
                        <div class="footer">
                            सत्र 12 रबी-उल-अव्वल 1447H • अधिकृत डिजिटल पहचान पत्र
                        </div>
                    </div>
                </body>
                </html>
            """.trimIndent()

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    val printAdapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter("TTS_ID_${member.memberCode}")
                    val jobName = "TTS_ID_Card_${member.fullName}"
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setResolution(PrintAttributes.Resolution("id_res", "Default", 300, 300))
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager?.print(jobName, printAdapter, printAttributes)
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "प्रिंट डायलॉग खोलने में समस्या: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
