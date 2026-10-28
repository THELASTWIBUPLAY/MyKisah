package com.example.mykisah

import android.content.Intent
import android.os.Bundle
import android.view.Menu // <-- 1. IMPORT INI
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView // <-- 2. IMPORT INI
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mykisah.adapter.NovelAdapter
import com.example.mykisah.databinding.HomepageMykisahBinding
import com.example.mykisah.viewmodel.MyKisahViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// 3. Tambahkan implementasi OnQueryTextListener
class MyKisahActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var binding: HomepageMykisahBinding
    private val viewModel: MyKisahViewModel by viewModels()
    private lateinit var novelAdapter: NovelAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomepageMykisahBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 4. Set Toolbar sebagai Action Bar
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerView()

        binding.TombolAddNovel.setOnClickListener {
            val intent = Intent(this, AddNovelActivity::class.java)
            startActivity(intent)
        }

        // Beri tanda kepemilikan (userId) ke data lama sebelum daftar novel dimuat.
        // Data lama dibuat sebelum fitur multi-user ada, jadi belum punya pemilik.
        // Aman: hanya menambah field, konten tidak diubah.
        val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (uid.isEmpty()) {
            observeNovels()
        } else {
            lifecycleScope.launch {
                viewModel.backfillOwnership()
                observeNovels()
            }
        }
    }

    private fun setupRecyclerView() {
        // (Ini kode Anda yang sudah ada, dengan onItemClicked dan onItemLongClicked)
        novelAdapter = NovelAdapter(
            onItemClicked = { novel ->
                val intent = Intent(this@MyKisahActivity, NovelDetailActivity::class.java)
                intent.putExtra(NovelDetailActivity.EXTRA_NOVEL_ID, novel.id)
                intent.putExtra(NovelDetailActivity.EXTRA_NOVEL_TITLE, novel.title)
                startActivity(intent)
            },
            onItemLongClicked = { novel ->
                showDeleteNovelDialog(novel)
            }
        )

        binding.container.apply {
            adapter = novelAdapter
            layoutManager = LinearLayoutManager(this@MyKisahActivity)
        }
    }

    private fun observeNovels() {
        lifecycleScope.launch {
            // 5. UBAH INI: Amati 'filteredNovels', BUKAN 'allNovels'
            viewModel.filteredNovels.collectLatest { novelList ->
                novelAdapter.submitList(novelList)
            }
        }
    }

    // ... (fungsi showDeleteNovelDialog Anda tetap di sini) ...
    private fun showDeleteNovelDialog(novel: com.example.mykisah.entity.MyNovel) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Novel?")
            .setMessage("Yakin ingin menghapus '${novel.title}'? Semua chapter dan entri wiki di dalamnya akan hilang permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                if (novel.id != null) {
                    viewModel.deleteNovel(novel.id!!)
                    Toast.makeText(this, "Novel dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    // --- 6. TAMBAHKAN SEMUA KODE DI BAWAH INI ---

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Kita gunakan file menu yang sudah ada dari Wiki
        menuInflater.inflate(R.menu.menu_search, menu)

        // Dapatkan item search
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "Cari novel..."
        searchView.setOnQueryTextListener(this)

        return true
    }

    // Dipanggil saat user menekan 'Enter'
    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    // Dipanggil setiap kali user mengetik
    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            // Kirim query ke ViewModel
            viewModel.setSearchQuery(newText)
        }
        return true
    }
}