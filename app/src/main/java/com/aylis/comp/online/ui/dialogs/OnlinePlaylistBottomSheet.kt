package com.aylis.comp.online.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.aylis.R
import com.aylis.comp.online.repository.OnlineTrack
import com.aylis.comp.online.repository.OnlinePlaylist
import com.aylis.comp.online.repository.OnlineMusicRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OnlinePlaylistBottomSheet(private val track: OnlineTrack) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_online_playlist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val rvPlaylists = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvPlaylists)
        val btnCreatePlaylist = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCreatePlaylist)

        rvPlaylists.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        
        btnCreatePlaylist.setOnClickListener {
            val context = requireContext()
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_playlist, null)
            val etPlaylistName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTxtPlaylistName)
            val spinnerType = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerType)
            spinnerType?.visibility = View.GONE

            com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle(R.string.online_dialog_new_playlist_title)
                .setView(dialogView)
                .setPositiveButton(R.string.online_dialog_create) { _, _ ->
                    val title = etPlaylistName.text.toString()
                    if (title.isNotBlank()) {
                        GlobalScope.launch(Dispatchers.Main) {
                            val newId = OnlineMusicRepository.createPlaylist(title)
                            context?.let { ctx ->
                                if (newId != null) {
                                    OnlineMusicRepository.addTrackToPlaylist(newId, track.videoId)
                                    Toast.makeText(ctx, R.string.online_toast_created_and_added, Toast.LENGTH_SHORT).show()
                                    dismiss()
                                } else {
                                    Toast.makeText(ctx, R.string.online_toast_failed_to_create, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                .setNegativeButton(R.string.online_dialog_cancel, null)
                .show()
        }

        GlobalScope.launch(Dispatchers.Main) {
            val playlists = OnlineMusicRepository.getLikedPlaylists()
            val adapter = PlaylistSelectionAdapter(playlists) { selectedPlaylist ->
                GlobalScope.launch(Dispatchers.Main) {
                    val success = OnlineMusicRepository.addTrackToPlaylist(selectedPlaylist.browseId, track.videoId)
                    context?.let { ctx ->
                        if (success) {
                            Toast.makeText(ctx, R.string.online_toast_added_to_playlist, Toast.LENGTH_SHORT).show()
                            dismiss()
                        } else {
                            Toast.makeText(ctx, R.string.online_toast_failed_to_add, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            rvPlaylists.adapter = adapter
        }
    }

    inner class PlaylistSelectionAdapter(
        private val items: List<OnlinePlaylist>,
        private val onItemClick: (OnlinePlaylist) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<PlaylistSelectionAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            val tvName: android.widget.TextView = itemView.findViewById(R.id.tvPlaylistName)
            init {
                itemView.setOnClickListener { onItemClick(items[adapterPosition]) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_bottom_sheet_playlist, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.tvName.text = items[position].title
        }

        override fun getItemCount() = items.size
    }
}
