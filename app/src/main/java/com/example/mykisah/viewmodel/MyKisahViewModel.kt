package com.example.mykisah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.MyNovel
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine // <-- 1. IMPORT INI
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyKisahViewModel : ViewModel() {

    private val repository = NovelRepository()

    // 2. Ini adalah daftar novel ASLI dari Firestore
    // 2. Ini adalah daftar novel ASLI dari Firestore
    private val _allNovels: StateFlow<List<MyNovel>> = repository.getAllNovels()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 3. Ini adalah StateFlow untuk menampung teks pencarian
    private val _searchQuery = MutableStateFlow("")

    // 4. GANTI 'allNovels' menjadi 'filteredNovels'
    //    Flow ini otomatis menggabungkan daftar novel dan query pencarian
    val filteredNovels: StateFlow<List<MyNovel>> =
        _searchQuery.combine(_allNovels) { query, novels ->
            if (query.isBlank()) {
                // Jika query kosong, tampilkan semua novel
                novels
            } else {
                // Jika query ada, filter novel berdasarkan judul atau sinopsis
                novels.filter {
                    it.title.contains(query, ignoreCase = true) ||
                            it.synopsis.contains(query, ignoreCase = true)
                }
            }
        }.stateIn( // Ubah hasil combine menjadi StateFlow
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 5. HAPUS 'val allNovels' yang lama (jika masih ada)

    /**
     * Fungsi untuk memicu penambahan novel baru
     */
    fun addNovel(title: String, synopsis: String) {
        viewModelScope.launch {
            val newNovel = MyNovel(
                title = title,
                synopsis = synopsis,
                timestamp = System.currentTimeMillis()
            )
            repository.addNovel(newNovel)
        }
    }

    // 6. TAMBAHKAN FUNGSI BARU INI untuk di panggil dari search bar
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Fungsi hapus novel (dari langkah sebelumnya)
    fun deleteNovel(novelId: String) {
        viewModelScope.launch {
            repository.deleteNovelAndSubCollections(novelId)
        }
    }
}