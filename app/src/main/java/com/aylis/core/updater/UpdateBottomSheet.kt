package com.aylis.core.updater

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.aylis.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class UpdateBottomSheet(
    private val release: GitHubRelease,
    private val onDownloadClicked: (String) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val tvUpdateTitle = view.findViewById<TextView>(R.id.tvUpdateTitle)
        val tvUpdateChangelog = view.findViewById<TextView>(R.id.tvUpdateChangelog)
        val btnDownloadUpdate = view.findViewById<MaterialButton>(R.id.btnDownloadUpdate)
        val btnOpenGitHub = view.findViewById<MaterialButton>(R.id.btnOpenGitHub)
        val btnCancelUpdate = view.findViewById<MaterialButton>(R.id.btnCancelUpdate)
        
        tvUpdateTitle.text = "Доступно обновление: ${release.tagName}"
        tvUpdateChangelog.text = release.body ?: "Нет описания изменений"
        
        btnDownloadUpdate.setOnClickListener {
            android.util.Log.d("UpdateManager", "btnDownloadUpdate clicked. Assets count: ${release.assets.size}")
            for (asset in release.assets) {
                android.util.Log.d("UpdateManager", "Asset: name=${asset.name}, url=${asset.browserDownloadUrl}")
            }
            // Find the first .apk asset
            val apkAsset = release.assets.firstOrNull { it.browserDownloadUrl.endsWith(".apk", ignoreCase = true) }
            if (apkAsset != null) {
                android.util.Log.d("UpdateManager", "Found APK asset: ${apkAsset.name}")
                onDownloadClicked(apkAsset.browserDownloadUrl)
            } else {
                android.util.Log.e("UpdateManager", "No .apk asset found in release!")
                android.widget.Toast.makeText(requireContext(), "Не найден APK файл для скачивания", android.widget.Toast.LENGTH_LONG).show()
            }
            dismiss()
        }
        
        btnOpenGitHub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/HeyHaku/AylisPlayer/releases/tag/${release.tagName}"))
            startActivity(intent)
            dismiss()
        }
        
        btnCancelUpdate.setOnClickListener {
            dismiss()
        }
    }
}
