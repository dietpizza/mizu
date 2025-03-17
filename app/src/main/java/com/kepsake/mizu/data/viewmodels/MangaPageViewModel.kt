package com.kepsake.mizu.data.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kepsake.mizu.data.MangaDatabase
import com.kepsake.mizu.data.models.MangaPage
import com.kepsake.mizu.data.repositores.MangaPageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MangaPageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MangaPageRepository

    init {
        val mangaPageDao = MangaDatabase.getDatabase(application).mangaPageDao()
        repository = MangaPageRepository(mangaPageDao)
    }

    fun getMangaPages(mangaId: String): Flow<List<MangaPage>> {
        return repository.getPagesByMangaId(mangaId)
    }

    fun addMangaPage(mangaPage: MangaPage) = viewModelScope.launch {
        repository.insert(mangaPage)
    }

    fun addMangaPages(mangaPages: List<MangaPage>) = viewModelScope.launch {
        repository.insertAll(mangaPages)
    }
}