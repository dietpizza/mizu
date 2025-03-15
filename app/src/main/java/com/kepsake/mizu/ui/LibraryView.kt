package com.kepsake.mizu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.kepsake.mizu.logic.NaturalOrderComparator
import com.kepsake.mizu.utils.getFilePathFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.zip.ZipFile

data class MangaFile(
    val uri: Uri,
    val fileName: String,
    val firstImageEntry: String,
    val id: String // Added UUID for cache management
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryView(innerPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var mangaFiles by remember { mutableStateOf<List<MangaFile>>(emptyList()) }
    var currentDirectoryUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Track which covers are currently extracted to avoid repeated extraction
    val extractedCovers = remember { mutableStateMapOf<String, File?>() }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            // Save the selected directory URI for future access
            currentDirectoryUri = it

            // Take persistent permission
            val takeFlags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)

            // Start loading comics from directory
            isLoading = true
            errorMessage = null

            coroutineScope.launch {
                try {
                    val comicUris = scanDirectoryForManga(context, it)
                    if (comicUris.isEmpty()) {
                        errorMessage = "No comic files found in this folder"
                    } else {
                        comicUris.forEach {
                            processManga(context, it)?.let {
                                mangaFiles += it
                            }
                        }
                    }
                } catch (e: Exception) {
                    errorMessage = "Error loading files: ${e.message}"
                    Log.e("Library", "Error processing directory", e)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            errorMessage != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        directoryPickerLauncher.launch(null)
                    }) {
                        Text("Retry")
                    }
                }
            }

            mangaFiles.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No comic files found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        directoryPickerLauncher.launch(null)
                    }) {
                        Text("Select Folder")
                    }
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding()
                        )
                        .fillMaxSize()
                ) {
                    items(mangaFiles, key = { it.id }) { comicFile ->
                        MangaCard(comicFile, extractedCovers)
                    }
                }
            }
        }
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { },
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) { }
    }
}

suspend fun processManga(context: Context, uri: Uri): MangaFile? =
    withContext(Dispatchers.IO) {
        try {
            val path = getFilePathFromUri(context, uri)
            if (path != null) {
                val fileName = File(path).name
                val firstImageEntry = findFirstImageEntryName(path)

                // Generate unique ID for each comic file for caching
                val id = UUID.randomUUID().toString()

                firstImageEntry?.let {
                    return@withContext MangaFile(uri, fileName, it, id)
                }
            }
            null
        } catch (e: Exception) {
            // Log error but continue processing other files
            Log.e("Library", "Error processing file $uri", e)
            null
        }
    }

//fun getFileName(context: Context, uri: Uri): String? {
//    val cursor = context.contentResolver.query(uri, null, null, null, null)
//    return cursor?.use {
//        val nameIndex = it.getColumnIndex("_display_name")
//        if (nameIndex >= 0 && it.moveToFirst()) {
//            it.getString(nameIndex)
//        } else null
//    } ?: uri.lastPathSegment
//}

suspend fun findFirstImageEntryName(path: String): String? =
    withContext(Dispatchers.IO) {
        try {
            val zipFile = ZipFile(File(path))
            val entries = zipFile.entries();
            val fileList = entries.toList().map { it.name }.sortedWith(NaturalOrderComparator())

            return@withContext fileList.firstOrNull()

        } catch (e: Exception) {
            Log.e("Library", "Error finding first image in zip", e)
        }
        null
    }


suspend fun scanDirectoryForManga(context: Context, directoryUri: Uri): List<Uri> =
    withContext(Dispatchers.IO) {
        val comicUris = mutableListOf<Uri>()

        try {
            val directory = DocumentFile.fromTreeUri(context, directoryUri)

            directory?.listFiles()?.forEach { file ->
                if (!file.isDirectory) {
                    val name = file.name?.lowercase() ?: ""
                    val mimeType = file.type

                    if (name.endsWith(".cbz") || name.endsWith(".zip") ||
                        mimeType == "application/zip" || mimeType == "application/x-cbz"
                    ) {
                        comicUris.add(file.uri)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Library", "Error scanning directory", e)
        }

        comicUris
    }