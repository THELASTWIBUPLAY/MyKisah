package com.example.mykisah

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mykisah.databinding.ActivityChapterEditorBinding
import com.example.mykisah.viewmodel.ChapterEditorViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChapterEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChapterEditorBinding
    private val viewModel: ChapterEditorViewModel by viewModels()
    private var currentChapterId: String? = null

    // Flag untuk mencegah update EditText saat user sedang mengetik
    private var hasLoadedInitialData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChapterEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Ambil Chapter ID
        currentChapterId = intent.getStringExtra(EXTRA_CHAPTER_ID)
        if (currentChapterId == null) {
            Toast.makeText(this, "Gagal memuat chapter", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()

        // 2. Mulai muat data
        viewModel.loadChapter(currentChapterId!!)

        // 3. Amati data
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.chapterDetails.collectLatest { chapter ->
                if (chapter != null && !hasLoadedInitialData) {
                    binding.etChapterTitle.setText(chapter.title)
                    binding.etChapterContent.setText(chapter.content)
                    // Set judul toolbar juga
                    binding.toolbar.title = chapter.title
                    hasLoadedInitialData = true // Tandai data awal sudah dimuat
                }
            }
        }
    }

    // --- Menu Simpan ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveChapter()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveChapter() {
        val newTitle = binding.etChapterTitle.text.toString().trim()
        val newContent = binding.etChapterContent.text.toString().trim()

        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.saveChapter(newTitle, newContent)
        Toast.makeText(this, "Perubahan disimpan!", Toast.LENGTH_SHORT).show()
        finish() // Tutup editor setelah menyimpan
    }
    // --------------------

    override fun onSupportNavigateUp(): Boolean {
        // Cek apakah ada perubahan? (Opsional, tapi bagus)
        // Jika ada, tampilkan dialog konfirmasi "Yakin ingin keluar?"
        // Untuk saat ini, kita langsung kembali
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_CHAPTER_ID = "EXTRA_CHAPTER_ID"
    }
}