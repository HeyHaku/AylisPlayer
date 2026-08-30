package com.aylis.comp.online.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.online.managers.SearchHistoryManager
import com.aylis.comp.online.repository.OnlineMusicRepository
import com.aylis.comp.online.repository.OnlineTrack
import com.aylis.comp.online.ui.adapter.OnlineTracksAdapter
import com.aylis.comp.online.ui.adapter.SearchHistoryAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.FrameLayout

class FragmentOnlineSearchSheet(
    private val onTrackPlay: (OnlineTrack, List<OnlineTrack>) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var searchAdapter: OnlineTracksAdapter
    private lateinit var historyAdapter: SearchHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_online_search_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearch(view)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        val bottomSheet = dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        
        if (bottomSheet != null) {
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet.requestLayout()

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
            behavior.peekHeight = resources.displayMetrics.heightPixels
        }
    }

    private fun setupSearch(view: View) {
        val searchView = view.findViewById<androidx.appcompat.widget.SearchView>(R.id.searchViewOnline)
        val recyclerSearch = view.findViewById<RecyclerView>(R.id.recyclerViewOnline)
        val recyclerHistory = view.findViewById<RecyclerView>(R.id.recyclerSearchHistory)
        val layoutHistory = view.findViewById<View>(R.id.layoutSearchHistory)
        val progressBar = view.findViewById<android.widget.ProgressBar>(R.id.progressOnline)

        // Search adapter
        searchAdapter = OnlineTracksAdapter { list, index ->
            onTrackPlay(list[index], list)
            dismiss()
        }
        val spanCount = resources.getInteger(R.integer.library_span_count)
        recyclerSearch.layoutManager = GridLayoutManager(context, spanCount)
        recyclerSearch.adapter = searchAdapter

        // History adapter
        historyAdapter = SearchHistoryAdapter(
            historyList = SearchHistoryManager.getHistory(requireContext()),
            onItemClick = { query ->
                searchView.setQuery(query, true)
            },
            onDeleteClick = { query ->
                SearchHistoryManager.removeQuery(requireContext(), query)
                historyAdapter.submitList(SearchHistoryManager.getHistory(requireContext()))
            }
        )
        val spanCountHistory = resources.getInteger(R.integer.library_span_count)
        recyclerHistory.layoutManager = GridLayoutManager(context, spanCountHistory)
        recyclerHistory.adapter = historyAdapter

        // Focus search view on open
        searchView.requestFocus()
        searchView.postDelayed({
            searchView.requestFocus()
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(searchView.findFocus(), InputMethodManager.SHOW_IMPLICIT)
        }, 300)

        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrEmpty()) {
                    SearchHistoryManager.addQuery(requireContext(), query)
                    historyAdapter.submitList(SearchHistoryManager.getHistory(requireContext()))
                    
                    layoutHistory.visibility = View.GONE
                    recyclerSearch.visibility = View.VISIBLE
                    progressBar.visibility = View.VISIBLE
                    
                    GlobalScope.launch(Dispatchers.Main) {
                        val results = OnlineMusicRepository.searchTracks(query)
                        
                        val ctx = context
                        if (ctx != null) {
                            results.filterIsInstance<OnlineTrack>().take(3).forEach { track ->
                                com.aylis.comp.playback.ExoMediaPlayer.YoutubeResolver.preResolve(ctx, "ytsearch://" + track.videoId)
                            }
                        }
                        
                        progressBar.visibility = View.GONE
                        searchAdapter.submitList(results)
                    }
                }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    layoutHistory.visibility = View.VISIBLE
                    recyclerSearch.visibility = View.GONE
                    historyAdapter.submitList(SearchHistoryManager.getHistory(requireContext()))
                }
                return false
            }
        })
    }
}
