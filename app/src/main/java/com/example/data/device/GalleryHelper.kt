package com.example.data.device

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GalleryPhoto(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val formattedDate: String,
    val sizeBytes: Long
)

object GalleryHelper {

    fun getLatestPhoto(context: Context): GalleryPhoto? {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateTakenColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)
                    val dateAddedColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "photo.jpg"
                    val size = cursor.getLong(sizeColumn)

                    val timestamp = if (dateTakenColumn != -1 && cursor.getLong(dateTakenColumn) > 0) {
                        cursor.getLong(dateTakenColumn)
                    } else if (dateAddedColumn != -1) {
                        cursor.getLong(dateAddedColumn) * 1000
                    } else {
                        System.currentTimeMillis()
                    }

                    val dateString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(Date(timestamp))

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    return GalleryPhoto(
                        id = id,
                        uri = contentUri,
                        displayName = name,
                        formattedDate = dateString,
                        sizeBytes = size
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("GalleryHelper", "Failed to query latest photo", e)
        }
        return null
    }

    fun loadBitmapFromUri(context: Context, uri: Uri, maxDimension: Int = 1024): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val width = info.size.width
                    val height = info.size.height
                    if (width > maxDimension || height > maxDimension) {
                        val scale = maxDimension.toFloat() / maxOf(width, height)
                        decoder.setTargetSize((width * scale).toInt(), (height * scale).toInt())
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e("GalleryHelper", "Failed to decode bitmap from uri $uri", e)
            null
        }
    }
}
