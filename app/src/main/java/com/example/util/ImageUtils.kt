package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object ImageUtils {

    // Fast in-memory bitmap cache to prevent redundant Base64 decoding
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(50) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Converts an image Uri (from Gallery / Camera) to an optimized, compressed Base64 Data URI string.
     * Dimensions are scaled to 180x180 px at quality 65% (~3-5KB), which guarantees instant rendering,
     * low memory footprint, and reliable transmission over real-time cloud sync to all devices.
     */
    suspend fun uriToBase64(
        context: Context,
        uri: Uri,
        maxDimension: Int = 140,
        quality: Int = 55
    ): String? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver

            // 1. Decode image bounds
            var inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val srcWidth = options.outWidth
            val srcHeight = options.outHeight
            if (srcWidth <= 0 || srcHeight <= 0) return@withContext null

            // 2. Compute sample size
            var inSampleSize = 1
            if (srcHeight > maxDimension || srcWidth > maxDimension) {
                val halfHeight = srcHeight / 2
                val halfWidth = srcWidth / 2
                while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 3. Decode sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            inputStream = contentResolver.openInputStream(uri)
            val sampledBitmap: Bitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions) ?: return@withContext null
            inputStream?.close()

            // 4. Handle EXIF Rotation
            var rotationAngle = 0f
            try {
                contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotationAngle = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                }
            } catch (e: Exception) {
                // Ignore EXIF errors
            }

            val rotatedBitmap: Bitmap = if (rotationAngle != 0f) {
                val matrix = Matrix().apply { postRotate(rotationAngle) }
                Bitmap.createBitmap(
                    sampledBitmap, 0, 0,
                    sampledBitmap.width, sampledBitmap.height,
                    matrix, true
                )
            } else {
                sampledBitmap
            }

            // 5. Scale down to exact maxDimension keeping aspect ratio
            val scale = min(
                maxDimension.toFloat() / rotatedBitmap.width,
                maxDimension.toFloat() / rotatedBitmap.height
            )
            val finalBitmap: Bitmap = if (scale < 1.0f) {
                val destWidth = (rotatedBitmap.width * scale).toInt().coerceAtLeast(1)
                val destHeight = (rotatedBitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rotatedBitmap, destWidth, destHeight, true)
            } else {
                rotatedBitmap
            }

            // 6. Compress to JPEG and encode to Base64 Data URI
            val outputStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64String"

            // Pre-cache bitmap in memory
            memoryCache.put(dataUri, finalBitmap)

            dataUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decodes a Base64 string / Data URI to an Android Bitmap with memory caching.
     * Returns null if string is empty or not a valid Base64 image.
     */
    fun getBitmapFromPhotoUri(photoUri: String?): Bitmap? {
        if (photoUri.isNullOrBlank()) return null

        // Check memory cache first
        memoryCache.get(photoUri)?.let { return it }

        return try {
            val base64Data = when {
                photoUri.startsWith("data:image/") -> photoUri.substringAfter(",")
                photoUri.startsWith("http://") || photoUri.startsWith("https://") || photoUri.startsWith("content://") || photoUri.startsWith("file://") -> {
                    // Not a raw base64 string, let AsyncImage handle it
                    return null
                }
                else -> photoUri
            }

            val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (decodedBytes.isEmpty()) return null

            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            if (bitmap != null) {
                memoryCache.put(photoUri, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Stores a bitmap to internal file storage for offline backup.
     */
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val directory = File(context.filesDir, "member_photos")
            if (!directory.exists()) directory.mkdirs()
            val file = File(directory, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
