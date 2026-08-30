package com.aylis.comp.LibraryQueueUI

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aylis.Common.MediaStoreUtils
import com.aylis.Common.UtilsMusic
import com.aylis.R
import com.aylis.comp.playback.Song.PlaylistSong
import com.aylis.comp.LibraryQueueUI.LibraryQueueFragmentBase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentLocalList : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefreshLocal: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private lateinit var adapter: LocalTracksAdapter
    
    private var showOnlyDownloaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOnlyDownloaded = arguments?.getBoolean("showOnlyDownloaded", false) ?: false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_local_list, container, false)
        
        recyclerView = view.findViewById(R.id.recyclerViewLocal)
        progressBar = view.findViewById(R.id.progressLocal)
        swipeRefreshLocal = view.findViewById(R.id.swipeRefreshLocal)
        
        swipeRefreshLocal.setOnRefreshListener {
            loadTracks()
        }
        
        adapter = LocalTracksAdapter { list, index ->
            val songs = list.map { data ->
                PlaylistSong(data.audioId, Uri.parse(data.dataSource?.toString() ?: ""))
            }
            LibraryQueueFragmentBase.onOpen2.invoke(songs, index, null)
        }
        
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
            }
        })
        
        val spanCount = resources.getInteger(R.integer.library_span_count)
        recyclerView.layoutManager = GridLayoutManager(context, spanCount)
        recyclerView.adapter = adapter
        
        loadTracks()
        
        return view
    }
    
    fun reload() {
        loadTracks()
    }

    private fun loadTracks() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val ctx = context ?: return@launch
            val cr: ContentResolver = ctx.contentResolver
            val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val columns = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION
            )

            // Simplest sort: by date added
            val orderBy = MediaStore.Audio.Media.DATE_ADDED + " DESC"
            
            var selection: String? = null
            if (showOnlyDownloaded) {
                selection = MediaStore.Audio.Media.DATA + " LIKE '%/AylisPlayer/%'"
            }
            
            val songs = mutableListOf<PlaylistSong.Data>()
            val cursor = MediaStoreUtils.querySafe(cr, uri, columns, selection, null, orderBy)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val dataPath = cursor.getString(1)
                    val title = cursor.getString(2)
                    val artist = cursor.getString(3)
                    val duration = cursor.getInt(4)
                    
                    val data = com.aylis.comp.playback.Song.SongHelper.createData(id, dataPath, title, artist, duration)
                    songs.add(data)
                }
                cursor.close()
            }
            
            withContext(Dispatchers.Main) {
                adapter.submitList(songs)
                progressBar.visibility = View.GONE
                swipeRefreshLocal.isRefreshing = false
            }
        }
    }

    companion object {
        fun newInstance(showOnlyDownloaded: Boolean): FragmentLocalList {
            val fragment = FragmentLocalList()
            val args = Bundle()
            args.putBoolean("showOnlyDownloaded", showOnlyDownloaded)
            fragment.arguments = args
            return fragment
        }
    }
}
