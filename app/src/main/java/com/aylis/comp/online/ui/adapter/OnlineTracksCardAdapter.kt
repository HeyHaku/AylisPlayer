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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.aylis.comp.online.repository.OnlineItem
import com.aylis.comp.online.repository.OnlinePlaylist
import com.aylis.comp.online.repository.OnlineTrack
class OnlineTracksCardAdapter(
    val isImmersive: Boolean = false,
    val isGrid: Boolean = false,
    val customLayoutRes: Int? = null,
    private val onClick: (OnlineItem, List<OnlineItem>) -> Unit
) : ListAdapter<OnlineItem, OnlineTracksCardAdapter.CardViewHolder>(TrackDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return if (isImmersive) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val layoutRes = customLayoutRes ?: if (viewType == 0) R.layout.item_track_immersive_online else R.layout.item_track_card_online
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        if (isGrid) {
            val params = view.layoutParams
            if (params != null) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                view.layoutParams = params
            }
        }
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val track = getItem(position)
        holder.bind(track, currentList, onClick)
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgThumb: ImageView = itemView.findViewById(R.id.imgCardThumb)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvCardTitle)
        private val tvArtist: TextView = itemView.findViewById(R.id.tvCardArtist)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        private val iconPlaylistOverlay: ImageView = itemView.findViewById(R.id.iconPlaylistOverlay)

        fun bind(item: OnlineItem, fullList: List<OnlineItem>, onClick: (OnlineItem, List<OnlineItem>) -> Unit) {
            tvTitle.text = item.title

            if (item is OnlineTrack) {
                tvArtist.text = item.artist
                btnLike.visibility = View.VISIBLE
                iconPlaylistOverlay.visibility = View.GONE
                val isLiked = com.aylis.comp.online.managers.LikedTracksManager.isLiked(item.videoId)
                btnLike.setImageResource(if (isLiked) R.drawable.ic_unliked else R.drawable.ic_liked)

                btnLike.setOnClickListener {
                    val nowLiked = com.aylis.comp.online.managers.LikedTracksManager.toggleLike(item)
                    btnLike.setImageResource(if (nowLiked) R.drawable.ic_unliked else R.drawable.ic_liked)
                }
            } else if (item is OnlinePlaylist) {
                tvArtist.text = item.subtitle
                btnLike.visibility = View.GONE
                iconPlaylistOverlay.visibility = View.VISIBLE
            }

            if (item.thumbnail.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.thumbnail)
                    .apply(RequestOptions().override(256))
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_delete)
                    .into(imgThumb)
            } else {
                imgThumb.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            itemView.setOnClickListener {
                onClick(item, fullList)
            }
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<OnlineItem>() {
        override fun areItemsTheSame(oldItem: OnlineItem, newItem: OnlineItem): Boolean {
            if (oldItem is OnlineTrack && newItem is OnlineTrack) return oldItem.videoId == newItem.videoId
            if (oldItem is OnlinePlaylist && newItem is OnlinePlaylist) return oldItem.browseId == newItem.browseId
            return false
        }

        override fun areContentsTheSame(oldItem: OnlineItem, newItem: OnlineItem): Boolean {
            return oldItem == newItem
        }
    }
}
