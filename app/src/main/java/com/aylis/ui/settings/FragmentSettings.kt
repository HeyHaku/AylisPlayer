package com.aylis.ui.settings

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aylis.R
import com.aylis.comp.AppPreferences.AppPreferences
import com.aylis.comp.online.managers.AuthManager
import com.aylis.comp.online.ui.dialogs.AccountBottomSheetDialog
import com.aylis.utils.HapticManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch
import nz.net.speakman.androidlicensespage.LicensesFragment

class FragmentSettings : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var appPrefs: AppPreferences

    private val exportBackupLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { outStream ->
                    com.aylis.utils.SettingsBackupManager.exportToStream(requireContext(), outStream)
                }
                android.widget.Toast.makeText(requireContext(), R.string.backup_saved_success, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(requireContext(), R.string.backup_saved_error, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importBackupLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val success = requireContext().contentResolver.openInputStream(uri)?.use { inStream ->
                    com.aylis.utils.SettingsBackupManager.importFromStream(requireContext(), inStream)
                } ?: false
                
                if (success) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.backup_restore_completed_title)
                        .setMessage(R.string.backup_restore_completed_message)
                        .setCancelable(false)
                        .setPositiveButton(R.string.backup_restore_restart_button) { _, _ ->
                            val intent = requireActivity().baseContext.packageManager.getLaunchIntentForPackage(requireActivity().baseContext.packageName)
                            if (intent != null) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                            Runtime.getRuntime().exit(0)
                        }
                        .show()
                } else {
                    android.widget.Toast.makeText(requireContext(), R.string.backup_import_error, android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.widget.Toast.makeText(requireContext(), R.string.backup_import_error, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shareBackupFile() {
        try {
            val cacheDir = java.io.File(requireContext().cacheDir, "backups")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val backupFile = java.io.File(cacheDir, "OpenPlayerBackup.zip")
            
            java.io.FileOutputStream(backupFile).use { outStream ->
                com.aylis.utils.SettingsBackupManager.exportToStream(requireContext(), outStream)
            }
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                backupFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.backup_share_title)))
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(requireContext(), R.string.backup_share_error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appPrefs = AppPreferences.createOrGetInstance()
        prefs = appPrefs.getPreferences(requireContext())

        setupProfileCard(view)
        setupOpenSourceCard(view)
        setupInterfaceCard(view)
        setupVibrationCard(view)
        setupVisualizerCard(view)
        setupPlaybackCard(view)
        setupFilesCard(view)
        setupSystemCard(view)
    }

    private fun setupProfileCard(view: View) {
        val cardProfile = view.findViewById<View>(R.id.cardProfile)
        val tvProfileName = view.findViewById<TextView>(R.id.tvProfileName)
        val tvProfileStatus = view.findViewById<TextView>(R.id.tvProfileStatus)
        val tvProfileMonogram = view.findViewById<TextView>(R.id.tvProfileMonogram)
        val ivProfileIcon = view.findViewById<ImageView>(R.id.ivProfileIcon)

        cardProfile.setOnClickListener {
            AccountBottomSheetDialog().show(childFragmentManager, "AccountBottomSheetDialog")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AuthManager.activeAccountFlow.collect { account ->
                    if (account != null) {
                        tvProfileName.text = account.name
                        tvProfileStatus.text = account.email?.ifEmpty { getString(R.string.profile_connected_ytm) } ?: getString(R.string.profile_connected_ytm)
                        ivProfileIcon.visibility = View.GONE
                        tvProfileMonogram.visibility = View.VISIBLE
                        tvProfileMonogram.text = account.name.firstOrNull()?.uppercase() ?: "Y"
                    } else {
                        tvProfileName.setText(R.string.profile_guest_mode)
                        tvProfileStatus.setText(R.string.profile_no_ytm_connection)
                        ivProfileIcon.visibility = View.VISIBLE
                        tvProfileMonogram.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun setupOpenSourceCard(view: View) {
        val cardOpenSource = view.findViewById<View>(R.id.cardOpenSource)
        cardOpenSource.setOnClickListener {
            com.aylis.ui.settings.dialogs.OpenSourceBottomSheetDialog()
                .show(childFragmentManager, "OpenSourceBottomSheetDialog")
        }
    }

    private fun setupInterfaceCard(view: View) {
        view.findViewById<View>(R.id.itemTheme).bindList(
            R.drawable.ic_liked, R.string.pref_theme_mode_title,
            R.array.pref_theme_mode_entries, R.array.pref_theme_mode_values,
            prefs.getString("key_theme_mode", "1") ?: "1"
        ) { newValue ->
            appPrefs.setInt(AppPreferences.PREF_Int_themeMode, newValue.toIntOrNull() ?: 1)
            prefs.edit().putString("key_theme_mode", newValue).apply()
            requireActivity().recreate()
        }

        view.findViewById<View>(R.id.itemAmbientSettings).bindAction(
            R.drawable.ic_visual, R.string.pref_ambient_settings_title, ""
        ) {
            parentFragmentManager.beginTransaction()
                .add(android.R.id.content, FragmentAmbientSettings())
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<View>(R.id.itemUiLayoutCcs).bindSwitch(
            R.drawable.ic_aspect, R.string.pref_ui_layout_ccs_title, R.string.pref_ui_layout_ccs_summary,
            appPrefs.getBool(AppPreferences.PREF_Bool_uiLayoutCCS)
        ) { newValue ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_restart_ccs_title)
                .setMessage(R.string.dialog_restart_ccs_msg)
                .setPositiveButton(R.string.dialog_ok) { _, _ ->
                    appPrefs.setBool(AppPreferences.PREF_Bool_uiLayoutCCS, newValue)
                    prefs.edit().putBoolean("bool" + (AppPreferences.PREF_Bool_uiLayoutCCS - 1000), newValue).commit()

                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = requireActivity().baseContext.packageManager.getLaunchIntentForPackage(requireActivity().baseContext.packageName)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                        Runtime.getRuntime().exit(0)
                    }, 150)
                }
                .setNegativeButton(R.string.dialog_cancel) { _, _ ->
                    val switchWidget = view.findViewById<View>(R.id.itemUiLayoutCcs).findViewById<MaterialSwitch>(R.id.itemSwitch)
                    switchWidget.isChecked = !newValue
                }
                .setOnCancelListener {
                    val switchWidget = view.findViewById<View>(R.id.itemUiLayoutCcs).findViewById<MaterialSwitch>(R.id.itemSwitch)
                    switchWidget.isChecked = !newValue
                }
                .show()
        }

        view.findViewById<View>(R.id.itemLibUseSwipeBack).bindSwitch(
            R.drawable.ic_back, R.string.pref_libswipeback, R.string.pref_libswipeback2,
            prefs.getBoolean("pref_libUseSwipeBack", true)
        ) { newValue ->
            prefs.edit().putBoolean("pref_libUseSwipeBack", newValue).apply()
        }

        view.findViewById<View>(R.id.itemToolButtonsShowTexts).bindSwitch(
            R.drawable.ic_text, R.string.pref_toolButtonsShowTexts, R.string.pref_toolButtonsShowTexts2,
            prefs.getBoolean("pref_toolButtonsShowTexts", true)
        ) { newValue ->
            prefs.edit().putBoolean("pref_toolButtonsShowTexts", newValue).apply()
        }

        view.findViewById<View>(R.id.itemVisControlsTimeout).bindSwitch(
            R.drawable.ic_maximize, R.string.pref_visControlsTimeout, R.string.pref_visControlsTimeout2,
            appPrefs.getBool(AppPreferences.PREF_Bool_pref_visControlsTimeout)
        ) { newValue ->
            appPrefs.setBool(AppPreferences.PREF_Bool_pref_visControlsTimeout, newValue)
            prefs.edit().putBoolean("pref_visControlsTimeout", newValue).apply()
        }
    }

    private fun setupVibrationCard(view: View) {
        val initialIntensity = prefs.getInt("pref_haptic_intensity", 0).coerceAtMost(5)
        
        view.findViewById<View>(R.id.itemHapticIntensity).bindSlider(
            R.drawable.ic_vid_settings, R.string.pref_hapticFeedback_intensity,
            initialIntensity
        ) { newValue ->
            prefs.edit().putInt("pref_haptic_intensity", newValue).apply()
            HapticManager.setIntensity(newValue)
        }

        view.findViewById<View>(R.id.itemHapticType).bindList(
            R.drawable.ic_vid_settings, R.string.pref_hapticFeedback_type,
            R.array.pref_hapticFeedback_type_entries, R.array.pref_hapticFeedback_type_values,
            prefs.getString("pref_haptic_type", "knock") ?: "knock"
        ) { newValue ->
            prefs.edit().putString("pref_haptic_type", newValue).apply()
            HapticManager.setType(newValue)
            HapticManager.performTick(view) // Vibrate on type change to preview
        }

        view.findViewById<View>(R.id.itemHapticGlobal).bindSwitch(
            R.drawable.ic_vid_settings, R.string.pref_haptic_global, R.string.pref_haptic_global2,
            prefs.getBoolean("pref_haptic_global", false)
        ) { newValue ->
            prefs.edit().putBoolean("pref_haptic_global", newValue).apply()
            HapticManager.globalVibration = newValue
        }
    }

    private fun setupVisualizerCard(view: View) {
        view.findViewById<View>(R.id.itemExoVisualizerOffset).bindList(
            R.drawable.ic_delay, R.string.pref_exoVisualizerOffset,
            R.array.pref_exoVisualizerOffset_entries, R.array.pref_exoVisualizerOffset_values,
            prefs.getString("pref_exoVisualizerOffset", "-500") ?: "-500"
        ) { newValue ->
            prefs.edit().putString("pref_exoVisualizerOffset", newValue).apply()
            appPrefs.setInt(AppPreferences.PREF_Int_exoVisualizerOffset, newValue.toIntOrNull() ?: -500)
        }

        view.findViewById<View>(R.id.itemVisualizerFps).bindList(
            R.drawable.ic_fps, R.string.pref_visualizerFrameRateLimit,
            R.array.pref_visualizerFrameRateLimit_entries, R.array.pref_visualizerFrameRateLimit_values,
            prefs.getString("pref_visualizerFrameRateLimit", "60") ?: "60"
        ) { newValue ->
            if (newValue == "120") {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.fps_warning_title)
                    .setMessage(R.string.fps_warning_message)
                    .setPositiveButton(R.string.fps_dialog_ok) { _, _ ->
                        prefs.edit().putString("pref_visualizerFrameRateLimit", "120").apply()
                        appPrefs.setInt(AppPreferences.PREF_Int_visualizerFrameRateLimit, 120, true)
                        view.findViewById<View>(R.id.itemVisualizerFps).findViewById<TextView>(R.id.itemValue).text = resources.getStringArray(R.array.pref_visualizerFrameRateLimit_entries)[1]
                    }
                    .setNegativeButton(R.string.fps_dialog_cancel) { _, _ ->
                        // Revert
                        view.findViewById<View>(R.id.itemVisualizerFps).findViewById<TextView>(R.id.itemValue).text = resources.getStringArray(R.array.pref_visualizerFrameRateLimit_entries)[0]
                    }
                    .show()
            } else {
                prefs.edit().putString("pref_visualizerFrameRateLimit", newValue).apply()
                appPrefs.setInt(AppPreferences.PREF_Int_visualizerFrameRateLimit, 60, true)
            }
        }

        view.findViewById<View>(R.id.itemVisualizerResolution).bindList(
            R.drawable.ic_quality, R.string.pref_visualizerResolutionScale,
            R.array.pref_visualizerResolutionScale_entries, R.array.pref_visualizerResolutionScale_values,
            prefs.getString("pref_visualizerResolutionScale", "2.0") ?: "2.0"
        ) { newValue ->
            prefs.edit().putString("pref_visualizerResolutionScale", newValue).apply()
            appPrefs.setString(AppPreferences.PREF_String_visualizerResolutionScale, newValue)
        }

        view.findViewById<View>(R.id.itemVisualizerGlobalSession).bindSwitch(
            R.drawable.ic_visual, R.string.pref_visualizerGlobalSession_txt1, R.string.pref_visualizerGlobalSession_txt2,
            appPrefs.getBool(AppPreferences.PREF_Bool_visualizerUseGlobalSession)
        ) { newValue ->
            appPrefs.setBool(AppPreferences.PREF_Bool_visualizerUseGlobalSession, newValue)
            prefs.edit().putBoolean("pref_visualizerGlobalSession", newValue).apply()
        }

        view.findViewById<View>(R.id.itemHighQualityBlur).bindSwitch(
            R.drawable.ic_quality, R.string.pref_highQualityBlur_txt1, R.string.pref_highQualityBlur_txt2,
            prefs.getBoolean("pref_highQualityBlur", false)
        ) { newValue ->
            prefs.edit().putBoolean("pref_highQualityBlur", newValue).apply()
        }
    }

    private fun setupPlaybackCard(view: View) {
        view.findViewById<View>(R.id.itemPlaybackEngine).bindList(
            R.drawable.ic_engine, R.string.pref_playbackEngine,
            R.array.pref_playbackEngine_entries, R.array.pref_playbackEngine_values,
            prefs.getString("pref_playbackEngine2", getString(R.string.pref_playbackEngine_default)) ?: "1"
        ) { newValue ->
            prefs.edit().putString("pref_playbackEngine2", newValue).apply()
            appPrefs.setInt(AppPreferences.PREF_Int_playbackEngine, newValue.toIntOrNull() ?: 1)
        }

        view.findViewById<View>(R.id.itemFadePlayPause).bindSwitch(
            R.drawable.ic_fade, R.string.pref_fadePlayPause, R.string.pref_fadePlayPause2,
            prefs.getBoolean("pref_fadePlayPause", true)
        ) { newValue ->
            prefs.edit().putBoolean("pref_fadePlayPause", newValue).apply()
        }

        view.findViewById<View>(R.id.itemResumePlayingAfterAudioFocusGained).bindSwitch(
            R.drawable.ic_resume, R.string.pref_resumePlayingAfterAudioFocusGained, R.string.pref_resumePlayingAfterAudioFocusGained2,
            prefs.getBoolean("pref_resumePlayingAfterAudioFocusGained", true)
        ) { newValue ->
            prefs.edit().putBoolean("pref_resumePlayingAfterAudioFocusGained", newValue).apply()
        }
    }

    private fun setupFilesCard(view: View) {
        val defPath = getString(R.string.pref_playlistDefaultPath_txt2)
        view.findViewById<View>(R.id.itemPlaylistDefaultPath).bindAction(
            R.drawable.ic_folder4, R.string.pref_playlistDefaultPath_txt1,
            prefs.getString("pref_playlistDefaultPath", defPath) ?: defPath
        ) {
            // Not editable currently in standard way, kept as simple action for now
        }
    }

    private fun setupSystemCard(view: View) {
        view.findViewById<View>(R.id.itemHoldExit).bindList(
            R.drawable.ic_close, R.string.pref_hold_exit,
            R.array.pref_hold_exit_entries, R.array.pref_holdexit_values,
            prefs.getString("pref_holdexit", getString(R.string.pref_holdexit_default)) ?: "0"
        ) { newValue ->
            prefs.edit().putString("pref_holdexit", newValue).apply()
        }

        view.findViewById<View>(R.id.itemResetTips).bindAction(
            R.drawable.ic_refresh, R.string.pref_resetTips_txt1, resources.getString(R.string.pref_resetTips_txt2)
        ) {
            appPrefs.resetTips()
        }

        view.findViewById<View>(R.id.itemResetToDefault).bindAction(
            R.drawable.ic_refresh, R.string.pref_resetToDefault_title, resources.getString(R.string.pref_reset_dialog_message)
        ) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_reset_dialog_title)
                .setMessage(R.string.pref_reset_dialog_message)
                .setPositiveButton(R.string.dialog_ok) { _, _ ->
                    prefs.edit().clear().commit()
                    requireActivity().recreate()
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }

        view.findViewById<View>(R.id.itemOpenSourceLicenses).bindAction(
            R.drawable.ic_info2, R.string.pref_openSourceLicenses, ""
        ) {
            LicensesFragment.displayLicensesFragment(parentFragmentManager, true)
        }

        view.findViewById<View>(R.id.itemBackupRestore).bindAction(
            R.drawable.ic_folder_settings, R.string.pref_backup_restore_title, resources.getString(R.string.pref_backup_restore_summary)
        ) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_backup_restore_title)
                .setMessage(R.string.backup_dialog_main_message)
                .setPositiveButton(R.string.backup_dialog_export_btn) { _, _ ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.backup_dialog_export_title)
                        .setMessage(R.string.backup_dialog_export_message)
                        .setPositiveButton(R.string.backup_dialog_save_device_btn) { _, _ ->
                            exportBackupLauncher.launch("OpenPlayerBackup.zip")
                        }
                        .setNeutralButton(R.string.backup_dialog_share_btn) { _, _ ->
                            shareBackupFile()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
                .setNeutralButton(R.string.backup_dialog_import_btn) { _, _ ->
                    importBackupLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        view.findViewById<View>(R.id.itemAbout).bindAction(
            R.drawable.ic_info2, R.string.pref_about_title, resources.getString(R.string.pref_about_summary)
        ) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.pref_about_title)
                .setMessage(R.string.pref_about_dialog_message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(R.drawable.ic_info2)
                .show()
        }

        view.findViewById<View>(R.id.itemReceiveBetaUpdates).bindSwitch(
            R.drawable.ic_info2, R.string.pref_receive_beta_updates, R.string.pref_receive_beta_updates_summary,
            requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE).getBoolean("receive_beta_updates", true)
        ) { newValue ->
            requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE).edit().putBoolean("receive_beta_updates", newValue).apply()
        }

        view.findViewById<View>(R.id.itemCheckUpdates).bindAction(
            R.drawable.ic_refresh, R.string.pref_check_updates, ""
        ) {
            val updateManager = com.aylis.core.updater.UpdateManager(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                updateManager.checkForUpdates(isSilent = false)
            }
        }
    }

    private fun View.bindSwitch(
        iconRes: Int,
        titleRes: Int,
        summaryRes: Int,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        val tvTitle = findViewById<TextView>(R.id.itemTitle)
        val tvSummary = findViewById<TextView>(R.id.itemSummary)
        val ivIcon = findViewById<ImageView>(R.id.itemIcon)
        val switchWidget = findViewById<MaterialSwitch>(R.id.itemSwitch)

        tvTitle.setText(titleRes)
        if (summaryRes != 0) {
            tvSummary.setText(summaryRes)
            tvSummary.visibility = View.VISIBLE
        } else {
            tvSummary.visibility = View.GONE
        }
        ivIcon.setImageResource(iconRes)

        switchWidget.isChecked = isChecked
        
        setOnClickListener {
            val newState = !switchWidget.isChecked
            switchWidget.isChecked = newState
            HapticManager.performTick(this)
            onCheckedChange(newState)
        }
    }

    private fun View.bindSlider(
        iconRes: Int,
        titleRes: Int,
        initialValue: Int,
        valueFrom: Float = 0f,
        valueTo: Float = 5f,
        stepSize: Float = 1f,
        onValueChanged: (Int) -> Unit
    ) {
        val tvTitle = findViewById<TextView>(R.id.itemTitle)
        val ivIcon = findViewById<ImageView>(R.id.itemIcon)
        val slider = findViewById<Slider>(R.id.itemSlider)

        tvTitle.setText(titleRes)
        ivIcon.setImageResource(iconRes)

        slider.valueFrom = valueFrom
        slider.valueTo = valueTo
        slider.stepSize = stepSize
        slider.value = initialValue.toFloat()

        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val newValue = value.toInt()
                onValueChanged(newValue)
                HapticManager.performTick(this)
            }
        }
    }

    private fun View.bindList(
        iconRes: Int,
        titleRes: Int,
        entriesResId: Int,
        valuesResId: Int,
        initialValue: String,
        onSelected: (String) -> Unit
    ) {
        val tvTitle = findViewById<TextView>(R.id.itemTitle)
        val tvValue = findViewById<TextView>(R.id.itemValue)
        val ivIcon = findViewById<ImageView>(R.id.itemIcon)

        tvTitle.setText(titleRes)
        ivIcon.setImageResource(iconRes)

        val entries = resources.getStringArray(entriesResId)
        val values = resources.getStringArray(valuesResId)

        // Храним текущее активное значение внутри вьюшки
        var currentValue = initialValue

        fun updateDisplayedText() {
            val index = values.indexOf(currentValue).takeIf { it >= 0 } ?: 0
            tvValue.text = entries.getOrNull(index) ?: ""
        }

        updateDisplayedText()

        setOnClickListener {
            val selectedIndex = values.indexOf(currentValue).takeIf { it >= 0 } ?: 0

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setSingleChoiceItems(entries, selectedIndex) { dialog, which ->
                    val newValue = values[which]
                    currentValue = newValue
                    updateDisplayedText()
                    onSelected(newValue)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun View.bindAction(
        iconRes: Int,
        titleRes: Int,
        valueText: String,
        onClick: () -> Unit
    ) {
        val tvTitle = findViewById<TextView>(R.id.itemTitle)
        val tvValue = findViewById<TextView>(R.id.itemValue)
        val ivIcon = findViewById<ImageView>(R.id.itemIcon)

        tvTitle.setText(titleRes)
        ivIcon.setImageResource(iconRes)
        
        if (valueText.isNotEmpty()) {
            tvValue.text = valueText
            tvValue.visibility = View.VISIBLE
        } else {
            tvValue.visibility = View.GONE
        }

        setOnClickListener { onClick() }
    }

    companion object {
        fun newInstance() = FragmentSettings()
    }
}
