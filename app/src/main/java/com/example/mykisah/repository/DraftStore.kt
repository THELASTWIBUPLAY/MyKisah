package com.example.mykisah.repository

import android.content.Context

/**
 * Penyimpanan draft lokal, bisa dipakai untuk chapter maupun entri wiki.
 *
 * Tujuannya: setiap kali user mengetik, isi tulisan langsung diamankan ke perangkat
 * (tanpa menunggu Firestore). Draft baru dihapus setelah simpan ke server terkonfirmasi,
 * jadi kalau penulisan gagal atau app ditutup paksa, isi tetap bisa dipulihkan.
 *
 * [scope] membedakan namespace tiap jenis konten (mis. "chapter" vs "entry")
 * supaya ID yang sama di jenis yang berbeda tidak saling menimpa.
 */
class DraftStore(context: Context, private val scope: String = "chapter") {

    private val prefs = context.applicationContext
        .getSharedPreferences("local_drafts", Context.MODE_PRIVATE)

    fun save(itemId: String, fields: Map<String, String>) {
        prefs.edit().apply {
            for ((key, value) in fields) {
                putString(keyText(itemId, key), value)
            }
            putLong(savedAtKey(itemId), System.currentTimeMillis())
        }.apply()
    }

    fun load(itemId: String): Map<String, String>? {
        val savedAt = prefs.getLong(savedAtKey(itemId), 0L)
        if (savedAt == 0L) return null

        // Kita tidak tahu field apa saja yang tersimpan, jadi baca semua kunci
        // yang diawali "$scope.$itemId." lalu kembalikan sebagai peta.
        val result = mutableMapOf<String, String>()
        val prefix = "$scope.$itemId."
        @Suppress("DEPRECATION")
        prefs.all.forEach { (key, value) ->
            if (key.startsWith(prefix) && value is String) {
                result[key.removePrefix(prefix)] = value
            }
        }
        return if (result.isEmpty()) null else result
    }

    fun clear(itemId: String) {
        val prefix = "$scope.$itemId."
        @Suppress("DEPRECATION")
        val keysToRemove = prefs.all.keys.filter { it.startsWith(prefix) }
        prefs.edit().apply {
            keysToRemove.forEach { remove(it) }
        }.apply()
    }

    private fun keyText(itemId: String, field: String) = "$scope.$itemId.$field"

    private fun savedAtKey(itemId: String) = "$scope.$itemId.__saved_at"
}