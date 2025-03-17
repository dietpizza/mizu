package com.kepsake.mizu.data.repositores

import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.data.models.MangaPageDao
import kotlinx.coroutines.flow.Flow

class MangaPageRepository(private val mangaPageDao: MangaPageDao) {
    fun getPagesByMangaId(mangaId: String): Flow<List<MangaPage>> {
        return mangaPageDao.getPagesByMangaId(mangaId)
    }

    suspend fun insert(mangaPage: MangaPage): Long {
        return mangaPageDao.insert(mangaPage)
    }

    suspend fun insertAll(mangaPages: List<MangaPage>) {
        mangaPageDao.insertAll(mangaPages)
    }
}