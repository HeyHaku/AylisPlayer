package com.aylis.comp.fonts.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aylis.R
import com.aylis.comp.fonts.api.FontModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class FontsAdapter(private val context: Context) : RecyclerView.Adapter<FontsAdapter.FontViewHolder>() {

    private val fonts = mutableListOf<FontModel>()
    private val httpClient = OkHttpClient()
    private val cacheDir = File(context.cacheDir, "fonts").apply { mkdirs() }
    private val permanentDir = File(
        com.aylis.comp.visual.core.CustomFontManager.getFontsFolder()
    ).apply { mkdirs() }

    fun submitList(newFonts: List<FontModel>) {
        fonts.clear()
        fonts.addAll(newFonts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_font, parent, false)
        return FontViewHolder(view)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        val font = fonts[position]
        holder.bind(font)
    }

    override fun getItemCount(): Int = fonts.size

    inner class FontViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFontPreview: TextView = itemView.findViewById(R.id.tvFontPreview)
        private val tvFontName: TextView = itemView.findViewById(R.id.tvFontName)
        private val ivDownloadStatus: ImageView = itemView.findViewById(R.id.ivDownloadStatus)

        fun bind(font: FontModel) {
            tvFontName.text = font.name
            
            // Reset preview to default typeface in case it is recycled
            tvFontPreview.typeface = Typeface.DEFAULT
            tvFontPreview.visibility = View.INVISIBLE
            
            val fileName = font.downloadUrl.substringAfterLast("/")
            val permanentFile = File(permanentDir, fileName)
            val cachedFile = File(cacheDir, fileName)

            if (permanentFile.exists()) {
                ivDownloadStatus.visibility = View.VISIBLE
                tryLoadTypeface(permanentFile)
            } else {
                ivDownloadStatus.visibility = View.GONE
                if (cachedFile.exists()) {
                    tryLoadTypeface(cachedFile)
                } else {
                    // Download to cache for preview
                    downloadAndPreview(font.downloadUrl, cachedFile)
                }
            }

            itemView.setOnClickListener {
                if (permanentFile.exists()) {
                    // Already downloaded
                    return@setOnClickListener
                }
                showDownloadDialog(font, cachedFile, permanentFile)
            }
        }

        private fun tryLoadTypeface(file: File) {
            try {
                val typeface = Typeface.createFromFile(file)
                tvFontPreview.typeface = typeface
                tvFontPreview.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun downloadAndPreview(url: String, destFile: File) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = Request.Builder().url(url).build()
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            tryLoadTypeface(destFile)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun showDownloadDialog(font: FontModel, cachedFile: File, permanentFile: File) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_font_download, null)
            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .create()

            val tvDialogFontName = dialogView.findViewById<TextView>(R.id.tvDialogFontName)
            val tvDialogFontAuthor = dialogView.findViewById<TextView>(R.id.tvDialogFontAuthor)
            val tvDialogFontPreview = dialogView.findViewById<TextView>(R.id.tvDialogFontPreview)
            val tvDialogFontLink = dialogView.findViewById<TextView>(R.id.tvDialogFontLink)
            val btnDialogCancel = dialogView.findViewById<View>(R.id.btnDialogCancel)
            val btnDialogDownload = dialogView.findViewById<View>(R.id.btnDialogDownload)

            tvDialogFontName.text = font.name
            tvDialogFontAuthor.text = context.getString(R.string.font_author_format, font.author)
            
            if (cachedFile.exists()) {
                try {
                    tvDialogFontPreview.typeface = Typeface.createFromFile(cachedFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            tvDialogFontLink.setOnClickListener {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("https://github.com/${font.author}/${font.repositoryName}")
                context.startActivity(intent)
            }

            btnDialogCancel.setOnClickListener {
                dialog.dismiss()
            }

            btnDialogDownload.setOnClickListener {
                dialog.dismiss()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (cachedFile.exists()) {
                            cachedFile.copyTo(permanentFile, overwrite = true)
                        } else {
                            // Download directly if cache failed or doesn't exist
                            val request = Request.Builder().url(font.downloadUrl).build()
                            val response = httpClient.newCall(request).execute()
                            if (response.isSuccessful) {
                                response.body?.byteStream()?.use { input ->
                                    FileOutputStream(permanentFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            ivDownloadStatus.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            dialog.show()
        }
    }
}
