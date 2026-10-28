package com.example.mykisah.repository

import android.content.Context

/**
 * Penyimpanan draft lokal per chapter.
 *
 * Tujuannya: setiap kali user mengetik, isi chapter langsung diamankan ke perangkat
 * (tanpa menunggu Firestore). Draft baru dihapus setelah simpan ke Firestore terkonfirmasi,
 * jadi kalau penulisan ke server gagal atau app ditutup paksa, isi cerita tetap bisa dipulihkan.
 */
class ChapterDraftStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(chapterId: String, title: String, content: String) {
        prefs.edit()
            .putString(keyTitle(chapterId), title)
            .putString(keyContent(chapterId), content)
            .putLong(keySavedAt(chapterId), System.currentTimeMillis())
            .apply()
    }

    fun load(chapterId: String): Draft? {
        if (!prefs.contains(keyContent(chapterId))) return null
        return Draft(
            title = prefs.getString(keyTitle(chapterId), "").orEmpty(),
            content = prefs.getString(keyContent(chapterId), "").orEmpty(),
            savedAt = prefs.getLong(keySavedAt(chapterId), 0L)
        )
    }

    fun clear(chapterId: String) {
        prefs.edit()
            .remove(keyTitle(chapterId))
            .remove(keyContent(chapterId))
            .remove(keySavedAt(chapterId))
            .apply()
    }

    private fun keyTitle(chapterId: String) = "$chapterId.title"
    private fun keyContent(chapterId: String) = "$chapterId.content"
    private fun keySavedAt(chapterId: String) = "$chapterId.saved_at"

    data class Draft(
        val title: String,
        val content: String,
        val savedAt: Long
    )

    private companion object {
        const val PREFS_NAME = "chapter_drafts"
    }
}