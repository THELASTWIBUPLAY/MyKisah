package com.example.mykisah.entity

data class MyNovel(
    var id: String? = null, // Akan menyimpan ID dokumen Firestore
    val title: String = "",
    val synopsis: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    var userId: String = "" // Pemilik novel (UID pengguna yang login)
)