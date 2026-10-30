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
import java.util.regex.Pattern

class ChapterEditorViewModel : ViewModel() {

    private val repository = NovelRepository()

    private val _chapterDetails = MutableStateFlow<MyChapter?>(null)
    val chapterDetails: StateFlow<MyChapter?> = _chapterDetails.asStateFlow()

    // Semua chapter di novel ini (urut dari timestamp, dipakai untuk nav prev/next).
    private val _chapters = MutableStateFlow<List<MyChapter>>(emptyList())
    val chapters: StateFlow<List<MyChapter>> = _chapters.asStateFlow()

    private val _worldEntries = MutableStateFlow<List<WorldEntry>>(emptyList())
    val worldEntries: StateFlow<List<WorldEntry>> = _worldEntries.asStateFlow()

    private val _mentionCounts = MutableStateFlow(MentionCounts())
    val mentionCounts: StateFlow<MentionCounts> = _mentionCounts.asStateFlow()

    private var currentChapterId: String? = null
    private var loadedNovelId: String? = null
    private var chaptersJob: Job? = null
    private var worldEntriesJob: Job? = null

    /**
     * Dipanggil Activity untuk memuat data chapter
     */
    fun loadChapter(chapterId: String) {
        // ViewModel bertahan saat Activity dibuat ulang (mis. rotasi), jadi jangan
        // pasang listener Firestore baru kalau chapter-nya sama.
        if (currentChapterId == chapterId) return
        currentChapterId = chapterId
        loadedNovelId = null

        viewModelScope.launch {
            try {
                repository.getChapterDetails(chapterId).collectLatest { chapter ->
                    _chapterDetails.value = chapter

                    // Muat daftar chapter + entri wiki sekali per novel. Tanpa guard ini,
                    // setiap snapshot (termasuk auto-save) menambah listener yang bocor.
                    val novelId = chapter?.novelId
                    if (novelId != null && novelId != loadedNovelId) {
                        loadedNovelId = novelId
                        loadChapters(novelId)
                        loadWorldEntries(novelId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memuat chapter $chapterId", e)
            }
        }
    }

    private fun loadChapters(novelId: String) {
        chaptersJob?.cancel()
        chaptersJob = viewModelScope.launch {
            try {
                repository.getChaptersForNovel(novelId).collectLatest { list ->
                    _chapters.value = list
                    recomputeMentions(list, _worldEntries.value)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gagal memuat daftar chapter novel $novelId", e)
            }
        }
    }

    private fun loadWorldEntries(novelId: String) {
        worldEntriesJob?.cancel()
        worldEntriesJob = viewModelScope.launch {
            try {
                repository.getWorldEntriesForNovel(novelId).collectLatest { entries ->
                    _worldEntries.value = entries
                    recomputeMentions(_chapters.value, entries)
                }
            } catch (e: Exception) {
                // Kegagalan wiki tidak boleh menghentikan editor/auto-save.
                Log.e(TAG, "Gagal memuat entri wiki untuk novel $novelId", e)
            }
        }
    }

    /** Hitung "disebut di mana" untuk setiap nama entri, dari chapter & deskripsi entri. */
    private fun recomputeMentions(chapters: List<MyChapter>, entries: List<WorldEntry>) {
        val chapterCounts = mutableMapOf<String, Int>()
        for (chapter in chapters) {
            for (name in mentionNames(chapter.content)) {
                chapterCounts[name] = (chapterCounts[name] ?: 0) + 1
            }
        }

        val entryCounts = mutableMapOf<String, Int>()
        for (entry in entries) {
            for (name in mentionNames(entry.description)) {
                entryCounts[name] = (entryCounts[name] ?: 0) + 1
            }
        }

        _mentionCounts.value = MentionCounts(chapterCounts, entryCounts)
    }

    private fun mentionNames(text: String): Set<String> {
        val names = mutableSetOf<String>()
        val matcher = WIKI_TAG_PATTERN.matcher(text)
        while (matcher.find()) {
            matcher.group(1)?.let { names.add(it) }
        }
        return names
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

    /** Toggle status draft <-> selesai untuk chapter yang sedang dibuka. */
    fun setChapterStatus(status: String, onResult: ((Boolean) -> Unit)? = null) {
        val chapterId = currentChapterId
        if (chapterId == null) {
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch {
            val success = repository.setChapterStatus(chapterId, status)
            // Setelah status berubah, snapshot chapter akan memperbarui chapterDetails.
            onResult?.invoke(success)
        }
    }

    data class MentionCounts(
        val chapters: Map<String, Int> = emptyMap(),
        val entries: Map<String, Int> = emptyMap()
    )

    private companion object {
        const val TAG = "ChapterEditorViewModel"
        val WIKI_TAG_PATTERN: Pattern = Pattern.compile("@\\[(.*?)]")
    }
}