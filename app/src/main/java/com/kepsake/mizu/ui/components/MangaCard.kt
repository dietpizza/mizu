package com.kepsake.mizu.ui.components

import android.content.Intent
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.kepsake.mizu.activities.MangaReaderActivity
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.utils.extractImageFromZip
import com.kepsake.mizu.utils.sanitizeCacheFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


@Composable
fun MangaCard(mangaFile: MangaFile) {
    val context = LocalContext.current
    var coverFile by remember { mutableStateOf<File?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun onClick() {
        val intent = Intent(context, MangaReaderActivity::class.java)
        intent.putExtra("MANGA_PATH", mangaFile.path)
        context.startActivity(intent)
    }

    LaunchedEffect(mangaFile.id) {
        isLoading = true

        var tempDir = File(context.cacheDir, "covers/${mangaFile.id}")
        var file = File(tempDir, sanitizeCacheFileName(mangaFile.firstImageEntry))


        if (file.exists()) {
            coverFile = file
        } else {
            Log.e("ROHAN", "File doesn't exists")
            coverFile = withContext(Dispatchers.IO) {
                try {
                    val file = extractImageFromZip(
                        context,
                        mangaFile.path,
                        mangaFile.firstImageEntry,
                        mangaFile.id,
                        "covers"
                    )
//                    extractedCovers[mangaFile.id] = file
                    file
                } catch (e: Exception) {
                    Log.e("Library", "Error extracting cover for ${mangaFile.fileName}", e)
                    null
                }
            }
        }
        isLoading = false
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(2 / 3f)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                    modifier = Modifier.padding(8.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

