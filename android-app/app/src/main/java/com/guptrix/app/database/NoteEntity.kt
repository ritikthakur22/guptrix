package com.guptrix.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val noteId: String,
    val title: String,
    val content: String,
    val updatedAt: Long,
    val isDraft: Boolean = false,
    val isFavorite: Boolean = false
)
