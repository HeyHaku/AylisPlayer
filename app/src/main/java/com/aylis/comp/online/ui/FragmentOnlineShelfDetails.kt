package com.aylis.comp.online.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.online.repository.OnlineItem
import com.aylis.comp.online.repository.OnlineTrack
import com.aylis.comp.online.repository.Shelf
import com.aylis.comp.online.ui.adapter.OnlineTracksAdapter
import com.aylis.comp.online.ui.adapter.OnlineTracksCardAdapter
import com.aylis.comp.playback.MediaPlaybackService
import com.aylis.comp.online.managers.StatsManager

class FragmentOnlineShelfDetails : Fragment() {

    companion object {
        var currentShelf: Shelf? = null

        fun newInstance(shelf: Shelf): FragmentOnlineShelfDetails {
            currentShelf = shelf
            return FragmentOnlineShelfDetails()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_online_shelf_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarShelfDetails)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerShelfDetails)

        val shelf = currentShelf
        if (shelf == null) {
            parentFragmentManager.popBackStack()
            return
        }

        toolbar.title = shelf.title
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val isTracks = shelf.items.isNotEmpty() && shelf.items.all { it is OnlineTrack }
        
        if (isTracks) {
            recycler.layoutManager = LinearLayoutManager(requireContext())
            val adapter = OnlineTracksAdapter(isHorizontal = false, showNumbers = false) { list, index ->
                com.aylis.comp.online.managers.OnlinePlaybackManager.playQueue(list, index, com.aylis.comp.online.managers.PlaySource.ALBUM)
            }
            adapter.submitList(shelf.items.filterIsInstance<OnlineTrack>())
            recycler.adapter = adapter
        } else {
            recycler.layoutManager = GridLayoutManager(requireContext(), 2)
            val adapter = OnlineTracksCardAdapter(isImmersive = shelf.isImmersive, isGrid = true) { item, list ->
                if (item is OnlineTrack) {
                    playTrack(item, list.filterIsInstance<OnlineTrack>())
                } else if (item is com.aylis.comp.online.repository.OnlinePlaylist) {
                    val fragment = FragmentPlaylist.newInstance(item.browseId, item.title)
                    fragment.show(childFragmentManager, "PlaylistDialog")
                }
            }
            adapter.submitList(shelf.items)
            recycler.adapter = adapter
        }
    }

    private fun playTrack(track: OnlineTrack, contextList: List<OnlineTrack>) {
        val index = contextList.indexOf(track).takeIf { it >= 0 } ?: 0
        com.aylis.comp.online.managers.OnlinePlaybackManager.playQueue(contextList, index, com.aylis.comp.online.managers.PlaySource.ALBUM)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        currentShelf = null
    }
}
