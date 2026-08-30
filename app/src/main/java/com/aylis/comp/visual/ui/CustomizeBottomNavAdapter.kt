package com.aylis.comp.visual.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import java.util.Locale

class CustomizeBottomNavAdapter(
    private val categories: List<String>,
    initialCategory: String?,
    private val onCategorySelected: (String) -> Unit
) : RecyclerView.Adapter<CustomizeBottomNavAdapter.ViewHolder>() {

    private var selectedPosition = 0

    init {
        if (initialCategory != null) {
            val idx = categories.indexOf(initialCategory)
            if (idx >= 0) {
                selectedPosition = idx
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val navTitle: TextView = itemView.findViewById(R.id.navTitle)
        val navIcon: ImageView = itemView.findViewById(R.id.navIcon)

        init {
            itemView.setOnClickListener {
                val pos = absoluteAdapterPosition
                if (pos != RecyclerView.NO_POSITION && pos != selectedPosition) {
                    val oldSelected = selectedPosition
                    selectedPosition = pos
                    notifyItemChanged(oldSelected)
                    notifyItemChanged(selectedPosition)
                    onCategorySelected(categories[pos])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_bottom_nav_item), parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        holder.navTitle.text = formatDisplayName(category)
        
        val lowerCategory = category.lowercase(Locale.US)
        val iconResId = when {
            lowerCategory.contains("audiocore") || lowerCategory.contains("audio core") || lowerCategory.contains("audio provider") || lowerCategory.contains("sampleprovider") -> R.drawable.nav_ic_audiocore
            lowerCategory.contains("audioconfig") || lowerCategory.contains("audio config") -> R.drawable.nav_ic_audioconfig1
            lowerCategory.contains("bars") || lowerCategory.contains("spectrum") || lowerCategory.contains("segment") -> R.drawable.nav_ic_bars
            lowerCategory.contains("motion") -> R.drawable.nav_ic_motion
            lowerCategory.contains("color") -> R.drawable.nav_ic_color
            lowerCategory.contains("image ") || lowerCategory.contains("image") || lowerCategory.contains("image") -> R.drawable.nav_ic_image_appearance
            lowerCategory.contains("appearance") -> R.drawable.nav_ic_image
            lowerCategory.contains("particle") -> R.drawable.nav_ic_particle
            lowerCategory.contains("font") -> R.drawable.nav_ic_text_font
            lowerCategory.contains("text") -> R.drawable.nav_ic_text
            lowerCategory.contains("behavior") || lowerCategory.contains("reaction") -> R.drawable.nav_ic_behavior
            lowerCategory.contains("modifier") -> R.drawable.nav_ic_modifier
            lowerCategory.contains("general") -> R.drawable.nav_ic_general
            lowerCategory.contains("variables") -> R.drawable.nav_ic_audiocore_general
            lowerCategory.contains("performance") -> R.drawable.nav_ic_audioconfig2
            lowerCategory.contains("beat") -> R.drawable.nav_ic_audioconfig3
            lowerCategory.contains("decay") -> R.drawable.nav_ic_audiocore
            lowerCategory.contains("shader") || lowerCategory.contains("dummy") || lowerCategory.contains("3d") || lowerCategory.contains("blur") || lowerCategory.contains("curve") || lowerCategory.contains("edge") || lowerCategory.contains("fov") || lowerCategory.contains("glitch") || lowerCategory.contains("godray") || lowerCategory.contains("kaleidoscope") || lowerCategory.contains("liquify") || lowerCategory.contains("mirror") || lowerCategory.contains("pixel") || lowerCategory.contains("raindrop") || lowerCategory.contains("rgb") || lowerCategory.contains("spherify") || lowerCategory.contains("twirl") || lowerCategory.contains("vignette") || lowerCategory.contains("zoomblur") -> R.drawable.nav_ic_custom_shader
            else -> R.drawable.nav_ic_general
        }
        holder.navIcon.setImageResource(iconResId)
        
        val isSelected = position == selectedPosition
        holder.itemView.isSelected = isSelected
        holder.navTitle.visibility = if (isSelected) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = categories.size

    private fun formatDisplayName(name: String): String {
        var str = name
        val index = str.indexOf('_')
        if (index in 0 until str.length - 1) {
            var onlyDigitsBefore = true
            for (i in 0 until index) {
                if (!Character.isDigit(str[i])) {
                    onlyDigitsBefore = false
                    break
                }
            }
            if (onlyDigitsBefore) {
                str = str.substring(index + 1)
            }
        }

        val sb = java.lang.StringBuilder()
        var lastLower = false

        if (str.isNotEmpty()) {
            val c = Character.toUpperCase(str[0])
            sb.append(c)
            lastLower = Character.isDigit(c)
        }

        for (i in 1 until str.length) {
            val c = str[i]
            val upper = Character.isUpperCase(c) || Character.isDigit(c)

            if (lastLower && upper) sb.append(' ')

            sb.append(c)
            lastLower = !upper
        }

        return sb.toString()
    }
}
