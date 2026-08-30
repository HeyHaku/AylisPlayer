package com.aylis.comp.photos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aylis.R
import com.aylis.comp.photos.data.PhotosRepository
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class FragmentPhotosExplore : Fragment() {

    private lateinit var adapter: PhotosAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var tabLayoutCategories: com.google.android.material.tabs.TabLayout
    
    private var currentQuery: String = ""
    private var currentCategories: String = "111" // Default: All
    
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photos_explore, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewPhotos)
        searchView = view.findViewById(R.id.searchViewPhotos)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        tabLayoutCategories = view.findViewById(R.id.tabLayoutCategories)

        setupRecyclerView()
        setupSearchView()
        setupTabs()
        setupSwipeRefresh()
        
        loadData(false)
    }

    private fun setupRecyclerView() {
        adapter = PhotosAdapter { photo ->
            val dialog = PhotoFullscreenDialog.newInstance(
                regularUrl = photo.thumbs.large,
                fullUrl = photo.path,
                rawUrl = photo.path,
                photoId = photo.id,
                sourceUrl = photo.source,
                postUrl = photo.url
            )
            dialog.show(childFragmentManager, "PhotoFullscreenDialog")
        }
        
        val lm = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }
        recyclerView.layoutManager = lm
        recyclerView.adapter = adapter
        
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var scrollDist = 0
            private var isVisible = true
            private val MIN_SCROLL = 50
            
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                if (isVisible && dy > 10) {
                    scrollDist += dy
                    if (scrollDist > MIN_SCROLL) {
                        com.aylis.MainActivity.onHideBottomNav.invoke(true)
                        isVisible = false
                        scrollDist = 0
                    }
                } else if (!isVisible && dy < -10) {
                    scrollDist += dy
                    if (scrollDist < -MIN_SCROLL) {
                        com.aylis.MainActivity.onHideBottomNav.invoke(false)
                        isVisible = true
                        scrollDist = 0
                    }
                }
                if ((isVisible && dy < 0) || (!isVisible && dy > 0)) {
                    scrollDist = 0
                }
                
                if (dy > 0) {
                    val visibleItemCount = lm.childCount
                    val totalItemCount = lm.itemCount
                    val firstVisibleItems = lm.findFirstVisibleItemPositions(null)
                    val pastVisibleItems = if (firstVisibleItems.isNotEmpty()) firstVisibleItems[0] else 0
                    
                    if (!isLoading && !isLastPage) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 6) {
                            loadData(true)
                        }
                    }
                }
            }
        })
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    currentQuery = query
                    loadData(false)
                }
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank() && currentQuery.isNotEmpty()) {
                    currentQuery = ""
                    loadData(false)
                }
                return false
            }
        })
    }
    
    private fun setupTabs() {
        tabLayoutCategories.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                currentCategories = when (tab?.position) {
                    1 -> "100" // General
                    2 -> "010" // Anime
                    3 -> "001" // People
                    else -> "111" // All
                }
                loadData(false)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            loadData(false)
        }
    }

    private fun loadData(isLoadMore: Boolean) {
        if (isLoading || isLastPage && isLoadMore) return
        isLoading = true
        
        if (!isLoadMore) {
            swipeRefreshLayout.isRefreshing = true
            currentPage = 1
            isLastPage = false
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            val photos = if (currentQuery.isBlank()) {
                PhotosRepository.getFeed(page = currentPage, categories = currentCategories)
            } else {
                PhotosRepository.searchPhotos(query = currentQuery, page = currentPage, categories = currentCategories)
            }
            
            if (photos.isEmpty()) {
                isLastPage = true
                if (!isLoadMore) {
                    android.widget.Toast.makeText(requireContext(), "Ничего не найдено.", android.widget.Toast.LENGTH_SHORT).show()
                    adapter.submitList(emptyList())
                }
            } else {
                if (isLoadMore) {
                    adapter.addPhotos(photos)
                } else {
                    adapter.submitList(photos)
                    recyclerView.scrollToPosition(0)
                }
                currentPage++
            }
            
            isLoading = false
            swipeRefreshLayout.isRefreshing = false
        }
    }

    companion object {
        fun newInstance() = FragmentPhotosExplore()
    }
}
