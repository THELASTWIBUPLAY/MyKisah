package com.example.mykisah.repository

import android.util.Log
import com.example.mykisah.entity.MyChapter
import com.example.mykisah.entity.MyNovel
import com.example.mykisah.entity.WorldEntry
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.snapshots

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NovelRepository {

    // KEMBALIKAN KE VERSI KTX
    private val novelCollection = Firebase.firestore.collection("novels")
    private val chapterCollection = Firebase.firestore.collection("chapters")
    private val worldEntryCollection = Firebase.firestore.collection("world_entries")

    fun getAllNovels(): Flow<List<MyNovel>> {
        return novelCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots() // <-- KTX Flow
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    val novel = document.toObject(MyNovel::class.java)
                    novel?.id = document.id
                    novel
                }
            }
    }

    fun getNovelDetails(novelId: String): Flow<MyNovel?> {
        return novelCollection.document(novelId)
            .snapshots()
            .map { it.toObject(MyNovel::class.java) }
    }

    fun getChaptersForNovel(novelId: String): Flow<List<MyChapter>> {
        return chapterCollection
            .whereEqualTo("novelId", novelId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .snapshots() // <-- KTX Flow
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    val chapter = document.toObject(MyChapter::class.java)
                    chapter?.id = document.id
                    chapter
                }
            }
    }

    fun getChapterDetails(chapterId: String): Flow<MyChapter?> {
        return chapterCollection.document(chapterId)
            .snapshots()
            .map { it.toObject(MyChapter::class.java) }
    }


    fun getWorldEntriesForNovel(novelId: String): Flow<List<WorldEntry>> {
        return worldEntryCollection
            .whereEqualTo("novelId", novelId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    val entry = document.toObject(WorldEntry::class.java)
                    entry?.id = document.id
                    entry
                }
            }
    }

    fun getWorldEntryDetails(entryId: String): Flow<WorldEntry?> {
        return worldEntryCollection.document(entryId)
            .snapshots()
            .map { it.toObject(WorldEntry::class.java) }
    }

    // --- FUNGSI CREATE, UPDATE, DELETE (Semua .await() sudah KTX-ready) ---

    suspend fun addNovel(novel: MyNovel) {
        try { novelCollection.add(novel).await() }
        catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun addChapter(chapter: MyChapter) {
        try { chapterCollection.add(chapter).await() }
        catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun addWorldEntry(entry: WorldEntry) {
        try {
            worldEntryCollection.add(entry).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateNovel(novelId: String, newTitle: String, newSynopsis: String) {
        try { novelCollection.document(novelId).update(mapOf("title" to newTitle, "synopsis" to newSynopsis)).await() }
        catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun updateChapter(chapterId: String, newTitle: String, newContent: String): Boolean {
        return try {
            chapterCollection.document(chapterId)
                .update(mapOf("title" to newTitle, "content" to newContent))
                .await()
            true
        } catch (e: Exception) {
            // Jangan ditelan diam-diam: tanpa log ini, auto-save yang gagal terlihat
            // seolah-olah berhasil.
            Log.e(TAG, "Gagal menyimpan chapter $chapterId", e)
            false
        }
    }

    suspend fun updateWorldEntry(
        entryId: String,
        newName: String,
        newCategory: String,
        newDescription: String,
        newTags: List<String> // <-- Kita tambahkan tags di sini
    ) {
        try {
            worldEntryCollection.document(entryId)
                .update(
                    mapOf(
                        "name" to newName,
                        "category" to newCategory,
                        "description" to newDescription,
                        "tags" to newTags // <-- Simpan list of tags
                    )
                ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteChapter(chapterId: String) {
        try { chapterCollection.document(chapterId).delete().await() }
        catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteWorldEntry(entryId: String) {
        try {
            worldEntryCollection.document(entryId).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun deleteNovelAndSubCollections(novelId: String) {
        try {
            val batch = Firebase.firestore.batch()

            // (Kode hapus chapters dan characters tetap ada)
            val chaptersQuery = chapterCollection.whereEqualTo("novelId", novelId).get().await()
            for (document in chaptersQuery.documents) {
                batch.delete(document.reference)
            }

            // <-- TAMBAHKAN INI
            val worldEntriesQuery = worldEntryCollection.whereEqualTo("novelId", novelId).get().await()
            for (document in worldEntriesQuery.documents) {
                batch.delete(document.reference)
            }
            // ------------------

            // (Kode hapus novel doc tetap ada)
            val novelDocRef = novelCollection.document(novelId)
            batch.delete(novelDocRef)

            batch.commit().await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private companion object {
        const val TAG = "NovelRepository"
    }
}