package com.example.mykisah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.MyCharacter
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CharacterListViewModel : ViewModel() {

    private val repository = NovelRepository()

    private val _characters = MutableStateFlow<List<MyCharacter>>(emptyList())
    val characters: StateFlow<List<MyCharacter>> = _characters.asStateFlow()

    private var currentNovelId: String? = null

    /**
     * Dipanggil Activity untuk memuat daftar karakter
     */
    fun loadCharacters(novelId: String) {
        currentNovelId = novelId // Simpan novelId
        viewModelScope.launch {
            repository.getCharactersForNovel(novelId).collect { characterList ->
                _characters.value = characterList
            }
        }
    }

    /**
     * Menambahkan karakter baru
     */
    fun addCharacter(name: String, description: String) {
        if (currentNovelId == null) return // Cek keamanan

        viewModelScope.launch {
            val newCharacter = MyCharacter(
                novelId = currentNovelId!!,
                name = name,
                description = description
            )
            repository.addCharacter(newCharacter)
        }
    }

    fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            repository.deleteCharacter(characterId)
        }
    }
}