# MyKisah

MyKisah adalah aplikasi Android untuk para penulis cerita/novel. Selain menulis bab, kamu bisa membangun "dunia" ceritamu dengan fitur wiki pribadi (world-building) yang saling terhubung lewat mention `@[Nama]`.

Dibangun dengan Kotlin + Firebase, berfokus pada kenyamanan menulis di HP: autosave, draft lokal, mode baca, word count, dan navigasi bab.

## Fitur Utama

### Menulis
- **Auto-save** dengan debounce — cerita disimpan otomatis saat berhenti mengetik.
- **Draft lokal** (SharedPreferences) — ketikan tidak hilang walau penulisan ke server gagal atau aplikasi ditutup paksa. Draft dipulihkan otomatis saat chapter dibuka lagi.
- **Live word count + estimasi waktu baca**.
- **Mode baca/pratinjau** untuk melihat cerita seperti buku tanpa kursor.
- **Cari & Ganti** untuk mengganti nama/mengingat istilah secara massal.
- **Writing toolbar** di atas keyboard: Undo, Redo, `@` (mention wiki), tanda petik `“”`, dan em-dash `—`.
- **Navigasi bab** sebelumnya/berikutnya langsung dari editor.
- **Status bab** (Draft / Selesai) dengan badge di daftar bab.

### Dunia / World-building
- **Entri wiki** terintegrasi (Karakter, Lokasi, Organisasi, Kerajaan, Item, Lore).
- **Field detail dinamis per kategori** — mis. Karakter mendapat *Peran/Usia/Penampilan*, Lokasi mendapat *Tipe/Iklim/Posisi*.
- **Mention `@[Nama]`** di teks cerita, dengan **quick-peek bottom sheet** untuk melihat ringkasan tanpa meninggalkan editor.
- **Backlink** — lihat berapa banyak chapter/entri yang menyebut sebuah entri.
- **Tags** untuk mengelompokkan entri.

### Manajemen
- **Ekspor/bagikan novel** sebagai teks Markdown.
- **Target jumlah kata per novel** dengan progress bar.
- **Cover novel** via URL.
- **Mode gelap** dengan preferensi tersimpan.
- **Kepemilikan data per user** (masing-masing akun hanya melihat datanya sendiri).

## Tech Stack

- **Kotlin** + **Android SDK** (minSdk 30, targetSdk 34)
- **Firebase** — Authentication (Google Sign-In) + Cloud Firestore
- **Material 3** (DayNight theme)
- **ViewModel** + **StateFlow/Flow**
- **ViewBinding**
- **Coil** (image loading)
- **Gradle Kotlin DSL**

## Prasyarat

- Android Studio (recent stable)
- JDK 11+
- Project Firebase dengan:
  - Firebase Authentication (Google provider) aktif
  - Cloud Firestore aktif
  - `google-services.json` diletakkan di `app/google-services.json`

## Build

```bash
./gradlew :app:assembleDebug
```

Untuk menjalankan unit test murni:

```bash
./gradlew :app:testDebugUnitTest
```

## Struktur Proyek (ringkas)

```
app/src/main/java/com/example/mykisah/
├── adapter/        RecyclerView adapter (novel, chapter, world entry)
├── entity/         Model data (MyNovel, MyChapter, WorldEntry)
├── repository/     Akses Firestore + penyimpanan draft lokal (DraftStore)
├── util/           Utilitas murni (WordCounter)
├── viewmodel/      ViewModel per layar
└── *.kt            Activity (Main, MyKisah, NovelDetail, ChapterEditor, World…)
```

## Catatan Penting

- **Security rules Firestore** tidak diatur lewat repo (dikelola di Firebase Console). Pastikan rules dikonfigurasi agar tiap user hanya bisa membaca/menulis dokumen miliknya (`request.auth.uid == resource.data.userId`).
- Data lama yang belum punya `userId` di-backfill otomatis saat aplikasi dibuka.

## Roadmap / Selanjutnya

- Tampilkan gambar entri wiki di quick-peek + field URL gambar di editor entri.
- Pagination (`limit` / infinite scroll) untuk koleksi yang besar.
- Versi riwayat (snapshot) per bab untuk undo di tingkat cloud.
- Ekspor/import penuh novel dalam satu file.

## Lisensi

Belum ditentukan.