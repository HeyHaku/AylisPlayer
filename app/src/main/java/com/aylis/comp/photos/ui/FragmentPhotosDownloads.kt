package com.aylis.comp.photos.ui

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.aylis.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FragmentPhotosDownloads : Fragment() {

    private lateinit var downloadsAdapter: PhotosAdapter
    private lateinit var recyclerViewDownloads: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_photos_downloads, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerViewDownloads = view.findViewById(R.id.recyclerViewDownloads)

        setupRecyclerView()
        loadDownloads()
    }

    private fun setupRecyclerView() {
        downloadsAdapter = PhotosAdapter { photo ->
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
        
        recyclerViewDownloads.layoutManager = StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }
        recyclerViewDownloads.adapter = downloadsAdapter
        
        recyclerViewDownloads.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
    }
    
    private fun loadDownloads() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenPlayer")
            val files = dir.listFiles { file -> file.isFile && file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }
            val downloadedPhotos = files?.map { file ->
                com.aylis.comp.photos.api.WallhavenPhoto(
                    id = file.name,
                    url = "",
                    path = "file://${file.absolutePath}",
                    ratio = "0.75",
                    thumbs = com.aylis.comp.photos.api.WallhavenThumbs(
                        large = "file://${file.absolutePath}",
                        original = "file://${file.absolutePath}",
                        small = "file://${file.absolutePath}"
                    )
                )
            }?.reversed() ?: emptyList()
            
            launch(Dispatchers.Main) {
                if (downloadedPhotos.isEmpty()) {
                    android.widget.Toast.makeText(requireContext(), "Нет скачанных обоев.", android.widget.Toast.LENGTH_SHORT).show()
                }
                downloadsAdapter.submitList(downloadedPhotos)
            }
        }
    }

    companion object {
        fun newInstance() = FragmentPhotosDownloads()
    }
}
