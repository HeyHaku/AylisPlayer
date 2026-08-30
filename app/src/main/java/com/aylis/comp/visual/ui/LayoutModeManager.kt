package com.aylis.comp.visual.ui

import com.aylis.R
import com.aylis.comp.AppPreferences.AppPreferences

object LayoutModeManager {

    val isCcsMode: Boolean
        get() = AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_uiLayoutCCS)

    @JvmStatic
    fun getLayout(defaultLayoutId: Int): Int {
        if (!isCcsMode) return defaultLayoutId

        return when (defaultLayoutId) {
            R.layout.fragment_visualizer -> R.layout.ccs_fragment_visualizer
            R.layout.customize_main -> R.layout.ccs_customize_main
            R.layout.customize_main_1 -> R.layout.ccs_customize_main_1
            R.layout.customize_main_2 -> R.layout.ccs_customize_main_2
            R.layout.customize_item_element -> R.layout.ccs_customize_item_element
            R.layout.customize_item_composition -> R.layout.ccs_customize_item_composition
            R.layout.customize_action -> R.layout.ccs_customize_action
            R.layout.customize_align -> R.layout.ccs_customize_align
            R.layout.customize_bottom_nav_item -> R.layout.ccs_customize_bottom_nav_item
            R.layout.customize_child -> R.layout.ccs_customize_child
            R.layout.customize_color -> R.layout.ccs_customize_color
            R.layout.customize_dialog_add_element -> R.layout.ccs_customize_dialog_add_element
            R.layout.customize_editor_color -> R.layout.ccs_customize_editor_color
            R.layout.customize_editor_measured_var -> R.layout.ccs_customize_editor_measured_var
            R.layout.customize_editor_seekbar -> R.layout.ccs_customize_editor_seekbar
            R.layout.customize_editor_seekbar_xy -> R.layout.ccs_customize_editor_seekbar_xy
            R.layout.customize_font -> R.layout.ccs_customize_font
            R.layout.customize_group_container -> R.layout.ccs_customize_group_container
            R.layout.customize_image -> R.layout.ccs_customize_image
            R.layout.customize_image_picker -> R.layout.ccs_customize_image_picker
            R.layout.customize_image_picker_item -> R.layout.ccs_customize_image_picker_item
            R.layout.customize_item_add_element -> R.layout.ccs_customize_item_add_element
            R.layout.customize_item_measured_var -> R.layout.ccs_customize_item_measured_var
            R.layout.customize_item_spinner_dropdown -> R.layout.ccs_customize_item_spinner_dropdown
            R.layout.customize_seekbar -> R.layout.ccs_customize_seekbar
            R.layout.customize_seekbar_xy -> R.layout.ccs_customize_seekbar_xy
            R.layout.customize_shader -> R.layout.ccs_customize_shader
            R.layout.customize_text -> R.layout.ccs_customize_text
            R.layout.customize_toggle -> R.layout.ccs_customize_toggle
            else -> defaultLayoutId
        }
    }
}
