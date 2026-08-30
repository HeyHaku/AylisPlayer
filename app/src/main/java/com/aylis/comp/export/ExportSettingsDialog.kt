package com.aylis.comp.export

import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.activity.result.contract.ActivityResultContracts
import com.aylis.R
import com.aylis.comp.AppPreferences.AppPreferences
import androidx.documentfile.provider.DocumentFile
import android.widget.EditText
import android.widget.TextView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

class ExportSettingsDialog : DialogFragment() {

    private lateinit var audioUriStr: String
    private lateinit var themeJson: String
    private lateinit var trackName: String
    private var trackDurationMs: Long = 0L
    private var selectedFolderUri: String? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var isDraggingSeekBar = false
    private var posOverride: Long = -1
    private var lastSeekEventTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_Dialog_Expressive)
        audioUriStr = arguments?.getString("audio_uri") ?: ""
        themeJson = arguments?.getString("theme_json") ?: ""
        trackName = arguments?.getString("track_name") ?: ""
        trackDurationMs = arguments?.getLong("track_duration", 0L) ?: 0L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_export_settings, container, false)
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // Подключаем AutoCompleteTextView вместо старых Spinner
        val spinnerQuality = view.findViewById<AutoCompleteTextView>(R.id.spinnerQuality)
        val spinnerFps = view.findViewById<AutoCompleteTextView>(R.id.spinnerFps)
        val spinnerBitrate = view.findViewById<AutoCompleteTextView>(R.id.spinnerBitrate)

        val btnStartTime = view.findViewById<Button>(R.id.btnStartTime)
        val btnEndTime = view.findViewById<Button>(R.id.btnEndTime)
        
        val tvCurrentTime = view.findViewById<TextView>(R.id.tvCurrentTime)
        val seekBarTime = view.findViewById<SeekBar>(R.id.seekBarTime)
        val tvTotalTime = view.findViewById<TextView>(R.id.tvTotalTime)
        
        val btnCancelExport = view.findViewById<ImageButton>(R.id.btnCancelExport)
        val btnExport = view.findViewById<Button>(R.id.btnExport)
        
        val etVideoName = view.findViewById<EditText>(R.id.etVideoName)
        val tvVideoInfo = view.findViewById<TextView>(R.id.tvVideoInfo)
        val layoutSelectFolder = view.findViewById<LinearLayout>(R.id.layoutSelectFolder)
        val tvFolderPath = view.findViewById<TextView>(R.id.tvFolderPath)

        val themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId)
        val initialName = if (trackName.isNotEmpty()) trackName else getString(R.string.export_template_name_format, themeId)
        etVideoName.setText(initialName)

        // Folder selection
        val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                selectedFolderUri = uri.toString()
                activity?.contentResolver?.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                val df = DocumentFile.fromTreeUri(requireContext(), uri)
                tvFolderPath.text = df?.name ?: uri.path ?: getString(R.string.export_selected_folder)
            }
        }
        
        layoutSelectFolder.setOnClickListener {
            folderPickerLauncher.launch(null)
        }

        // State for start/end times in seconds
        var startSec = 0
        var endSec = if (trackDurationMs > 0) (trackDurationMs / 1000).toInt() else 62

        fun formatTime(totalSecs: Int): String {
            val m = totalSecs / 60
            val s = totalSecs % 60
            return "%d:%02d".format(m, s)
        }
        
        // Setup SeekBar for track preview
        tvTotalTime.text = formatTime(if (trackDurationMs > 0) (trackDurationMs / 1000).toInt() else 0)
        
        seekBarTime.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                if (trackDurationMs > 0) {
                    posOverride = trackDurationMs * progress / 1000
                    tvCurrentTime.text = formatTime((posOverride / 1000).toInt())
                    
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastSeekEventTime > 250) {
                        lastSeekEventTime = now
                        com.aylis.comp.MediaControlsUI.MediaControlsUI.onSetTrackPosition.invoke(posOverride)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isDraggingSeekBar = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isDraggingSeekBar = false
                if (posOverride >= 0) {
                    com.aylis.comp.MediaControlsUI.MediaControlsUI.onSetTrackPosition.invoke(posOverride)
                    posOverride = -1
                }
            }
        })
        
        startPositionPoller(tvCurrentTime, seekBarTime)

        val bitrates = arrayOf(
            getString(R.string.export_bitrate_low),
            getString(R.string.export_bitrate_medium),
            getString(R.string.export_bitrate_high)
        )
        val bitrateValues = intArrayOf(5000000, 10000000, 20000000)

        fun updateVideoInfo() {
            val diff = (endSec - startSec).coerceAtLeast(0)
            val bitratePos = bitrates.indexOf(spinnerBitrate.text.toString()).coerceAtLeast(0)
            val bitrate = bitrateValues[bitratePos]
            val sizeMb = (diff * bitrate.toLong()) / (8 * 1024 * 1024)
            tvVideoInfo.text = getString(R.string.export_video_info_format, sizeMb, formatTime(diff))
        }

        btnStartTime.text = getString(R.string.export_btn_start_time_format, formatTime(startSec))
        btnEndTime.text = getString(R.string.export_btn_end_time_format, formatTime(endSec))

        btnStartTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, minutes, seconds ->
                    startSec = minutes * 60 + seconds
                    btnStartTime.text = getString(R.string.export_btn_start_time_format, formatTime(startSec))
                    updateVideoInfo()
                },
                startSec / 60,
                startSec % 60,
                true
            ).show()
        }

        btnEndTime.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                { _, minutes, seconds ->
                    endSec = minutes * 60 + seconds
                    btnEndTime.text = getString(R.string.export_btn_end_time_format, formatTime(endSec))
                    updateVideoInfo()
                },
                endSec / 60,
                endSec % 60,
                true
            ).show()
        }

        // Setup Dropdowns (MD3 Style)
        val qualities = arrayOf("720p", "1080p")
        val adapterQuality = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, qualities)
        spinnerQuality.setAdapter(adapterQuality)
        spinnerQuality.setText(qualities[1], false) // Default 1080p

        val fpsOptions = arrayOf("30", "60")
        val adapterFps = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, fpsOptions)
        spinnerFps.setAdapter(adapterFps)
        spinnerFps.setText(fpsOptions[1], false) // Default 60

        val adapterBitrate = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, bitrates)
        spinnerBitrate.setAdapter(adapterBitrate)
        spinnerBitrate.setText(bitrates[1], false) // Default 10 Mbps
        spinnerBitrate.setOnItemClickListener { _, _, _, _ -> updateVideoInfo() }

        updateVideoInfo()

        btnCancelExport.setOnClickListener { dismiss() }

        btnExport.setOnClickListener {
            val qualityPos = qualities.indexOf(spinnerQuality.text.toString()).coerceAtLeast(0)
            val fpsPos = fpsOptions.indexOf(spinnerFps.text.toString()).coerceAtLeast(0)
            val bitratePos = bitrates.indexOf(spinnerBitrate.text.toString()).coerceAtLeast(0)

            val qualityStr = qualities[qualityPos]
            val fps = fpsOptions[fpsPos].toInt()
            val bitrate = bitrateValues[bitratePos]

            val ratioId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerAspectRatio)
            var width = 1080
            var height = 1080

            val baseSize = if (qualityStr == "720p") 720 else 1080

            when (ratioId) {
                1 -> { width = baseSize; height = (baseSize * 19) / 9 }
                2 -> { width = baseSize; height = (baseSize * 16) / 9 }
                3 -> { height = baseSize; width = (baseSize * 16) / 9 }
                4 -> { height = baseSize; width = (baseSize * 4) / 3 }
                5 -> { width = baseSize; height = (baseSize * 4) / 3 }
                6 -> { width = baseSize; height = baseSize }
                else -> { height = baseSize; width = (baseSize * 16) / 9 }
            }

            if (width % 2 != 0) width++
            if (height % 2 != 0) height++

            val intent = Intent(activity, ExportVideoActivity::class.java)
            intent.putExtra("audio_uri", audioUriStr)
            intent.putExtra("theme_json", themeJson)
            intent.putExtra("track_name", trackName)
            intent.putExtra("width", width)
            intent.putExtra("height", height)
            intent.putExtra("fps", fps)
            intent.putExtra("bitrate", bitrate)
            intent.putExtra("startSec", startSec)
            intent.putExtra("endSec", endSec)
            
            val customName = etVideoName.text.toString().trim()
            if (customName.isNotEmpty()) {
                intent.putExtra("custom_filename", customName)
            }
            if (selectedFolderUri != null) {
                intent.putExtra("output_folder_uri", selectedFolderUri)
            }
            
            startActivity(intent)

            dismiss()
        }

        return view
    }

    private fun startPositionPoller(tvCurrentTime: TextView, seekBarTime: SeekBar) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (activity == null || !isAdded) return
                
                val trackPosition = com.aylis.comp.MediaControlsUI.MediaControlsUI.onRequestTrackPosition.invoke(-1L)
                if (trackPosition >= 0 && trackDurationMs > 0 && !isDraggingSeekBar) {
                    val progress = (1000 * trackPosition / trackDurationMs).toInt()
                    seekBarTime.progress = progress
                    val currentSecs = (trackPosition / 1000).toInt()
                    tvCurrentTime.text = "%d:%02d".format(currentSecs / 60, currentSecs % 60)
                }
                
                handler.postDelayed(this, 500)
            }
        }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val params = window.attributes
            params.width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = params
        }
    }

    companion object {
        fun newInstance(audioUriStr: String, themeJson: String, trackName: String = "", durationMs: Long = 0L): ExportSettingsDialog {
            val dialog = ExportSettingsDialog()
            val args = Bundle()
            args.putString("audio_uri", audioUriStr)
            args.putString("theme_json", themeJson)
            args.putString("track_name", trackName)
            args.putLong("track_duration", durationMs)
            dialog.arguments = args
            return dialog
        }
    }
}