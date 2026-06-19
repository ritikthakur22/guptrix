package com.guptrix.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.guptrix.app.database.AppDatabase
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    val allNotes: Flow<List<NoteEntity>>
    val favorites: Flow<List<NoteEntity>>
    val drafts: Flow<List<NoteEntity>>

    init {
        val noteDao = AppDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
        allNotes = repository.allNotes
        favorites = repository.favorites
        drafts = repository.drafts
    }

    fun insert(note: NoteEntity) = viewModelScope.launch {
        repository.insert(note)
    }

    fun searchNotes(query: String): Flow<List<NoteEntity>> {
        return repository.searchNotes(query)
    }

    fun clearAll() = viewModelScope.launch {
        repository.clearAll()
    }

    fun clearDrafts() = viewModelScope.launch {
        repository.clearDrafts()
    }

    fun toggleFavorite(noteId: String, isFavorite: Boolean) = viewModelScope.launch {
        repository.toggleFavorite(noteId, isFavorite)
    }
}
