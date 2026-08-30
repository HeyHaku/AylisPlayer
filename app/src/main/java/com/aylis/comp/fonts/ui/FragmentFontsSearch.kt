package com.aylis.comp.fonts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.fonts.api.GitHubFontsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentFontsSearch : Fragment() {

    private lateinit var adapter: FontsAdapter
    private var progressBar: ProgressBar? = null
    private var pendingQuery: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fonts_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFonts)
        progressBar = view.findViewById(R.id.progressBar)

        adapter = FontsAdapter(requireContext())
        recyclerView.adapter = adapter

        pendingQuery?.let {
            performSearch(it)
            pendingQuery = null
        }
    }

    fun performSearch(query: String) {
        if (progressBar == null) {
            pendingQuery = query
            return
        }
        progressBar?.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val results = GitHubFontsRepository.searchFonts(query)
                withContext(Dispatchers.Main) {
                    adapter.submitList(results)
                    progressBar?.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressBar?.visibility = View.GONE
                }
            }
        }
    }
}
