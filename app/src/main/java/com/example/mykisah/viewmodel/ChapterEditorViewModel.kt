package com.example.mykisah.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.MyChapter
import com.example.mykisah.entity.WorldEntry
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChapterEditorViewModel : ViewModel() {

    private val repository = NovelRepository()

    private val _chapterDetails = MutableStateFlow<MyChapter?>(null)
    val chapterDetails: StateFlow<MyChapter?> = _chapterDetails.asStateFlow()
    private val _worldEntries = MutableStateFlow<List<WorldEntry>>(emptyList())
    val worldEntries: StateFlow<List<WorldEntry>> = _worldEntries.asStateFlow()

    private var currentChapterId: String? = null
    private var worldEntriesJob: Job? = null
    private var worldEntriesLoadedFor: String? = null

    /**
     * Dipanggil Activity untuk memuat data chapter
     */
    fun loadChapter(chapterId: String) {
        // ViewModel bertahan saat Activity dibuat ulang (mis. rotasi), jadi jangan
        // pasang listener Firestore baru kalau chapter-nya sama.
        if (currentChapterId == chapterId) return
        currentChapterId = chapterId
        worldEntriesLoadedFor = null

        viewModelScope.launch {
            try {
                repository.getChapterDetails(chapterId).collectLatest { chapter ->
                    _chapterDetails.value = chapter

                    // Muat entri wiki sekali per novel. Tanpa guard ini, setiap snapshot
                    // (termasuk setiap auto-save) menambah snapshot listener baru yang
                    // tidak pernah dilepas.
                    val novelId = chapter?.novelId
                    if (novelId != null && novelId != worldEntriesLoadedFor) {
                        worldEntriesLoadedFor = novelId
                        loadWorldEntries(novelId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memuat chapter $chapterId", e)
            }
        }
    }

    private fun loadWorldEntries(novelId: String) {
        worldEntriesJob?.cancel()
        worldEntriesJob = viewModelScope.launch {
            try {
                repository.getWorldEntriesForNovel(novelId).collectLatest { entries ->
                    _worldEntries.value = entries
                }
            } catch (e: Exception) {
                // Kegagalan wiki tidak boleh menghentikan editor/auto-save.
                Log.e(TAG, "Gagal memuat entri wiki untuk novel $novelId", e)
            }
        }
    }

    /**
     * Dipanggil untuk menyimpan perubahan.
     *
     * [onResult] menerima hasil penulisan ke Firestore supaya pemanggil bisa
     * memutuskan kapan draft lokal boleh dihapus.
     */
    fun saveChapter(newTitle: String, newContent: String, onResult: ((Boolean) -> Unit)? = null) {
        val chapterId = currentChapterId
        if (chapterId == null) {
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch {
            val success = repository.updateChapter(chapterId, newTitle, newContent)
            onResult?.invoke(success)
        }
    }

    private companion object {
        const val TAG = "ChapterEditorViewModel"
    }
}