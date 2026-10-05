package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Copies user-selected images (Profile Pictures / PP, Chat Images, Status SW Images)
 * from temporary picker URIs into permanent internal app storage AND generates a compact
 * Base64 thumbnail string so Profile Pictures (PP) and Status photos can be synced
 * directly via Firebase Firestore documents across devices.
 */
object ProfileMediaHelper {

    data class ProcessedImageResult(
        val localFilePath: String,
        val base64DataUri: String?
    )

    suspend fun copyAndEncodeImage(
        context: Context,
        sourceUriString: String,
        subFolder: String = "avatars",
        maxDimensionPx: Int = 360
    ): ProcessedImageResult? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(sourceUriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap == null) return@withContext null

            val ratio = minOf(
                maxDimensionPx.toFloat() / originalBitmap.width.coerceAtLeast(1),
                maxDimensionPx.toFloat() / originalBitmap.height.coerceAtLeast(1),
                1.0f
            )
            val targetW = (originalBitmap.width * ratio).toInt().coerceAtLeast(1)
            val targetH = (originalBitmap.height * ratio).toInt().coerceAtLeast(1)
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)

            val dir = File(context.filesDir, subFolder).apply { mkdirs() }
            val outFile = File(dir, "img_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, fos)
            }

            // Also create a compact Base64 representation for Firestore real-time sync
            val baos = ByteArrayOutputStream()
            val thumbRatio = minOf(180f / targetW, 180f / targetH, 1.0f)
            val thumbBitmap = Bitmap.createScaledBitmap(
                scaledBitmap,
                (targetW * thumbRatio).toInt().coerceAtLeast(1),
                (targetH * thumbRatio).toInt().coerceAtLeast(1),
                true
            )
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 72, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            ProcessedImageResult(
                localFilePath = outFile.absolutePath,
                base64DataUri = "data:image/jpeg;base64,$b64"
            )
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToBitmap(dataUriOrB64: String?): Bitmap? {
        if (dataUriOrB64.isNullOrBlank()) return null
        return try {
            val raw = dataUriOrB64.substringAfter("base64,", dataUriOrB64)
            val bytes = Base64.decode(raw, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
