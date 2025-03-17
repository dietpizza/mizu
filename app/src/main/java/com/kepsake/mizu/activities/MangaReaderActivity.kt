package com.kepsake.mizu.activities

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.kepsake.mizu.data.models.MangaFile

import com.kepsake.mizu.ui.screens.MangaViewerTab
import com.kepsake.mizu.ui.theme.MizuTheme

class MangaReaderActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        val manga = intent.getParcelableExtra<MangaFile>("manga")
//        val path = intent.getStringExtra("manga.path") as String
//        val name = intent.getStringExtra("manga.name") as String
//        val coverPath = intent.getStringExtra("manga.cover_path") as String
//        val id = intent.getStringExtra("manga.id") as String
//        val lastPage = intent.getIntExtra("manga.last_page", 0)
//        val totalPages = intent.getIntExtra("manga.total_pages", 0)
//        val manga =
//            MangaFile(id, path, name, coverPath, lastPage, totalPages)


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MizuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (manga != null) {
                        MangaViewerTab(innerPadding, manga)
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish() // Ensures the activity is destroyed when navigating back
            }
        })
    }
}
