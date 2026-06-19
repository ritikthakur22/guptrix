package com.guptrix.app.repository

import com.guptrix.app.database.NoteDao
import com.guptrix.app.database.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val favorites: Flow<List<NoteEntity>> = noteDao.getFavorites()
    val drafts: Flow<List<NoteEntity>> = noteDao.getDrafts()

    suspend fun insert(note: NoteEntity) {
        noteDao.insert(note)
    }

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return noteDao.searchNotes(query)
    }

    suspend fun clearAll() {
        noteDao.deleteAll()
    }
    
    suspend fun clearDrafts() {
        noteDao.deleteDrafts()
    }

    suspend fun toggleFavorite(noteId: String, isFavorite: Boolean) {
        noteDao.updateFavorite(noteId, isFavorite)
    }
}
