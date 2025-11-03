package com.example.mykisah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.MyChapter
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChapterEditorViewModel : ViewModel() {

    private val repository = NovelRepository()

    private val _chapterDetails = MutableStateFlow<MyChapter?>(null)
    val chapterDetails: StateFlow<MyChapter?> = _chapterDetails.asStateFlow()

    private var currentChapterId: String? = null

    /**
     * Dipanggil Activity untuk memuat data chapter
     */
    fun loadChapter(chapterId: String) {
        currentChapterId = chapterId
        viewModelScope.launch {
            repository.getChapterDetails(chapterId).collect { chapter ->
                _chapterDetails.value = chapter
            }
        }
    }

    /**
     * Dipanggil untuk menyimpan perubahan
     */
    fun saveChapter(newTitle: String, newContent: String) {
        if (currentChapterId == null) return // Cek keamanan

        viewModelScope.launch {
            repository.updateChapter(currentChapterId!!, newTitle, newContent)
        }
    }
}