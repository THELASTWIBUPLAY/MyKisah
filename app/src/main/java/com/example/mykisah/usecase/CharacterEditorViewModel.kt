package com.example.mykisah.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mykisah.entity.MyCharacter
import com.example.mykisah.repository.NovelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CharacterEditorViewModel : ViewModel() {

    private val repository = NovelRepository()

    private val _characterDetails = MutableStateFlow<MyCharacter?>(null)
    val characterDetails: StateFlow<MyCharacter?> = _characterDetails.asStateFlow()

    private var currentCharacterId: String? = null

    fun loadCharacter(characterId: String) {
        currentCharacterId = characterId
        viewModelScope.launch {
            repository.getCharacterDetails(characterId).collect { character ->
                _characterDetails.value = character
            }
        }
    }

    fun saveCharacter(newName: String, newDescription: String) {
        if (currentCharacterId == null) return

        viewModelScope.launch {
            repository.updateCharacter(currentCharacterId!!, newName, newDescription)
        }
    }
}