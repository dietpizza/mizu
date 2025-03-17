package com.kepsake.mizu.ui.components

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun MangaPanel(
    zipFilePath: String,
    mangaPage: MangaPage,
    extractedImages: MutableMap<String, File?>,
    mangaId: String
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val cardHeight by remember { mutableStateOf(screenWidthDp / mangaPage.aspect_ratio) }
    var imageFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(mangaPage, mangaId) {
        imageFile = extractedImages[mangaPage.page_name] ?: withContext(Dispatchers.IO) {
            val file = extractImageFromZip(
                context,
                zipFilePath,
                mangaPage.page_name,
                mangaId,
                "images"
            )
            extractedImages[mangaPage.page_name] = file
            file
        }
    }

    SubcomposeAsyncImage(
        model = imageFile?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .diskCacheKey("${mangaId}_${mangaPage.page_name}")  // Use mangaId in cache key
                .memoryCacheKey("${mangaId}_${mangaPage.page_name}") // Use mangaId in cache key
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        },
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

