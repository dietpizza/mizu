package com.kepsake.mizu.ui.screens

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
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
import androidx.recyclerview.widget.RecyclerView
import com.kepsake.mizu.R
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.data.models.toMangaFile
import com.kepsake.mizu.data.models.toMap
import com.kepsake.mizu.data.viewmodels.MangaFileViewModel
import com.kepsake.mizu.data.viewmodels.MangaPageViewModel
import com.kepsake.mizu.logic.NaturalOrderComparator
import com.kepsake.mizu.ui.components.MangaPanel
import com.kepsake.mizu.ui.components.PageTrackingRecyclerView
import com.kepsake.mizu.utils.extractImageFromZip
import com.kepsake.mizu.utils.getMangaPagesAspectRatios
import com.kepsake.mizu.utils.getZipFileEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


@Composable
fun MangaViewerTab(
    innerPadding: PaddingValues,
    manga: MangaFile,
    mangaPageViewModel: MangaPageViewModel = viewModel(),
    mangaViewModel: MangaFileViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val mangaPages by mangaPageViewModel.getMangaPages(manga.id)
        .map { pages -> pages.sortedWith(compareBy(NaturalOrderComparator()) { it.page_name }) }
        .collectAsState(initial = null)

    val pageCache = remember { mutableStateMapOf<String, Bitmap?>() }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Adapter for RecyclerView
    val adapter = remember {
        object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): RecyclerView.ViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val view = inflater.inflate(R.layout.manga_panel, parent, false)
                return object : RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val mangaPage = mangaPages?.get(position)
                if (mangaPage != null) {
                    val imageView =
                        holder.itemView.findViewById<ImageView>(R.id.mangaImageViewSmall)
                    // Load the image into the ImageView (you can use Coil, Glide, etc.)
                    CoroutineScope(Dispatchers.IO).launch {
                        val bitmap = extractImageFromZip(manga.path, mangaPage.page_name)
                        Log.e(TAG, "Extracted image: ${mangaPage.page_name}")
                        withContext(Dispatchers.Main) {
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }
            }

            override fun getItemCount(): Int = mangaPages?.size ?: 0
        }
    }

    // Scroll to the last read page when mangaPages is loaded
    LaunchedEffect(mangaPages) {
        if (mangaPages?.isNotEmpty() == true) {
            scope.launch {
                // Scroll to the last page (you may need to adjust this logic for RecyclerView)
                // This requires access to the RecyclerView instance, which is not directly available here.
                // You can use a callback or a custom scroll listener to achieve this.
            }
        }
    }

    // Load manga pages if they are not already loaded
    LaunchedEffect(mangaPages) {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            pageCache.clear()

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
            PageTrackingRecyclerView(
                modifier = Modifier.fillMaxSize(),
                scrollToIndex = manga.last_page,
                onPageChange = { page ->
                    scope.launch {
                        val _current = manga.toMap()
                        val _newData = _current + mapOf("last_page" to page)
                        val _manga = _newData.toMangaFile()
                        Log.e(TAG, "Page change: $page")

                        mangaViewModel.update(_manga)
                    }
                },
                content = adapter
            )
        }
    }
}