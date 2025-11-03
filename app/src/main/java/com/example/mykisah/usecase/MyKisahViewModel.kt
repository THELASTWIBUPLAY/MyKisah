package com.example.mykisah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.MyNovel
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyKisahViewModel : ViewModel() {

    private val repository = NovelRepository()

    // Ambil data novel sebagai StateFlow
    // Data akan tetap ada selama ViewModel hidup dan di-share ke UI
    val allNovels: StateFlow<List<MyNovel>> = repository.getAllNovels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Berhenti ambil data 5 detik setelah UI hancur
            initialValue = emptyList() // Nilai awal
        )

    /**
     * Fungsi untuk memicu penambahan novel baru
     */
    fun addNovel(title: String, synopsis: String) {
        viewModelScope.launch {
            val newNovel = MyNovel(
                title = title,
                synopsis = synopsis,
                timestamp = System.currentTimeMillis()
                // ID akan dibuat oleh Firestore
                // userId diabaikan sesuai permintaan
            )
            repository.addNovel(newNovel)
        }
    }

    fun deleteNovel(novelId: String) {
        viewModelScope.launch {
            repository.deleteNovelAndSubCollections(novelId)
        }
    }
}