package com.example.mykisah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.MyNovel
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NovelSettingsViewModel : ViewModel() {

    private val repository = NovelRepository()

    private val _novelDetails = MutableStateFlow<MyNovel?>(null)
    val novelDetails: StateFlow<MyNovel?> = _novelDetails.asStateFlow()

    private var currentNovelId: String? = null

    /**
     * Dipanggil Activity untuk memuat data novel
     */
    fun loadNovel(novelId: String) {
        currentNovelId = novelId
        viewModelScope.launch {
            // Kita hanya butuh data sekali, jadi tidak pakai Flow.collect
            repository.getNovelDetails(novelId).collect { novel ->
                _novelDetails.value = novel
            }
        }
    }

    /**
     * Dipanggil untuk menyimpan perubahan
     */
    fun saveSettings(newTitle: String, newSynopsis: String) {
        if (currentNovelId == null) return
        viewModelScope.launch {
            repository.updateNovel(currentNovelId!!, newTitle, newSynopsis)
        }
    }

    /**
     * Dipanggil untuk menghapus novel
     */
    fun deleteNovel() {
        if (currentNovelId == null) return
        viewModelScope.launch {
            repository.deleteNovelAndSubCollections(currentNovelId!!)
        }
    }
}