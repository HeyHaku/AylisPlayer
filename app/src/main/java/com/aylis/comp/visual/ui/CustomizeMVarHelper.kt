package com.aylis.comp.visual.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.aylis.R
import com.aylis.comp.visual.core.Elements.Base.MVariableFloat
import com.aylis.comp.visual.core.Elements.Base.MeasureDefs
import com.aylis.comp.visual.core.Elements.Base.MeasuredVar
import com.aylis.comp.visual.core.Elements.Element
import java.util.Locale

object CustomizeMVarHelper {
    @JvmStatic
    fun createPropertyViewMVarFloat(
        dialog: CustomizeVisDialog,
        customData: Element.CustomizationData,
        contentView: ViewGroup,
        displayName: String,
        name: String,
        min: Float,
        max: Float,
        step: Float
    ) {
        val defaultMVar = MVariableFloat.Companion.createConstantFloat(min)
        val currentMVar = customData.getPropertyMVariableFloat(name, defaultMVar)

        val itemView = LayoutInflater.from(dialog.activity).inflate(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_seekbar), contentView, false)
        val txtTitle = itemView.findViewById<TextView>(R.id.title)
        txtTitle.text = displayName

        val txtValue = itemView.findViewById<TextView>(R.id.txtValue)
        val valStr = StringBuilder()
        for(i in currentMVar.measures.indices) {
            currentMVar.measures[i].toDisplayString1d(valStr, true)
            if(i < currentMVar.measures.size - 1) valStr.append(" + ")
        }
        txtValue.text = if (valStr.isEmpty()) "0.0" else valStr.toString()

        val progressPreview = itemView.findViewById<android.widget.ProgressBar>(R.id.progressPreview)
        if (currentMVar.measures.isNotEmpty()) {
            val previewVal = currentMVar.measures[0].measureArg.x
            progressPreview.max = 1000
            progressPreview.progress = (((previewVal - min) / (max - min)) * 1000).toInt()
        }

        itemView.setOnClickListener {
            val editorView = LayoutInflater.from(dialog.activity).inflate(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_editor_measured_var), null, false)
            val btnAdd = editorView.findViewById<Button>(R.id.btnAdd)
            val btnRemove = editorView.findViewById<Button>(R.id.btnRemove)
            val varContainer = editorView.findViewById<ViewGroup>(R.id.varContainer)

            editorView.findViewById<View>(R.id.title)?.visibility = View.GONE
            editorView.findViewById<View>(R.id.elementDetailContent)?.visibility = View.VISIBLE

            fun updateUI() {
                varContainer.removeAllViews()
                val liveMVar = customData.getPropertyMVariableFloat(name, defaultMVar)

                for (i in liveMVar.measures.indices) {
                    val measureVar = liveMVar.measures[i]
                    val elemView = LayoutInflater.from(dialog.activity).inflate(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_item_measured_var), varContainer, false)

                    // M3 AutoCompleteTextView вместо Spinner
                    val spinner = elemView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerType)
                    val seekBarX = elemView.findViewById<SeekBar>(R.id.seekBar)
                    val seekBarY = elemView.findViewById<SeekBar>(R.id.seekBar2)
                    val editTxtX = elemView.findViewById<android.widget.EditText>(R.id.editTxt)
                    val editTxtY = elemView.findViewById<android.widget.EditText>(R.id.editTxt2)

                    editTxtX?.setText(String.format(Locale.US, "%.3f", measureVar.measureArg.x))
                    editTxtY?.setText(String.format(Locale.US, "%.3f", measureVar.measureArg.y))

                    var isUpdatingText = false

