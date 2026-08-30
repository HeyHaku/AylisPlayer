package com.aylis.comp.fonts.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.aylis.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FragmentFonts : Fragment() {

    private lateinit var etSearchFonts: EditText
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    private var searchFragment: FragmentFontsSearch? = null
    private var downloadedFragment: FragmentFontsDownloaded? = null

    companion object {
        fun newInstance() = FragmentFonts()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fonts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etSearchFonts = view.findViewById(R.id.etSearchFonts)
        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)

        val adapter = FontsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.setText(R.string.tab_search_fonts)
                1 -> tab.setText(R.string.tab_downloaded_fonts)
            }
        }.attach()

        etSearchFonts.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                searchFragment?.performSearch(query)
                viewPager.currentItem = 0
                true
            } else {
                false
            }
        }
    }

    private inner class FontsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> {
                    val frag = FragmentFontsSearch()
                    searchFragment = frag
                    frag.performSearch(etSearchFonts.text.toString().trim())
                    frag
                }
                1 -> {
                    val frag = FragmentFontsDownloaded()
                    downloadedFragment = frag
                    frag
                }
                else -> throw IllegalArgumentException("Invalid position")
            }
        }
    }
}
