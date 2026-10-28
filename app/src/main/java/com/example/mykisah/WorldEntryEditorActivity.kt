package com.example.mykisah

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mykisah.databinding.ActivityWorldEntryEditorBinding
import com.example.mykisah.entity.WorldEntry
import com.example.mykisah.viewmodel.WorldEntryEditorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorldEntryEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorldEntryEditorBinding
    private val viewModel: WorldEntryEditorViewModel by viewModels()
    private var currentEntryId: String? = null

    private var hasLoadedInitialData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorldEntryEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentEntryId = intent.getStringExtra(EXTRA_ENTRY_ID)
        if (currentEntryId == null) {
            Toast.makeText(this, "Gagal memuat entri", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        viewModel.loadEntry(currentEntryId!!)
        observeViewModel()
        setupCategoryDropdown() // Siapkan dropdown (opsional)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupCategoryDropdown() {
        // Anda bisa isi ini dengan kategori yang sudah ada, sama seperti di dialog
        // Untuk saat ini, kita biarkan user mengetik manual
        val categories = listOf("Karakter", "Lokasi", "Organisasi", "Kerajaan", "Item", "Lore")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        binding.acEntryCategory.setAdapter(adapter)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.entryDetails.collectLatest { entry ->
                if (entry != null && !hasLoadedInitialData) {
                    loadEntryData(entry)
                    hasLoadedInitialData = true
                }
            }
        }
    }

    private fun loadEntryData(entry: WorldEntry) {
        binding.etEntryName.setText(entry.name)
        binding.acEntryCategory.setText(entry.category, false) // 'false' agar tidak memicu filter
        binding.etEntryDescription.setText(entry.description)

        // --- LOGIKA TAGS (Load) ---
        // Ubah List<String> ["tag1", "tag2"] menjadi "tag1, tag2"
        val tagsString = entry.tags.joinToString(", ")
        binding.etEntryTags.setText(tagsString)
        // -------------------------

        binding.toolbar.title = entry.name
    }

    // Gunakan menu "Simpan" yang sudah ada (menu_editor.xml)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveEntry()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveEntry() {
        val newName = binding.etEntryName.text.toString().trim()
        val newCategory = binding.acEntryCategory.text.toString().trim()
        val newDescription = binding.etEntryDescription.text.toString().trim()

        // --- LOGIKA TAGS (Save) ---
        // Ubah String "tag1, tag2, " menjadi List<String> ["tag1", "tag2"]
        val tagsString = binding.etEntryTags.text.toString()
        val newTags = tagsString.split(",") // Pisahkan dengan koma
            .map { it.trim() } // Hapus spasi
            .filter { it.isNotEmpty() } // Hapus tag kosong
        // -------------------------

        if (newName.isEmpty() || newCategory.isEmpty()) {
            Toast.makeText(this, "Nama dan Kategori wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveEntry(newName, newCategory, newDescription, newTags)
        Toast.makeText(this, "Perubahan disimpan!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_ENTRY_ID = "EXTRA_ENTRY_ID"
    }
}