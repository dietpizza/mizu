package com.kepsake.mizu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.kepsake.mizu.ui.ImageGrid
import com.kepsake.mizu.ui.Library
import com.kepsake.mizu.ui.theme.MizuTheme
import com.kepsake.mizu.utils.getImages

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MizuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Library(innerPadding)
                }
            }
        }
    }
}

