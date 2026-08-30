package com.aylis.comp.online.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aylis.R
import com.aylis.comp.online.managers.AuthManager
import com.aylis.comp.online.managers.StatsManager
import com.aylis.comp.online.repository.OnlineMusicRepository
import com.aylis.comp.online.repository.OnlineTrack
import com.aylis.comp.online.repository.OnlinePlaylist
import com.aylis.comp.online.repository.OnlineItem
import com.aylis.comp.online.repository.Shelf
import com.aylis.comp.online.ui.adapter.ShelfAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide
import kotlinx.coroutines.flow.collectLatest

class FragmentDiscover : Fragment() {

    private lateinit var discoverAdapter: ShelfAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_online_discover, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerDiscover = view.findViewById<RecyclerView>(R.id.recyclerDiscover)
        discoverAdapter = ShelfAdapter(
            requireContext(),
            onItemClick = { item, list, shelfTitle -> handleItemClick(item, list, shelfTitle) },
            onShelfTitleClick = { shelf ->
                val detailsFrag = FragmentOnlineShelfDetails.newInstance(shelf)
                requireActivity().supportFragmentManager.beginTransaction()
                    .setCustomAnimations(
                        R.anim.slide_in_right,
                        R.anim.slide_out_left,
                        R.anim.slide_in_left,
                        R.anim.slide_out_right
                    )
                    .add(android.R.id.content, detailsFrag, "ShelfDetails")
                    .addToBackStack(null)
                    .commit()
            }
        )
        
        recyclerDiscover.setHasFixedSize(true)
        recyclerDiscover.setItemViewCacheSize(15) // Cache shelves to prevent lag when scrolling back
        
        val layoutManager = object : LinearLayoutManager(requireContext()) {
            override fun getExtraLayoutSpace(state: RecyclerView.State): Int {
                return 5000 // Pre-render 5000px offscreen (almost the whole list)
            }
        }
        layoutManager.initialPrefetchItemCount = 5
        recyclerDiscover.layoutManager = layoutManager
        
        recyclerDiscover.setItemViewCacheSize(20) // CRITICAL: Stop nested RV tearing down
        recyclerDiscover.adapter = discoverAdapter

        recyclerDiscover.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
            }
        })

        val swipeRefreshDiscover = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshDiscover)
        swipeRefreshDiscover.setOnRefreshListener {
            discoverAdapter.submitList(emptyList())
            loadHomeData(view)
        }

        GlobalScope.launch(Dispatchers.Main) {
            AuthManager.activeAccountFlow.collectLatest {
                discoverAdapter.submitList(emptyList())
                loadHomeData(view)
            }
        }
    }

    private fun loadHomeData(view: View) {
        val swipeRefreshDiscover = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshDiscover)
        
        swipeRefreshDiscover.isRefreshing = true
        GlobalScope.launch(Dispatchers.Main) {
            val finalShelves = mutableListOf<Shelf>()
            val realShelves = OnlineMusicRepository.getHomeRecommendations()
            
            val quickPicksShelf = realShelves.firstOrNull { 
                it.title.contains("Quick picks", ignoreCase = true) || 
                it.title.contains("Быстрые", ignoreCase = true) || 
                it.title.contains("Быстрый", ignoreCase = true) ||
                it.title.contains("Начать радио", ignoreCase = true) ||
                it.title.contains("Start radio", ignoreCase = true) ||
                it.title.equals("Радио", ignoreCase = true) 
            }
            if (quickPicksShelf != null && quickPicksShelf.items.isNotEmpty()) {
                val infiniteItems = quickPicksShelf.items.take(9)
                if (infiniteItems.isNotEmpty()) {
                    finalShelves.add(Shelf(getString(R.string.online_infinite_recommendations), infiniteItems))
                }
            }
            
            finalShelves.addAll(realShelves)
            
            // Preload all thumbnails to prevent massive layout lag during scroll
            val ctx = view.context
            for (shelf in finalShelves) {
                var preloadedTracksCount = 0
                for (item in shelf.items) {
                    if (item.thumbnail.isNotEmpty()) {
                        Glide.with(ctx)
                            .load(item.thumbnail)
                            .preload(256, 256)
                    }
                    if (item is OnlineTrack && preloadedTracksCount < 3) {
                        com.aylis.comp.playback.ExoMediaPlayer.YoutubeResolver.preResolve(ctx, "ytsearch://" + item.videoId)
                        preloadedTracksCount++
                    }
                }
            }
            
            discoverAdapter.submitList(finalShelves.toList())
            swipeRefreshDiscover.isRefreshing = false
        }
    }

    private fun handleItemClick(item: OnlineItem, contextList: List<OnlineItem>, shelfTitle: String) {
        if (item is OnlineTrack) {
            val isEndless = shelfTitle == getString(R.string.online_infinite_recommendations)
            if (isEndless) {
                com.aylis.comp.online.managers.OnlinePlaybackManager.startEndlessRadioSession(item)
            } else {
                val trackList = contextList.filterIsInstance<OnlineTrack>()
                val index = trackList.indexOf(item).takeIf { it >= 0 } ?: 0
                com.aylis.comp.online.managers.OnlinePlaybackManager.playQueue(trackList, index, com.aylis.comp.online.managers.PlaySource.ONLINE_SHELF)
            }
        } else if (item is OnlinePlaylist) {
            val fragment = FragmentPlaylist.newInstance(item.browseId, item.title)
            fragment.show(childFragmentManager, "PlaylistDialog")
        }
    }

    private fun playTrack(track: OnlineTrack, contextList: List<OnlineTrack>) {
        com.aylis.comp.online.managers.OnlinePlaybackManager.playOnlineTrack(track, false)
    }

    companion object {
        fun newInstance() = FragmentDiscover()
    }
}