                    editTxtX?.addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: android.text.Editable?) {
                            if (isUpdatingText || dialog.isUpdatingProgrammatically) return
                            try {
                                val newVal = s.toString().toFloat()
                                val latestMVar = customData.getPropertyMVariableFloat(name, defaultMVar)
                                val newVar = MeasuredVar(measureVar.measure, newVal, latestMVar.measures[i].measureArg.y)
                                val newList = latestMVar.measures.toMutableList()
                                newList[i] = newVar
                                customData.putPropertyMVariableFloat(name, MVariableFloat(newList), "", min, max)
                                dialog.onPropertyChanged()

                                isUpdatingText = true
                                val progress = (((newVal - min) / (max - min)) * 1000).toInt()
                                seekBarX.progress = Math.max(0, Math.min(1000, progress))
                                isUpdatingText = false

                                val sb = StringBuilder()
                                for(idx in newList.indices) {
                                    newList[idx].toDisplayString1d(sb, true)
                                    if(idx < newList.size - 1) sb.append(" + ")
                                }
                                dialog.setEditorValueText(sb.toString())
                                txtValue.text = sb.toString()
                                if (i == 0) progressPreview.progress = Math.max(0, Math.min(1000, progress))
                            } catch (e: Exception) {}
                        }
                    })

                    // Настройка M3 Дропдауна
                    val adapter = ArrayAdapter(dialog.activity!!, android.R.layout.simple_dropdown_item_1line, MeasureDefs.measures1dMVar)
                    spinner.setAdapter(adapter)

                    val pos = MeasureDefs.measures1dMVar.indexOf(measureVar.measure)
                    if (pos >= 0) {
                        spinner.setText(adapter.getItem(pos).toString(), false)
                    }

                    spinner.setOnItemClickListener { _, _, position, _ ->
                        val newMeasure = MeasureDefs.measures1dMVar[position]
                        if (newMeasure != measureVar.measure) {
                            val latestMVar = customData.getPropertyMVariableFloat(name, defaultMVar)
                            val newVar = MeasuredVar(newMeasure, measureVar.measureArg.x, measureVar.measureArg.y)
                            val newList = latestMVar.measures.toMutableList()
                            newList[i] = newVar
                            customData.putPropertyMVariableFloat(name, MVariableFloat(newList), "", min, max)
                            dialog.onPropertyChanged()

                            val sb = StringBuilder()
                            newList[i].toDisplayString1d(sb, true)
                            dialog.setEditorValueText(sb.toString())
                            txtValue.text = sb.toString()
                            updateUI()
                        }
                    }

                    seekBarX.max = 1000
                    seekBarX.progress = (((measureVar.measureArg.x - min) / (max - min)) * 1000).toInt()
                    seekBarX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                val newVal = min + (progress / 1000f) * (max - min)
                                val latestMVar = customData.getPropertyMVariableFloat(name, defaultMVar)
                                val newVar = MeasuredVar(measureVar.measure, newVal, latestMVar.measures[i].measureArg.y)
                                val newList = latestMVar.measures.toMutableList()
                                newList[i] = newVar
                                customData.putPropertyMVariableFloat(name, MVariableFloat(newList), "", min, max)
                                dialog.onPropertyChanged()

                                isUpdatingText = true
                                editTxtX?.setText(String.format(Locale.US, "%.3f", newVal))
                                isUpdatingText = false

                                val sb = StringBuilder()
                                for(idx in newList.indices) {
                                    newList[idx].toDisplayString1d(sb, true)
                                    if(idx < newList.size - 1) sb.append(" + ")
                                }
                                dialog.setEditorValueText(sb.toString())
                                txtValue.text = sb.toString()
                                if (i == 0) {
                                    progressPreview.progress = progress
                                }
                            }
                        }
                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })

                    val isBUsed = MeasureDefs.getHintArgBisUsedFor1d(measureVar.measure)
                    if (isBUsed) {
                        seekBarY.visibility = View.VISIBLE
                        seekBarY.max = 1000
                        seekBarY.progress = (((measureVar.measureArg.y - min) / (max - min)) * 1000).toInt()

                        editTxtY?.addTextChangedListener(object : android.text.TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: android.text.Editable?) {
                                if (isUpdatingText || dialog.isUpdatingProgrammatically) return
                                try {
                                    val newVal = s.toString().toFloat()
                                    val latestMVar = customData.getPropertyMVariableFloat(name, defaultMVar)
                                    val newVar = MeasuredVar(measureVar.measure, latestMVar.measures[i].measureArg.x, newVal)
                                    val newList = latestMVar.measures.toMutableList()
                                    newList[i] = newVar
                                    customData.putPropertyMVariableFloat(name, MVariableFloat(newList), "", min, max)
                                    dialog.onPropertyChanged()

                                    isUpdatingText = true
                                    val progress = (((newVal - min) / (max - min)) * 1000).toInt()
                                    seekBarY.progress = Math.max(0, Math.min(1000, progress))
                                    isUpdatingText = false

                                    val sb = StringBuilder()
                                    for(idx in newList.indices) {
                                        newList[idx].toDisplayString1d(sb, true)
                                        if(idx < newList.size - 1) sb.append(" + ")
                                    }
                                    dialog.setEditorValueText(sb.toString())
                                    txtValue.text = sb.toString()
                                } catch (e: Exception) {}
                            }
                        })

                        seekBarY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                if (fromUser) {
                                    val newVal = min + (progress / 1000f) * (max - min)
                                    val latestMVar = customData.getPropertyMVariableFloat(name, defaultMVar)
                                    val newVar = MeasuredVar(measureVar.measure, latestMVar.measures[i].measureArg.x, newVal)
                                    val newList = latestMVar.measures.toMutableList()
                                    newList[i] = newVar
                                    customData.putPropertyMVariableFloat(name, MVariableFloat(newList), "", min, max)
                                    dialog.onPropertyChanged()

                                    isUpdatingText = true
                                    editTxtY?.setText(String.format(Locale.US, "%.3f", newVal))
                                    isUpdatingText = false

                                    val sb = StringBuilder()
                                    for(idx in newList.indices) {
                                        newList[idx].toDisplayString1d(sb, true)
                                        if(idx < newList.size - 1) sb.append(" + ")
                                    }
                                    dialog.setEditorValueText(sb.toString())
                                    txtValue.text = sb.toString()
                                }
                            }
                            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                        })
                    } else {
                        seekBarY.visibility = View.GONE
                        editTxtY?.visibility = View.GONE
                    }

                    varContainer.addView(elemView)
                }
            }

            btnAdd.setOnClickListener {
                val latestMVar = customData.getPropertyMVariableFloat(name, defaultMVar)
                val newList = latestMVar.measures.toMutableList()
                newList.add(MeasuredVar(MeasureDefs.Constant, min, min))
                customData.putPropertyMVariableFloat(name, MVariableFloat(newList), "", min, max)
                dialog.onPropertyChanged()
                updateUI()
            }

            btnRemove.setOnClickListener {
                val latestMVar = customData.getPropertyMVariableFloat(name, defaultMVar)
                if (latestMVar.measures.isNotEmpty()) {
                    val newList = latestMVar.measures.toMutableList()
                    newList.removeAt(newList.size - 1)
                    customData.putPropertyMVariableFloat(name, MVariableFloat(newList), "", min, max)
                    dialog.onPropertyChanged()
                    updateUI()
                }
            }

            updateUI()
            dialog.showBottomEditor(displayName, editorView)

            val liveMVar2 = customData.getPropertyMVariableFloat(name, defaultMVar)
            val valStr2 = StringBuilder()
            for(i in liveMVar2.measures.indices) {
                liveMVar2.measures[i].toDisplayString1d(valStr2, true)
                if(i < liveMVar2.measures.size - 1) valStr2.append(" + ")
            }
            dialog.setEditorValueText(valStr2.toString())
        }

        contentView.addView(itemView)
    }

    @JvmStatic
    fun createPropertyViewMeasuredVar(
        dialog: CustomizeVisDialog,
        customData: Element.CustomizationData,
        contentView: ViewGroup,
        displayName: String,
        name: String,
        min: Float,
        max: Float,
        step: Float
    ) {
        val defaultMVar = MeasuredVar(MeasureDefs.Constant, min, min)
        val currentMVar = customData.getPropertyMeasuredVar(name, defaultMVar) ?: defaultMVar

        val itemView = LayoutInflater.from(dialog.activity).inflate(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_seekbar), contentView, false)
        val txtTitle = itemView.findViewById<TextView>(R.id.title)
        txtTitle.text = displayName

        val txtValue = itemView.findViewById<TextView>(R.id.txtValue)
        val valStr = StringBuilder()
        currentMVar.toDisplayString1d(valStr, true)
        txtValue.text = valStr.toString()

        val progressPreview = itemView.findViewById<android.widget.ProgressBar>(R.id.progressPreview)
        progressPreview.max = 1000
        progressPreview.progress = (((currentMVar.measureArg.x - min) / (max - min)) * 1000).toInt()

        itemView.setOnClickListener {
            val editorView = LayoutInflater.from(dialog.activity).inflate(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_editor_measured_var), null, false)
            editorView.findViewById<View>(R.id.title)?.visibility = View.GONE
            editorView.findViewById<View>(R.id.elementDetailContent)?.visibility = View.VISIBLE

            val btnAdd = editorView.findViewById<Button>(R.id.btnAdd)
            val btnRemove = editorView.findViewById<Button>(R.id.btnRemove)
            val varContainer = editorView.findViewById<ViewGroup>(R.id.varContainer)

            btnAdd.visibility = View.GONE
            btnRemove.visibility = View.GONE

            fun updateUI() {
                varContainer.removeAllViews()
                val liveMVar = customData.getPropertyMeasuredVar(name, defaultMVar) ?: defaultMVar

                val elemView = LayoutInflater.from(dialog.activity).inflate(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_item_measured_var), varContainer, false)

                // M3 AutoCompleteTextView вместо Spinner
                val spinner = elemView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerType)
                val seekBarX = elemView.findViewById<SeekBar>(R.id.seekBar)
                val seekBarY = elemView.findViewById<SeekBar>(R.id.seekBar2)
                val editTxtX = elemView.findViewById<TextView>(R.id.editTxt)
                val editTxtY = elemView.findViewById<TextView>(R.id.editTxt2)

                editTxtX?.text = String.format(Locale.US, "%.3f", liveMVar.measureArg.x)
                editTxtY?.text = String.format(Locale.US, "%.3f", liveMVar.measureArg.y)

                // Настройка M3 Дропдауна
                val adapter = ArrayAdapter(dialog.activity!!, android.R.layout.simple_dropdown_item_1line, MeasureDefs.measures1d)
                spinner.setAdapter(adapter)

                val pos = MeasureDefs.measures1d.indexOf(liveMVar.measure)
                if (pos >= 0) {
                    spinner.setText(adapter.getItem(pos).toString(), false)
                }

                spinner.setOnItemClickListener { _, _, position, _ ->
                    val newMeasure = MeasureDefs.measures1d[position]
                    if (newMeasure != liveMVar.measure) {
                        val newVar = MeasuredVar(newMeasure, liveMVar.measureArg.x, liveMVar.measureArg.y)
                        customData.putPropertyMeasuredVar(name, newVar, "", min, max)
                        dialog.onPropertyChanged()

                        val sb = StringBuilder()
                        newVar.toDisplayString1d(sb, true)
                        dialog.setEditorValueText(sb.toString())
                        txtValue.text = sb.toString()
                        updateUI()
                    }
                }

                seekBarX.max = 1000
                seekBarX.progress = (((liveMVar.measureArg.x - min) / (max - min)) * 1000).toInt()
                seekBarX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            val newVal = min + (progress / 1000f) * (max - min)
                            val latestVar = customData.getPropertyMeasuredVar(name, defaultMVar) ?: defaultMVar
                            val newVar = MeasuredVar(liveMVar.measure, newVal, latestVar.measureArg.y)
                            customData.putPropertyMeasuredVar(name, newVar, "", min, max)
                            dialog.onPropertyChanged()

                            editTxtX?.text = String.format(Locale.US, "%.3f", newVal)

                            val sb = StringBuilder()
                            newVar.toDisplayString1d(sb, true)
                            dialog.setEditorValueText(sb.toString())
                            txtValue.text = sb.toString()
                            progressPreview.progress = progress
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })

                seekBarY.visibility = View.VISIBLE
                seekBarY.max = 1000
                seekBarY.progress = (((liveMVar.measureArg.y - min) / (max - min)) * 1000).toInt()
                seekBarY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            val newVal = min + (progress / 1000f) * (max - min)
                            val latestVar = customData.getPropertyMeasuredVar(name, defaultMVar) ?: defaultMVar
                            val newVar = MeasuredVar(liveMVar.measure, latestVar.measureArg.x, newVal)
                            customData.putPropertyMeasuredVar(name, newVar, "", min, max)
                            dialog.onPropertyChanged()

                            editTxtY?.text = String.format(Locale.US, "%.3f", newVal)

                            val sb = StringBuilder()
                            newVar.toDisplayString1d(sb, true)
                            dialog.setEditorValueText(sb.toString())
                            txtValue.text = sb.toString()
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })

                varContainer.addView(elemView)
            }

            updateUI()
            dialog.showBottomEditor(displayName, editorView)

            val liveMVar2 = customData.getPropertyMeasuredVar(name, defaultMVar) ?: defaultMVar
            val valStr2 = StringBuilder()
            liveMVar2.toDisplayString1d(valStr2, true)
            dialog.setEditorValueText(valStr2.toString())
        }

        contentView.addView(itemView)
    }
}