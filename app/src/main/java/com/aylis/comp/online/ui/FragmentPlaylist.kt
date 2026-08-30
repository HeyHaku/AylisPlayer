package com.aylis.comp.online.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.online.managers.StatsManager
import com.aylis.comp.online.repository.OnlineMusicRepository
import com.aylis.comp.online.repository.OnlineTrack
import com.aylis.comp.online.ui.adapter.OnlineTracksAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentPlaylist : BottomSheetDialogFragment() {

    private lateinit var adapter: OnlineTracksAdapter
    private var browseId: String? = null
    private var playlistTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        browseId = arguments?.getString(ARG_BROWSE_ID)
        playlistTitle = arguments?.getString(ARG_TITLE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_online_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val tvTitle = view.findViewById<TextView>(R.id.tvPlaylistTitle)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerPlaylist)
        val progress = view.findViewById<View>(R.id.progressPlaylist)

        tvTitle.text = playlistTitle ?: getString(R.string.online_playlist_default_title)

        btnBack.setOnClickListener {
            dismiss()
        }

        adapter = OnlineTracksAdapter { list, index ->
            com.aylis.comp.online.managers.OnlinePlaybackManager.playQueue(list, index, com.aylis.comp.online.managers.PlaySource.PLAYLIST)
        }
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter

        val id = browseId
        if (!id.isNullOrEmpty()) {
            loadPlaylist(id, progress)
        }
    }

    private fun loadPlaylist(id: String, progress: View) {
        progress.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tracks = withContext(Dispatchers.IO) {
                    OnlineMusicRepository.getPlaylistTracks(id)
                }
                
                val ctx = context
                if (ctx != null) {
                    tracks.take(3).forEach { track ->
                        com.aylis.comp.playback.ExoMediaPlayer.YoutubeResolver.preResolve(ctx, "ytsearch://" + track.videoId)
                    }
                }
                
                adapter.submitList(tracks)
            } catch (e: Exception) {
                context?.let {
                    Toast.makeText(it, R.string.online_toast_failed_to_load_playlist, Toast.LENGTH_SHORT).show()
                }
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val ARG_BROWSE_ID = "browse_id"
        private const val ARG_TITLE = "title"

        fun newInstance(browseId: String, title: String) = FragmentPlaylist().apply {
            arguments = Bundle().apply {
                putString(ARG_BROWSE_ID, browseId)
                putString(ARG_TITLE, title)
            }
        }
    }
}