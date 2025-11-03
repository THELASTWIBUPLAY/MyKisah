package com.example.mykisah

import android.content.Intent // Import Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels // Import viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mykisah.adapter.NovelAdapter // Import Adapter
import com.example.mykisah.databinding.HomepageMykisahBinding
import com.example.mykisah.viewmodel.MyKisahViewModel // Import ViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest // Import collectLatest
import kotlinx.coroutines.launch // Import launch

class MyKisahActivity : AppCompatActivity() {

    private lateinit var binding: HomepageMykisahBinding

    // Inisialisasi ViewModel
    private val viewModel: MyKisahViewModel by viewModels()

    // Inisialisasi Adapter
    private lateinit var novelAdapter: NovelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomepageMykisahBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup Adapter dan RecyclerView
        setupRecyclerView()

        // Setup listener FAB untuk pindah ke AddNovelActivity
        binding.TombolAddNovel.setOnClickListener {
            val intent = Intent(this, AddNovelActivity::class.java)
            startActivity(intent)
        }

        // Mulai "mendengarkan" data dari ViewModel
        observeNovels()
    }

    private fun setupRecyclerView() {
        novelAdapter = NovelAdapter(
            onItemClicked = { novel ->
                // Kode intent Anda yang sudah ada
                val intent = Intent(this@MyKisahActivity, NovelDetailActivity::class.java)
                intent.putExtra(NovelDetailActivity.EXTRA_NOVEL_ID, novel.id)
                intent.putExtra(NovelDetailActivity.EXTRA_NOVEL_TITLE, novel.title)
                startActivity(intent)
            },
            onItemLongClicked = { novel ->
                // Tampilkan dialog konfirmasi hapus
                showDeleteNovelDialog(novel)
            }
        )
        binding.container.apply {
            adapter = novelAdapter
            layoutManager = LinearLayoutManager(this@MyKisahActivity)
        }
    }

    private fun showDeleteNovelDialog(novel: com.example.mykisah.entity.MyNovel) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Novel?")
            .setMessage("Yakin ingin menghapus '${novel.title}'? Semua chapter dan karakter di dalamnya akan hilang permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                if (novel.id != null) {
                    viewModel.deleteNovel(novel.id!!)
                    Toast.makeText(this, "Novel dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun observeNovels() {
        // Gunakan lifecycleScope untuk mengamati Flow
        lifecycleScope.launch {
            viewModel.allNovels.collectLatest { novelList ->
                // Setiap ada data baru (dari StateFlow),
                // kirim ke adapter
                novelAdapter.submitList(novelList)

                // (Opsional) Tampilkan/sembunyikan loading
                // binding.uiLoading.visibility = View.GONE
            }
        }
    }
}