package com.example.mykisah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.WorldEntry
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorldBrowserViewModel : ViewModel() {

    private val repository = NovelRepository()
    private var currentNovelId: String? = null

    private val _allEntries = MutableStateFlow<List<WorldEntry>>(emptyList())
    private val _categoryFilter = MutableStateFlow("SEMUA")

    // 1. TAMBAHKAN STATEFLOW BARU UNTUK SEARCH
    private val _searchQuery = MutableStateFlow("")

    // 2. Kategori unik (Kode ini tidak berubah)
    val categories: StateFlow<List<String>> = _allEntries
        .map { entries ->
            listOf("SEMUA") + entries.map { it.category }.distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("SEMUA"))

    // 3. UPDATE 'filteredEntries' UNTUK MENGGABUNGKAN 3 FLOW
    val filteredEntries: StateFlow<List<WorldEntry>> =
        combine(_allEntries, _categoryFilter, _searchQuery) { entries, category, query ->

            // Langkah 1: Filter berdasarkan Kategori
            val entriesByCategory = if (category == "SEMUA") {
                entries
            } else {
                entries.filter { it.category == category }
            }

            // Langkah 2: Filter hasil di atas berdasarkan Search Query
            if (query.isBlank()) {
                entriesByCategory // Jika query kosong, kembalikan hasil filter kategori
            } else {
                entriesByCategory.filter {
                    // Cari di nama, deskripsi, atau tags
                    it.name.contains(query, ignoreCase = true) ||
                            it.description.contains(query, ignoreCase = true) ||
                            it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. FUNGSI BARU UNTUK DIPANGGIL DARI SEARCH BAR
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Dipanggil Activity untuk memuat data awal
     */
    fun loadData(novelId: String) {
        currentNovelId = novelId
        viewModelScope.launch {
            repository.getWorldEntriesForNovel(novelId).collect { entries ->
                _allEntries.value = entries
            }
        }
    }

    /**
     * Dipanggil oleh Tab saat user memilih kategori
     */
    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    /**
     * Menambah entri baru
     */
    fun addWorldEntry(name: String, category: String, description: String) {
        if (currentNovelId == null) return

        val newEntry = WorldEntry(
            novelId = currentNovelId!!,
            name = name,
            category = category,
            description = description,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.addWorldEntry(newEntry)
        }
    }

    /**
     * Menghapus entri
     */
    fun deleteWorldEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteWorldEntry(entryId)
        }
    }
}