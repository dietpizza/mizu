package com.kepsake.mizu.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "manga_files")
data class MangaFile(
    @PrimaryKey val id: String,
    val path: String,
    val name: String,
    val cover_path: String,
    val last_page: Int,
    val total_pages: Int
) : Parcelable