package com.kepsake.mizu.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.utils.extractImageFromZip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val TAG = "MangaPanel"

@Composable
fun MangaPanel(
    zipFilePath: String,
    mangaPage: MangaPage,
    pageCache: MutableMap<String, Bitmap?>,
    mangaId: String
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val cardHeight by remember { mutableStateOf(screenWidthDp / mangaPage.aspect_ratio) }
    var imageData by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(mangaPage, mangaId) {
        if (pageCache[mangaPage.page_name] == null) {
            imageData = withContext(Dispatchers.IO) {
                val bitmap = pageCache[mangaPage.page_name] ?: extractImageFromZip(
                    zipFilePath,
                    mangaPage.page_name,
                )
                pageCache[mangaPage.page_name] = bitmap
                bitmap
            }
        }
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(pageCache[mangaPage.page_name] ?: imageData)
            .crossfade(120)
            .diskCacheKey("${mangaId}_${mangaPage.page_name}")  // Use mangaId in cache key
            .memoryCacheKey("${mangaId}_${mangaPage.page_name}") // Use mangaId in cache key
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentScale = ContentScale.FillWidth,
        loading = {
            Card(
                shape = RectangleShape,
                modifier = Modifier
                    .height(cardHeight.dp)
                    .fillMaxWidth(),
            ) {}
        },
        error = {
            Card(
                shape = RectangleShape,
                modifier = Modifier
                    .height(cardHeight.dp)
                    .fillMaxWidth(),
            ) {}
        }
    )
}

