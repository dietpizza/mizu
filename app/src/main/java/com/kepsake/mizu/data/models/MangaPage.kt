package com.kepsake.mizu.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "manga_pages")
data class MangaPage(
    val page_name: String,
    val aspect_ratio: Float,
    val manga_id: String,
    @PrimaryKey val id: String
) : Parcelable