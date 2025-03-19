package com.kepsake.mizu.activities

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.kepsake.mizu.R
import com.kepsake.mizu.adapters.MangaPanelAdapter
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.data.viewmodels.MangaFileViewModel
import com.kepsake.mizu.data.viewmodels.MangaPageViewModel
import com.kepsake.mizu.layout.FixedHeightLinearLayoutManager
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ConstraintLayout
    private lateinit var progressBarComponent: ProgressBar
    private lateinit var imageAdapter: MangaPanelAdapter
    private val context = this
    private var currentPage = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyIfAvailable(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manga_viewer)


        // Load content
        val manga = intent.getParcelableExtra<MangaFile>("manga")

        recyclerView = findViewById(R.id.mangaReaderRecyclerView)
        progressBar = findViewById(R.id.mangaProcessing)
        progressBarComponent = findViewById(R.id.mangaProcessingProgressBar)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.apply {
            setHasFixedSize(true)
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition =
                    layoutManager.findFirstCompletelyVisibleItemPosition()
                if (firstVisibleItemPosition >= 0 && firstVisibleItemPosition != currentPage) {
                    currentPage = firstVisibleItemPosition
                    manga?.let {
                        mangaFileViewModel.updateLastPage(it.id, currentPage)
                    }
                }
            }
        })


        lifecycleScope.launch {
            manga?.let { it ->
                mangaPagesViewModel.getMangaPages(it.id).collectLatest { pages ->
                    if (pages.isEmpty()) {
                        delay(100)
                        val entries = getZipFileEntries(manga.path)
                            .sortedWith(compareBy(NaturalOrderComparator()) { it.name })
                        val pageAspectRatioMap =
                            getMangaPagesAspectRatios(context, manga.path, { p ->
                                val progress = (p * 100f).toInt()
                                progressBarComponent.setProgress(progress, true)
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
                            mangaPagesViewModel.addMangaPages(allPages)
                        }
                    }
                    progressBar.visibility = View.INVISIBLE
                    recyclerView.adapter = MangaPanelAdapter(manga, pages)

                    if (manga.last_page > 0) {
                        recyclerView.scrollToPosition(manga.last_page)
                    }
                }
            }
        }
    }
}