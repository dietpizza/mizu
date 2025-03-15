package com.kepsake.mizu.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manga_files")
data class MangaFile(
    val path: String,
    val fileName: String,
    val firstImageEntry: String,
    @PrimaryKey val id: String
)