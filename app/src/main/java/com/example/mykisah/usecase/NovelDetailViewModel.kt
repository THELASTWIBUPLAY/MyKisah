package com.example.mykisah.usecase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.MyChapter
import com.example.mykisah.entity.MyNovel
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NovelDetailViewModel : ViewModel() {

    private val repository = NovelRepository()

    // StateFlow untuk menampung detail novel
    private val _novelDetails = MutableStateFlow<MyNovel?>(null)
    val novelDetails: StateFlow<MyNovel?> = _novelDetails.asStateFlow()

    // StateFlow untuk menampung daftar chapter
    private val _chapters = MutableStateFlow<List<MyChapter>>(emptyList())
    val chapters: StateFlow<List<MyChapter>> = _chapters.asStateFlow()

    /**
     * Dipanggil oleh Activity untuk mulai mengambil data
     */
    fun loadData(novelId: String) {
        // Ambil detail novel
        viewModelScope.launch {
            repository.getNovelDetails(novelId).collect { novel ->
                _novelDetails.value = novel
            }
        }

        // Ambil daftar chapter
        viewModelScope.launch {
            repository.getChaptersForNovel(novelId).collect { chapterList ->
                _chapters.value = chapterList
            }
        }
    }

    /**
     * Dipanggil untuk menambahkan chapter baru
     */
    fun addChapter(novelId: String, title: String, content: String) {
        viewModelScope.launch {
            val newChapter = MyChapter(
                novelId = novelId,
                title = title,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            repository.addChapter(newChapter)
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            repository.deleteChapter(chapterId)
        }
    }
}