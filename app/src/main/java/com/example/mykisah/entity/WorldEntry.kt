package com.example.mykisah.entity

/**
 * Data model tunggal untuk semua entri "Wiki" atau World-Building.
 * Bisa berupa Karakter, Lokasi, Organisasi, Item, dll.
 */
data class WorldEntry(
    var id: String? = null,
    val novelId: String = "", // Menautkan entri ini ke novel yang benar
    val name: String = "",    // Nama entri (mis: "Kerajaan Astina", "Budi")
    val description: String = "", // Deskripsi lengkap / isi wiki

    // INI KUNCINYA: Tipe entri (Organisasi, Kerajaan, Lokasi, dll)
    val category: String = "Karakter",

    // INI UNTUK SISTEM TAGS ANDA
    val tags: List<String> = emptyList(),

    val timestamp: Long = System.currentTimeMillis(),
    var userId: String = "", // Pemilik entri
    val updatedAt: Long = 0L // Waktu terakhir diperbarui (untuk deteksi konflik)
)