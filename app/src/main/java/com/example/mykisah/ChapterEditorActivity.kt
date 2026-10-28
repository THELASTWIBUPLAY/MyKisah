package com.example.mykisah

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mykisah.databinding.ActivityChapterEditorBinding
import com.example.mykisah.repository.ChapterDraftStore
import com.example.mykisah.viewmodel.ChapterEditorViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class ChapterEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChapterEditorBinding
    private val viewModel: ChapterEditorViewModel by viewModels()
    private var currentChapterId: String? = null

    // Flag untuk mencegah update EditText saat user sedang mengetik
    private var hasLoadedInitialData = false

    // 4. Flag untuk mencegah dialog muncul berulang kali
    private var isMentionDialogShowing = false

    // 2. TAMBAHKAN Regex Pattern sebagai variabel global
    // Ini adalah pola untuk mencari "@[Teks Apapun]"
    private val WIKI_TAG_PATTERN: Pattern = Pattern.compile("@\\[(.*?)]")

    // --- Auto-save state ---
    private var autoSaveJob: Job? = null
    private var isDirty = false // ada perubahan yang belum tersimpan?
    private var isFormatting = false // sedang menyorot tag wiki?
    private lateinit var draftStore: ChapterDraftStore
    private val saveTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChapterEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Draft lokal: jaring pengaman supaya ketikan tidak pernah hilang
        // walau penulisan ke Firestore gagal.
        draftStore = ChapterDraftStore(this)

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

        // 5. PASANG LISTENER KETIKAN DI SINI
        setupTextWatcher()

        // 6. PASANG AUTO-SAVE UNTUK JUDUL
        setupTitleAutoSave()

        // 3. TAMBAHKAN INI:
        // Agar ClickableSpan di dalam EditText bisa di-klik
        binding.etChapterContent.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun highlightWikiTags(s: Editable) {
        // Ambil daftar entri TEPAT SAAT INI dari viewmodel
        val entriesMap = viewModel.worldEntries.value.associateBy { it.name }
        if (entriesMap.isEmpty()) return // Tidak ada entri, tidak perlu highlight

        // Hapus span lama dulu (ini penting agar tidak tumpang tindih)
        val oldSpans = s.getSpans(0, s.length, ClickableSpan::class.java)
        for (span in oldSpans) {
            s.removeSpan(span)
        }
        val oldColorSpans = s.getSpans(0, s.length, ForegroundColorSpan::class.java)
        for (span in oldColorSpans) {
            s.removeSpan(span)
        }

        // Cari semua tag @[...] menggunakan Regex
        val matcher = WIKI_TAG_PATTERN.matcher(s)
        while (matcher.find()) {
            val start = matcher.start() // Posisi awal "@["
            val end = matcher.end()     // Posisi akhir "]"

            // Dapatkan nama di dalamnya (Grup 1 dari Regex)
            val entryName = matcher.group(1)

            if (entryName != null) {
                // Cek apakah nama ini ada di database wiki kita
                val entry = entriesMap[entryName]

                if (entry != null) {
                    // --- Buat Span yang bisa di-klik ---
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            // SAAT DI-KLIK: Buka WorldEntryEditorActivity
                            val intent = Intent(this@ChapterEditorActivity, WorldEntryEditorActivity::class.java)
                            intent.putExtra(WorldEntryEditorActivity.EXTRA_ENTRY_ID, entry.id)
                            startActivity(intent)
                        }
                    }

                    // --- Buat Span pewarna ---
                    // (Gunakan warna biru, atau warna 'colorPrimary' Anda)
                    val colorSpan = ForegroundColorSpan(Color.CYAN) // Ganti warnanya jika mau

                    // Terapkan span ke teks
                    s.setSpan(clickableSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    s.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                } else {
                    // Opsional: Beri warna merah jika tag-nya "broken" (tidak ada di wiki)
                    val colorSpan = ForegroundColorSpan(Color.RED)
                    s.setSpan(colorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
        }
    }

    // 6. UPDATE 'loadEntryData' di observeViewModel
    // (Kita perlu memanggil highlight saat data pertama kali dimuat)
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.chapterDetails.collectLatest { chapter ->
                if (chapter != null && !hasLoadedInitialData) {
                    val chapterId = currentChapterId ?: return@collectLatest

                    // Draft lokal hanya ada kalau simpan ke Firestore belum terkonfirmasi.
                    val draft = draftStore.load(chapterId)
                    val title = draft?.title?.takeIf { it.isNotBlank() } ?: chapter.title
                    val content = draft?.content ?: chapter.content

                    binding.etChapterTitle.setText(title)
                    binding.etChapterContent.setText(content)
                    binding.toolbar.title = title
                    hasLoadedInitialData = true

                    if (draft != null) {
                        // Ada ketikan yang belum sampai ke Firestore: tampilkan dan kirim ulang.
                        isDirty = true
                        setSaveStatus("Draft lokal dimuat")
                        scheduleAutoSave()
                    } else {
                        setSaveStatus("Tersimpan")
                    }

                    // PENTING: Panggil highlight setelah teks pertama dimuat
                    // (Butuh delay sedikit agar EditText selesai memproses teks)
                    binding.etChapterContent.postDelayed({
                        applyHighlight(binding.etChapterContent.editableText)
                    }, 100)
                }
            }
        }
    }

    /** Auto-save untuk judul chapter (terpisah dari watcher konten yang lebih kompleks). */
    private fun setupTitleAutoSave() {
        binding.etChapterTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onUserEdited()
            }
        })
    }

    private fun setupTextWatcher() {
        binding.etChapterContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (s == null || isFormatting) return

                // Catatan: listener TIDAK dilepas-pasang lagi. Dulu listener dilepas lalu
                // dipasang lagi di akhir fungsi; kalau salah satu langkah di tengah melempar
                // exception, listener tidak pernah terpasang kembali sehingga auto-save
                // mati diam-diam sampai editor ditutup.
                applyHighlight(s)
                showMentionDialogIfNeeded(s)
                onUserEdited()
            }
        })
    }

    /** Satu titik masuk untuk semua perubahan dari user (judul maupun isi). */
    private fun onUserEdited() {
        // setText() saat load data awal juga memicu watcher; abaikan itu.
        if (!hasLoadedInitialData) return

        isDirty = true
        persistDraftLocally()
        scheduleAutoSave()
    }

    /** Simpan draft ke perangkat lebih dulu, tanpa menunggu jaringan. */
    private fun persistDraftLocally() {
        val chapterId = currentChapterId ?: return
        if (!hasLoadedInitialData) return

        draftStore.save(
            chapterId = chapterId,
            title = binding.etChapterTitle.text.toString(),
            content = binding.etChapterContent.text.toString()
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
        val chapterId = currentChapterId ?: return
        if (!hasLoadedInitialData) return

        val title = binding.etChapterTitle.text.toString().trim()
        val content = binding.etChapterContent.text.toString().trim()

        // Judul kosong tidak boleh dikirim ke Firestore (bisa merusak daftar chapter),
        // tapi draft lokalnya tetap aman.
        if (title.isEmpty()) {
            setSaveStatus("Judul kosong - belum disimpan")
            return
        }

        setSaveStatus("Menyimpan...")
        viewModel.saveChapter(title, content) { success ->
            if (success) {
                // Baru hapus draft lokal setelah server menerima datanya.
                isDirty = false
                draftStore.clear(chapterId)
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

    /** Sorot tag wiki tanpa pernah membiarkan kegagalan formatting menghentikan auto-save. */
    private fun applyHighlight(s: Editable) {
        if (isFormatting) return
        isFormatting = true
        try {
            highlightWikiTags(s)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menyorot tag wiki", e)
        } finally {
            isFormatting = false
        }
    }

    /** Tampilkan dialog wiki hanya kalau karakter tepat sebelum kursor adalah '@'. */
    private fun showMentionDialogIfNeeded(s: Editable) {
        if (isMentionDialogShowing) return

        val cursorPosition = binding.etChapterContent.selectionStart
        if (cursorPosition <= 0 || cursorPosition > s.length) return

        if (s[cursorPosition - 1] == '@') {
            showWikiMentionDialog()
        }
    }

    // 7. FUNGSI BARU UNTUK MENAMPILKAN DIALOG WIKI
    private fun showWikiMentionDialog() {
        isMentionDialogShowing = true // Set flag agar dialog tidak tumpang tindih

        // Ambil daftar entri TEPAT SAAT INI dari viewmodel
        val entries = viewModel.worldEntries.value
        if (entries.isEmpty()) {
            Toast.makeText(this, "Tidak ada entri wiki ditemukan", Toast.LENGTH_SHORT).show()
            isMentionDialogShowing = false
            return
        }

        // Ubah list of objects menjadi list of names (String)
        val entryNames = entries.map { "${it.name} (${it.category})" }.toTypedArray()

        try {
            MaterialAlertDialogBuilder(this)
                .setTitle("Sebutkan Entri Wiki")
                .setItems(entryNames) { _, which ->
                    // getOrNull: daftar entri bisa berubah saat dialog masih terbuka.
                    entries.getOrNull(which)?.let { insertMention(it.name) }
                    isMentionDialogShowing = false
                }
                .setOnDismissListener {
                    // Jika dialog ditutup (misal, tekan 'back' atau area luar)
                    isMentionDialogShowing = false
                }
                .show()
        } catch (e: Exception) {
            // Jangan biarkan flag nyangkut di true: itu akan mematikan fitur mention.
            isMentionDialogShowing = false
            Log.w(TAG, "Gagal menampilkan dialog entri wiki", e)
        }
    }

    // 8. FUNGSI BARU UNTUK MEMASUKKAN TEKS KE EDITOR
    private fun insertMention(mentionText: String) {
        val editor = binding.etChapterContent
        val editable = editor.text ?: return

        // Batasi posisi kursor: selectionStart bisa di luar batas teks setelah
        // user menggeser kursor atau memilih blok teks.
        val cursorPosition = editor.selectionStart.coerceIn(0, editable.length)
        val start = (cursorPosition - 1).coerceAtLeast(0)

        // Buat markup @[Nama Entri]
        val markedUpText = "@[$mentionText] " // <-- Perubahan utamanya di sini

        editable.replace(start, cursorPosition, markedUpText)

        // Taruh kursor setelah markup yang baru disisipkan.
        editor.setSelection((start + markedUpText.length).coerceIn(0, editable.length))
    }

    // --- Menu Simpan ---
    // Tombol ini sekarang opsional (auto-save sudah berjalan di background),
    // tapi tetap dipertahankan untuk simpan instan + langsung keluar.
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
        autoSaveJob?.cancel()

        val newTitle = binding.etChapterTitle.text.toString().trim()
        val newContent = binding.etChapterContent.text.toString().trim()

        if (newTitle.isEmpty()) {
            Toast.makeText(this, "Judul tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Amankan dulu ke perangkat, baru kirim ke server.
        persistDraftLocally()

        viewModel.saveChapter(newTitle, newContent) { success ->
            if (success) {
                isDirty = false
                currentChapterId?.let { draftStore.clear(it) }
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
        private const val AUTO_SAVE_DEBOUNCE_MS = 800L
        private const val TAG = "ChapterEditor"
    }
}