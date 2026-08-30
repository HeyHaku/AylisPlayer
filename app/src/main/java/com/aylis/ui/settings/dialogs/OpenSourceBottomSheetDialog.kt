package com.aylis.ui.settings.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class OpenSourceBottomSheetDialog : BottomSheetDialogFragment() {

    data class OpenSourceItem(
        val name: String,
        val license: String,
        val descriptionRes: Int? = null,
        val thanksNoteRes: Int? = null,
        val url: String? = null
    )

    private val items = listOf(
        OpenSourceItem(
            name = "Avee Open Player (azy kun)",
            license = "Apache 2.0",
            thanksNoteRes = R.string.open_source_thanks_avee,
            url = "https://github.com/Azy-Kun/AveeOpenPlayer_1.0.34"
        ),
        OpenSourceItem(
            name = "Cubiq",
            license = "Community",
            thanksNoteRes = R.string.open_source_thanks_cubiq,
            url = "https://github.com/TheCubiq"
        ),
        OpenSourceItem(
            name = "Google (Media3 / ExoPlayer)",
            license = "Apache 2.0",
            descriptionRes = R.string.open_source_desc_media3,
            url = "https://github.com/androidx/media"
        ),
        OpenSourceItem(
            name = "NewPipeExtractor",
            license = "GPLv3",
            descriptionRes = R.string.open_source_desc_newpipe,
            url = "https://github.com/TeamNewPipe/NewPipeExtractor"
        ),
        OpenSourceItem(
            name = "GitHub",
            license = "Platform",
            descriptionRes = R.string.open_source_desc_github,
            url = "https://github.com"
        ),
        OpenSourceItem(
            name = "Wallhaven",
            license = "API / Content",
            descriptionRes = R.string.open_source_desc_wallhaven,
            url = "https://wallhaven.cc"
        ),
        OpenSourceItem(
            name = "Google (Android OS)",
            license = "Apache 2.0 / Open Source",
            thanksNoteRes = R.string.open_source_thanks_android,
            url = "https://developer.android.com"
        ),
        OpenSourceItem(
            name = "Glide",
            license = "BSD / MIT / Apache 2.0",
            descriptionRes = R.string.open_source_desc_glide,
            url = "https://github.com/bumptech/glide"
        ),
        OpenSourceItem(
            name = "Retrofit & OkHttp",
            license = "Apache 2.0",
            descriptionRes = R.string.open_source_desc_retrofit,
            url = "https://github.com/square/okhttp"
        ),
        OpenSourceItem(
            name = "Moshi",
            license = "Apache 2.0",
            descriptionRes = R.string.open_source_desc_moshi,
            url = "https://github.com/square/moshi"
        ),
        OpenSourceItem(
            name = "AnimatedBottomBar",
            license = "MIT",
            descriptionRes = R.string.open_source_desc_bottombar,
            url = "https://github.com/DroppingCircle/AnimatedBottomBar"
        ),
        OpenSourceItem(
            name = "jaudiotagger",
            license = "LGPL",
            descriptionRes = R.string.open_source_desc_jaudiotagger,
            url = "https://github.com/ijabz/jaudiotagger"
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_open_source, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvOpenSourceItems = view.findViewById<RecyclerView>(R.id.rvOpenSourceItems)
        rvOpenSourceItems.layoutManager = LinearLayoutManager(requireContext())
        rvOpenSourceItems.adapter = OpenSourceAdapter(items) { item ->
            item.url?.let { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }
    }

    private class OpenSourceAdapter(
        private val list: List<OpenSourceItem>,
        private val onItemClick: (OpenSourceItem) -> Unit
    ) : RecyclerView.Adapter<OpenSourceAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_open_source_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            holder.tvProjectName.text = item.name
            holder.tvLicenseBadge.text = item.license

            if (item.descriptionRes != null) {
                holder.tvDescription.setText(item.descriptionRes)
                holder.tvDescription.visibility = View.VISIBLE
            } else {
                holder.tvDescription.visibility = View.GONE
            }

            if (item.thanksNoteRes != null) {
                holder.tvThanksNote.setText(item.thanksNoteRes)
                holder.tvThanksNote.visibility = View.VISIBLE
            } else {
                holder.tvThanksNote.visibility = View.GONE
            }

            if (!item.url.isNullOrEmpty()) {
                holder.ivExternalLink.visibility = View.VISIBLE
                holder.itemView.setOnClickListener { onItemClick(item) }
            } else {
                holder.ivExternalLink.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
                holder.itemView.isClickable = false
            }
        }

        override fun getItemCount(): Int = list.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvProjectName: TextView = view.findViewById(R.id.tvProjectName)
            val tvLicenseBadge: TextView = view.findViewById(R.id.tvLicenseBadge)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
            val tvThanksNote: TextView = view.findViewById(R.id.tvThanksNote)
            val ivExternalLink: ImageView = view.findViewById(R.id.ivExternalLink)
        }
    }
}
