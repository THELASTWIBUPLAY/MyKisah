package com.example.mykisah

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mykisah.adapter.WorldEntryAdapter
import com.example.mykisah.databinding.ActivityWorldBrowserBinding
import com.example.mykisah.databinding.DialogAddWorldEntryBinding
import com.example.mykisah.entity.WorldEntry
import com.example.mykisah.viewmodel.WorldBrowserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WorldBrowserActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var binding: ActivityWorldBrowserBinding
    private val viewModel: WorldBrowserViewModel by viewModels()
    private lateinit var worldEntryAdapter: WorldEntryAdapter
    private var currentNovelId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorldBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentNovelId = intent.getStringExtra(EXTRA_NOVEL_ID)
        if (currentNovelId == null) {
            Toast.makeText(this, "Gagal memuat wiki", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupClickListeners()

        viewModel.loadData(currentNovelId!!)
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        worldEntryAdapter = WorldEntryAdapter(
            onItemClicked = { entry ->
                // ----- UPDATE DI SINI -----
                // Ganti Toast dengan Intent ke Editor
                val intent = Intent(this, WorldEntryEditorActivity::class.java)
                intent.putExtra(WorldEntryEditorActivity.EXTRA_ENTRY_ID, entry.id)
                startActivity(intent)
                // --------------------------
            },
            onItemLongClicked = { entry ->
                showDeleteEntryDialog(entry)
            }
        )
        binding.rvWorldEntries.apply {
            adapter = worldEntryAdapter
            layoutManager = LinearLayoutManager(this@WorldBrowserActivity)
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Panggil ViewModel saat tab dipilih
                viewModel.setCategoryFilter(tab?.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupClickListeners() {
        binding.fabAddEntry.setOnClickListener {
            showAddEntryDialog()
        }
    }

    private fun observeViewModel() {
        // Amati daftar entri yang sudah difilter
        lifecycleScope.launch {
            viewModel.filteredEntries.collectLatest { entries ->
                worldEntryAdapter.submitList(entries)
            }
        }

        // Amati daftar kategori untuk me-refresh Tab
        lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                // Simpan tab yang sedang dipilih
                val selectedTab = binding.tabLayout.getTabAt(binding.tabLayout.selectedTabPosition)

                binding.tabLayout.removeAllTabs()
                var newTabIndex = 0
                categories.forEachIndexed { index, category ->
                    val tab = binding.tabLayout.newTab().setText(category)
                    binding.tabLayout.addTab(tab)
                    // Pilih kembali tab yang tadi aktif
                    if (category == selectedTab?.text) {
                        newTabIndex = index
                    }
                }
                binding.tabLayout.getTabAt(newTabIndex)?.select()
            }
        }
    }

    private fun showAddEntryDialog() {
        val dialogBinding = DialogAddWorldEntryBinding.inflate(LayoutInflater.from(this))

        // Setup dropdown untuk kategori
        // Kita ambil kategori yang sudah ada dari ViewModel
        val categories = viewModel.categories.value.filter { it != "SEMUA" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        dialogBinding.acEntryCategory.setAdapter(adapter)

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan") { _, _ ->
                val name = dialogBinding.etEntryName.text.toString().trim()
                val category = dialogBinding.acEntryCategory.text.toString().trim()
                val description = dialogBinding.etEntryDescription.text.toString().trim()

                if (name.isNotEmpty() && category.isNotEmpty()) {
                    viewModel.addWorldEntry(name, category, description)
                } else {
                    Toast.makeText(this, "Nama dan Kategori wajib diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDeleteEntryDialog(entry: WorldEntry) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus Entri?")
            .setMessage("Yakin ingin menghapus '${entry.name}'?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                if (entry.id != null) {
                    viewModel.deleteWorldEntry(entry.id!!)
                    Toast.makeText(this, "Entri dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Kita gunakan lagi menu yang sudah kita buat untuk Novel
        menuInflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(this)
        searchView.queryHint = "Cari di Wiki (nama, tag, deskripsi)..."

        return true
    }

    // Dipanggil saat user menekan 'Enter' (tidak kita pakai)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_NOVEL_ID = "EXTRA_NOVEL_ID"
    }
}