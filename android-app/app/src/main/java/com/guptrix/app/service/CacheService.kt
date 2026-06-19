package com.guptrix.app.service

import android.webkit.JavascriptInterface
import com.guptrix.app.database.NoteEntity
import com.guptrix.app.repository.NoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class CacheService(private val repository: NoteRepository) {

    @JavascriptInterface
    fun saveExtractedNote(url: String, title: String, content: String) {
        if (title.isBlank() && content.isBlank()) return
        
        // Use URL as noteId or generate one if not available
        val noteId = if (url.contains("/card/")) {
            url.substringAfterLast("/card/")
        } else {
            UUID.nameUUIDFromBytes(url.toByteArray()).toString()
        }

        val note = NoteEntity(
            noteId = noteId,
            title = title,
            content = content,
            updatedAt = System.currentTimeMillis(),
            isDraft = false,
            isFavorite = false
        )

        CoroutineScope(Dispatchers.IO).launch {
            repository.insert(note)
        }
    }
}
