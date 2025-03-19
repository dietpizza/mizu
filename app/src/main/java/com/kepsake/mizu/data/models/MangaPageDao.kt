package com.kepsake.mizu.data.models

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MangaPageDao {
    @Query("SELECT * FROM manga_pages WHERE manga_id = :mangaId")
    fun getPagesByMangaId(mangaId: String): Flow<List<MangaPage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mangaPage: MangaPage): Long

    @Transaction
    suspend fun insertAll(mangaPages: List<MangaPage>) {
        for (page in mangaPages) {
            val exists = checkIfExists(page.manga_id, page.page_name)
            if (!exists) {
                insert(page)
            }
        }
    }

    @Query("SELECT EXISTS(SELECT 1 FROM manga_pages WHERE manga_id = :mangaId AND page_name = :pageName)")
    suspend fun checkIfExists(mangaId: String, pageName: String): Boolean
}