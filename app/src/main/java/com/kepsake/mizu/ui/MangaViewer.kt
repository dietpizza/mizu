package com.kepsake.mizu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kepsake.mizu.utils.getZipFileEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile


@Composable
fun MangaView(innerPadding: PaddingValues) {
    val context = LocalContext.current
    var filePath by remember { mutableStateOf<String>("") }
    var imagesInside by remember { mutableStateOf(emptyList<ZipEntry>()) }

    // Add a paging state to only load visible images
    val lazyListState = rememberLazyListState()

    // Track which images are currently extracted to avoid repeated extraction
    val extractedImages = remember { mutableStateMapOf<String, File?>() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val path = getPathFromUri(context, it)
            if (path != null) {
                filePath = path
                // Just get the entries but don't extract yet
                val entries = getZipFileEntries(path)
                imagesInside = entries
                Log.d("ROHAN", "Filename: ${filePath}")
                entries.forEach { Log.d("ROHAN", "File: ${it.name}") }

                // Clear any previously extracted images
                extractedImages.clear()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            context.startActivity(intent)
        } else {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
    }

    // Calculate the visible item indexes
    val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
    val firstVisibleItemIndex =
        if (visibleItemsInfo.isNotEmpty()) visibleItemsInfo.first().index else 0
    val lastVisibleItemIndex =
        if (visibleItemsInfo.isNotEmpty()) visibleItemsInfo.last().index else 0

    // Prefetch images around visible area
    LaunchedEffect(firstVisibleItemIndex, lastVisibleItemIndex) {
        if (imagesInside.isNotEmpty() && filePath.isNotEmpty()) {
            val prefetchStart = maxOf(0, firstVisibleItemIndex - 2)
            val prefetchEnd = minOf(imagesInside.size - 1, lastVisibleItemIndex + 2)

            // Prefetch visible and nearby images
            for (i in prefetchStart..prefetchEnd) {
                if (i >= 0 && i < imagesInside.size) {
                    val entry = imagesInside[i]
                    if (!extractedImages.containsKey(entry.name)) {
                        launch(Dispatchers.IO) {
                            val file = extractImageFromZip(filePath, entry.name, context)
                            extractedImages[entry.name] = file
                        }
                    }
                }
            }

            // Clean up images that are far from view
            val keysToRemove = extractedImages.keys.filter { entryName ->
                val index = imagesInside.indexOfFirst { it.name == entryName }
                index < prefetchStart - 3 || index > prefetchEnd + 3
            }

            keysToRemove.forEach { key ->
                extractedImages[key]?.delete()
                extractedImages.remove(key)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues.Absolute(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 8.dp
            ),
        ) {
            items(imagesInside, key = { it.name }) { zipEntry ->
                MangaPanel(extractedImages[zipEntry.name], zipEntry.name)
            }
        }

        if (imagesInside.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(50.dp)
            )
        }
    }
}

@Composable
fun MangaPanel(imageFile: File?, entryName: String) {
    val context = LocalContext.current

    SubcomposeAsyncImage(
        model = imageFile?.let { ImageRequest.Builder(context).data(it).build() },
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentScale = ContentScale.Fit,
        loading = {
            Box(
                modifier = Modifier
                    .height(600.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp)
                )
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("Failed to load: $entryName")
            }
        }
    )
}

// Modified to return a list of sorted zip entries
fun getZipFileEntries(zipFilePath: String): List<ZipEntry> {
    val zipEntries = mutableListOf<ZipEntry>()
    try {
        ZipFile(zipFilePath).use { zipFile ->
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && isImageFile(entry.name)) {
                    zipEntries.add(entry)
                }
            }
        }
    } catch (e: Exception) {
        Log.e("MangaView", "Error reading zip file", e)
    }

    // Sort entries by name for correct order
    return zipEntries.sortedBy { it.name.lowercase() }
}

// Helper function to check if a file is an image
fun isImageFile(fileName: String): Boolean {
    val extensions = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif")
    return extensions.any { fileName.lowercase().endsWith(it) }
}

// Modified to create a temporary file in the app's cache directory
fun extractImageFromZip(zipFilePath: String, entryName: String, context: Context): File? {
    return try {
        val tempDir = File(context.cacheDir, "manga_images").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File(tempDir, entryName.replace('/', '_'))

        // If the file already exists, just return it
        if (tempFile.exists()) {
            return tempFile
        }

        ZipFile(zipFilePath).use { zipFile ->
            val entry = zipFile.getEntry(entryName)
            if (entry != null && !entry.isDirectory) {
                zipFile.getInputStream(entry).use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                tempFile
            } else null
        }
    } catch (e: Exception) {
        Log.e("MangaView", "Error extracting image", e)
        null
    }
}

fun getPathFromUri(context: Context, uri: Uri): String? {
    var path: String? = null
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