package com.aylis.comp.photos.ui

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.aylis.R
import com.aylis.comp.photos.data.PhotosRepository
import com.aylis.comp.photos.manager.PhotoDownloadManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoFullscreenDialog : BottomSheetDialogFragment() {

    private var regularUrl: String? = null
    private var fullUrl: String? = null
    private var rawUrl: String? = null
    private var photoId: String? = null
    private var sourceUrl: String? = null
    private var postUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        regularUrl = arguments?.getString(ARG_REGULAR)
        fullUrl = arguments?.getString(ARG_FULL)
        rawUrl = arguments?.getString(ARG_RAW)
        photoId = arguments?.getString(ARG_ID)
        sourceUrl = arguments?.getString(ARG_SOURCE)
        postUrl = arguments?.getString(ARG_POST_URL)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_photo_fullscreen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Expand the bottom sheet to full height
        dialog?.setOnShowListener {
            val d = it as BottomSheetDialog
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }

        val ivFullscreen = view.findViewById<ImageView>(R.id.ivFullscreen)
        val btnClose = view.findViewById<View>(R.id.btnClose)
        val btnDownload = view.findViewById<MaterialButton>(R.id.btnDownload)
        val btnVisit = view.findViewById<MaterialButton>(R.id.btnVisit)
        val txtAuthorName = view.findViewById<TextView>(R.id.txtAuthorName)
        val txtDescription = view.findViewById<TextView>(R.id.txtDescription)
        val ivAuthorAvatar = view.findViewById<ImageView>(R.id.ivAuthorAvatar)
        val txtStats = view.findViewById<TextView>(R.id.txtStats)

        val previewUrl = fullUrl ?: regularUrl
        if (previewUrl != null) {
            val metrics = resources.displayMetrics
            Glide.with(this)
                .load(previewUrl)
                .override(metrics.widthPixels, metrics.heightPixels)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivFullscreen)
        }

        // Fetch details from API
        txtAuthorName.text = "Loading..."
        txtDescription.text = "Fetching tags and info..."
        txtStats.text = "..."
        
        photoId?.let { id ->
            lifecycleScope.launch(Dispatchers.IO) {
                val details = PhotosRepository.getPhotoDetails(id)
                withContext(Dispatchers.Main) {
                    if (details != null && isAdded) {
                        txtAuthorName.text = details.uploader?.username ?: "Wallhaven Uploader"
                        
                        val formatNum = { num: Int ->
                            if (num >= 1000) String.format("%.1fk", num / 1000.0) else num.toString()
                        }
                        txtStats.text = "👁 ${formatNum(details.views)}  🤍 ${formatNum(details.favorites)}"
                        
                        val avatarUrl = details.uploader?.avatar?.px128
                        if (!avatarUrl.isNullOrEmpty()) {
                            ivAuthorAvatar.setPadding(0, 0, 0, 0)
                            ivAuthorAvatar.imageTintList = null
                            Glide.with(this@PhotoFullscreenDialog)
                                .load(avatarUrl)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(ivAuthorAvatar)
                        }

                        if (details.tags.isNotEmpty()) {
                            txtDescription.text = "Tags: " + details.tags.joinToString(", ") { it.name }
                        } else {
                            txtDescription.text = "High-quality wallpaper from Wallhaven."
                        }
                    } else if (isAdded) {
                        txtAuthorName.text = "Wallhaven"
                        txtStats.text = "Stats unavailable"
                        txtDescription.text = "Failed to load details."
                    }
                }
            }
        }

        btnClose.setOnClickListener { dismiss() }

        // Always point Visit to the original Wallhaven post
        if (!postUrl.isNullOrEmpty()) {
            btnVisit.visibility = View.VISIBLE
            btnVisit.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(postUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            btnVisit.visibility = View.GONE
        }

        btnDownload.setOnClickListener {
            showQualitySelector()
        }
        
        PhotoDownloadManager.addListener(downloadListener)
    }

    private fun extractAuthor(source: String?): String? {
        if (source.isNullOrEmpty()) return null
        try {
            val uri = Uri.parse(source)
            if (uri.host?.contains("pixiv") == true) {
                val path = uri.path
                if (path != null && path.contains("/users/")) {
                    val segments = uri.pathSegments
                    if (segments.size > 2) return "Pixiv User ${segments.last()}"
                }
                return "Pixiv Artist"
            }
            if (uri.host?.contains("x.com") == true || uri.host?.contains("twitter") == true) {
                val segments = uri.pathSegments
                if (segments.isNotEmpty()) return "@${segments[0]}"
                return "Twitter User"
            }
            if (uri.host?.contains("artstation") == true) {
                val segments = uri.pathSegments
                if (segments.isNotEmpty()) return segments[0]
                return "ArtStation"
            }
            return uri.host?.replace("www.", "")
        } catch (e: Exception) {
            return null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        PhotoDownloadManager.removeListener(downloadListener)
    }

    private val downloadListener = object : PhotoDownloadManager.DownloadListener {
        override fun onProgress(id: String, progress: Int) {
            if (id == photoId && isAdded) {
                view?.findViewById<MaterialButton>(R.id.btnDownload)?.text = getString(R.string.photo_downloading_progress, progress)
            }
        }

        override fun onCompleted(id: String, success: Boolean, file: java.io.File?) {
            if (id == photoId && isAdded) {
                val statusText = if (success) getString(R.string.photo_downloaded) else getString(R.string.photo_download_error)
                view?.findViewById<MaterialButton>(R.id.btnDownload)?.text = statusText
            }
        }
    }

    private fun showQualitySelector() {
        val options = arrayOf(
            getString(R.string.photo_quality_raw),
            getString(R.string.photo_quality_full),
            getString(R.string.photo_quality_regular)
        )
        val urls = arrayOf(rawUrl, fullUrl, regularUrl)
        
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.photo_select_quality)
            .setItems(options) { _, which ->
                val selectedUrl = urls[which]
                if (selectedUrl != null && photoId != null) {
                    PhotoDownloadManager.downloadPhoto(requireContext(), photoId!!, selectedUrl)
                }
            }
            .show()
    }

    companion object {
        private const val ARG_REGULAR = "arg_regular"
        private const val ARG_FULL = "arg_full"
        private const val ARG_RAW = "arg_raw"
        private const val ARG_ID = "arg_id"
        private const val ARG_SOURCE = "arg_source"
        private const val ARG_POST_URL = "arg_post_url"

        fun newInstance(regularUrl: String, fullUrl: String, rawUrl: String, photoId: String, sourceUrl: String? = null, postUrl: String? = null): PhotoFullscreenDialog {
            val args = Bundle().apply {
                putString(ARG_REGULAR, regularUrl)
                putString(ARG_FULL, fullUrl)
                putString(ARG_RAW, rawUrl)
                putString(ARG_ID, photoId)
                putString(ARG_SOURCE, sourceUrl)
                putString(ARG_POST_URL, postUrl)
            }
            val fragment = PhotoFullscreenDialog()
            fragment.arguments = args
            return fragment
        }
    }
}
