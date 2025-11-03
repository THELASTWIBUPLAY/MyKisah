package com.example.mykisah

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mykisah.adapter.ChapterAdapter
import com.example.mykisah.databinding.ActivityNovelDetailBinding // Gunakan binding ini
import com.example.mykisah.databinding.DialogAddChapterBinding
import com.example.mykisah.usecase.NovelDetailViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NovelDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNovelDetailBinding
    // Inisialisasi ViewModel
    private val viewModel: NovelDetailViewModel by viewModels()
    private lateinit var chapterAdapter: ChapterAdapter

    private var currentNovelId: String? = null
    private var currentNovelTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNovelDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Ambil data yang dikirim dari Intent
        currentNovelId = intent.getStringExtra(EXTRA_NOVEL_ID)
        currentNovelTitle = intent.getStringExtra(EXTRA_NOVEL_TITLE)

        // 2. Validasi: Jika tidak ada ID, tutup halaman
        if (currentNovelId == null) {
            Toast.makeText(this, "Gagal memuat novel", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 3. Setup Toolbar
        binding.toolbar.title = currentNovelTitle ?: "Detail Novel"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Tampilkan tombol kembali

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()

        // Mulai ambil data
        viewModel.loadData(currentNovelId!!)

        // Mulai "mendengarkan" perubahan data
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.title = currentNovelTitle ?: "Detail Novel"
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        chapterAdapter = ChapterAdapter(
            onChapterClicked = { chapter ->
                // Kode intent Anda yang sudah ada
                val intent = Intent(this, ChapterEditorActivity::class.java)
                intent.putExtra(ChapterEditorActivity.EXTRA_CHAPTER_ID, chapter.id)
                startActivity(intent)
            },
            onChapterLongClicked = { chapter ->
                // Tampilkan dialog konfirmasi hapus
                showDeleteChapterDialog(chapter)
            }
        )
        binding.rvChapters.apply {
            adapter = chapterAdapter
            layoutManager = LinearLayoutManager(this@NovelDetailActivity)
        }
    }

    private fun showDeleteChapterDialog(chapter: com.example.mykisah.entity.MyChapter) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Chapter?")
            .setMessage("Yakin ingin menghapus '${chapter.title}'?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                if (chapter.id != null) {
                    viewModel.deleteChapter(chapter.id!!)
                    Toast.makeText(this, "Chapter dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun setupClickListeners() {
        // Tombol FAB Add Chapter
        binding.fabAddChapter.setOnClickListener {
            showAddChapterDialog()
        }

        // Tombol List Karakter
        binding.btnCharacterList.setOnClickListener {
            val intent = Intent(this, CharacterListActivity::class.java)
            intent.putExtra(CharacterListActivity.EXTRA_NOVEL_ID, currentNovelId)
            startActivity(intent)
        }
    }

    private fun observeViewModel() {
        // Amati perubahan detail novel (untuk sinopsis)
        lifecycleScope.launch {
            viewModel.novelDetails.collectLatest { novel ->
                binding.tvNovelSynopsis.text = novel?.synopsis ?: "Memuat sinopsis..."
            }
        }

        // Amati perubahan daftar chapter
        lifecycleScope.launch {
            viewModel.chapters.collectLatest { chapters ->
                chapterAdapter.submitList(chapters)
            }
        }
    }

    private fun showAddChapterDialog() {
        // Inflate layout dialog
        val dialogBinding = DialogAddChapterBinding.inflate(LayoutInflater.from(this))

        MaterialAlertDialogBuilder(this)
            .setTitle("Chapter Baru")
            .setView(dialogBinding.root)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->  // <-- Ganti 'dialog' menjadi '_'
                val title = dialogBinding.etChapterTitle.text.toString().trim()
                val content = dialogBinding.etChapterContent.text.toString().trim()

                if (title.isNotEmpty()) {
                    viewModel.addChapter(currentNovelId!!, title, content)
                    Toast.makeText(this, "Chapter disimpan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    // --- Menu Pengaturan Novel ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_novel_detail, menu) // Kita perlu buat file menu ini
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, NovelSettingsActivity::class.java)
                intent.putExtra(NovelSettingsActivity.EXTRA_NOVEL_ID, currentNovelId)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Fungsi untuk tombol kembali di toolbar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    // Ini adalah 'kunci' untuk mengirim dan menerima data
    companion object {
        const val EXTRA_NOVEL_ID = "EXTRA_NOVEL_ID"
        const val EXTRA_NOVEL_TITLE = "EXTRA_NOVEL_TITLE"
    }
}