package com.example.mykisah

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mykisah.databinding.ActivityWorldEntryEditorBinding
import com.example.mykisah.entity.WorldEntry
import com.example.mykisah.repository.DraftStore
import com.example.mykisah.viewmodel.WorldEntryEditorViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorldEntryEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWorldEntryEditorBinding
    private val viewModel: WorldEntryEditorViewModel by viewModels()
    private var currentEntryId: String? = null

    // Flag untuk mencegah update EditText saat user sedang mengetik
    private var hasLoadedInitialData = false

    // --- Auto-save state ---
    private var autoSaveJob: Job? = null
    private var isDirty = false // ada perubahan yang belum tersimpan?
    private lateinit var draftStore: DraftStore
    private val saveTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWorldEntryEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Draft lokal: jaring pengaman supaya ketikan tidak pernah hilang,
        // sama seperti di editor chapter.
        draftStore = DraftStore(this, scope = "entry")

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
        setupAutoSaveWatchers()
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
                    val entryId = currentEntryId ?: return@collectLatest

                    // Draft lokal hanya ada kalau simpan ke Firestore belum terkonfirmasi.
                    val draft = draftStore.load(entryId)
                    val name = draft?.get("name")?.takeIf { it.isNotBlank() } ?: entry.name
                    val category = draft?.get("category") ?: entry.category
                    val description = draft?.get("description") ?: entry.description
                    val tags = draft?.get("tags") ?: entry.tags.joinToString(", ")
                    val labels = detailLabels(category)
                    val detailValues = if (draft != null) {
                        listOf(
                            draft["detail_1"] ?: "",
                            draft["detail_2"] ?: "",
                            draft["detail_3"] ?: ""
                        )
                    } else {
                        listOf(
                            entry.details[labels[0]] ?: "",
                            entry.details[labels[1]] ?: "",
                            entry.details[labels[2]] ?: ""
                        )
                    }

                    loadEntryData(
                        name = name,
                        category = category,
                        description = description,
                        tagsString = tags,
                        detailValues = detailValues
                    )
                    hasLoadedInitialData = true

                    if (draft != null) {
                        // Ada ketikan yang belum sampai ke Firestore: tampilkan dan kirim ulang.
                        isDirty = true
                        setSaveStatus("Draft lokal dimuat")
                        scheduleAutoSave()
                    } else {
                        setSaveStatus("Tersimpan")
                    }
                }
            }
        }
    }

    private fun loadEntryData(
        name: String,
        category: String,
        description: String,
        tagsString: String,
        detailValues: List<String>
    ) {
        binding.etEntryName.setText(name)
        binding.acEntryCategory.setText(category, false) // 'false' agar tidak memicu filter
        applyDetailHints(category)
        binding.etDetail1.setText(detailValues.getOrElse(0) { "" })
        binding.etDetail2.setText(detailValues.getOrElse(1) { "" })
        binding.etDetail3.setText(detailValues.getOrElse(2) { "" })
        binding.etEntryDescription.setText(description)
        binding.etEntryTags.setText(tagsString)
        binding.toolbar.title = name
    }

    /** Label field detail sesuai kategori entri. */
    private fun detailLabels(category: String): List<String> =
        when (category.trim().lowercase(Locale.getDefault())) {
            "karakter" -> listOf("Peran", "Usia", "Penampilan")
            "lokasi" -> listOf("Tipe", "Iklim", "Posisi")
            "organisasi" -> listOf("Tujuan", "Pemimpin", "Anggota")
            "kerajaan" -> listOf("Pemimpin", "Wilayah", "Musuh")
            "item" -> listOf("Asal-usul", "Kekuatan", "Pemilik")
            "lore" -> listOf("Konteks", "Sumber", "Catatan")
            else -> listOf("Detail 1", "Detail 2", "Detail 3")
        }

    private fun applyDetailHints(category: String) {
        val labels = detailLabels(category)
        binding.tilDetail1.hint = labels[0]
        binding.tilDetail2.hint = labels[1]
        binding.tilDetail3.hint = labels[2]
    }

    /** Pasang auto-save di semua kolom entri (nama, kategori, deskripsi, tags). */
    private fun setupAutoSaveWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onUserEdited()
            }
        }
        binding.etEntryName.addTextChangedListener(watcher)
        binding.acEntryCategory.addTextChangedListener(watcher)
        binding.etEntryDescription.addTextChangedListener(watcher)
        binding.etEntryTags.addTextChangedListener(watcher)
        binding.etDetail1.addTextChangedListener(watcher)
        binding.etDetail2.addTextChangedListener(watcher)
        binding.etDetail3.addTextChangedListener(watcher)
    }

    /** Satu titik masuk untuk semua perubahan dari user. */
    private fun onUserEdited() {
        // setText() saat load data awal juga memicu watcher; abaikan itu.
        if (!hasLoadedInitialData) return

        // Label detail bisa berubah saat kategori diubah.
        applyDetailHints(binding.acEntryCategory.text.toString())
        isDirty = true
        persistDraftLocally()
        scheduleAutoSave()
    }

    /** Simpan draft ke perangkat lebih dulu, tanpa menunggu jaringan. */
    private fun persistDraftLocally() {
        val entryId = currentEntryId ?: return
        if (!hasLoadedInitialData) return

        draftStore.save(
            itemId = entryId,
            fields = mapOf(
                "name" to binding.etEntryName.text.toString(),
                "category" to binding.acEntryCategory.text.toString(),
                "description" to binding.etEntryDescription.text.toString(),
                "tags" to binding.etEntryTags.text.toString(),
                "detail_1" to binding.etDetail1.text.toString(),
                "detail_2" to binding.etDetail2.text.toString(),
                "detail_3" to binding.etDetail3.text.toString()
            )
        )
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = lifecycleScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            performAutoSave()
        }
    }

    /** Simpan diam-diam (tanpa Toast/finish) supaya tidak mengganggu user saat masih mengetik. */
    private fun performAutoSave() {
        val entryId = currentEntryId ?: return
        if (!hasLoadedInitialData) return

        val newName = binding.etEntryName.text.toString().trim()
        val newCategory = binding.acEntryCategory.text.toString().trim()
        val newDescription = binding.etEntryDescription.text.toString().trim()
        val newTags = parseTags(binding.etEntryTags.text.toString())
        val labels = detailLabels(newCategory)
        val newDetails = mapOf(
            labels[0] to binding.etDetail1.text.toString().trim(),
            labels[1] to binding.etDetail2.text.toString().trim(),
            labels[2] to binding.etDetail3.text.toString().trim()
        )

        // Nama/kategori kosong tidak boleh dikirim ke Firestore (bisa merusak daftar),
        // tapi draft lokalnya tetap aman.
        if (newName.isEmpty() || newCategory.isEmpty()) {
            setSaveStatus("Nama/Kategori kosong - belum disimpan")
            return
        }

        setSaveStatus("Menyimpan...")
        viewModel.saveEntry(newName, newCategory, newDescription, newTags, newDetails) { success ->
            if (success) {
                // Baru hapus draft lokal setelah server menerima datanya.
                isDirty = false
                draftStore.clear(entryId)
                setSaveStatus("Tersimpan ${saveTimeFormat.format(Date())}")
            } else {
                isDirty = true
                setSaveStatus("Gagal sinkron - draft aman di perangkat")
            }
        }
    }

    private fun flushPendingSave() {
        autoSaveJob?.cancel()
        if (isDirty) {
            performAutoSave()
        }
    }

    override fun onPause() {
        super.onPause()
        // Safety net: kalau user keluar/pindah app sebelum jeda debounce selesai,
        // simpan langsung sekarang juga.
        flushPendingSave()
    }

    private fun setSaveStatus(text: String) {
        binding.toolbar.subtitle = text
    }

    /** Ubah String "tag1, tag2, " menjadi List<String> ["tag1", "tag2"]. */
    private fun parseTags(tagsString: String): List<String> {
        return tagsString.split(",")
            .map { it.trim() } // Hapus spasi
            .filter { it.isNotEmpty() } // Hapus tag kosong
    }

    // Gunakan menu "Simpan" yang sudah ada (menu_editor.xml)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_entry_editor, menu)
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
        autoSaveJob?.cancel()

        val newName = binding.etEntryName.text.toString().trim()
        val newCategory = binding.acEntryCategory.text.toString().trim()
        val newDescription = binding.etEntryDescription.text.toString().trim()

        // --- LOGIKA TAGS (Save) ---
        val newTags = parseTags(binding.etEntryTags.text.toString())
        // -------------------------

        val labels = detailLabels(newCategory)
        val newDetails = mapOf(
            labels[0] to binding.etDetail1.text.toString().trim(),
            labels[1] to binding.etDetail2.text.toString().trim(),
            labels[2] to binding.etDetail3.text.toString().trim()
        )

        if (newName.isEmpty() || newCategory.isEmpty()) {
            Toast.makeText(this, "Nama dan Kategori wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        // Amankan dulu ke perangkat, baru kirim ke server.
        persistDraftLocally()

        viewModel.saveEntry(newName, newCategory, newDescription, newTags, newDetails) { success ->
            if (success) {
                isDirty = false
                currentEntryId?.let { draftStore.clear(it) }
                Toast.makeText(this, "Perubahan disimpan!", Toast.LENGTH_SHORT).show()
                finish() // Tutup editor setelah menyimpan
            } else {
                Toast.makeText(
                    this,
                    "Gagal menyimpan ke server. Draft tetap tersimpan di perangkat.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_ENTRY_ID = "EXTRA_ENTRY_ID"
        private const val AUTO_SAVE_DEBOUNCE_MS = 800L
    }
}