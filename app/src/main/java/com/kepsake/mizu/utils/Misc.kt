package com.kepsake.mizu.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File

@SuppressLint("DefaultLocale")
fun getImages(): List<String> {
    return (1..100).map { "http://192.168.0.106:3000/image_${String.format("%03d", it)}.png" }
}

fun isImageFile(fileName: String): Boolean {
    val extensions = listOf(".jpg", ".jpeg", ".png", ".webp")
    return extensions.any { fileName.lowercase().endsWith(it) }
}

// Function to get path from Uri (unchanged)
fun getPathFromUri(context: Context, uri: Uri): String? {
    var path: String? = null
    Log.d("ROHAN", "${uri.path}, ${uri.encodedPath}")
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (columnIndex != -1) {
                val fileName = cursor.getString(columnIndex)
                val file = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                path = file.absolutePath
            }
        }
    }
    return path
}
