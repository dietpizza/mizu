package com.kepsake.mizu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry

@Composable
fun MangaPanel(
    zipFilePath: String,
    zipEntry: ZipEntry,
    extractedImages: MutableMap<String, File?>,
    mangaId: String
) {
    val context = LocalContext.current
    var imageFile by remember { mutableStateOf<File?>(null) }

    // Get the file from the cache map or extract it if needed
    LaunchedEffect(zipEntry, mangaId) {
        imageFile = extractedImages[zipEntry.name] ?: withContext(Dispatchers.IO) {
            val file = com.kepsake.mizu.utils.extractImageFromZip(
                zipFilePath,
                zipEntry.name,
                context,
                mangaId
            )
            extractedImages[zipEntry.name] = file
            file
        }
    }

    SubcomposeAsyncImage(
        model = imageFile?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .diskCacheKey("${mangaId}_${zipEntry.name}")  // Use mangaId in cache key
                .memoryCacheKey("${mangaId}_${zipEntry.name}") // Use mangaId in cache key
                .build()
        },
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentScale = ContentScale.FillWidth,
        loading = {
            Card(
                modifier = Modifier
                    .height(600.dp)
                    .fillMaxWidth(),
            ) {}
        },
        error = {
            Card(
                modifier = Modifier
                    .height(600.dp)
                    .fillMaxWidth(),
            ) {}
        }
    )
}

