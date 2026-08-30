package com.aylis.comp.visual.core.Elements

import java.util.Locale

object PropertySorter {

    val TAGS = listOf(
        "00_sampleProvider",
        "0_general",
        "1_appearance",
        "1_bars",
        "1_Cube",
        "1_image",
        "1_particles",
        "1_text",
        "2_Behavior",
        "2_color",
        "2_motion",
        "3_measures",
        "3_shader",
        "5_Camera",
        "6_modifier"
    )

    private val COLOR_KEYWORDS = listOf("color", "fill", "blur", "radius", "divider", "opacity", "saturation", "glow", "softness")
    private val MEASURE_KEYWORDS = listOf("measure", "shake", "react", "beat", "spectrum", "motion", "pulse", "bass", "audio")
    private val GENERAL_EXACT_MATCHES = listOf("visible", "description", "anchorx", "anchory", "position", "anchor", "scale", "scaleisuniform", "rotation")

    @JvmStatic
    fun resolveUiTag(propertyName: String?, rawTag: String?): String {
        var tag = rawTag
        if (tag == null || tag == "General") {
            tag = TAGS[1] // 0_general
        }

        if (tag.isNotEmpty() && tag != TAGS[1] && tag != "None") {
            return tag
        }

        val lowerName = propertyName?.lowercase(Locale.US) ?: ""
        val lowerTag = tag.lowercase(Locale.US)

        if (lowerName.contains("sampleprovider") || lowerTag.contains("spectrum") || lowerTag.contains("sample")) {
            return tag
        }

        if (COLOR_KEYWORDS.any { lowerName.contains(it) }) return TAGS[9] // 2_color
        if (MEASURE_KEYWORDS.any { lowerName.contains(it) }) return TAGS[11] // 3_measures
        if (GENERAL_EXACT_MATCHES.contains(lowerName)) return TAGS[1] // 0_general

        return tag
    }

    @JvmStatic
    fun resolveUiOrder(propertyName: String?, tag: String?, currentOrder: Int): Int {
        if (currentOrder != -1) return currentOrder
        if (propertyName == null) return 999
        
        val lowerName = propertyName.lowercase(Locale.US)
        
        // =========================================================================
        // ТОПОРНАЯ И ПОНЯТНАЯ СОРТИРОВКА
        // Сначала мы смотрим, в какой мы вкладке (переменная tag),
        // а затем жестко назначаем, что на каком месте должно быть.
        // =========================================================================

        when (tag) {
            "1_image" -> {
                // Если мы во вкладке Image, то:
                if (lowerName == "customimage") return 3
                if (lowerName == "color") return 4
                if (lowerName == "keepaspectratio") return 5
                if (lowerName == "blurredborder") return 6
                if (lowerName == "corners") return 7
                
                // Маска в той же вкладке будет ниже
                if (lowerName == "maskimage") return 10
                if (lowerName == "maskmode") return 11
                if (lowerName == "maskscale") return 12
                if (lowerName == "masklockscaleratio") return 13
            }
            
            "0_general" -> {
                // Если мы во вкладке General, то:
                if (lowerName == "visible") return 1
                if (lowerName == "description") return 2
                if (lowerName == "blendmode") return 3
                if (lowerName == "blend") return 4
                if (lowerName == "position") return 5
                if (lowerName == "anchor") return 6
                if (lowerName == "anchorx") return 6
                if (lowerName == "anchory") return 6
                if (lowerName == "scale") return 7
                if (lowerName == "lockscaleratio") return 8
                if (lowerName == "rotation") return 9
            }

            "1_appearance" -> {
                // Если мы во вкладке General, то:
                if (lowerName == "customfont") return 1
                if (lowerName == "textcolor") return 2
                if (lowerName == "textsize") return 3
                if (lowerName == "text") return 4
            }
            
            "1_text" -> {
                if (lowerName == "text") return 1
                if (lowerName == "customfont") return 2
                if (lowerName == "fontsize") return 3
                if (lowerName == "gravity") return 4
                if (lowerName == "color") return 5
            }
            
            "1_particles" -> {
                if (lowerName == "particleslimit") return 1
                if (lowerName == "particlespeed") return 2
                if (lowerName == "particlescale") return 3
                if (lowerName == "color") return 4
            }
            
            "1_bars" -> {
                if (lowerName == "shapepath") return 1
                if (lowerName == "segment1") return 2
                if (lowerName == "segment2") return 3
                if (lowerName == "color") return 4
                if (lowerName == "colorfrom") return 5
                if (lowerName == "colorto") return 6
                if (lowerName == "heightscale") return 7
                if (lowerName == "minheightscale") return 8
                if (lowerName == "maxheightscale") return 9
                if (lowerName == "barheightmultiplier") return 10
                if (lowerName == "fixedheight") return 11
                if (lowerName == "barwidth") return 10
                if (lowerName == "barwidthaffectedbyshape") return 11
                if (lowerName == "glowwidth") return 12
                if (lowerName == "glowalpha") return 13
                if (lowerName == "radius") return 20
                if (lowerName == "sides") return 21
                if (lowerName == "vertical") return 22
                if (lowerName == "flipinput") return 30
                if (lowerName == "mirror") return 31
                if (lowerName == "mirrorx") return 32
                if (lowerName == "flipeveryother") return 33
            }
            
            "2_Behavior" -> {
                if (lowerName == "audioproviderindex") return 1
                if (lowerName == "reactiondelay") return 2
                if (lowerName == "reactionaccumulateddelay") return 3
                if (lowerName == "softness") return 4
            }
            
            "2_color" -> {
                if (lowerName.contains("color")) return 1
                if (lowerName.contains("opacity")) return 2
                if (lowerName.contains("fill")) return 3
                if (lowerName.contains("glow")) return 4
                if (lowerName.contains("blur")) return 5
            }
            
            "3_measures" -> {
                if (lowerName.contains("measurepos")) return 1
                if (lowerName.contains("measurescale")) return 2
                if (lowerName.contains("measurerot")) return 3
                if (lowerName.contains("beat")) return 4
                if (lowerName.contains("shake")) return 5
            }
        }
        
        // Если свойство не попало ни в один жесткий список выше,
        // то мы сортируем его по умолчанию:
        if (lowerName.contains("color")) return 500

        return 999
    }
}
