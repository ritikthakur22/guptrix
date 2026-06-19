package com.guptrix.app.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guptrix.app.R
import com.guptrix.app.database.NoteEntity

class NoteAdapter(private var notes: List<NoteEntity> = emptyList()) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvNoteTitle)
        val content: TextView = view.findViewById(R.id.tvNoteContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.title.text = note.title
        holder.content.text = note.content

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, NoteDetailActivity::class.java).apply {
                putExtra("NOTE_TITLE", note.title)
                putExtra("NOTE_CONTENT", note.content)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = notes.size

    fun submitList(newNotes: List<NoteEntity>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}
