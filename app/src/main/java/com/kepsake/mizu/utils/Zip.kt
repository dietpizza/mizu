package com.kepsake.mizu.utils

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


fun readFileFromZip(zipFilePath: String, fileName: String): ByteArray? {
    ZipFile(File(zipFilePath)).use { zipFile ->
        val entry: ZipEntry? = zipFile.getEntry(fileName)
        return entry?.let {
            zipFile.getInputStream(it).use { inputStream ->
                ByteArrayOutputStream().use { outputStream ->
                    val buffer = ByteArray(8192) // 8 KB buffer
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.toByteArray() // Convert to ByteArray and return
                }
            }
        }
    }
}

fun extractImageFromZip(zipFilePath: String, fileName: String, context: Context): File? {
    val tempFile = File(context.cacheDir, fileName)

    ZipFile(File(zipFilePath)).use { zipFile ->
        val entry: ZipEntry? = zipFile.getEntry(fileName)
        entry?.let {
            zipFile.getInputStream(it).use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192) // 8KB buffer
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
            return tempFile
        }
    }
    return null
}


//fun readFileFromZip(zipFilePath: String, fileName: String): ByteArray? {
//    ZipFile(File(zipFilePath)).use { zipFile ->
//        val entry: ZipEntry? = zipFile.getEntry(fileName)
//        return entry?.let {
//            zipFile.getInputStream(it).use { inputStream ->
//                inputStream.readBytes()
//            }
//        }
//    }
//}


fun getZipFileEntries(zipFilePath: String): List<ZipEntry> {
    val zipFile = ZipFile(File(zipFilePath))
    val entries = zipFile.entries().toList()

    return entries;
}