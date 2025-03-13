package com.kepsake.mizu.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier

import com.kepsake.mizu.ui.MangaView
import com.kepsake.mizu.ui.theme.MizuTheme
import com.kepsake.mizu.utils.getImages

class MangaReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MizuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MangaView(images = getImages(), innerPadding)
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
