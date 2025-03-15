package com.kepsake.mizu.data.models

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MangaFileDao {
    @Query("SELECT * FROM manga_files")
    fun getAllMangaFiles(): LiveData<List<MangaFile>>

    @Query("SELECT * FROM manga_files WHERE id = :id")
    fun getMangaFileById(id: String): LiveData<MangaFile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mangaFile: MangaFile)

//    @Insert(onConflict = OnConflictStrategy.IGNORE)  // Changed from REPLACE to IGNORE
//    suspend fun insertAll(mangaFiles: List<MangaFile>)

    @Delete
    suspend fun deleteMangaFile(mangaFile: MangaFile)

    @Query("DELETE FROM manga_files")
    suspend fun deleteAllMangaFiles()

    @Query("SELECT * FROM manga_files WHERE path = :path LIMIT 1")
    suspend fun getMangaFileByPathSync(path: String): MangaFile?
}