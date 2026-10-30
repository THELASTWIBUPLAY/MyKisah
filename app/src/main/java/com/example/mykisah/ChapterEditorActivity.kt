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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mykisah.databinding.ActivityChapterEditorBinding
import com.example.mykisah.entity.WorldEntry
import com.example.mykisah.repository.DraftStore
import com.example.mykisah.viewmodel.ChapterEditorViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
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
    private var isPreviewing = false // sedang melihat pratinjau baca?
    private var currentStatus = "draft" // status bab: draft / selesai
    private lateinit var draftStore: DraftStore
    private val saveTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val WHITESPACE = Regex("\\s+")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChapterEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Draft lokal: jaring pengaman supaya ketikan tidak pernah hilang
        // walau penulisan ke Firestore gagal.
        draftStore = DraftStore(this, scope = "chapter")

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

        // Bar aksesori cepat (Undo/Redo/@/petik/em-dash) di atas keyboard.
        setupWritingToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun highlightWikiTags(s: Spannable) {
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
                            // SAAT DI-KLIK: Bukan pindah activity, tapi intip ringkas
                            // lewat bottom sheet supaya penulis tetap fokus menulis.
                            showEntryQuickPeek(entry)
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

    /** Tampilkan ringkasan entri wiki di bottom sheet tanpa meninggalkan editor. */
    private fun showEntryQuickPeek(entry: WorldEntry) {
        val peekView = layoutInflater.inflate(R.layout.view_wiki_peek, null)
        peekView.findViewById<TextView>(R.id.tv_peek_name).text = entry.name
        peekView.findViewById<TextView>(R.id.tv_peek_category).text = entry.category
        peekView.findViewById<TextView>(R.id.tv_peek_mentions).text = mentionLabel(entry.name)
        peekView.findViewById<TextView>(R.id.tv_peek_description).text =
            entry.description.ifBlank { "Belum ada deskripsi." }
        peekView.findViewById<Button>(R.id.btn_peek_edit).setOnClickListener {
            val intent = Intent(this, WorldEntryEditorActivity::class.java)
            intent.putExtra(WorldEntryEditorActivity.EXTRA_ENTRY_ID, entry.id)
            startActivity(intent)
        }

        val bottomSheet = BottomSheetDialog(this)
        bottomSheet.setContentView(peekView)
        bottomSheet.show()
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
                    val title = draft?.get("title")?.takeIf { it.isNotBlank() } ?: chapter.title
                    val content = draft?.get("content") ?: chapter.content

                    binding.etChapterTitle.setText(title)
                    binding.etChapterContent.setText(content)
                    binding.toolbar.title = title
                    hasLoadedInitialData = true
                    currentStatus = chapter.status

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

                updateWordCount()

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
            itemId = chapterId,
            fields = mapOf(
                "title" to binding.etChapterTitle.text.toString(),
                "content" to binding.etChapterContent.text.toString()
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

    /** Bar aksesori cepat untuk para penulis. */
    private fun setupWritingToolbar() {
        binding.btnUndo.setOnClickListener { undo() }
        binding.btnRedo.setOnClickListener { redo() }
        binding.btnInsertMention.setOnClickListener { insertAtCursor("@") }
        binding.btnInsertQuote.setOnClickListener { insertQuotePair() }
        binding.btnInsertDash.setOnClickListener { insertAtCursor("\u2014") }
    }

    private fun undo() {
        runCatching {
            binding.etChapterContent.onTextContextMenuItem(android.R.id.undo)
        }.onFailure { Log.w(TAG, "Undo gagal", it) }
    }

    private fun redo() {
        runCatching {
            binding.etChapterContent.onTextContextMenuItem(android.R.id.redo)
        }.onFailure { Log.w(TAG, "Redo gagal", it) }
    }

    /** Sisipkan teks di posisi kursor lalu letakkan kursor setelah teks. */
    private fun insertAtCursor(text: String) {
        val editor = binding.etChapterContent
        val editable = editor.text ?: return
        val cursor = editor.selectionStart.coerceIn(0, editable.length)
        editable.insert(cursor, text)
        editor.setSelection(cursor + text.length)
    }

    /** Sisipkan tanda petik “” lalu taruh kursor di antara keduanya. */
    private fun insertQuotePair() {
        val editor = binding.etChapterContent
        val editable = editor.text ?: return
        val cursor = editor.selectionStart.coerceIn(0, editable.length)
        editable.insert(cursor, "\u201C\u201D")
        editor.setSelection(cursor + 1)
    }

    /** Hitung kata (tanpa markup wiki) dan estimasi waktu baca. */
    private fun updateWordCount() {
        val text = binding.etChapterContent.text?.toString().orEmpty()
        val plain = WIKI_TAG_PATTERN.matcher(text).replaceAll("")
        val words = plain.trim().split(WHITESPACE).filter { it.isNotBlank() }.count()
        val minutes = if (words == 0) 0 else (words + READING_WPM - 1) / READING_WPM
        binding.tvWordCount.text = if (minutes > 0) {
            "$words kata · $minutes mnt baca"
        } else {
            "$words kata"
        }
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

    // --- Menu ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.action_toggle_status)?.title =
            if (currentStatus == "done") "Tandai Draft" else "Tandai Selesai"
        menu?.findItem(R.id.action_preview)?.title =
            if (isPreviewing) "Kembali Menulis" else "Pratinjau"
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveChapter()
                true
            }
            R.id.action_prev_chapter -> {
                goToAdjacentChapter(-1)
                true
            }
            R.id.action_next_chapter -> {
                goToAdjacentChapter(1)
                true
            }
            R.id.action_preview -> {
                togglePreview()
                true
            }
            R.id.action_find_replace -> {
                showFindReplaceDialog()
                true
            }
            R.id.action_toggle_status -> {
                toggleStatus()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Pindah ke bab sebelum/sebelumnya di novel yang sama. */
    private fun goToAdjacentChapter(direction: Int) {
        val chapters = viewModel.chapters.value
        val current = currentChapterId ?: return
        val index = chapters.indexOfFirst { it.id == current }
        if (index == -1) return

        val target = index + direction
        if (target in chapters.indices) {
            chapters[target].id?.let { openChapter(it) }
        }
    }

    private fun openChapter(newChapterId: String) {
        if (newChapterId == currentChapterId) return
        flushPendingSave()
        val intent = Intent(this, ChapterEditorActivity::class.java)
        intent.putExtra(EXTRA_CHAPTER_ID, newChapterId)
        startActivity(intent)
        finish()
    }

    /** Ganti tampilan editor dengan tampilan baca (tanpa kursor/IME). */
    private fun togglePreview() {
        isPreviewing = !isPreviewing
        if (isPreviewing) {
            renderPreview()
            binding.editorScroll.visibility = View.GONE
            binding.writingToolbar.visibility = View.GONE
            binding.previewScroll.visibility = View.VISIBLE
        } else {
            binding.previewScroll.visibility = View.GONE
            binding.editorScroll.visibility = View.VISIBLE
            binding.writingToolbar.visibility = View.VISIBLE
        }
        invalidateOptionsMenu()
    }

    private fun renderPreview() {
        binding.tvPreviewTitle.text = binding.etChapterTitle.text.toString()
        val content = binding.etChapterContent.text?.toString().orEmpty()
        binding.tvPreviewContent.setText(content, TextView.BufferType.SPANNABLE)
        val spannable = binding.tvPreviewContent.text as? Spannable
        if (spannable != null) {
            try {
                highlightWikiTags(spannable)
            } catch (e: Exception) {
                Log.w(TAG, "Gagal render pratinjau", e)
            }
            binding.tvPreviewContent.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun showFindReplaceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_find_replace, null)
        val etFind = dialogView.findViewById<EditText>(R.id.et_find)
        val etReplace = dialogView.findViewById<EditText>(R.id.et_replace)
        MaterialAlertDialogBuilder(this)
            .setTitle("Cari & Ganti")
            .setView(dialogView)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Ganti Semua") { _, _ ->
                val find = etFind.text.toString()
                val replace = etReplace.text.toString()
                if (find.isNotEmpty()) replaceAll(find, replace)
            }
            .show()
    }

    private fun replaceAll(find: String, replace: String) {
        val editor = binding.etChapterContent
        val editable = editor.text ?: return
        val before = editable.toString()
        val after = before.replace(find, replace)
        if (after != before) {
            val cursor = editor.selectionStart.coerceIn(0, after.length)
            editor.setText(after)
            editor.setSelection(cursor)
        }
    }

    private fun toggleStatus() {
        val new = if (currentStatus == "done") "draft" else "done"
        viewModel.setChapterStatus(new) { ok ->
            Toast.makeText(
                this,
                if (ok) (if (new == "done") "Ditandai selesai" else "Ditandai draft")
                else "Gagal mengubah status",
                Toast.LENGTH_SHORT
            ).show()
            if (ok) {
                currentStatus = new
                invalidateOptionsMenu()
            }
        }
    }

    /** Label "disebut di mana" untuk quick peek entri wiki. */
    private fun mentionLabel(name: String): String {
        val counts = viewModel.mentionCounts.value
        val chapterCount = counts.chapters[name] ?: 0
        val entryCount = counts.entries[name] ?: 0
        if (chapterCount == 0 && entryCount == 0) return "Belum pernah disebut"

        val parts = mutableListOf<String>()
        if (chapterCount > 0) parts.add("$chapterCount chapter")
        if (entryCount > 0) parts.add("$entryCount entri")
        return "Disebut di " + parts.joinToString(" · ")
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
        private const val READING_WPM = 200
        private const val TAG = "ChapterEditor"
    }
}