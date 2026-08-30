package com.aylis.ui.dialogs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.aylis.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProjectAboutBottomSheetDialog : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_about_project, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnTelegram = view.findViewById<Button>(R.id.btn_about_telegram)
        val btnGithub = view.findViewById<Button>(R.id.btn_about_github)
        val btnClose = view.findViewById<Button>(R.id.btn_about_close)

        btnTelegram?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/aylis_player"))
            startActivity(intent)
        }

        btnGithub?.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aylis/OpenPlayer"))
            startActivity(intent)
        }

        btnClose?.setOnClickListener {
            dismiss()
        }
    }

    companion object {
        const val TAG = "ProjectAboutBottomSheetDialog"

        fun newInstance(): ProjectAboutBottomSheetDialog {
            return ProjectAboutBottomSheetDialog()
        }
    }
}
