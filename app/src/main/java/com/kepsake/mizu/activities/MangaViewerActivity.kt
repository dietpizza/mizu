package com.kepsake.mizu.activities

import android.animation.ObjectAnimator
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
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
import com.kepsake.mizu.utils.dpToPx
import com.kepsake.mizu.utils.getMangaPagesAspectRatios
import com.kepsake.mizu.utils.getZipFileEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SpaceItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = spaceHeight // Adds 8dp space below each item
    }
}

class MangaViewerActivity : ComponentActivity() {
    private val mangaPagesViewModel: MangaPageViewModel by viewModels()
    private val mangaFileViewModel: MangaFileViewModel by viewModels()
    private lateinit var binding: ActivityMangaViewerBinding
    private var currentPage = 0
    private val manga by lazy { intent.getParcelableExtra<MangaFile>("manga") }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
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
            addItemDecoration(SpaceItemDecoration(8.dpToPx()))
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
        val entries = getZipFileEntries(mangaFile.path)
            .sortedWith(compareBy(NaturalOrderComparator()) { it.name })


        val progressFlow = MutableStateFlow(0f)

        val progressJob = lifecycleScope.launch(Dispatchers.Main) {
            progressFlow.sample(300).collect { progress ->
                val progressValue = (progress * 100f)
                ObjectAnimator.ofInt(
                    binding.mangaProcessingProgressBar,
                    "progress",
                    binding.mangaProcessingProgressBar.progress,
                    progressValue.toInt()
                ).apply {
                    duration = 200 // Animation duration in milliseconds
                    interpolator = DecelerateInterpolator() // For a smooth deceleration effect
                    start()
                }
//                binding.mangaProcessingProgressBar.progress = progressValue.toInt()
            }
        }

        val pageAspectRatioMap = withContext(Dispatchers.IO) {
            getMangaPagesAspectRatios(
                this@MangaViewerActivity,  // Replace with your actual context
                mangaFile.path
            ) { progress ->
                progressFlow.value = progress
            }
        }

        progressJob.cancel()



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