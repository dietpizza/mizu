package com.kepsake.mizu.utils

import android.content.Context
import android.util.Log
import com.kepsake.mizu.logic.NaturalOrderComparator
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

val TAG = "ZipUtil"

fun extractImageFromZip(
    context: Context,
    zipFilePath: String,
    entryName: String,
    mangaId: String,
    cachePath: String
): File? {
    return try {
        val tempDir = File(context.cacheDir, "${cachePath}/$mangaId").apply {
            if (!exists()) mkdirs()
        }

        val sanitizedName = sanitizeFileName(entryName)
        val tempFile = File(tempDir, sanitizedName)

        if (tempFile.exists()) {
            Log.d(TAG, "Cache hit!")
            tempFile
        } else {
            Log.d(TAG, "Extracted!")
            extractEntryToFile(zipFilePath, tempFile.path, entryName)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error extracting image: $entryName", e)
        null
    }
}

fun extractEntryToFile(
    zipFilePath: String,
    outFilePath: String,
    entryName: String
): File? {
    return try {
        ZipFile(File(zipFilePath)).use { zipFile ->
            val entry = zipFile.getEntry(entryName)
            if (entry != null && !entry.isDirectory) {
                val outFile = File(outFilePath)
                zipFile.getInputStream(entry).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Verify the file was created successfully
                if (outFile.exists() && outFile.length() > 0) {
                    return outFile
                } else {
                    Log.e("MangaView", "Failed to extract: $entryName (Empty file)")
                    null
                }
            } else {
                Log.e("MangaView", "Entry not found or is directory: $entryName")
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

// This function remains unchanged
fun getZipFileEntries(zipFilePath: String): List<ZipEntry> {
    try {
        ZipFile(zipFilePath).use { zipFile ->
            val entries =
                zipFile.entries().asSequence().filter { !it.isDirectory && isImageFile(it.name) }
                    .toList().sortedWith(compareBy(NaturalOrderComparator()) { it.name })

            return entries
        }
    } catch (e: Exception) {
        Log.e("MangaView", "Error reading zip file", e)
    }

    // Sort entries by name for correct order
    return emptyList()
}

fun sanitizeFileName(name: String): String {
    return name.replace('/', '_').replace('\\', '_')
}


fun extractCoverImage(context: Context, mangaId: String, mangaPath: String): String? {
    val coversDir = File(context.cacheDir, "covers/$mangaId").apply {
        if (!exists()) mkdirs()
    }
    val entry = getZipFileEntries(mangaPath).firstOrNull()

    if (entry != null) {
        val outFile = File(coversDir, sanitizeFileName(entry.name))

        if (!outFile.exists()) {
            val extractedFile = extractEntryToFile(mangaPath, outFile.path, entry.name)
            return extractedFile?.path
        } else {
            return outFile.path
        }
    }

    return null
}