package com.aylis.comp.photos.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.photos.api.WallhavenPhoto
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class PhotosAdapter(
    private val onClick: (WallhavenPhoto) -> Unit
) : RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    private var photos = listOf<WallhavenPhoto>()

    fun submitList(newPhotos: List<WallhavenPhoto>) {
        photos = newPhotos
        notifyDataSetChanged()
    }

    fun addPhotos(newPhotos: List<WallhavenPhoto>) {
        val startPosition = photos.size
        photos = photos + newPhotos
        notifyItemRangeInserted(startPosition, newPhotos.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount(): Int = photos.size

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val txtViews: TextView? = itemView.findViewById(R.id.txtViews)
        private val txtFavorites: TextView? = itemView.findViewById(R.id.txtFavorites)

        fun bind(photo: WallhavenPhoto) {
            Glide.with(itemView.context)
                .load(photo.thumbs.original)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivPhoto)

            txtViews?.text = formatNumber(photo.views)
            txtFavorites?.text = formatNumber(photo.favorites)

            itemView.setOnClickListener {
                onClick(photo)
            }
        }
        
        private fun formatNumber(num: Int): String {
            return if (num >= 1000) {
                String.format("%.1fk", num / 1000.0)
            } else {
                num.toString()
            }
        }
    }
}
