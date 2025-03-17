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

fun MangaFile.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "path" to path,
        "name" to name,
        "cover_path" to cover_path,
        "last_page" to last_page,
        "total_pages" to total_pages
    )
}

fun Map<String, Any>.toMangaFile(): MangaFile {
    return MangaFile(
        id = this["id"] as String,
        path = this["path"] as String,
        name = this["name"] as String,
        cover_path = this["cover_path"] as String,
        last_page = this["last_page"] as Int,
        total_pages = this["total_pages"] as Int
    )
}
