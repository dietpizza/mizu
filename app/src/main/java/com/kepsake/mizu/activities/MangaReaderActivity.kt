package com.kepsake.mizu.activities

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.kepsake.mizu.data.MangaDatabase
import com.kepsake.mizu.data.models.MangaFile

import com.kepsake.mizu.ui.screens.MangaTab
import com.kepsake.mizu.ui.theme.MizuTheme

class MangaReaderActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        val mangaPath = intent.getStringExtra("manga.path") as String
        val mangaName = intent.getStringExtra("manga.name") as String
        val mangaCover = intent.getStringExtra("manga.cover") as String
        val mangaId = intent.getStringExtra("manga.id") as String

        val manga = MangaFile(mangaPath, mangaName, mangaCover, mangaId)


        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MizuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MangaTab(innerPadding, manga)
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
