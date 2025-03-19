package com.kepsake.mizu.data.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.kepsake.mizu.data.MangaDatabase
import com.kepsake.mizu.data.models.MangaFile
import com.kepsake.mizu.data.repositores.MangaFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MangaFileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MangaFileRepository
    val allMangaFiles: LiveData<List<MangaFile>>

    init {
        val mangaFileDao = MangaDatabase.getDatabase(application).mangaFileDao()
        repository = MangaFileRepository(mangaFileDao)
        allMangaFiles = repository.allMangaFiles
    }

    fun insert(mangaFile: MangaFile) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(mangaFile)
    }

    fun insertAll(mangaFiles: List<MangaFile>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertAll(mangaFiles)
    }

    fun delete(mangaFile: MangaFile) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(mangaFile)
    }

    fun update(mangaFile: MangaFile) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(mangaFile)
    }

    fun updateLastPage(mangaId: String, lastPage: Int) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateLastPage(mangaId, lastPage)
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }

    fun getMangaFileById(id: String): LiveData<MangaFile> {
        return repository.getMangaFileById(id)
    }
}