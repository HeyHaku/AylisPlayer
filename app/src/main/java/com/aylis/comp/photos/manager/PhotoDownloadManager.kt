package com.aylis.comp.photos.manager

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object PhotoDownloadManager {

    private val client = OkHttpClient()

    interface DownloadListener {
        fun onProgress(id: String, progress: Int)
        fun onCompleted(id: String, success: Boolean, file: File?)
    }

    private val listeners = mutableListOf<DownloadListener>()
    private val activeDownloads = mutableMapOf<String, Int>()

    fun addListener(listener: DownloadListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: DownloadListener) {
        listeners.remove(listener)
    }

    fun downloadPhoto(context: Context, id: String, url: String) {
        if (activeDownloads.containsKey(id)) {
            Toast.makeText(context, "Уже скачивается", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(context, "Начало загрузки...", Toast.LENGTH_SHORT).show()
        activeDownloads[id] = 0
        notifyProgress(id, 0)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val safeId = id.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val fileName = "Unsplash_$safeId.jpg"
                
                val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenPlayer")
                if (!picturesDir.exists()) picturesDir.mkdirs()
                
                val outputFile = File(picturesDir, fileName)
                
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body
                
                if (!response.isSuccessful || body == null) {
                    notifyCompleted(id, false, null)
                    return@launch
                }
                
                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(outputFile)
                
                val buffer = ByteArray(16 * 1024)
                var bytesRead: Int
                var totalRead = 0L
                var lastProgress = 0
                
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (contentLength > 0) {
                        val currentProgress = ((totalRead * 100) / contentLength).toInt()
                        if (currentProgress > lastProgress) {
                            lastProgress = currentProgress
                            activeDownloads[id] = currentProgress
                            notifyProgress(id, currentProgress)
                        }
                    }
                }
                
                outputStream.flush()
                outputStream.close()
                inputStream.close()
                
                MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null) { _, _ ->
                    notifyCompleted(id, true, outputFile)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                notifyCompleted(id, false, null)
            }
        }
    }
    
    private fun notifyProgress(id: String, progress: Int) {
        Handler(Looper.getMainLooper()).post {
            listeners.forEach { it.onProgress(id, progress) }
        }
    }

    private fun notifyCompleted(id: String, success: Boolean, file: File?) {
        activeDownloads.remove(id)
        Handler(Looper.getMainLooper()).post {
            listeners.forEach { it.onCompleted(id, success, file) }
        }
    }
}
