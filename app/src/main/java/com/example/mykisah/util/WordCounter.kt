package com.example.mykisah.util

/**
 * Utilitas murni untuk menghitung kata dan membersihkan markup wiki.
 * Dipisahkan agar mudah diuji (unit test) tanpa bergantung ke Android/Firestore.
 */
object WordCounter {

    /**
     * Hitung jumlah kata setelah markup `@[Nama]` diganti menjadi kata biasa.
     */
    fun count(text: String): Int {
        val plain = stripWikiMarkup(text)
        return plain.split(' ', '\n', '\r', '\t').count { it.isNotBlank() }
    }

    /**
     * Ubah `@[Kerajaan Astina]` menjadi ` Kerajaan Astina ` supaya tetap terhitung
     * sebagai kata yang utuh.
     */
    fun stripWikiMarkup(text: String): String =
        text.replace("@[", " ").replace("]", " ")
}