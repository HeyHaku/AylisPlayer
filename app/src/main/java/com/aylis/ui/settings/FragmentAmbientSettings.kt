package com.aylis.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.aylis.R
import com.aylis.comp.visual.ambient.AmbientPointEditorView
import com.aylis.comp.visual.ambient.AmbientSettingsHelper
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class FragmentAmbientSettings : Fragment() {

    private val profiles = listOf("dialog", "plB", "plM", "main")
    private val profileNameResIds = listOf(
        R.string.ambient_profile_dialogs,
        R.string.ambient_profile_player_big,
        R.string.ambient_profile_player_mini,
        R.string.ambient_profile_main
    )
    
    private var currentHelper: AmbientSettingsHelper? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ambient_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchGlobalEnabled = view.findViewById<MaterialSwitch>(R.id.switchGlobalEnabled)
        val spinnerProfile = view.findViewById<Spinner>(R.id.spinnerProfile)
        val switchEnabled = view.findViewById<MaterialSwitch>(R.id.switchEnabled)
        val sliderBrightness = view.findViewById<Slider>(R.id.sliderBrightness)
        val sliderDuration = view.findViewById<Slider>(R.id.sliderDuration)
        val pointEditorView = view.findViewById<AmbientPointEditorView>(R.id.pointEditorView)
        val btnAddPoint = view.findViewById<Button>(R.id.btnAddPoint)
        val btnRemovePoint = view.findViewById<Button>(R.id.btnRemovePoint)
        val btnResetPoints = view.findViewById<Button>(R.id.btnResetPoints)
        val btnBack = view.findViewById<View>(R.id.btnBack)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val profileNames = profileNameResIds.map { getString(it) }
        spinnerProfile.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, profileNames)

        fun loadProfileSettings(profileId: String) {
            currentHelper = AmbientSettingsHelper(requireContext(), profileId)
            val helper = currentHelper!!

            switchEnabled.isChecked = helper.isEnabled
            sliderBrightness.value = helper.brightness
            sliderDuration.value = helper.animationDuration.toFloat()
            pointEditorView.brightness = helper.brightness
            pointEditorView.points = helper.getPoints().toMutableList()
            pointEditorView.invalidate()
        }

        spinnerProfile.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadProfileSettings(profiles[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val globalPrefs = requireContext().getSharedPreferences("AmbientSettings", android.content.Context.MODE_PRIVATE)
        switchGlobalEnabled.isChecked = globalPrefs.getBoolean("ambient_global_enabled", false)
        
        switchGlobalEnabled.setOnCheckedChangeListener { _, isChecked ->
            globalPrefs.edit().putBoolean("ambient_global_enabled", isChecked).apply()
        }

        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            currentHelper?.isEnabled = isChecked
        }

        sliderBrightness.addOnChangeListener { _, value, _ ->
            currentHelper?.brightness = value
            pointEditorView.brightness = value
        }

        sliderDuration.addOnChangeListener { _, value, _ ->
            currentHelper?.animationDuration = value.toLong()
        }

        pointEditorView.onPointsChanged = { points ->
            currentHelper?.savePoints(points)
        }

        btnAddPoint.setOnClickListener {
            // Adds a default point in the center if less than 10
            if (pointEditorView.points.size < 10) {
                pointEditorView.points.add(com.aylis.comp.visual.ambient.AmbientPoint(0.5f, 0.5f))
                pointEditorView.invalidate()
                currentHelper?.savePoints(pointEditorView.points)
            }
        }

        btnRemovePoint.setOnClickListener {
            pointEditorView.removeLastPoint()
            currentHelper?.savePoints(pointEditorView.points)
        }

        btnResetPoints.setOnClickListener {
            currentHelper?.resetToDefault()
            pointEditorView.points = currentHelper?.getPoints()?.toMutableList() ?: mutableListOf()
            pointEditorView.invalidate()
        }

        // Load default on start
        loadProfileSettings(profiles[0])
    }
}
