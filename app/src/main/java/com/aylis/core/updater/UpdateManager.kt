package com.aylis.core.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class UpdateManager(private val context: Context) {

    private val okHttpClient = OkHttpClient()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    fun checkForUpdatesAsync(isSilent: Boolean = true) {
        kotlinx.coroutines.GlobalScope.launch {
            checkForUpdates(isSilent)
        }
    }

    suspend fun checkForUpdates(isSilent: Boolean = true) {
        withContext(Dispatchers.IO) {
            try {
                val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                val receiveBeta = prefs.getBoolean("receive_beta_updates", true)

                val url = if (receiveBeta) {
                    "https://api.github.com/repos/HeyHaku/AylisPlayer/releases"
                } else {
                    "https://api.github.com/repos/HeyHaku/AylisPlayer/releases/latest"
                }

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    responseBody?.let {
                        val release: GitHubRelease? = if (receiveBeta) {
                            // API returns a list if we query /releases
                            val adapter = moshi.adapter(Array<GitHubRelease>::class.java)
                            val releases = adapter.fromJson(it)
                            releases?.firstOrNull()
                        } else {
                            // API returns a single object if we query /releases/latest
                            val adapter = moshi.adapter(GitHubRelease::class.java)
                            adapter.fromJson(it)
                        }

                        if (release != null) {
                            val tagName = release.tagName.removePrefix("v")
                            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            val currentVersion = (packageInfo.versionName ?: "1.0.0").removePrefix("v")
                            
                            if (isNewerVersion(tagName, currentVersion)) {
                                withContext(Dispatchers.Main) {
                                    showUpdateDialog(release)
                                }
                            } else {
                                if (!isSilent) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Установлена последняя версия", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (!isSilent) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка проверки обновлений", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (!isSilent) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun isNewerVersion(remoteVersion: String, localVersion: String): Boolean {
        // Basic semantic versioning comparison (e.g., "1.0.1" vs "1.0.0")
        val remoteParts = remoteVersion.split(".").mapNotNull { it.toIntOrNull() }
        val localParts = localVersion.split(".").mapNotNull { it.toIntOrNull() }
        
        val length = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until length) {
            val remotePart = remoteParts.getOrElse(i) { 0 }
            val localPart = localParts.getOrElse(i) { 0 }
            if (remotePart > localPart) return true
            if (remotePart < localPart) return false
        }
        return false
    }

    private fun showUpdateDialog(release: GitHubRelease) {
        if (context is FragmentActivity) {
            val bottomSheet = UpdateBottomSheet(release) { downloadUrl ->
                downloadAndInstall(downloadUrl)
            }
            bottomSheet.show(context.supportFragmentManager, "UpdateBottomSheet")
        } else {
            Toast.makeText(context, "Доступно обновление: ${release.tagName}", Toast.LENGTH_LONG).show()
            // Fallback if context is not FragmentActivity, though usually it will be.
        }
    }

    private fun downloadAndInstall(downloadUrl: String) {
        Log.d("UpdateManager", "Start downloadAndInstall for URL: $downloadUrl")
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(downloadUrl)
        
        val request = DownloadManager.Request(uri).apply {
            setTitle("Загрузка обновления")
            setDescription("Скачивание новой версии AylisPlayer")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            addRequestHeader("User-Agent", "Mozilla/5.0")
            // Use external files dir so FileProvider can access it
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
        }

        // Delete old file if exists
        val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (oldFile.exists()) {
            oldFile.delete()
        }

        val downloadId = downloadManager.enqueue(request)
        Log.d("UpdateManager", "Download enqueued with ID: $downloadId")
        
        // Register receiver for when download is complete
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                Log.d("UpdateManager", "Received broadcast for download ID: $id")
                if (id == downloadId) {
                    Log.d("UpdateManager", "Download complete, starting installation")
                    installApk()
                    context.unregisterReceiver(this)
                }
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onComplete, 
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(
                onComplete, 
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
        
        Toast.makeText(context, "Загрузка началась...", Toast.LENGTH_SHORT).show()
    }

    private fun installApk() {
        Log.d("UpdateManager", "Checking permission to install APK")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Log.w("UpdateManager", "Cannot request package installs. Prompting user.")
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                // Returning early so the user can grant permission.
                // In a perfect world, we'd wait for a result, but for now we'll just open the settings.
                Toast.makeText(context, "Разрешите установку из неизвестных источников и повторите", Toast.LENGTH_LONG).show()
                return
            }
        }

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        Log.d("UpdateManager", "Installing APK from file: ${file.absolutePath}")
        if (!file.exists()) {
            Log.e("UpdateManager", "APK file does not exist!")
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        Log.d("UpdateManager", "Generated URI for file: $uri")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            Log.d("UpdateManager", "Starting install activity")
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Failed to start install activity", e)
            Toast.makeText(context, "Ошибка при запуске установки", Toast.LENGTH_SHORT).show()
        }
    }
}
