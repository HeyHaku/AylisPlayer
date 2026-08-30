package com.aylis.comp.online.managers

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.aylis.comp.online.repository.OnlineMusicRepository
import com.aylis.comp.online.repository.OnlineTrack
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.StandardArtwork
import java.io.File
import java.io.FileOutputStream

object OnlineDownloadManager {

    private val listeners = mutableListOf<DownloadListener>()
    private val activeDownloads = mutableMapOf<String, Int>() // videoId to progress %
    private val client = OkHttpClient()

    interface DownloadListener {
        fun onProgress(videoId: String, progress: Int)
        fun onCompleted(videoId: String, success: Boolean, file: File?)
    }

    fun addListener(listener: DownloadListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: DownloadListener) {
        listeners.remove(listener)
    }
    
    fun getProgress(videoId: String): Int? {
        return activeDownloads[videoId]
    }

    fun isTrackDownloaded(videoId: String): Boolean {
        return getDownloadedFile(videoId) != null
    }
    
    fun getDownloadedFile(videoId: String): File? {
        val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "AylisPlayer")
        if (!musicDir.exists()) return null
        val prefix = "[$videoId]"
        return musicDir.listFiles { _, name -> name.contains(prefix) }?.firstOrNull()
    }

    fun downloadTrack(context: Context, track: OnlineTrack) {
        if (activeDownloads.containsKey(track.videoId)) {
            Toast.makeText(context, "Уже скачивается", Toast.LENGTH_SHORT).show()
            return
        }
        if (isTrackDownloaded(track.videoId)) {
            Toast.makeText(context, "Трек уже скачан", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(context, "Начало загрузки...", Toast.LENGTH_SHORT).show()
        activeDownloads[track.videoId] = 0
        notifyProgress(track.videoId, 0)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val streamResult = OnlineMusicRepository.getDownloadStreamUrl(track.videoId)
                if (streamResult == null) {
                    notifyCompleted(track.videoId, false, null)
                    return@launch
                }
                
                val url = streamResult.url
                // Since getDownloadStreamUrl prioritizes M4A, we default to m4a
                val ext = "m4a"
                
                val safeTitle = track.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val safeArtist = track.artist.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val fileName = "$safeArtist - $safeTitle [${track.videoId}].$ext"
                
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "AylisPlayer")
                if (!musicDir.exists()) musicDir.mkdirs()
                
                val outputFile = File(musicDir, fileName)
                
                // Fast download logic using Range headers
                var totalBytes = -1L
                val headRequest = Request.Builder().head().url(url).build()
                client.newCall(headRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val lengthStr = response.header("Content-Length")
                        if (lengthStr != null) {
                            totalBytes = lengthStr.toLongOrNull() ?: -1L
                        }
                    }
                }
                
                if (totalBytes > 0) {
                    val outputStream = FileOutputStream(outputFile)
                    var downloadedBytes = 0L
                    val chunkSize = 1024L * 1024L // 1 MB chunks
                    var lastProgress = 0
                    
                    for (start in 0 until totalBytes step chunkSize) {
                        val end = Math.min(start + chunkSize - 1, totalBytes - 1)
                        val chunkRequest = Request.Builder()
                            .url(url)
                            .header("Range", "bytes=$start-$end")
                            .build()
                            
                        client.newCall(chunkRequest).execute().use { response ->
                            if (response.isSuccessful || response.code == 206) {
                                val body = response.body
                                if (body != null) {
                                    val inputStream = body.byteStream()
                                    val buffer = ByteArray(64 * 1024)
                                    var bytesRead: Int
                                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                        outputStream.write(buffer, 0, bytesRead)
                                        downloadedBytes += bytesRead
                                        
                                        val currentProgress = ((downloadedBytes * 100) / totalBytes).toInt()
                                        if (currentProgress > lastProgress) {
                                            lastProgress = currentProgress
                                            activeDownloads[track.videoId] = currentProgress
                                            notifyProgress(track.videoId, currentProgress)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    outputStream.flush()
                    outputStream.close()
                } else {
                    // Fallback to normal download
                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()
                    val body = response.body
                    if (!response.isSuccessful || body == null) {
                        notifyCompleted(track.videoId, false, null)
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
                                activeDownloads[track.videoId] = currentProgress
                                notifyProgress(track.videoId, currentProgress)
                            }
                        }
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                }
                
                var thumbBytes: ByteArray? = null
                if (track.thumbnail.isNotEmpty()) {
                    try {
                        val thumbRequest = Request.Builder().url(track.thumbnail).build()
                        val thumbResponse = client.newCall(thumbRequest).execute()
                        thumbBytes = thumbResponse.body?.bytes()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                try {
                    val audioFile = AudioFileIO.read(outputFile)
                    val tag = audioFile.tagOrCreateAndSetDefault
                    tag.setField(FieldKey.TITLE, track.title)
                    tag.setField(FieldKey.ARTIST, track.artist)
                    
                    if (thumbBytes != null) {
                        try {
                            val bitmap = android.graphics.BitmapFactory.decodeByteArray(thumbBytes, 0, thumbBytes.size)
                            if (bitmap != null) {
                                val out = java.io.ByteArrayOutputStream()
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                val jpegBytes = out.toByteArray()
                                val artwork = org.jaudiotagger.tag.images.ArtworkFactory.getNew()
                                artwork.binaryData = jpegBytes
                                artwork.mimeType = "image/jpeg"
                                artwork.description = ""
                                tag.deleteArtworkField()
                                tag.setField(artwork)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    audioFile.commit()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                
                MediaScannerConnection.scanFile(context, arrayOf(outputFile.absolutePath), null) { _, _ ->
                    notifyCompleted(track.videoId, true, outputFile)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                notifyCompleted(track.videoId, false, null)
            }
        }
    }
    
    private fun notifyProgress(videoId: String, progress: Int) {
        Handler(Looper.getMainLooper()).post {
            listeners.forEach { it.onProgress(videoId, progress) }
        }
    }

    private fun notifyCompleted(videoId: String, success: Boolean, file: File?) {
        activeDownloads.remove(videoId)
        Handler(Looper.getMainLooper()).post {
            listeners.forEach { it.onCompleted(videoId, success, file) }
        }
    }
}
