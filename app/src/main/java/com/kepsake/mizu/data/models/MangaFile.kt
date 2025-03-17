package com.kepsake.mizu.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "manga_files")
data class MangaFile(
    val path: String,
    val fileName: String,
    val firstImageEntry: String,
    @PrimaryKey val id: String
) : Parcelable