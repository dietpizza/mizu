package com.kepsake.mizu.utils

import android.content.Context
import android.util.Log
import com.kepsake.mizu.logic.NaturalOrderComparator
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

// Modified to create a temporary file in the app's cache directory with manga ID
fun extractImageFromZip(
    context: Context,
    zipFilePath: String,
    entryName: String,
    mangaId: String,
    cachePath: String
): File? {
    return try {
        // Create a directory for this specific manga using the manga ID
        val tempDir = File(context.cacheDir, "${cachePath}/$mangaId").apply {
            if (!exists()) mkdirs()
        }

        // Create a unique filename based on the entry name
        val sanitizedName = entryName.replace('/', '_').replace('\\', '_')
        val tempFile = File(tempDir, sanitizedName)

        // Force extraction for each new manga by not reusing existing files
        ZipFile(zipFilePath).use { zipFile ->
            val entry = zipFile.getEntry(entryName)
            if (entry != null && !entry.isDirectory) {
                zipFile.getInputStream(entry).use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                // Verify the file was created successfully
                if (tempFile.exists() && tempFile.length() > 0) {
                    tempFile
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
        Log.e("MangaView", "Error extracting image: $entryName", e)
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
