package com.example.mykisah

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mykisah.databinding.ActivityCharacterEditorBinding
import com.example.mykisah.viewmodel.CharacterEditorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CharacterEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCharacterEditorBinding
    private val viewModel: CharacterEditorViewModel by viewModels()
    private var currentCharacterId: String? = null

    private var hasLoadedInitialData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentCharacterId = intent.getStringExtra(EXTRA_CHARACTER_ID)
        if (currentCharacterId == null) {
            Toast.makeText(this, "Gagal memuat karakter", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        viewModel.loadCharacter(currentCharacterId!!)
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.characterDetails.collectLatest { character ->
                if (character != null && !hasLoadedInitialData) {
                    binding.etCharacterName.setText(character.name)
                    binding.etCharacterDesc.setText(character.description)
                    binding.toolbar.title = character.name
                    hasLoadedInitialData = true
                }
            }
        }
    }

    // Gunakan menu "Simpan" yang sudah ada (menu_editor.xml)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveCharacter()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveCharacter() {
        val newName = binding.etCharacterName.text.toString().trim()
        val newDesc = binding.etCharacterDesc.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveCharacter(newName, newDesc)
        Toast.makeText(this, "Karakter disimpan!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_CHARACTER_ID = "EXTRA_CHARACTER_ID"
    }
}