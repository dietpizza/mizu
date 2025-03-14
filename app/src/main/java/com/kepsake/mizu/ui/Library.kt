package com.kepsake.mizu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.kepsake.mizu.utils.getPathFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.IOException
import java.io.BufferedInputStream
import java.io.File
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ComicFile(
    val uri: Uri,
    val fileName: String,
    val firstImageEntry: String,
    val id: String // Added UUID for cache management
)

@Composable
fun Library(innerPadding: PaddingValues = PaddingValues()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var comicFiles by remember { mutableStateOf<List<ComicFile>>(emptyList()) }
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
                    val comicUris = findComicFilesInDirectory(context, it)
                    if (comicUris.isEmpty()) {
                        errorMessage = "No comic files found in this folder"
                    } else {
                        val processedFiles = processComicFiles(context, comicUris)
                        comicFiles = processedFiles
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

//    LaunchedEffect(Unit) {
//    directoryPickerLauncher.launch(null)
//        pickMultipleFiles.launch(
//            arrayOf(
//                "application/zip",
//                "application/x-cbz",
//                "application/octet-stream"
//            )
//        )
//    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Scanning folder for comics...",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

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

            comicFiles.isEmpty() -> {
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
                    items(comicFiles, key = { it.id }) { comicFile ->
                        MangaCardItem(comicFile, extractedCovers)
                    }
                }
            }
        }
    }
}

@Composable
fun MangaCardItem(comicFile: ComicFile, extractedCovers: MutableMap<String, File?>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var coverFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Try to get the cover image file from cache or extract it
    LaunchedEffect(comicFile.id) {
        isLoading = true
        coverFile = extractedCovers[comicFile.id] ?: withContext(Dispatchers.IO) {
            try {
                val file = extractImageFromZip(
                    context,
                    comicFile.uri,
                    comicFile.firstImageEntry,
                    comicFile.id
                )
                extractedCovers[comicFile.id] = file
                file
            } catch (e: Exception) {
                Log.e("Library", "Error extracting cover for ${comicFile.fileName}", e)
                null
            }
        }
        isLoading = false
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(2 / 3f)
            .clickable {
                // Handle opening comic
                coroutineScope.launch {
                    // Navigation to reader would go here
                    Log.d("Library", "Opening comic: ${comicFile.fileName}")
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Use Coil's SubcomposeAsyncImage for loading with placeholders
            SubcomposeAsyncImage(
                model = coverFile?.let {
                    ImageRequest.Builder(context)
                        .data(it)
                        .crossfade(true)
                        .diskCacheKey("${comicFile.id}_cover")
                        .memoryCacheKey("${comicFile.id}_cover")
                        .build()
                },
                contentDescription = comicFile.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No Cover",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            )

            // Title overlay at bottom
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = comicFile.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

suspend fun processComicFiles(context: Context, uris: List<Uri>): List<ComicFile> =
    withContext(Dispatchers.IO) {
        uris.filter { uri ->
            val mimeType = context.contentResolver.getType(uri)
            mimeType == "application/zip" || mimeType == "application/x-cbz" ||
                    mimeType == "application/octet-stream" || uri.toString()
                .endsWith(".cbz") || uri.toString().endsWith(".zip")
        }.mapNotNull { uri ->
            try {
                val fileName = getFileName(context, uri) ?: "Unknown"
                val firstImageEntry = findFirstImageEntryName(context, uri)

                // Generate unique ID for each comic file for caching
                val id = UUID.randomUUID().toString()

                firstImageEntry?.let {
                    ComicFile(uri, fileName, it, id)
                }
            } catch (e: Exception) {
                // Log error but continue processing other files
                Log.e("Library", "Error processing file $uri", e)
                null
            }
        }
    }

fun getFileName(context: Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        val nameIndex = it.getColumnIndex("_display_name")
        if (nameIndex >= 0 && it.moveToFirst()) {
            it.getString(nameIndex)
        } else null
    } ?: uri.lastPathSegment
}

suspend fun findFirstImageEntryName(context: Context, uri: Uri): String? =
    withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipInputStream ->
                    var entry: ZipEntry? = zipInputStream.nextEntry

                    // Just find the first image without collecting all entries
                    while (entry != null) {
                        val entryName = entry.name.lowercase()
                        // Skip directories and non-image files
                        if (!entry.isDirectory && (entryName.endsWith(".jpg") ||
                                    entryName.endsWith(".jpeg") ||
                                    entryName.endsWith(".png") ||
                                    entryName.endsWith(".webp"))
                        ) {
                            // Found an image, return immediately
                            return@withContext entry.name
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }

                    // No images found
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("Library", "Error finding first image in zip", e)
            null
        }
    }

suspend fun extractImageFromZip(
    context: Context,
    uri: Uri,
    entryName: String,
    cacheId: String
): File? = withContext(Dispatchers.IO) {
    try {
        // Create a cache directory for this specific comic
        val cacheDir = File(context.cacheDir, "mangas/$cacheId")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        // Create a file for the extracted image
        val outputFile = File(cacheDir, sanitizeFileName(entryName))

        // If the file already exists, just return it
        if (outputFile.exists() && outputFile.length() > 0) {
            return@withContext outputFile
        }

        // Use a more efficient buffer size for copying
        val bufferSize = 8 * 1024 // 8KB buffer

        // Extract the image
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
                var entry: ZipEntry? = zipInputStream.nextEntry
                while (entry != null) {
                    if (entry.name == entryName) {
                        // Found the image, extract it
                        outputFile.outputStream().buffered().use { output ->
                            zipInputStream.copyTo(output, bufferSize)
                        }
                        return@withContext outputFile
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
            }
        }
        null
    } catch (e: IOException) {
        Log.e("Library", "I/O error extracting image: $entryName", e)
        null
    } catch (e: Exception) {
        Log.e("Library", "Error extracting image: $entryName", e)
        null
    }
}


fun sanitizeFileName(fileName: String): String {
    return fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
}

suspend fun findComicFilesInDirectory(context: Context, directoryUri: Uri): List<Uri> =
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