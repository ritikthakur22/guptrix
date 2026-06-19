package com.guptrix.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :searchQuery || '%' OR content LIKE '%' || :searchQuery || '%' ORDER BY updatedAt DESC")
    fun searchNotes(searchQuery: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDraft = 1 ORDER BY updatedAt DESC")
    fun getDrafts(): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes")
    suspend fun deleteAll()
    
    @Query("DELETE FROM notes WHERE isDraft = 1")
    suspend fun deleteDrafts()

    @Query("UPDATE notes SET isFavorite = :isFavorite WHERE noteId = :noteId")
    suspend fun updateFavorite(noteId: String, isFavorite: Boolean)
}
