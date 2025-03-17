package com.kepsake.mizu.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes


fun isImageFile(fileName: String): Boolean {
    val extensions = listOf(".jpg", ".jpeg", ".png", ".webp")
    return extensions.any { fileName.lowercase().endsWith(it) }
}

fun getMangaFiles(dirPath: String): List<Map<String, Any>> {
    val directory = File(dirPath)

    if (!directory.exists() || !directory.isDirectory) {
        throw IllegalArgumentException("Invalid directory path: $dirPath")
    }

    return directory.listFiles()
        ?.filter { file ->
            file.isFile && (file.name.endsWith(
                ".zip",
                ignoreCase = true
            ) || file.name.endsWith(".cbz", ignoreCase = true))
        }
        ?.map { file ->
            val path = Paths.get(file.absolutePath)
            val attrs = Files.readAttributes(path, BasicFileAttributes::class.java)

            mapOf(
                "name" to file.name,
                "path" to file.absolutePath,
                "size" to file.length(),
                "lastModified" to file.lastModified(),
                "creationTime" to attrs.creationTime().toMillis(),
            )
        } ?: emptyList()
}

fun getFilePathFromUri(context: Context, uri: Uri): String? {
    // Handle content:// scheme
    if (uri.scheme == "content") {
        // For media content URIs
        if (uri.authority == MediaStore.AUTHORITY) {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                    return cursor.getString(columnIndex)
                }
            }
        }
        if ("com.android.externalstorage.documents" == uri.authority) {
            // Check first if it's a document URI
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // Original code for document URIs
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                return if ("primary".equals(type, ignoreCase = true)) {
                    Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                } else {
                    "/storage/" + type + "/" + split[1]
                }
            } else if (DocumentsContract.isTreeUri(uri)) {
                val treeId = DocumentsContract.getTreeDocumentId(uri)
                val split =
                    treeId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]
                return if ("primary".equals(type, ignoreCase = true)) {
                    Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                } else {
                    // Handle secondary storage or SD card
                    "/storage/" + type + "/" + split[1]
                }
            }
        }

        // For DocumentProvider URIs
        val docId = DocumentsContract.getDocumentId(uri)
        if (docId.startsWith("raw:")) {
            // For raw file paths
            return docId.substring(4)
        }
    }

    // For file:// scheme
    if (uri.scheme == "file") {
        return uri.path
    }

    return null
}

fun getImageAspectRatio(filePath: String): Float {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true  // This avoids loading the full bitmap into memory
    }
    BitmapFactory.decodeFile(filePath, options)
   
    val width = options.outWidth
    val height = options.outHeight

    return if (height > 0) {
        width.toFloat() / height.toFloat()
    } else {
        0f  // Handle error case
    }
}
