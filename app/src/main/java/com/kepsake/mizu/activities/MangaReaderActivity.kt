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
    }
}
