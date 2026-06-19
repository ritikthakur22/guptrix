package com.guptrix.app.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.guptrix.app.R

class NoteDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        val title = intent.getStringExtra("NOTE_TITLE") ?: "Untitled"
        val content = intent.getStringExtra("NOTE_CONTENT") ?: ""

        findViewById<TextView>(R.id.noteTitle).text = title
        findViewById<TextView>(R.id.noteContent).text = content
    }
}
