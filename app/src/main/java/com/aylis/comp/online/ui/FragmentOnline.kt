package com.aylis.comp.online.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aylis.R
import com.aylis.comp.online.managers.AuthManager
import com.aylis.comp.online.managers.StatsManager
import com.aylis.comp.online.repository.OnlineMusicRepository
import com.aylis.comp.online.repository.OnlineTrack
import com.aylis.comp.online.ui.adapter.OnlineTracksAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import android.widget.TextView
import android.widget.FrameLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.aylis.comp.online.managers.SearchHistoryManager
import com.aylis.comp.online.ui.adapter.SearchHistoryAdapter
import java.util.LinkedList

class FragmentOnline : Fragment() {

    private lateinit var viewPager: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_online_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        StatsManager.startTracking()

        setupViewPager(view)
        setupSearchAndAuth(view)
        setupModeSwitcher(view)
        setupBackPressed()
    }
    
    private fun setupModeSwitcher(view: View) {
        val modeSwitcher = view.findViewById<TabLayout>(R.id.tabLayoutModeSwitcher)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerOnline)
        val tabLayoutOnline = view.findViewById<TabLayout>(R.id.tabLayoutOnline)
        val photosContainer = view.findViewById<View>(R.id.photosContainer)
        val btnSearchToggle = view.findViewById<View>(R.id.btnSearchToggle)

        modeSwitcher.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    1 -> {
                        // Photos mode
                        viewPager.visibility = View.GONE
                        tabLayoutOnline.visibility = View.GONE
                        btnSearchToggle.visibility = View.GONE
                        photosContainer.visibility = View.VISIBLE
                        view.findViewById<View>(R.id.fontsContainer)?.visibility = View.GONE
                        
                        if (childFragmentManager.findFragmentByTag("FragmentPhotos") == null) {
                            childFragmentManager.beginTransaction()
                                .replace(R.id.photosContainer, com.aylis.comp.photos.ui.FragmentPhotos.newInstance(), "FragmentPhotos")
                                .commit()
                        }
                    }
                    2 -> {
                        // Fonts mode
                        viewPager.visibility = View.GONE
                        tabLayoutOnline.visibility = View.GONE
                        btnSearchToggle.visibility = View.GONE
                        photosContainer.visibility = View.GONE
                        val fontsContainer = view.findViewById<View>(R.id.fontsContainer)
                        fontsContainer?.visibility = View.VISIBLE
                        
                        if (childFragmentManager.findFragmentByTag("FragmentFonts") == null) {
                            childFragmentManager.beginTransaction()
                                .replace(R.id.fontsContainer, com.aylis.comp.fonts.ui.FragmentFonts.newInstance(), "FragmentFonts")
                                .commit()
                        }
                    }
                    else -> {
                        // YouTube mode
                        viewPager.visibility = View.VISIBLE
                        tabLayoutOnline.visibility = View.VISIBLE
                        btnSearchToggle.visibility = View.VISIBLE
                        photosContainer.visibility = View.GONE
                        view.findViewById<View>(R.id.fontsContainer)?.visibility = View.GONE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupViewPager(view: View) {
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutOnline)
        viewPager = view.findViewById(R.id.viewPagerOnline)

        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> FragmentDiscover.newInstance()
                    1 -> FragmentPlaylists.newInstance()
                    2 -> FragmentRecap.newInstance()
                    else -> FragmentDiscover.newInstance()
                }
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.online_tab_discover)
                1 -> getString(R.string.online_tab_playlists)
                2 -> getString(R.string.online_tab_recap)
                else -> ""
            }
        }.attach()
    }

    private fun setupSearchAndAuth(view: View) {
        val btnSearchToggle = view.findViewById<ImageView>(R.id.btnSearchToggle)

        // Open Search Bottom Sheet
        btnSearchToggle.setOnClickListener {
            val searchSheet = FragmentOnlineSearchSheet { track, contextList ->
                playTrack(track, contextList)
            }
            searchSheet.show(childFragmentManager, "SearchSheet")
        }
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentManager = childFragmentManager
                if (fragmentManager.backStackEntryCount > 0) {
                    // Pop child fragments (like FragmentPlaylist)
                    fragmentManager.popBackStack()
                } else if (viewPager.currentItem != 0) {
                    // Go back to Discover tab
                    viewPager.currentItem = 0
                } else {
                    // Proceed with standard back behavior (exit)
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }

    private fun playTrack(track: OnlineTrack, contextList: List<OnlineTrack>) {
        val index = contextList.indexOf(track).takeIf { it >= 0 } ?: 0
        com.aylis.comp.online.managers.OnlinePlaybackManager.playQueue(contextList, index, com.aylis.comp.online.managers.PlaySource.PLAYLIST)
    }

    companion object {
        fun newInstance() = FragmentOnline()
    }
}
