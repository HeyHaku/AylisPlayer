package com.aylis.comp.fonts.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.visual.core.CustomFontManager
import java.io.File

class FragmentFontsDownloaded : Fragment() {

    private lateinit var adapter: FontsDownloadedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fonts_downloaded, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewDownloadedFonts)
        adapter = FontsDownloadedAdapter(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.loadFonts()
    }
}

class FontsDownloadedAdapter(private val context: Context) : RecyclerView.Adapter<FontsDownloadedAdapter.FontViewHolder>() {

    private val fonts = mutableListOf<String>()

    fun loadFonts() {
        val available = CustomFontManager.getAvailableFontNames().filter { it != "Default" }
        fonts.clear()
        fonts.addAll(available)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_font, parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(fonts[position])
    }

    override fun getItemCount(): Int = fonts.size

    inner class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFontPreview: TextView = itemView.findViewById(R.id.tvFontPreview)
        private val tvFontName: TextView = itemView.findViewById(R.id.tvFontName)
        private val ivDownloadStatus: ImageView = itemView.findViewById(R.id.ivDownloadStatus)

        fun bind(fontName: String) {
            tvFontName.text = fontName
            ivDownloadStatus.visibility = View.GONE

            tvFontPreview.typeface = Typeface.DEFAULT
            tvFontPreview.visibility = View.INVISIBLE

            try {
                val typeface = CustomFontManager.getTypeface(fontName)
                if (typeface != null) {
                    tvFontPreview.typeface = typeface
                    tvFontPreview.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            itemView.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_delete_font_title)
                    .setMessage(R.string.dialog_delete_font_msg)
                    .setPositiveButton(R.string.action_yes) { _, _ ->
                        val path = CustomFontManager.getFontFilePath(fontName)
                        if (path != null) {
                            val file = File(path)
                            if (file.exists()) {
                                file.delete()
                                CustomFontManager.scanFonts()
                                loadFonts()
                            }
                        }
                    }
                    .setNegativeButton(R.string.action_no, null)
                    .show()
            }
        }
    }
}
