package com.aylis.comp.online.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.online.managers.AuthManager
import com.aylis.comp.online.managers.StatsManager
import com.aylis.comp.online.repository.OnlineMusicRepository
import com.aylis.comp.online.repository.OnlineTrack
import com.aylis.comp.online.repository.OnlinePlaylist
import com.aylis.comp.online.repository.OnlineItem
import com.aylis.comp.online.ui.adapter.OnlineTracksCardAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FragmentPlaylists : Fragment() {

    private lateinit var playlistsTabAdapter: OnlineTracksCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_online_playlists, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerPlaylistsTab = view.findViewById<RecyclerView>(R.id.recyclerPlaylistsTab)
        playlistsTabAdapter = OnlineTracksCardAdapter(customLayoutRes = R.layout.item_playlist_card_online) { item, list -> handleItemClick(item, list) }
        val spanCount = 2
        recyclerPlaylistsTab.layoutManager = GridLayoutManager(context, spanCount)
        recyclerPlaylistsTab.adapter = playlistsTabAdapter

        val swipeRefreshPlaylists = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshPlaylists)
        swipeRefreshPlaylists.setOnRefreshListener {
            playlistsTabAdapter.submitList(emptyList())
            loadPlaylists(view)
        }
        
        val btnPlaylistsMore = view.findViewById<android.widget.ImageButton>(R.id.btnPlaylistsMore)
        btnPlaylistsMore?.setOnClickListener { v ->
            val popup = android.widget.PopupMenu(v.context, v)
            popup.menu.add(0, 1, 0, getString(R.string.online_create_new_playlist))
            popup.setOnMenuItemClickListener { item ->
                if (item.itemId == 1) {
                    val context = requireContext()
                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_playlist, null)
                    val etPlaylistName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTxtPlaylistName)
                    val spinnerType = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerType)
                    spinnerType?.visibility = android.view.View.GONE
                    
                    com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.online_dialog_new_playlist_title)
                        .setView(dialogView)
                        .setPositiveButton(R.string.online_dialog_create) { _, _ ->
                            val title = etPlaylistName.text.toString()
                            if (title.isNotBlank()) {
                                GlobalScope.launch(Dispatchers.Main) {
                                    val newId = OnlineMusicRepository.createPlaylist(title)
                                    context?.let { ctx ->
                                        if (newId != null) {
                                            android.widget.Toast.makeText(ctx, R.string.online_toast_created_playlist, android.widget.Toast.LENGTH_SHORT).show()
                                            loadPlaylists(view)
                                        } else {
                                            android.widget.Toast.makeText(ctx, R.string.online_toast_failed_to_create, android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                        .setNegativeButton(R.string.online_dialog_cancel, null)
                        .show()
                }
                true
            }
            popup.show()
        }

        loadPlaylists(view)
    }

    private fun loadPlaylists(view: View) {
        val swipeRefreshPlaylists = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefreshPlaylists)
        if (AuthManager.isLoggedIn()) {
            swipeRefreshPlaylists.isRefreshing = true
            GlobalScope.launch(Dispatchers.Main) {
                val playlists: MutableList<OnlineItem> = OnlineMusicRepository.getLikedPlaylists().toMutableList()
                
                val hasLikedPlaylist = playlists.filterIsInstance<OnlinePlaylist>().any { 
                    it.browseId == "LM" || it.title.contains("Liked", ignoreCase = true) 
                }
                
                if (!hasLikedPlaylist) {
                    playlists.add(0, OnlinePlaylist(
                        browseId = "LM", // Typically LM is the ID for Liked Music on YouTube
                        title = getString(R.string.online_liked_music),
                        subtitle = getString(R.string.online_auto_playlist),
                        thumbnail = "https://www.gstatic.com/youtube/media/ytm/images/pbg/liked-music-@576.png"
                    ))
                }
                
                playlistsTabAdapter.submitList(playlists.toList())
                swipeRefreshPlaylists.isRefreshing = false
            }
        } else {
            swipeRefreshPlaylists.isRefreshing = false
        }
    }

    private fun handleItemClick(item: OnlineItem, contextList: List<OnlineItem>) {
        if (item is OnlineTrack) {
            val trackList = contextList.filterIsInstance<OnlineTrack>()
            playTrack(item, trackList)
        } else if (item is OnlinePlaylist) {
            val fragment = FragmentPlaylist.newInstance(item.browseId, item.title)
            fragment.show(childFragmentManager, "PlaylistDialog")
        }
    }

    private fun playTrack(track: OnlineTrack, contextList: List<OnlineTrack>) {
        val index = contextList.indexOf(track).takeIf { it >= 0 } ?: 0
        com.aylis.comp.online.managers.OnlinePlaybackManager.playQueue(contextList, index, com.aylis.comp.online.managers.PlaySource.PLAYLIST)
    }

    companion object {
        fun newInstance() = FragmentPlaylists()
    }
}
