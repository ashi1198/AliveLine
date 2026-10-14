package com.aliveline.app.data.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Photos on notes live in the app's PRIVATE storage (filesDir/note_photos), so they keep working
 * even if the original picture is deleted from the gallery. Each photo is resized when imported.
 */
@Singleton
class PhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir = File(context.filesDir, "note_photos")
    private val cache = LruCache<String, ImageBitmap>(12)

    fun file(name: String): File = File(dir, name)

    /** Copies the picked picture into private storage. Returns the saved file name, or null on failure. */
    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching { doImport(uri) }.getOrNull()
    }

    private fun doImport(uri: Uri): String? {
        dir.mkdirs()
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        var sample = 1
        while (longest / (sample * 2) >= MAX_SAVED_PX) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val degrees = resolver.openInputStream(uri)?.use { exifDegrees(it) } ?: 0
        val bitmap = if (degrees == 0) {
            decoded
        } else {
            Bitmap.createBitmap(
                decoded, 0, 0, decoded.width, decoded.height,
                Matrix().apply { postRotate(degrees.toFloat()) },
                true,
            )
        }

        val name = "${UUID.randomUUID()}.jpg"
        FileOutputStream(file(name)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return name
    }

    private fun exifDegrees(stream: InputStream): Int = runCatching {
        when (
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)

    /** A screen-sized version of the photo, cached so scrolling and dragging stay smooth. */
    suspend fun load(name: String): ImageBitmap? {
        cache.get(name)?.let { return it }
        val loaded = withContext(Dispatchers.IO) { runCatching { decodeForDisplay(file(name)) }.getOrNull() }
        if (loaded != null) cache.put(name, loaded)
        return loaded
    }

    private fun decodeForDisplay(file: File): ImageBitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null

        var sample = 1
        while (longest / (sample * 2) >= DISPLAY_PX) sample *= 2
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )?.asImageBitmap()
    }

    fun delete(name: String) {
        cache.remove(name)
        file(name).delete()
    }

    private companion object {
        const val MAX_SAVED_PX = 1600
        const val DISPLAY_PX = 900
    }
}