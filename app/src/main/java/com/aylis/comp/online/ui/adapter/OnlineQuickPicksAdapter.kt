package com.aylis.comp.online.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.online.repository.OnlineTrack
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class OnlineQuickPicksAdapter(
    private val onItemClick: (List<OnlineTrack>, Int) -> Unit
) : ListAdapter<OnlineTrack, OnlineQuickPicksAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quick_pick_track, parent, false)
            
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position) ?: return
        holder.bind(track)
    }

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitle: TextView = itemView.findViewById(R.id.txtItemLine1)
        private val txtArtist: TextView = itemView.findViewById(R.id.txtItemLine2)
        private val imgArt: ImageView = itemView.findViewById(R.id.imgArt)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(currentList, pos)
                }
            }
        }

        fun bind(track: OnlineTrack) {
            txtTitle.text = track.title
            txtArtist.text = track.artist

            if (track.thumbnail.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(track.thumbnail)
                    .apply(RequestOptions().override(256))
                    .placeholder(R.drawable.ic_art)
                    .into(imgArt)
            } else {
                imgArt.setImageResource(R.drawable.ic_art)
            }
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<OnlineTrack>() {
        override fun areItemsTheSame(oldItem: OnlineTrack, newItem: OnlineTrack): Boolean {
            return oldItem.videoId == newItem.videoId
        }

        override fun areContentsTheSame(oldItem: OnlineTrack, newItem: OnlineTrack): Boolean {
            return oldItem == newItem
        }
    }
}
