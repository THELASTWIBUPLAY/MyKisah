package com.example.mykisah

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mykisah.databinding.ActivityNovelSettingsBinding
import com.example.mykisah.viewmodel.NovelSettingsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NovelSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNovelSettingsBinding
    private val viewModel: NovelSettingsViewModel by viewModels()
    private var currentNovelId: String? = null

    private var hasLoadedInitialData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovelSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Ambil Novel ID
        currentNovelId = intent.getStringExtra(EXTRA_NOVEL_ID)
        if (currentNovelId == null) {
            Toast.makeText(this, "Gagal memuat pengaturan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()

        // 2. Mulai muat data
        viewModel.loadNovel(currentNovelId!!)

        // 3. Amati data
        observeViewModel()

        // 4. Setup Tombol Hapus
        binding.btnDeleteNovel.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.novelDetails.collectLatest { novel ->
                if (novel != null && !hasLoadedInitialData) {
                    binding.etNovelTitle.setText(novel.title)
                    binding.etNovelSynopsis.setText(novel.synopsis)
                    hasLoadedInitialData = true
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Novel?")
            .setMessage("Apakah Anda yakin ingin menghapus novel ini? Semua chapter dan karakter akan dihapus secara permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteNovel()
                Toast.makeText(this, "Novel dihapus", Toast.LENGTH_SHORT).show()

                // Kembali ke halaman utama (MyKisahActivity)
                val intent = Intent(this, MyKisahActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish() // Tutup semua activity di atasnya
            }
            .show()
    }

    // --- Menu Simpan (Reuse menu_editor.xml) ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveSettings() {
        val newTitle = binding.etNovelTitle.text.toString().trim()
        val newSynopsis = binding.etNovelSynopsis.text.toString().trim()

        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveSettings(newTitle, newSynopsis)
        Toast.makeText(this, "Perubahan disimpan!", Toast.LENGTH_SHORT).show()
        finish() // Tutup halaman pengaturan setelah menyimpan
    }
    // --------------------

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_NOVEL_ID = "EXTRA_NOVEL_ID"
    }
}