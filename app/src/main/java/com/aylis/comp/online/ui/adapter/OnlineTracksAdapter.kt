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
import com.aylis.comp.playback.MediaPlaybackService
import com.aylis.Design.PlaybackControlsDesign
import com.aylis.Common.Events.WeakEvent4
import kotlinx.coroutines.launch

class OnlineTracksAdapter(
    private val isHorizontal: Boolean = false,
    private val showNumbers: Boolean = false,
    private val onItemClick: (List<OnlineTrack>, Int) -> Unit
) : ListAdapter<OnlineTrack, OnlineTracksAdapter.TrackViewHolder>(TrackDiffCallback()) {

    private val listenerRefHolder = mutableListOf<Any>()
    
    private val trackChangeListener = WeakEvent4.Handler<com.aylis.comp.playback.Song.PlaylistSong, com.aylis.comp.Common.IItemIdentifier, com.aylis.comp.playback.Song.PlaylistSong.Data, com.aylis.comp.playback.PlayingMediaInfo> { _, _, _, _ ->
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            notifyDataSetChanged()
        }
    }
    
    private val downloadListener = object : com.aylis.comp.online.managers.OnlineDownloadManager.DownloadListener {
        override fun onProgress(videoId: String, progress: Int) {}
        override fun onCompleted(videoId: String, success: Boolean, file: java.io.File?) {
            if (success) {
                notifyDataSetChanged()
            }
        }
    }

    init {
        MediaPlaybackService.onDisplayMetaDataStateChanged.subscribeWeak(trackChangeListener, listenerRefHolder)
        com.aylis.comp.online.managers.OnlineDownloadManager.addListener(downloadListener)
    }

    fun getCurrentListSafe(): List<OnlineTrack> = currentList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_song_online, parent, false)
            
        if (isHorizontal) {
            val displayMetrics = parent.context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val params = view.layoutParams as RecyclerView.LayoutParams
            params.width = (screenWidth * 0.88).toInt()
            view.layoutParams = params
        }
            
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val track = getItem(position) ?: return
        holder.bind(track)
    }

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtTitle: TextView = itemView.findViewById(R.id.txtItemLine1)
        private val txtArtist: TextView = itemView.findViewById(R.id.txtItemLine2)
        private val txtDuration: TextView? = itemView.findViewById(R.id.txtItemDuration)
        private val imgArt: ImageView? = itemView.findViewById(R.id.imgArt)
        private val txtNum: TextView? = itemView.findViewById(R.id.txtNum)
        private val btnItemMore: View? = itemView.findViewById(R.id.btnItemMore)
        private val imgDownloaded: ImageView? = itemView.findViewById(R.id.imgDownloaded)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(currentList, pos)
                }
            }
            btnItemMore?.setOnClickListener { view ->
                val item = getItem(bindingAdapterPosition)
                if (item != null) {
                    showPopupMenu(view, item)
                }
            }
        }

        private fun showPopupMenu(anchor: View, track: OnlineTrack) {
            val popup = android.widget.PopupMenu(anchor.context, anchor)
            val isLiked = com.aylis.comp.online.managers.LikedTracksManager.isLiked(track.videoId)
            val context = anchor.context
            val isDownloaded = com.aylis.comp.online.managers.OnlineDownloadManager.isTrackDownloaded(track.videoId)
            
            popup.menu.add(0, 1, 0, if (isLiked) context.getString(R.string.online_menu_remove_from_liked) else context.getString(R.string.online_menu_like))
            popup.menu.add(0, 2, 0, context.getString(R.string.online_menu_add_to_playlist))
            if (!isDownloaded) {
                popup.menu.add(0, 4, 0, "Скачать")
            }
            popup.menu.add(0, 3, 0, context.getString(R.string.online_menu_not_interested))

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            if (com.aylis.comp.online.managers.AuthManager.isLoggedIn()) {
                                if (isLiked) {
                                    val success = com.aylis.comp.online.repository.OnlineMusicRepository.removeLikeTrack(track.videoId)
                                    if (success) {
                                        com.aylis.comp.online.managers.LikedTracksManager.toggleLike(track)
                                    }
                                } else {
                                    val success = com.aylis.comp.online.repository.OnlineMusicRepository.likeTrack(track.videoId)
                                    if (success) {
                                        com.aylis.comp.online.managers.LikedTracksManager.toggleLike(track)
                                    }
                                }
                            } else {
                                com.aylis.comp.online.managers.LikedTracksManager.toggleLike(track)
                            }
                            android.widget.Toast.makeText(anchor.context, if (isLiked) context.getString(R.string.online_toast_removed_from_likes) else context.getString(R.string.online_toast_added_to_likes), android.widget.Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    2 -> {
                        // Add to playlist logic
                        val activity = getActivity(anchor.context)
                        if (activity != null) {
                            com.aylis.comp.online.ui.dialogs.OnlinePlaylistBottomSheet(track).show(activity.supportFragmentManager, "PlaylistSheet")
                        }
                        true
                    }
                    3 -> {
                        // Not interested logic
                        val pos = bindingAdapterPosition
                        if (pos != androidx.recyclerview.widget.RecyclerView.NO_POSITION) {
                            val newList = currentList.toMutableList()
                            newList.removeAt(pos)
                            submitList(newList)
                            android.widget.Toast.makeText(anchor.context, context.getString(R.string.online_toast_removed_from_list), android.widget.Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    4 -> {
                        com.aylis.comp.online.managers.OnlineDownloadManager.downloadTrack(anchor.context, track)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        
        private fun getActivity(context: android.content.Context): androidx.appcompat.app.AppCompatActivity? {
            var currentContext = context
            while (currentContext is android.content.ContextWrapper) {
                if (currentContext is androidx.appcompat.app.AppCompatActivity) {
                    return currentContext
                }
                currentContext = currentContext.baseContext
            }
            return null
        }

        fun bind(track: OnlineTrack) {
            txtTitle.text = track.title
            txtArtist.text = track.artist

            if (txtDuration != null) {
                txtDuration.text = "YT Music"
            }
            
            if (imgArt != null) {
                val currentThumb = imgArt.tag as? String
                if (currentThumb != track.thumbnail) {
                    if (track.thumbnail.isNotEmpty()) {
                        Glide.with(itemView.context)
                            .load(track.thumbnail)
                            .apply(RequestOptions().override(256))
                            .placeholder(R.drawable.ic_art)
                            .into(imgArt)
                    } else {
                        imgArt.setImageResource(R.drawable.ic_art)
                    }
                    imgArt.tag = track.thumbnail
                }
            }
            
            val currentPath = PlaybackControlsDesign.currentTrack?.getConstrucPath() ?: ""
            val isPlaying = currentPath.contains(track.videoId)
            val viewActiveBg = itemView.findViewById<View>(R.id.viewActiveBg)
            viewActiveBg?.visibility = if (isPlaying) View.VISIBLE else View.GONE
            
            if (txtNum != null) {
                if (showNumbers) {
                    txtNum.visibility = View.VISIBLE
                    txtNum.text = (bindingAdapterPosition + 1).toString()
                } else {
                    txtNum.visibility = View.GONE
                }
            }
            
            if (imgDownloaded != null) {
                val isDownloaded = com.aylis.comp.online.managers.OnlineDownloadManager.isTrackDownloaded(track.videoId)
                imgDownloaded.visibility = if (isDownloaded) View.VISIBLE else View.GONE
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
