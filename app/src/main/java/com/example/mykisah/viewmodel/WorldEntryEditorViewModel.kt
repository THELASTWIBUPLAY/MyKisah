package com.example.mykisah.viewmodel

import android.util.Log
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
        // ViewModel bertahan saat Activity dibuat ulang, jadi jangan pasang
        // listener Firestore baru kalau entri yang dibuka sama.
        if (currentEntryId == entryId) return
        currentEntryId = entryId

        viewModelScope.launch {
            try {
                repository.getWorldEntryDetails(entryId).collect { entry ->
                    _entryDetails.value = entry
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memuat entri $entryId", e)
            }
        }
    }

    /**
     * Dipanggil untuk menyimpan perubahan.
     *
     * [onResult] menerima hasil penulisan ke Firestore supaya pemanggil bisa
     * memutuskan kapan draft lokal boleh dihapus.
     */
    fun saveEntry(
        newName: String,
        newCategory: String,
        newDescription: String,
        newTags: List<String>,
        newDetails: Map<String, String> = emptyMap(),
        onResult: ((Boolean) -> Unit)? = null
    ) {
        val entryId = currentEntryId
        if (entryId == null) {
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch {
            val success = repository.updateWorldEntry(
                entryId,
                newName,
                newCategory,
                newDescription,
                newTags,
                newDetails
            )
            onResult?.invoke(success)
        }
    }

    private companion object {
        const val TAG = "WorldEntryEditorViewModel"
    }
}