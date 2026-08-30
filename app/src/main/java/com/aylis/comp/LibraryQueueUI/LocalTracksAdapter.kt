package com.aylis.comp.LibraryQueueUI

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.playback.Song.PlaylistSong
import com.aylis.comp.playback.MediaPlaybackService
import com.aylis.Common.Events.WeakEvent4
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

class LocalTracksAdapter(
    private val onItemClick: (List<PlaylistSong.Data>, Int) -> Unit
) : ListAdapter<PlaylistSong.Data, LocalTracksAdapter.TrackViewHolder>(TrackDiffCallback()) {

    private val listenerRefHolder = mutableListOf<Any>()
    
    private val trackChangeListener = WeakEvent4.Handler<PlaylistSong, com.aylis.comp.Common.IItemIdentifier, PlaylistSong.Data, com.aylis.comp.playback.PlayingMediaInfo> { _, _, _, _ ->
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            notifyDataSetChanged()
        }
    }
    
    init {
        MediaPlaybackService.onDisplayMetaDataStateChanged.subscribeWeak(trackChangeListener, listenerRefHolder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_song, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val data = getItem(position) ?: return
        holder.bind(data)
    }

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitle: TextView = itemView.findViewById(R.id.txtItemLine1)
        private val txtArtist: TextView = itemView.findViewById(R.id.txtItemLine2)
        private val txtDuration: TextView? = itemView.findViewById(R.id.txtItemDuration)
        private val imgArt: ImageView? = itemView.findViewById(R.id.imgArt)
        private val btnItemMore: View? = itemView.findViewById(R.id.btnItemMore)
        private val txtNum: TextView? = itemView.findViewById(R.id.txtNum)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(currentList, pos)
                }
            }
            // For now, no "More" menu for local tracks as requested, or we can add delete later.
            btnItemMore?.visibility = View.GONE
            txtNum?.visibility = View.GONE
        }

        fun bind(data: PlaylistSong.Data) {
            txtTitle.text = data.trackName
            txtArtist.text = if (data.artistName.isNullOrEmpty() || data.artistName == "<unknown>") "Unknown Artist" else data.artistName

            if (txtDuration != null) {
                val seconds = data.duration / 1000
                txtDuration.text = String.format("%02d:%02d", seconds / 60, seconds % 60)
            }
            
            val isPlaying = MediaPlaybackService.getInstance()?.let { service ->
                val currentSong = service.currentSong
                val currentData = currentSong?.data
                if (currentData != null) {
                    val sameId = currentData.audioId == data.audioId && data.audioId > 0
                    val samePath = currentData.dataSource != null && data.dataSource != null && 
                                   (currentData.dataSource.toString() == data.dataSource.toString() ||
                                    currentData.dataSource.toString().endsWith(data.dataSource.toString()))
                    val sameTitle = data.audioId == -1L && currentData.trackName == data.trackName
                    sameId || samePath || sameTitle
                } else {
                    false
                }
            } ?: false
            val viewActiveBg = itemView.findViewById<View>(R.id.viewActiveBg)
            viewActiveBg?.visibility = if (isPlaying) View.VISIBLE else View.GONE

            if (imgArt != null) {
                val currentThumb = imgArt.tag as? String
                val artPath = data.albumArtPath0Str ?: ""
                
                if (currentThumb != artPath) {
                    imgArt.tag = artPath
                    if (artPath.isNotEmpty()) {
                        Glide.with(itemView.context)
                            .load(android.net.Uri.parse(artPath))
                            .apply(RequestOptions()
                                .placeholder(R.drawable.ic_queue_music)
                                .error(R.drawable.ic_queue_music)
                                .centerCrop()
                            )
                            .into(imgArt)
                    } else {
                        imgArt.setImageResource(R.drawable.ic_queue_music)
                    }
                }
            }
        }
    }
}

class TrackDiffCallback : DiffUtil.ItemCallback<PlaylistSong.Data>() {
    override fun areItemsTheSame(oldItem: PlaylistSong.Data, newItem: PlaylistSong.Data): Boolean {
        if (oldItem.audioId > 0 && newItem.audioId > 0) {
            return oldItem.audioId == newItem.audioId
        }
        return oldItem.dataSource?.toString() == newItem.dataSource?.toString()
    }

    override fun areContentsTheSame(oldItem: PlaylistSong.Data, newItem: PlaylistSong.Data): Boolean {
        return oldItem.trackName == newItem.trackName && oldItem.artistName == newItem.artistName
    }
}
