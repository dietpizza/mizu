package com.kepsake.mizu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kepsake.mizu.utils.clearPreviousMangaCache
import com.kepsake.mizu.utils.extractImageFromZip
import com.kepsake.mizu.utils.getZipFileEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry


@Composable
fun MangaViewer(innerPadding: PaddingValues, initialFilePath: String?) {
    val context = LocalContext.current
    val filePath by remember { mutableStateOf(initialFilePath ?: "") }
    var imagesInside by remember { mutableStateOf(emptyList<ZipEntry>()) }
    var isLoading by remember { mutableStateOf(false) }
    var currentMangaId by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val extractedImages = remember { mutableStateMapOf<String, File?>() }


    LaunchedEffect(filePath) {
        if (filePath.isNotEmpty()) {
            isLoading = true
            CoroutineScope(Dispatchers.IO).launch {
                val newMangaId = UUID.randomUUID().toString()
                extractedImages.clear()
                clearPreviousMangaCache(context, currentMangaId)
                val entries = getZipFileEntries(filePath)
                withContext(Dispatchers.Main) {
                    currentMangaId = newMangaId
                    imagesInside = entries
                    isLoading = false
                }
            }
        }
    }

    val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
    val firstVisibleItemIndex = visibleItemsInfo.firstOrNull()?.index ?: 0
    val lastVisibleItemIndex = visibleItemsInfo.lastOrNull()?.index ?: 0

    LaunchedEffect(firstVisibleItemIndex, lastVisibleItemIndex, currentMangaId) {
        if (imagesInside.isNotEmpty() && filePath.isNotEmpty()) {
            val prefetchStart = maxOf(0, firstVisibleItemIndex - 2)
            val prefetchEnd = minOf(imagesInside.size - 1, lastVisibleItemIndex + 2)
            for (i in prefetchStart..prefetchEnd) {
                if (i in imagesInside.indices) {
                    val entry = imagesInside[i]
                    if (!extractedImages.containsKey(entry.name)) {
                        launch(Dispatchers.IO) {
                            extractImageFromZip(
                                context,
                                filePath,
                                entry.name,
                                currentMangaId,
                                cachePath = "images"
                            ).let {
                                extractedImages[entry.name] = it
                            }
                        }
                    }
                }
            }
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
                    bottom = innerPadding.calculateBottomPadding() + 0.dp
                ),
            ) {
                items(items = imagesInside, key = { it.name }) { zipEntry ->
                    MangaPanel(filePath, zipEntry, extractedImages, currentMangaId)
                }
            }
        }
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
                        modifier = Modifier.size(50.dp),
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
        if (imagesInside.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No manga loaded",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}
