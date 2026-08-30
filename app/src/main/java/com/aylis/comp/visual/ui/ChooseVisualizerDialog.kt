package com.aylis.comp.visual.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aylis.Common.Events.WeakEvent
import com.aylis.Common.Events.WeakEvent1
import com.aylis.Common.Events.WeakEventR1
import com.aylis.R
import com.aylis.comp.AppPreferences.AppPreferences
import com.aylis.comp.visual.core.Elements.Element
import com.aylis.comp.visual.design.VisualizerThemes
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ChooseVisualizerDialog : DialogFragment() {

    companion object {
        const val READ_REQUEST_CODE = 42
        const val WRITE_REQUEST_CODE = 43

        val onRequestSkinThemePresetList = WeakEventR1<MutableList<VisualizerThemeInfo>, Int>()
        val onSkinThemePresetSelected = WeakEvent1<VisualizerThemeInfo>()
        val onShowVideoContentAction = WeakEvent()

        fun createAndShowDialog(fragmentManager: FragmentManager): ChooseVisualizerDialog {
            val dialog = ChooseVisualizerDialog()
            dialog.show(fragmentManager, "ChooseVisualizerDialog")
            return dialog
        }
    }

    private lateinit var recyclerViewVisualizers: RecyclerView
    private lateinit var tabLayout: TabLayout

    private lateinit var layoutGetStartedOverlay: View
    private lateinit var btnDismissGetStarted: Button

    private lateinit var tvSelectedTemplateName: TextView
    private lateinit var etSaveAs: android.widget.EditText

    private val allThemes = mutableListOf<VisualizerThemeInfo>()
    private val templateThemes = mutableListOf<VisualizerThemeInfo>()
    private val myTemplateThemes = mutableListOf<VisualizerThemeInfo>()
    private var currentSelectedThemeId = -1
    private var isMyTemplatesTab = false
    private lateinit var adapter: VisualizerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.Theme_App_Dialog_Animated)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val metrics = android.util.DisplayMetrics()
            requireActivity().windowManager.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels
            var dialogWidth = (screenWidth * 0.90).toInt()
            val maxDialogWidth = (480 * metrics.density).toInt()
            if (dialogWidth > maxDialogWidth) {
                dialogWidth = maxDialogWidth
            }
            window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.vis_choose_visualizer_dialog, container, false)

        recyclerViewVisualizers = view.findViewById(R.id.recyclerViewVisualizers)
        tabLayout = view.findViewById(R.id.tabLayout)
        layoutGetStartedOverlay = view.findViewById(R.id.layoutGetStartedOverlay)
        btnDismissGetStarted = view.findViewById(R.id.btnDismissGetStarted)

        tvSelectedTemplateName = view.findViewById(R.id.tvSelectedTemplateName)
        etSaveAs = view.findViewById(R.id.etSaveAs)

        view.findViewById<ImageButton>(R.id.btnImport).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            startActivityForResult(intent, READ_REQUEST_CODE)
        }

        view.findViewById<ImageButton>(R.id.btnSave).setOnClickListener {
            val themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId)
            val customName = etSaveAs.text.toString().trim()
            val fileName = if (customName.isNotEmpty()) "$customName.ayp" else "preset_$themeId.ayp"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
            startActivityForResult(intent, WRITE_REQUEST_CODE)
        }

        view.findViewById<ImageButton>(R.id.btnDuplicate).setOnClickListener {
            val customName = etSaveAs.text.toString().trim()
            copyCurrentTheme(customName)
        }

        view.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            deleteCurrentTheme()
        }

        view.findViewById<ImageButton>(R.id.btnNewProject).setOnClickListener {
            val customName = etSaveAs.text.toString().trim()
            val name = if (customName.isNotEmpty()) customName else "New Project"
            createNewProject(name)
            tabLayout.getTabAt(1)?.select()
        }

        setupTabs()
        setupRecyclerView()
        loadThemes()

        val prefs = AppPreferences.createOrGetInstance()
        val sharedPrefs = prefs.getPreferences(activity)
        val hasSeenGetStarted = sharedPrefs.getBoolean("pref_has_seen_get_started", false)
        if (!hasSeenGetStarted) {
            layoutGetStartedOverlay.visibility = View.VISIBLE
            btnDismissGetStarted.setOnClickListener {
                layoutGetStartedOverlay.visibility = View.GONE
                sharedPrefs.edit().putBoolean("pref_has_seen_get_started", true).apply()
            }
        } else {
            layoutGetStartedOverlay.visibility = View.GONE
        }

        val lastTab = sharedPrefs.getInt("pref_last_selected_tab", 0)
        if (lastTab == 1) {
            tabLayout.getTabAt(1)?.select()
        }

        dialog?.setCanceledOnTouchOutside(true)
        return view
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isMyTemplatesTab = tab?.position == 1

                val prefs = AppPreferences.createOrGetInstance()
                prefs.getPreferences(activity).edit().putInt("pref_last_selected_tab", tab?.position ?: 0).apply()

                recyclerViewVisualizers.scheduleLayoutAnimation()
                adapter.submitList(if (isMyTemplatesTab) myTemplateThemes else templateThemes)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = VisualizerAdapter { themeId ->
            currentSelectedThemeId = themeId
            adapter.notifyDataSetChanged()
            updateSelectedTemplateName()
            if (themeId == -1) {
                onShowVideoContentAction.invoke()
            } else {
                allThemes.find { it.id == themeId }?.let { themeInfo ->
                    onSkinThemePresetSelected.invoke(themeInfo)
                }
            }
        }
        recyclerViewVisualizers.layoutManager = GridLayoutManager(context, 4)
        val animation = android.view.animation.AnimationUtils.loadLayoutAnimation(context, R.anim.layout_animation_fade_in)
        recyclerViewVisualizers.layoutAnimation = animation
        recyclerViewVisualizers.adapter = adapter
    }

    private fun loadThemes() {
        allThemes.clear()
        templateThemes.clear()
        myTemplateThemes.clear()

        currentSelectedThemeId = onRequestSkinThemePresetList.invoke(allThemes, -1)

        val templateCount = if (allThemes.size >= 8) 8 else allThemes.size

        for (i in 0 until templateCount) {
            templateThemes.add(allThemes[i])
        }

        for (i in templateCount until allThemes.size) {
            myTemplateThemes.add(allThemes[i])
        }

        adapter.submitList(if (isMyTemplatesTab) myTemplateThemes else templateThemes)
        updateSelectedTemplateName()
    }

    private fun updateSelectedTemplateName() {
        val selectedTheme = allThemes.find { it.id == currentSelectedThemeId }
        if (selectedTheme != null && selectedTheme.id != -1) {
            tvSelectedTemplateName.text = "Template #${selectedTheme.id}"
        } else {
            tvSelectedTemplateName.text = "No template selected"
        }
    }

    private fun createNewProject(projectName: String) {
        val baseId = 1
        val root = VisualizerThemes.s().getThemeObject(baseId)
        val cust = Element.CustomizationList()
        if (root != null) {
            root.getCustomization(cust, 0)
        }

        val newId = AppPreferences.createOrGetInstance().addCustomTheme(activity, baseId, projectName)
        AppPreferences.createOrGetInstance().savePrefThemeCustomizationData(newId, cust)

        VisualizerThemes.s().loadCustomThemes()
        loadThemes()
        Toast.makeText(activity, "Project created!", Toast.LENGTH_SHORT).show()
    }

    private fun copyCurrentTheme(customName: String) {
        val themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId)
        var cust = AppPreferences.createOrGetInstance().getPrefThemeCustomizationData(themeId)
        var scene = AppPreferences.createOrGetInstance().getPrefThemeScene(themeId)

        val root = VisualizerThemes.s().getThemeObject(themeId)
        if (root != null) {
            if (scene == null) {
                scene = com.aylis.comp.visual.scene.SceneBuilder.exportToScene(root)
            }
            if (cust == null) {
                cust = Element.CustomizationList()
                root.getCustomization(cust, 0)
            }
        }

        var baseId = 0
        if (themeId < 10) {
            baseId = themeId
        } else {
            val customs = AppPreferences.createOrGetInstance().getCustomThemes(activity)
            for (info in customs) {
                if (info.id == themeId) {
                    baseId = info.baseId
                    break
                }
            }
        }

        val name = if (customName.isNotEmpty()) customName else "Copy of $themeId"
        val newId = AppPreferences.createOrGetInstance().addCustomTheme(activity, baseId, name)
        AppPreferences.createOrGetInstance().savePrefThemeCustomizationData(newId, cust)
        if (scene != null) {
            AppPreferences.createOrGetInstance().savePrefThemeScene(newId, scene)
        }

        VisualizerThemes.s().loadCustomThemes()
        loadThemes()
        Toast.makeText(activity, "Copied!", Toast.LENGTH_SHORT).show()
    }

    private fun deleteCurrentTheme() {
        val themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId)
        if (themeId < 10) {
            Toast.makeText(activity, "Cannot delete base theme", Toast.LENGTH_SHORT).show()
            return
        }

        AppPreferences.createOrGetInstance().deleteCustomTheme(activity, themeId)
        AppPreferences.createOrGetInstance().setInt(AppPreferences.PREF_Int_visualizerThemeId, 0, true)

        VisualizerThemes.s().loadCustomThemes()
        loadThemes()
        Toast.makeText(activity, "Deleted", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data == null) return
        val uri = data.data ?: return

        if (requestCode == READ_REQUEST_CODE) {
            importTheme(uri)
        } else if (requestCode == WRITE_REQUEST_CODE) {
            exportTheme(uri)
        }
    }

    private fun copyStream(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
    }

    private fun getExtension(path: String?): String {
        if (path == null) return ".png"
        val dot = path.lastIndexOf('.')
        if (dot >= 0) {
            val ext = path.substring(dot).lowercase()
            if (ext in listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".ttf", ".otf")) {
                return ext
            }
        }
        return ".png"
    }

    private fun importTheme(uri: Uri) {
        try {
            var isZip = false
            try {
                activity?.contentResolver?.openInputStream(uri)?.use { testIs ->
                    val signature = ByteArray(4)
                    val read = testIs.read(signature)
                    if (read == 4 && signature[0] == 0x50.toByte() && signature[1] == 0x4B.toByte() && signature[2] == 0x03.toByte() && signature[3] == 0x04.toByte()) {
                        isZip = true
                    }
                }
            } catch (ignored: Exception) {}

            var newId = -1

            if (isZip) {
                // Режим 1: Импорт .ayp (ZIP-архив)
                var manifestContent: String? = null
                var sceneJson: String? = null
                var customizationJson: String? = null
                val filesMap = mutableMapOf<String, ByteArray>()

                activity?.contentResolver?.openInputStream(uri)?.use { zipIs ->
                    ZipInputStream(zipIs).use { zis ->
                        var entry: ZipEntry?
                        while (zis.nextEntry.also { entry = it } != null) {
                            val name = entry!!.name
                            val baos = ByteArrayOutputStream()
                            copyStream(zis, baos)
                            val dataBytes = baos.toByteArray()

                            when (name) {
                                "aylis_manifest.json" -> manifestContent = String(dataBytes, Charsets.UTF_8)
                                "scene.json" -> sceneJson = String(dataBytes, Charsets.UTF_8)
                                "customization.json" -> customizationJson = String(dataBytes, Charsets.UTF_8)
                                else -> {
                                    if (!name.endsWith("/")) {
                                        filesMap[name] = dataBytes
                                    }
                                }
                            }
                            zis.closeEntry()
                        }
                    }
                }

                val baseId = if (manifestContent != null) {
                    try { JSONObject(manifestContent!!).optInt("baseId", 0) } catch (e: Exception) { 0 }
                } else 0

                val presetName = if (manifestContent != null) {
                    try { JSONObject(manifestContent!!).optString("name", "Imported Preset") } catch (e: Exception) { "Imported Preset" }
                } else "Imported Preset"

                newId = AppPreferences.createOrGetInstance().addCustomTheme(activity, baseId, presetName)

                val pathMap = mutableMapOf<String, String>()
                if (filesMap.isNotEmpty()) {
                    val destDir = File(activity?.filesDir, "custom_images/theme_$newId").apply { mkdirs() }

                    for ((entryName, value) in filesMap) {
                        val cleanFileName = File(entryName).name
                        val isFont = cleanFileName.lowercase().endsWith(".ttf") || cleanFileName.lowercase().endsWith(".otf")
                        val targetDir = if (isFont) {
                            File(com.aylis.comp.visual.core.CustomFontManager.getFontsFolder()).apply { mkdirs() }
                        } else {
                            destDir
                        }

                        val destFile = File(targetDir, cleanFileName)
                        FileOutputStream(destFile).use { fos ->
                            fos.write(value)
                        }
                        pathMap[entryName] = Uri.fromFile(destFile).toString()
                        pathMap[cleanFileName] = Uri.fromFile(destFile).toString()
                    }

                    if (filesMap.keys.any { it.lowercase().endsWith(".ttf") || it.lowercase().endsWith(".otf") }) {
                        com.aylis.comp.visual.core.CustomFontManager.scanFonts()
                    }
                }

                if (sceneJson != null) {
                    val scene = com.aylis.comp.visual.scene.SceneSerializer.fromJson(sceneJson!!)
                    if (scene != null) {
                        val updatedScene = com.aylis.comp.visual.scene.SceneBuilder.updateImagePaths(scene, pathMap)
                        AppPreferences.createOrGetInstance().savePrefThemeScene(newId, updatedScene)

                        val root = com.aylis.comp.visual.scene.SceneBuilder.buildFromScene(newId, updatedScene)
                        if (root != null) {
                            val newList = Element.CustomizationList()
                            root.getCustomization(newList, 0)
                            AppPreferences.createOrGetInstance().savePrefThemeCustomizationData(newId, newList)
                        }
                    }
                } else if (customizationJson != null) {
                    AppPreferences.createOrGetInstance().savePrefThemeCustomizationData(
                        newId,
                        Element.CustomizationList.deserialize(customizationJson!!)
                    )
                }

            } else {
                // Режим 2: Прямой импорт чистого .json файла
                var rawJsonString: String? = null
                activity?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                    val baos = ByteArrayOutputStream()
                    copyStream(inputStream, baos)
                    rawJsonString = String(baos.toByteArray(), Charsets.UTF_8)
                }

                if (rawJsonString.isNullOrEmpty()) {
                    Toast.makeText(activity, "Файл пуст или повреждён", Toast.LENGTH_SHORT).show()
                    return
                }

                val scene = com.aylis.comp.visual.scene.SceneSerializer.fromJson(rawJsonString!!)
                val presetName = (uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "Imported JSON")

                newId = AppPreferences.createOrGetInstance().addCustomTheme(activity, 0, presetName)

                if (scene != null) {
                    AppPreferences.createOrGetInstance().savePrefThemeScene(newId, scene)
                    val root = com.aylis.comp.visual.scene.SceneBuilder.buildFromScene(newId, scene)
                    if (root != null) {
                        val newList = Element.CustomizationList()
                        root.getCustomization(newList, 0)
                        AppPreferences.createOrGetInstance().savePrefThemeCustomizationData(newId, newList)
                    }
                } else {
                    try {
                        val custList = Element.CustomizationList.deserialize(rawJsonString!!)
                        AppPreferences.createOrGetInstance().savePrefThemeCustomizationData(newId, custList)
                    } catch (e: Exception) {
                        Toast.makeText(activity, "Неизвестная структура JSON", Toast.LENGTH_LONG).show()
                        return
                    }
                }
            }

            if (newId != -1) {
                AppPreferences.createOrGetInstance().setInt(AppPreferences.PREF_Int_visualizerThemeId, newId, true)
                currentSelectedThemeId = newId

                VisualizerThemes.s().loadCustomThemes()
                loadThemes()

                tabLayout.getTabAt(1)?.select()

                allThemes.find { it.id == newId }?.let { themeInfo ->
                    onSkinThemePresetSelected.invoke(themeInfo)
                }

                Toast.makeText(activity, "Пресет успешно импортирован!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(activity, "Ошибка импорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun exportTheme(uri: Uri) {
        try {
            val themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId)
            val cust = AppPreferences.createOrGetInstance().getPrefThemeCustomizationData(themeId)

            if (cust == null) {
                Toast.makeText(activity, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
                return
            }

            var scene = AppPreferences.createOrGetInstance().getPrefThemeScene(themeId)
            if (scene == null) {
                val root = VisualizerThemes.s().getThemeObject(themeId)
                if (root != null) {
                    scene = com.aylis.comp.visual.scene.SceneBuilder.exportToScene(root)
                }
            }

            val localImages = if (scene != null) {
                com.aylis.comp.visual.scene.SceneBuilder.collectLocalImages(scene)
            } else {
                emptyList()
            }

            var baseId = 0
            var themeName = "Preset $themeId"
            if (themeId < 10) {
                baseId = themeId
            } else {
                val customs = AppPreferences.createOrGetInstance().getCustomThemes(activity)
                for (info in customs) {
                    if (info.id == themeId) {
                        baseId = info.baseId
                        themeName = info.name
                        break
                    }
                }
            }

            val manifestObj = JSONObject().apply {
                put("engine", "aylis")
                put("version", 1)
                put("baseId", baseId)
                put("name", themeName)
                put("created", System.currentTimeMillis())
            }

            activity?.contentResolver?.openOutputStream(uri)?.use { os ->
                ZipOutputStream(os).use { zos ->
                    val pathMap = mutableMapOf<String, String>()
                    var fileCounter = 0
                    val usedNames = mutableSetOf<String>()

                    for (originalPath in localImages) {
                        if (originalPath.isNullOrEmpty()) continue

                        val uriObj = Uri.parse(originalPath)
                        var entryName = uriObj.lastPathSegment ?: "asset_$fileCounter${getExtension(originalPath)}"

                        val isFont = originalPath.lowercase().endsWith(".ttf") || originalPath.lowercase().endsWith(".otf")
                        val archiveEntryName = if (isFont) "fonts/$entryName" else "assets/$entryName"

                        if (!isFont && usedNames.contains(archiveEntryName)) {
                            entryName = "${fileCounter}_$entryName"
                        }
                        usedNames.add(archiveEntryName)

                        try {
                            activity?.contentResolver?.openInputStream(uriObj)?.use { imgIs ->
                                zos.putNextEntry(ZipEntry(archiveEntryName))
                                copyStream(imgIs, zos)
                                zos.closeEntry()
                                pathMap[originalPath] = archiveEntryName
                                fileCounter++
                            }
                        } catch (ignored: Exception) {}
                    }

                    if (scene != null) {
                        val updatedScene = com.aylis.comp.visual.scene.SceneBuilder.updateImagePaths(scene, pathMap)
                        val sceneJson = com.aylis.comp.visual.scene.SceneSerializer.toJson(updatedScene)
                        zos.putNextEntry(ZipEntry("scene.json"))
                        zos.write(sceneJson.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }

                    zos.putNextEntry(ZipEntry("customization.json"))
                    zos.write(cust.serialize().toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("aylis_manifest.json"))
                    zos.write(manifestObj.toString(4).toByteArray(Charsets.UTF_8))
                    zos.closeEntry()
                }
            }

            Toast.makeText(activity, "Экспортировано в .ayp!", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(activity, "Ошибка экспорта: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    inner class VisualizerAdapter(private val onThemeSelected: (Int) -> Unit) : RecyclerView.Adapter<VisualizerAdapter.ViewHolder>() {
        private var items: List<VisualizerThemeInfo> = emptyList()

        fun submitList(newItems: List<VisualizerThemeInfo>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.vis_choose_vizstyle_element_dialog, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val theme = items[position]
            holder.bind(theme)
            holder.itemView.setOnClickListener {
                onThemeSelected(theme.id)
            }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardElement: MaterialCardView = itemView.findViewById(R.id.cardElement)
            private val imgElement: ImageView = itemView.findViewById(R.id.imgElement)

            fun bind(theme: VisualizerThemeInfo) {
                val preview = PreviewGenerator.generateVisualizerPreview(itemView.context, theme.id, 160, 160)
                if (preview != null) {
                    imgElement.setImageBitmap(preview)
                } else if (theme.iconResId > 0) {
                    imgElement.setImageResource(theme.iconResId)
                } else {
                    imgElement.setImageDrawable(null)
                }

                val aPrimary = itemView.context.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorPrimary))
                val colorPrimary = aPrimary.getColor(0, Color.parseColor("#42A5F5"))
                aPrimary.recycle()

                val aOutline = itemView.context.obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOutlineVariant))
                val colorOutline = aOutline.getColor(0, Color.parseColor("#1AFFFFFF"))
                aOutline.recycle()

                if (theme.id == currentSelectedThemeId) {
                    cardElement.strokeColor = colorPrimary
                    cardElement.strokeWidth = (2 * itemView.resources.displayMetrics.density).toInt()
                } else {
                    cardElement.strokeColor = colorOutline
                    cardElement.strokeWidth = (1 * itemView.resources.displayMetrics.density).toInt()
                }
            }
        }
    }
}