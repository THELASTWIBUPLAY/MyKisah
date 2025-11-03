package com.example.mykisah.entity

data class MyCharacter(
    var id: String? = null,
    val novelId: String = "", // Untuk menautkan ke novel
    val name: String = "",
    val description: String = "" // Bio atau deskripsi karakter
)