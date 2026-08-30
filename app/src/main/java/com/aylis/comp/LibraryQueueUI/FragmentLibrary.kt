package com.aylis.comp.LibraryQueueUI

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aylis.R
import com.aylis.comp.GlobalSearch.SearchEntryOptions
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FragmentLibrary : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: LibraryPagerAdapter
    
    private val downloadListener = object : com.aylis.comp.online.managers.OnlineDownloadManager.DownloadListener {
        override fun onProgress(videoId: String, progress: Int) {}
        override fun onCompleted(videoId: String, success: Boolean, file: java.io.File?) {
            if (success) {
                updateLibraryItems()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aylis.comp.online.managers.OnlineDownloadManager.addListener(downloadListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.aylis.comp.online.managers.OnlineDownloadManager.removeListener(downloadListener)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_library, container, false)
        
        tabLayout = view.findViewById(R.id.tabLayoutLibrary)
        viewPager = view.findViewById(R.id.viewPagerLibrary)
        
        adapter = LibraryPagerAdapter(this)
        viewPager.adapter = adapter
        
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_library_all)
                1 -> getString(R.string.tab_library_download)
                else -> ""
            }
        }.attach()

        return view
    }

    // --- Legacy Stub Methods for Java *Design classes ---
    
    fun updateLibraryItems() {
        val fragments = childFragmentManager.fragments
        for (f in fragments) {
            if (f is FragmentLocalList) {
                f.reload()
            }
        }
    }

    fun updateTrackInfo() {
        // No-op for new design
    }

    fun navigateForBackwardLibraryAddress(): Boolean {
        // We don't have folders anymore
        return false
    }

    fun navigateLibraryAddress(currentLocationAdapter: Any?, relativeAddress: String?) {}
    
    fun navigateLibraryAddress(currentLocationAdapter: Any?, relativeAddress: String?, refresh: Boolean) {}

    fun navigateForwardLibraryAddress(currentLocationAdapter: Any?, relativeAddress: String?) {}

    fun updateSearchQuery(context: Context?, query: String?) {}

    fun getSearchEntryOptions(): SearchEntryOptions? {
        return null
    }
    
    fun updateSearchInfo() {}

    fun refreshAdapter(containerIdentifier: Any?) {}

    // ----------------------------------------------------

    inner class LibraryPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FragmentLocalList.newInstance(showOnlyDownloaded = false)
                1 -> FragmentLocalList.newInstance(showOnlyDownloaded = true)
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }

    companion object {
        @JvmField
        val onTrackInfoChanged = com.aylis.Common.Events.WeakEvent1<com.aylis.comp.playback.Song.PlaylistSong.Data>()

        @JvmStatic
        fun newInstance(): FragmentLibrary {
            return FragmentLibrary()
        }
    }
}
