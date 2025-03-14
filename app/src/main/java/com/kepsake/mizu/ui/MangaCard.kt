package com.kepsake.mizu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.kepsake.mizu.activities.MangaReaderActivity
import com.kepsake.mizu.utils.getPathFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

@Composable
fun MangaCard(mangaFile: MangaFile, extractedCovers: MutableMap<String, File?>) {
    val context = LocalContext.current
    var coverFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun onClick() {
        val intent = Intent(context, MangaReaderActivity::class.java)
        intent.putExtra("MANGA_PATH", getPathFromUri(context, mangaFile.uri))
        context.startActivity(intent)
    }

    // Try to get the cover image file from cache or extract it
    LaunchedEffect(mangaFile.id) {
        isLoading = true
        coverFile = extractedCovers[mangaFile.id] ?: withContext(Dispatchers.IO) {
            try {
                val file = extractImageFromZip(
                    context,
                    mangaFile.uri,
                    mangaFile.firstImageEntry,
                    mangaFile.id
                )
                extractedCovers[mangaFile.id] = file
                file
            } catch (e: Exception) {
                Log.e("Library", "Error extracting cover for ${mangaFile.fileName}", e)
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
                onClick()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Use Coil's SubcomposeAsyncImage for loading with placeholders
            SubcomposeAsyncImage(
                model = coverFile?.let {
                    ImageRequest.Builder(context)
                        .data(it)
                        .crossfade(true)
                        .diskCacheKey("${mangaFile.id}_cover")
                        .memoryCacheKey("${mangaFile.id}_cover")
                        .build()
                },
                contentDescription = mangaFile.fileName,
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
                    text = mangaFile.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
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

