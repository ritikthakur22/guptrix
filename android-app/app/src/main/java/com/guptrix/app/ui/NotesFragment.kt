package com.guptrix.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.guptrix.app.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {

    private val viewModel: NoteViewModel by viewModels()
    private lateinit var adapter: NoteAdapter
    private var filterJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notes, container, false)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = NoteAdapter()
        recyclerView.adapter = adapter

        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        val searchView = view.findViewById<SearchView>(R.id.searchView)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                loadNotes(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                loadNotes(tab?.position ?: 0)
            }
        })

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                search(query ?: "")
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                search(newText ?: "")
                return true
            }
        })

        loadNotes(0)
        return view
    }

    private fun loadNotes(tabIndex: Int) {
        filterJob?.cancel()
        filterJob = viewLifecycleOwner.lifecycleScope.launch {
            val flow = when (tabIndex) {
                1 -> viewModel.favorites
                2 -> viewModel.drafts
                else -> viewModel.allNotes
            }
            flow.collectLatest { notes ->
                adapter.submitList(notes)
            }
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) {
            loadNotes(view?.findViewById<TabLayout>(R.id.tabLayout)?.selectedTabPosition ?: 0)
            return
        }
        filterJob?.cancel()
        filterJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchNotes("%$query%").collectLatest { notes ->
                adapter.submitList(notes)
            }
        }
    }
}
