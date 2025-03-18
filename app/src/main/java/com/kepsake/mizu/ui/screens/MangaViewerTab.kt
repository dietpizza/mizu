package com.kepsake.mizu.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.data.models.toMangaFile
import com.kepsake.mizu.data.models.toMap
import com.kepsake.mizu.data.viewmodels.MangaFileViewModel
import com.kepsake.mizu.data.viewmodels.MangaPageViewModel
import com.kepsake.mizu.logic.NaturalOrderComparator
import com.kepsake.mizu.ui.components.MangaPanel
import com.kepsake.mizu.ui.components.PageTrackingLazyColumn
import com.kepsake.mizu.utils.getMangaPagesAspectRatios
import com.kepsake.mizu.utils.getZipFileEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID


@Composable
fun MangaViewerTab(
    innerPadding: PaddingValues,
    manga: MangaFile,
    mangaPageViewModel: MangaPageViewModel = viewModel(),
    mangaViewModel: MangaFileViewModel = viewModel()

) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val mangaPages by mangaPageViewModel.getMangaPages(manga.id)
        .map { pages -> pages.sortedWith(compareBy(NaturalOrderComparator()) { it.page_name }) }
        .collectAsState(initial = null)

    val extractedImages = remember { mutableStateMapOf<String, File?>() }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }


    LaunchedEffect(mangaPages) {
        if (mangaPages?.isNotEmpty() == true) {
            scope.launch {
                listState.scrollToItem(manga.last_page)
            }
        }
    }

    LaunchedEffect(mangaPages) {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            extractedImages.clear()

            if (mangaPages?.isEmpty() == true) {
                val entries = getZipFileEntries(manga.path)
                    .sortedWith(compareBy(NaturalOrderComparator()) { it.name })
                val pageAspectRatioMap = getMangaPagesAspectRatios(context, manga.path, { p ->
                    progress = p
                })

                if (pageAspectRatioMap != null) {
                    val allPages = entries.mapNotNull {
                        val id = UUID.randomUUID().toString()
                        val aspectRatio = pageAspectRatioMap.get(it.name)
                        if (aspectRatio != null) {
                            return@mapNotNull MangaPage(it.name, aspectRatio, manga.id, id)
                        }
                        null
                    }
                    mangaPageViewModel.addMangaPages(allPages)
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(mangaPages) {
        Log.e("ROHAN", "${mangaPages?.size}")
    }


    Box(modifier = Modifier.fillMaxSize()) {
        if (mangaPages?.isEmpty() == true) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Empty",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        if (isLoading || mangaPages?.isEmpty() == true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(30.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                    )
                }
            }
        }
        if (mangaPages?.isNotEmpty() == true) {
            PageTrackingLazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                onPageChange = { page ->
                    scope.launch {
                        val _current = manga.toMap()
                        val _newData = _current + mapOf("last_page" to page)
                        val _manga = _newData.toMangaFile()

                        mangaViewModel.update(_manga)
                        Log.e("ROHAN", "Page: ${page + 1}")
                    }
                },
                contentPadding = PaddingValues.Absolute(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 0.dp
                ),
            ) {
                items(items = mangaPages ?: emptyList(), key = { it.page_name }) { mangaPage ->
                    MangaPanel(manga.path, mangaPage, extractedImages, manga.id)
                }
            }
        }
    }
}
