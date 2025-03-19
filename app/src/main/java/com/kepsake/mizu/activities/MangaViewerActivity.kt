package com.kepsake.mizu.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kepsake.mizu.R
import com.kepsake.mizu.adapters.MangaImageAdapter
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.viewmodels.MangaPageViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class MangaViewerActivity : ComponentActivity() {

    private val viewModel: MangaPageViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var imageAdapter: MangaImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manga_viewer)

        // Load content
        val manga = intent.getParcelableExtra<MangaFile>("manga")

        recyclerView = findViewById(R.id.mangaReaderRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            if (manga != null) {
                viewModel.getMangaPages(manga.id).collectLatest { pages ->
                    // Update RecyclerView adapter with new data
                    imageAdapter = MangaImageAdapter(manga, pages)
                    recyclerView.adapter = imageAdapter
                }
            }
        }
    }
}