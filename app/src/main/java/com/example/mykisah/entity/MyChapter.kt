package com.example.mykisah.entity

data class MyChapter(
    var id: String? = null,
    val novelId: String = "", // Untuk menautkan ke novel
    val title: String = "",
    val content: String = "", // Isi dari chapter
    val timestamp: Long = System.currentTimeMillis()
)