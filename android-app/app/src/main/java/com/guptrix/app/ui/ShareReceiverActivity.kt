package com.guptrix.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.guptrix.app.database.AppDatabase
import com.guptrix.app.database.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class ShareReceiverActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (sharedText != null) {
                val note = NoteEntity(
                    noteId = UUID.randomUUID().toString(),
                    title = "Shared Draft",
                    content = sharedText,
                    updatedAt = System.currentTimeMillis(),
                    isDraft = true,
                    isFavorite = false
                )
                
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getDatabase(this@ShareReceiverActivity).noteDao().insert(note)
                    runOnUiThread {
                        Toast.makeText(this@ShareReceiverActivity, "Saved as Draft in Guptrix", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } else {
                finish()
            }
        } else {
            finish()
        }
    }
}
