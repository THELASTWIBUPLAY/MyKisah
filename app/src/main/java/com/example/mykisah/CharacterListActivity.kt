package com.example.mykisah

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mykisah.adapter.CharacterAdapter
import com.example.mykisah.databinding.ActivityCharacterListBinding
import com.example.mykisah.databinding.DialogAddCharacterBinding
import com.example.mykisah.viewmodel.CharacterListViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CharacterListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCharacterListBinding
    private val viewModel: CharacterListViewModel by viewModels()
    private lateinit var characterAdapter: CharacterAdapter
    private var currentNovelId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Ambil Novel ID dari intent
        currentNovelId = intent.getStringExtra(EXTRA_NOVEL_ID)
        if (currentNovelId == null) {
            Toast.makeText(this, "Gagal memuat daftar karakter", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()

        // 2. Mulai muat data
        viewModel.loadCharacters(currentNovelId!!)

        // 3. Amati perubahan data
        observeCharacters()

        // 4. Setup FAB
        binding.fabAddCharacter.setOnClickListener {
            showAddCharacterDialog()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        characterAdapter = CharacterAdapter(
            onCharacterClicked = { character ->
                // Kode intent Anda dari Bagian 1
                val intent = Intent(this, CharacterEditorActivity::class.java)
                intent.putExtra(CharacterEditorActivity.EXTRA_CHARACTER_ID, character.id)
                startActivity(intent)
            },
            onCharacterLongClicked = { character ->
                // Tampilkan dialog konfirmasi hapus
                showDeleteCharacterDialog(character)
            }
        )
        binding.rvCharacters.apply {
            adapter = characterAdapter
            layoutManager = LinearLayoutManager(this@CharacterListActivity)
        }
    }

    private fun showDeleteCharacterDialog(character: com.example.mykisah.entity.MyCharacter) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Karakter?")
            .setMessage("Yakin ingin menghapus '${character.name}'?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                if (character.id != null) {
                    viewModel.deleteCharacter(character.id!!)
                    Toast.makeText(this, "Karakter dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun observeCharacters() {
        lifecycleScope.launch {
            viewModel.characters.collectLatest { characters ->
                characterAdapter.submitList(characters)
            }
        }
    }

    private fun showAddCharacterDialog() {
        val dialogBinding = DialogAddCharacterBinding.inflate(LayoutInflater.from(this))

        MaterialAlertDialogBuilder(this)
            .setTitle("Karakter Baru")
            .setView(dialogBinding.root)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val name = dialogBinding.etCharacterName.text.toString().trim()
                val desc = dialogBinding.etCharacterDesc.text.toString().trim()

                if (name.isNotEmpty()) {
                    viewModel.addCharacter(name, desc)
                    Toast.makeText(this, "Karakter disimpan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        // Kunci yang sama dengan NovelDetailActivity
        const val EXTRA_NOVEL_ID = "EXTRA_NOVEL_ID"
    }
}