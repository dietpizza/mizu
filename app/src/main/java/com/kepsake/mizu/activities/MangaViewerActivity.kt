package com.kepsake.mizu.activities

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.kepsake.mizu.adapters.MangaPanelAdapter
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.data.viewmodels.MangaFileViewModel
import com.kepsake.mizu.data.viewmodels.MangaPageViewModel
import com.kepsake.mizu.databinding.ActivityMangaViewerBinding
import com.kepsake.mizu.logic.NaturalOrderComparator
import com.kepsake.mizu.utils.getMangaPagesAspectRatios
import com.kepsake.mizu.utils.getZipFileEntries
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

class MangaViewerActivity : ComponentActivity() {
    private val mangaPagesViewModel: MangaPageViewModel by viewModels()
    private val mangaFileViewModel: MangaFileViewModel by viewModels()
    private lateinit var binding: ActivityMangaViewerBinding
    private var currentPage = 0
    private val manga by lazy { intent.getParcelableExtra<MangaFile>("manga") }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyIfAvailable(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMangaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadMangaContent()
    }

    private fun setupRecyclerView() {
        binding.mangaReaderRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MangaViewerActivity)
            setHasFixedSize(true)
            addOnScrollListener(createScrollListener())
        }
    }

    private fun createScrollListener() = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()

            if (firstVisibleItemPosition >= 0 && firstVisibleItemPosition != currentPage) {
                currentPage = firstVisibleItemPosition
                manga?.let {
                    mangaFileViewModel.updateLastPage(it.id, currentPage)
                }
            }
        }
    }

    private fun loadMangaContent() {
        lifecycleScope.launch {
            manga?.let { mangaFile ->
                mangaPagesViewModel.getMangaPages(mangaFile.id).collectLatest { pages ->
                    if (pages.isEmpty()) {
                        loadNewPages(mangaFile)
                    } else {
                        displayPages(pages, mangaFile)
                    }
                }
            }
        }
    }

    private suspend fun loadNewPages(mangaFile: MangaFile) {
        delay(100)
        binding.mangaProcessingProgressBar.progress = 0

        val entries = getZipFileEntries(mangaFile.path)
            .sortedWith(compareBy(NaturalOrderComparator()) { it.name })

        val pageAspectRatioMap = getMangaPagesAspectRatios(
            this,
            mangaFile.path
        ) { progress ->
            val progressValue = (progress * 100f).toInt()
            binding.mangaProcessingProgressBar.setProgress(progressValue, true)
        }

        pageAspectRatioMap?.let { ratioMap ->
            val allPages = entries.mapNotNull { entry ->
                ratioMap[entry.name]?.let { aspectRatio ->
                    MangaPage(
                        entry.name,
                        aspectRatio,
                        mangaFile.id,
                        UUID.randomUUID().toString()
                    )
                }
            }
            mangaPagesViewModel.addMangaPages(allPages)
        }
    }

    private fun displayPages(pages: List<MangaPage>, mangaFile: MangaFile) {
        binding.mangaProcessing.visibility = View.INVISIBLE
        binding.mangaReaderRecyclerView.adapter = MangaPanelAdapter(mangaFile, pages)

        if (mangaFile.last_page > 0) {
            binding.mangaReaderRecyclerView.scrollToPosition(mangaFile.last_page)
        }
    }
}