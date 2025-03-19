package com.kepsake.mizu.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kepsake.mizu.activities.MangaReaderActivity
import com.kepsake.mizu.activities.MangaViewerActivity
import com.kepsake.mizu.data.models.MangaFile
import java.io.File

@Composable
fun SimpleProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(Color.White.copy(alpha = 0.2f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(2.dp)
                .background(Color.White)
        )
    }
}

@Composable
fun MangaCard(manga: MangaFile) {
    val context = LocalContext.current

    fun onClick() {
        val intent = Intent(context, MangaViewerActivity::class.java)
        intent.putExtra("manga", manga)

        context.startActivity(intent)
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
                model = ImageRequest.Builder(context)
                    .data(File(manga.cover_path))
                    .crossfade(true)
                    .memoryCacheKey("${manga.id}_cover")
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = manga.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {}
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Retry",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            )

            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Text(
                        text = manga.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (manga.last_page > 0 && manga.total_pages > 0) {
                    SimpleProgressBar(
                        progress =
                        (manga.last_page.toFloat() + 1f) / manga.total_pages.toFloat()
                    )
                }
            }
        }
    }
}

