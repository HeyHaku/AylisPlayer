package com.aylis.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.aylis.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File

object CrashReporter {
    fun checkAndShowCrashDialog(activity: Activity) {
        val crashFile = File(activity.filesDir, CrashHandler.CRASH_LOG_FILE_NAME)
        if (crashFile.exists()) {
            val crashLog = crashFile.readText()
            
            // Show custom expressive M3 bottom sheet
            val dialog = BottomSheetDialog(activity)
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_crash_report, null)
            dialog.setContentView(view)
            dialog.setCancelable(false)
            
            val txtCrashPreview = view.findViewById<TextView>(R.id.txtCrashPreview)
            val btnShare = view.findViewById<android.view.View>(R.id.btnShare)
            val btnTelegram = view.findViewById<android.view.View>(R.id.btnTelegram)
            val btnClose = view.findViewById<android.view.View>(R.id.btnClose)
            
            // Show a preview of the crash (first 10 lines)
            val logLines = crashLog.lines()
            val preview = logLines.take(15).joinToString("\n") + if (logLines.size > 15) "\n..." else ""
            txtCrashPreview.text = preview

            btnShare.setOnClickListener {
                shareCrashLogGeneric(activity, crashFile)
            }

            btnTelegram.setOnClickListener {
                openTelegramAuthor(activity, crashFile, crashLog)
            }

            btnClose.setOnClickListener {
                crashFile.delete()
                dialog.dismiss()
            }

            dialog.show()
        }
    }

    private fun shareCrashLogGeneric(activity: Activity, logFile: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", logFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.crash_share_chooser_title)))
    }

    private fun openTelegramAuthor(activity: Activity, logFile: File, logText: String) {
        try {
            // Send direct to hey_haku
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=hey_haku"))
            activity.startActivity(intent)
            
            // Also copy the log to clipboard since we can't easily pre-fill a specific chat in TG with a file
            val clipboard = activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Crash Log", logText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(activity, activity.getString(R.string.crash_toast_copied), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(activity, activity.getString(R.string.crash_toast_tg_not_found), Toast.LENGTH_SHORT).show()
            shareCrashLogGeneric(activity, logFile)
        }
    }
}
