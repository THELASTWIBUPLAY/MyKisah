package com.example.mykisah.repository

import com.example.mykisah.entity.MyChapter
import com.example.mykisah.entity.MyCharacter
import com.example.mykisah.entity.MyNovel
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
    private val characterCollection = Firebase.firestore.collection("characters")

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

    fun getCharactersForNovel(novelId: String): Flow<List<MyCharacter>> {
        return characterCollection
            .whereEqualTo("novelId", novelId)
            .snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { document ->
                    val character = document.toObject(MyCharacter::class.java)
                    character?.id = document.id
                    character
                }
            }
    }

    fun getChapterDetails(chapterId: String): Flow<MyChapter?> {
        return chapterCollection.document(chapterId)
            .snapshots()
            .map { it.toObject(MyChapter::class.java) }
    }

    fun getCharacterDetails(characterId: String): Flow<MyCharacter?> {
        return characterCollection.document(characterId)
            .snapshots()
            .map { it.toObject(MyCharacter::class.java) }
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
    suspend fun addCharacter(character: MyCharacter) {
        try { characterCollection.add(character).await() }
        catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun updateNovel(novelId: String, newTitle: String, newSynopsis: String) {
        try { novelCollection.document(novelId).update(mapOf("title" to newTitle, "synopsis" to newSynopsis)).await() }
        catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun updateChapter(chapterId: String, newTitle: String, newContent: String) {
        try { chapterCollection.document(chapterId).update(mapOf("title" to newTitle, "content" to newContent)).await() }
        catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun updateCharacter(characterId: String, newName: String, newDescription: String) {
        try { characterCollection.document(characterId).update(mapOf("name" to newName, "description" to newDescription)).await() }
        catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun deleteChapter(chapterId: String) {
        try { chapterCollection.document(chapterId).delete().await() }
        catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun deleteCharacter(characterId: String) {
        try { characterCollection.document(characterId).delete().await() }
        catch (e: Exception) { e.printStackTrace() }
    }
    suspend fun deleteNovelAndSubCollections(novelId: String) {
        try {
            val batch = Firebase.firestore.batch()
            val chaptersQuery = chapterCollection.whereEqualTo("novelId", novelId).get().await()
            for (document in chaptersQuery.documents) { batch.delete(document.reference) }
            val charactersQuery = characterCollection.whereEqualTo("novelId", novelId).get().await()
            for (document in charactersQuery.documents) { batch.delete(document.reference) }
            val novelDocRef = novelCollection.document(novelId)
            batch.delete(novelDocRef)
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}