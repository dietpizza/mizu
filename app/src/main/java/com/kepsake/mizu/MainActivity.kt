package com.kepsake.mizu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.kepsake.mizu.ui.screens.HomeScreen
import com.kepsake.mizu.ui.theme.MizuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // In your Application class or main Activity's onCreate
        val imageLoader = ImageLoader.Builder(applicationContext)
            .memoryCache {
                MemoryCache.Builder(applicationContext)
                    // Use 25% of app memory for image cache
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    // Set a specific directory for caching
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    // Set maximum size for disk cache (e.g., 512MB)
                    .maxSizeBytes(512L * 1024 * 1024)
                    .build()
            }
            // Optional: Set a custom fetcher for specific URIs
            .components {
                // Add custom components if needed
                // For example: add(SvgDecoder.Factory())
            }
            .build()

        // Set the ImageLoader as the default for Coil
        Coil.setImageLoader(imageLoader)
        setContent {
            MizuTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(innerPadding)
                }
            }
        }
        if (!Environment.isExternalStorageManager()) {
            requestManageExternalStoragePermission()
        }
    }

    private fun requestManageExternalStoragePermission() {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }
}

