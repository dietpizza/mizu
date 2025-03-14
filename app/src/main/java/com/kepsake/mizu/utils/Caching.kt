package com.kepsake.mizu.utils

import android.content.Context
import java.io.File

fun clearPreviousMangaCache(context: Context, mangaId: String) {
    if (mangaId.isNotEmpty()) {
        val cacheDir = File(context.cacheDir, "manga_images/$mangaId")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
}

