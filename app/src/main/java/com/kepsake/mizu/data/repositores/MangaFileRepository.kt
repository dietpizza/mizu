package com.kepsake.mizu.data.repositores

import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.models.MangaFileDao

import androidx.lifecycle.LiveData

class MangaFileRepository(private val mangaFileDao: MangaFileDao) {
    val allMangaFiles: LiveData<List<MangaFile>> = mangaFileDao.getAllMangaFiles()

    suspend fun insert(mangaFile: MangaFile) {
        mangaFileDao.insert(mangaFile)
    }

    suspend fun insertAll(mangaFiles: List<MangaFile>) {
        // For each manga file, check if it exists first
        for (mangaFile in mangaFiles) {
            val existingManga = mangaFileDao.getMangaFileByPathSync(mangaFile.path)
            if (existingManga == null) {
                // Only insert if no manga with the same path exists
                mangaFileDao.insert(mangaFile)
            }
        }
    }

    suspend fun delete(mangaFile: MangaFile) {
        mangaFileDao.delete(mangaFile)
    }

    suspend fun deleteAll() {
        mangaFileDao.deleteAll()
    }

    suspend fun update(mangaFile: MangaFile) {
        mangaFileDao.update(mangaFile)
    }

    fun getMangaFileById(id: String): LiveData<MangaFile> {
        return mangaFileDao.getMangaFileById(id)
    }
}