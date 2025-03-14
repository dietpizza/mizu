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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kepsake.mizu.utils.clearPreviousMangaCache
import com.kepsake.mizu.utils.extractImageFromZip
import com.kepsake.mizu.utils.getPathFromUri
import com.kepsake.mizu.utils.getZipFileEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry

@Composable
fun MangaView(innerPadding: PaddingValues) {
    val context = LocalContext.current
    var filePath by remember { mutableStateOf<String>("") }
    var imagesInside by remember { mutableStateOf(emptyList<ZipEntry>()) }
    var isLoading by remember { mutableStateOf(false) }

    // Generate a unique ID for each loaded manga to distinguish cache files
    var currentMangaId by remember { mutableStateOf("") }

    // Add a paging state to only load visible images
    val lazyListState = rememberLazyListState()

    // Track which images are currently extracted to avoid repeated extraction
    val extractedImages = remember { mutableStateMapOf<String, File?>() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Set loading state to true immediately after file selection
            isLoading = true

            // Process the zip file on a background thread
            CoroutineScope(Dispatchers.IO).launch {
                val path = getPathFromUri(context, it)
                if (path != null) {
                    // Generate a new manga ID for this file
                    val newMangaId = UUID.randomUUID().toString()

                    // Clear any previously extracted images
                    extractedImages.clear()

                    // Delete previous manga cache files
                    clearPreviousMangaCache(context, currentMangaId)

                    // Get the entries in the background
                    val entries = getZipFileEntries(path)

                    // Update the UI on the main thread
                    withContext(Dispatchers.Main) {
                        filePath = path
                        currentMangaId = newMangaId
                        imagesInside = entries
                        Log.d("ROHAN", "Filename: ${filePath}")
                        entries.forEach { Log.d("ROHAN", "File: ${it.name}") }

                        // Loading complete
                        isLoading = false
                    }
                } else {
                    // If path extraction failed, update UI
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        } ?: run {
            // If user cancels file selection, update UI
            isLoading = false
        }
    }

    // Calculate the visible item indexes
    val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
    val firstVisibleItemIndex =
        if (visibleItemsInfo.isNotEmpty()) visibleItemsInfo.first().index else 0
    val lastVisibleItemIndex =
        if (visibleItemsInfo.isNotEmpty()) visibleItemsInfo.last().index else 0

    // Prefetch images around visible area
    LaunchedEffect(firstVisibleItemIndex, lastVisibleItemIndex, currentMangaId) {
        if (imagesInside.isNotEmpty() && filePath.isNotEmpty()) {
            val prefetchStart = maxOf(0, firstVisibleItemIndex - 2)
            val prefetchEnd = minOf(imagesInside.size - 1, lastVisibleItemIndex + 2)

            // Prefetch visible and nearby images
            for (i in prefetchStart..prefetchEnd) {
                if (i >= 0 && i < imagesInside.size) {
                    val entry = imagesInside[i]
                    if (!extractedImages.containsKey(entry.name)) {
                        launch(Dispatchers.IO) {
                            val file =
                                extractImageFromZip(filePath, entry.name, context, currentMangaId)
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
        if (imagesInside.isNotEmpty()) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues.Absolute(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 8.dp
                ),
            ) {
                items(imagesInside, key = { it.name }) { zipEntry ->
                    MangaPanel(filePath, zipEntry, extractedImages, currentMangaId)
                }
            }
        }

        // Show loading indicator when processing the zip file
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Loading...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Show empty state if no file is selected and not loading
        if (imagesInside.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "No manga loaded",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(
                        onClick = {
                            if (Environment.isExternalStorageManager()) {
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/x-cbz"
                                    )
                                )
                            } else {
                                val intent =
                                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Text("Select File")
                    }
                }
            }
        }
    }
}

