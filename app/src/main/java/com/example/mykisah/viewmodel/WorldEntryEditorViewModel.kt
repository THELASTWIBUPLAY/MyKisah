package com.example.mykisah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.WorldEntry
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WorldEntryEditorViewModel : ViewModel() {

    private val repository = NovelRepository()

    private val _entryDetails = MutableStateFlow<WorldEntry?>(null)
    val entryDetails: StateFlow<WorldEntry?> = _entryDetails.asStateFlow()

    private var currentEntryId: String? = null

    /**
     * Dipanggil Activity untuk memuat data entri
     */
    fun loadEntry(entryId: String) {
        currentEntryId = entryId
        viewModelScope.launch {
            repository.getWorldEntryDetails(entryId).collect { entry ->
                _entryDetails.value = entry
            }
        }
    }

    /**
     * Dipanggil untuk menyimpan perubahan
     */
    fun saveEntry(
        newName: String,
        newCategory: String,
        newDescription: String,
        newTags: List<String>
    ) {
        if (currentEntryId == null) return

        viewModelScope.launch {
            repository.updateWorldEntry(
                currentEntryId!!,
                newName,
                newCategory,
                newDescription,
                newTags
            )
        }
    }
}