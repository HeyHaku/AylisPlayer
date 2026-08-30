

package com.aylis.comp.visual.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.ArrayList
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import com.aylis.Common.Events.WeakEvent
import com.aylis.Common.Events.WeakEvent1
import com.aylis.Common.Events.WeakEvent3
import com.aylis.Common.Events.WeakEvent4
import com.aylis.Common.Events.WeakEventR
import com.aylis.Common.Tuple2
import com.aylis.Common.UtilsUI
import com.aylis.ContextData
import com.aylis.R
import com.aylis.comp.AppPreferences.AppPreferences
import com.aylis.comp.visual.core.Elements.Element.CustomizationList
import com.aylis.comp.visual.core.VisualizerViewCore
import com.aylis.comp.visual.core.Elements.RootElement

@UnstableApi
class FragmentVisualizer : Fragment() {
    private var rootView: View? = null
    private var visualizerFrame: AspectRatioFrameLayout? = null
    private var surfaceViewVisualizer: VisualizerViewCore? = null
    private var surfaceViewTag = 0
    private var layoutButtons: View? = null
    private var btn1: View? = null
    private var btn3: View? = null
    private var btn5Img: ImageView? = null

    private val listenerReferenceHolder = ArrayList<Any>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layoutId = com.aylis.comp.visual.ui.LayoutModeManager.getLayout(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.fragment_visualizer))
        rootView = inflater.inflate(layoutId, container, false)

        rootView!!.addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                val w = right - left
                val h = bottom - top

                rootView!!.post(object : Runnable {
                    override fun run() {
                        val visualAspectRatio = AppPreferences.createOrGetInstance()
                            .getInt(AppPreferences.PREF_Int_visualizerAspectRatio)
                        updateVisualizerAspectRatio(visualAspectRatio)
                    }
                })
            }
        })

        layoutButtons = rootView!!.findViewById<View?>(R.id.layoutButtons)
        

        val btn0 = layoutButtons!!.findViewById<View?>(R.id.btn0)!!
        btn0.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                com.aylis.utils.HapticManager.performClick(v)
                createThemeChooserMenu(v)
            }
        })

        btn1 = layoutButtons!!.findViewById<View?>(R.id.btn1)!!
        btn1!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                com.aylis.utils.HapticManager.performClick(v)
                onCustomizeAction.invoke()
            }
        })

        val btnExport = layoutButtons!!.findViewById<View?>(R.id.btnExport)!!
        btnExport.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                com.aylis.utils.HapticManager.performClick(v)
                val prefs = AppPreferences.createOrGetInstance()
                
                val trackInfo = com.aylis.comp.MediaControlsUI.MediaControlsUI.onRequestTrackInfo.invoke(
                    com.aylis.Common.Tuple2(com.aylis.comp.playback.Song.PlaylistSong.emptyData, com.aylis.comp.playback.PlayingMediaInfo.empty)
                )
                
                var audioUriStr = ""
                var trackName = ""
                var durationMs = 0L
                if (trackInfo != null && trackInfo.obj1 != null && trackInfo.obj1!!.dataSource != null) {
                    audioUriStr = trackInfo.obj1!!.dataSource.toString()
                }
                if (trackInfo != null && trackInfo.obj1 != null) {
                    trackName = trackInfo.obj1!!.trackName ?: ""
                }
                if (trackInfo != null && trackInfo.obj2 != null) {
                    durationMs = trackInfo.obj2!!.duration
                }

                var themeJson = ""
                val rootElement = getThemeElements()
                if (rootElement != null) {
                    val scene = com.aylis.comp.visual.scene.SceneBuilder.exportToScene(rootElement)
                    themeJson = com.aylis.comp.visual.scene.SceneSerializer.toJson(scene)
                }

                val dialog = com.aylis.comp.export.ExportSettingsDialog.newInstance(audioUriStr, themeJson, trackName, durationMs)
                dialog.show(requireActivity().supportFragmentManager, "ExportSettingsDialog")
            }
        })

        val btn5 = layoutButtons!!.findViewById<View?>(R.id.btn5)!!
        btn5Img = btn5 as ImageView?
        
        setupAspectClickListeners(rootView!!)
        
        btn5.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                com.aylis.utils.HapticManager.performClick(v)
                val prefs = AppPreferences.createOrGetInstance()

                val layoutAspectRatioMenu = rootView!!.findViewById<View>(R.id.layoutAspectRatioMenu)
                if (layoutAspectRatioMenu != null) {
                    if (layoutAspectRatioMenu.visibility == View.VISIBLE) {
                        val anim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.fade_out)
                        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                                layoutAspectRatioMenu.clearAnimation()
                                layoutAspectRatioMenu.visibility = View.GONE
                            }
                            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                        })
                        layoutAspectRatioMenu.startAnimation(anim)
                    } else {
                        layoutAspectRatioMenu.visibility = View.VISIBLE
                        updateAspectIcons(rootView!!)
                        val anim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.fade_in)
                        layoutAspectRatioMenu.startAnimation(anim)
                    }
                }
            }
        })

        // Removed click listener

        visualizerFrame =
            rootView!!.findViewById<View?>(R.id.visualizerFrame) as AspectRatioFrameLayout?
        surfaceViewVisualizer =
            rootView!!.findViewById<View?>(R.id.surfaceViewVisualizer) as VisualizerViewCore?
        // Removed click listener

        onSurfaceCreated.invoke(surfaceViewVisualizer)

        val visualAspectRatio = AppPreferences.createOrGetInstance()
            .getInt(AppPreferences.PREF_Int_visualizerAspectRatio)
        updateVisualizerAspectRatio(visualAspectRatio)

        run {
            val need: Boolean = onRequestUIComponentNeedChangedValue.invoke(true)!!
            val showVideoContent: Boolean = onRequestShowVideoContentState.invoke(false)!!
            updateSurfaceVisibility(need, showVideoContent)
        }

        return rootView
    }

    override fun onDestroyView() {

        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        surfaceViewVisualizer?.onResume() // Будим рендер
    }

    override fun onPause() {
        surfaceViewVisualizer?.onPause() // Ставим на паузу, чтобы не ломать контекст
        super.onPause()
    }

    val isViewCreated: Boolean
        get() = rootView != null

    val isSurfaceVisible: Boolean
        get() = surfaceViewVisualizer != null && (surfaceViewVisualizer!!.getVisibility() == View.VISIBLE || surfaceViewTag == 1)

    fun updateSurfaceVisibility(visible: Boolean, showVideoContent: Boolean) {
        if (visible && !showVideoContent) {
            if (surfaceViewVisualizer != null) {
                surfaceViewTag = 1
                surfaceViewVisualizer!!.postDelayed(object : Runnable {
                    override fun run() {
                        if (surfaceViewTag == 1) {
                            surfaceViewVisualizer!!.visibility = View.VISIBLE
                            val anim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.fade_in_visualizer)
                            surfaceViewVisualizer!!.clearAnimation()
                            surfaceViewVisualizer!!.startAnimation(anim)
                        }
                    }
                }, 250)
            }
        } else {
            if (surfaceViewVisualizer != null) {
                surfaceViewTag = 0
                val anim = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.fade_out_visualizer)
                anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        if (surfaceViewTag == 0) surfaceViewVisualizer!!.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })
                surfaceViewVisualizer!!.clearAnimation()
                surfaceViewVisualizer!!.startAnimation(anim)
            }
        }
    }

    fun setShowVideoContentState(state: Boolean) {
        updateSurfaceVisibility(this.isSurfaceVisible, state)
    }

    fun animateShow(show: Boolean) {
        if (layoutButtons == null) return

        val mShortAnimTime: Int

        mShortAnimTime = layoutButtons!!.getResources().getInteger(
            android.R.integer.config_shortAnimTime
        )

        if (show) {
            layoutButtons!!.animate()
                .translationX(0f).alpha(1.0f)
                .setDuration(mShortAnimTime.toLong())
        } else {
            layoutButtons!!.animate()
                .translationX(layoutButtons!!.getWidth().toFloat()).alpha(0.0f)
                .setDuration(mShortAnimTime.toLong())
        }
    }

    private fun createThemeChooserMenu(v: View?) {
        val contextData = ContextData(getActivity())
        val fragmentManager = contextData.getFragmentManager()

        if (fragmentManager != null) {
            ChooseVisualizerDialog.createAndShowDialog(fragmentManager)
        }
    }

    fun showCustomizationMenu(currentCustomization: Tuple2<Int?, CustomizationList>?) {
        if (currentCustomization == null) return
        if (currentCustomization.obj2 == null) return

        val rootIdentifier: Int = currentCustomization.obj1!!
        val customizationDataList = currentCustomization.obj2

        onPickElementAction.invoke(
            ContextData(getActivity()),
            rootIdentifier,
            customizationDataList,
            -1
        )
    }

    private fun updateAspectIcons(view: View) {
        val ratio = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerAspectRatio)
        
        val activeColor = android.graphics.Color.parseColor("#448AFF") // Blue
        val inactiveColor = android.graphics.Color.parseColor("#888888") // Gray

        val color0 = if (ratio == 0) activeColor else inactiveColor
        val color1 = if (ratio == 1) activeColor else inactiveColor
        val color2 = if (ratio == 2) activeColor else inactiveColor
        val color3 = if (ratio == 3) activeColor else inactiveColor
        val color4 = if (ratio == 4) activeColor else inactiveColor
        val color5 = if (ratio == 5) activeColor else inactiveColor
        val color6 = if (ratio == 6) activeColor else inactiveColor

        view.findViewById<View>(R.id.iconAspectFull)?.backgroundTintList = android.content.res.ColorStateList.valueOf(color0)
        view.findViewById<View>(R.id.iconAspect9_19)?.backgroundTintList = android.content.res.ColorStateList.valueOf(color1)
        view.findViewById<View>(R.id.iconAspect9_16)?.backgroundTintList = android.content.res.ColorStateList.valueOf(color2)
        view.findViewById<View>(R.id.iconAspect16_9)?.backgroundTintList = android.content.res.ColorStateList.valueOf(color3)
        view.findViewById<View>(R.id.iconAspect4_3)?.backgroundTintList = android.content.res.ColorStateList.valueOf(color4)
        view.findViewById<View>(R.id.iconAspect3_4)?.backgroundTintList = android.content.res.ColorStateList.valueOf(color5)
        view.findViewById<View>(R.id.iconAspect1_1)?.backgroundTintList = android.content.res.ColorStateList.valueOf(color6)
    }

    private fun setupAspectClickListeners(view: View) {
        val layoutAspectRatioMenu = view.findViewById<View>(R.id.layoutAspectRatioMenu)
        val clickListener = View.OnClickListener { v ->
            com.aylis.utils.HapticManager.performClick(v)
            val ratioId = v.tag as? Int ?: 0
            setAspectRatio(ratioId)
            
            updateAspectIcons(view)

            if (layoutAspectRatioMenu?.visibility == View.VISIBLE) {
                val anim = android.view.animation.AnimationUtils.loadAnimation(view.context, R.anim.fade_out)
                anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}
                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        layoutAspectRatioMenu.clearAnimation()
                        layoutAspectRatioMenu.visibility = View.GONE
                    }
                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })
                layoutAspectRatioMenu.startAnimation(anim)
            }
        }
        view.findViewById<View>(R.id.btnAspectFull)?.apply { tag = 0; setOnClickListener(clickListener) }
        view.findViewById<View>(R.id.btnAspect9_19)?.apply { tag = 1; setOnClickListener(clickListener) }
        view.findViewById<View>(R.id.btnAspect9_16)?.apply { tag = 2; setOnClickListener(clickListener) }
        view.findViewById<View>(R.id.btnAspect16_9)?.apply { tag = 3; setOnClickListener(clickListener) }
        view.findViewById<View>(R.id.btnAspect4_3)?.apply { tag = 4; setOnClickListener(clickListener) }
        view.findViewById<View>(R.id.btnAspect3_4)?.apply { tag = 5; setOnClickListener(clickListener) }
        view.findViewById<View>(R.id.btnAspect1_1)?.apply { tag = 6; setOnClickListener(clickListener) }
    }

    private fun setAspectRatio(ratioId: Int) {
        val animOut = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.fade_out_visualizer)
        animOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                AppPreferences.createOrGetInstance()
                    .setInt(AppPreferences.PREF_Int_visualizerAspectRatio, ratioId, true)
                updateVisualizerAspectRatio(ratioId)
                
                visualizerFrame?.requestLayout()
                visualizerFrame?.invalidate()
                
                surfaceViewVisualizer?.requestLayout()
                surfaceViewVisualizer?.invalidate()
                surfaceViewVisualizer?.postDelayed({
                    val width = surfaceViewVisualizer?.width ?: 0
                    val height = surfaceViewVisualizer?.height ?: 0
                    if (width > 0 && height > 0) {
                        surfaceViewVisualizer?.forceUpdateViewport(width, height)
                    }
                    val animIn = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.fade_in_visualizer)
                    visualizerFrame?.startAnimation(animIn)
                }, 100)
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
        visualizerFrame?.startAnimation(animOut)
    }

    private fun updateVisualizerAspectRatio(ratioId: Int) {
        if (visualizerFrame == null) return

        var ratio = 0.0f
        when (ratioId) {
            0 -> ratio = rootView!!.getWidth().toFloat() / rootView!!.getHeight().toFloat()
            1 -> ratio = 9.0f / 19.0f
            2 -> ratio = 9.0f / 16.0f
            3 -> ratio = 16.0f / 9.0f
            4 -> ratio = 4.0f / 3.0f
            5 -> ratio = 3.0f / 4.0f
            6 -> ratio = 1.0f / 1.0f
        }

        if (ratioId == 0) {

            if (rootView!!.getWidth() > 0 && rootView!!.getHeight() > 0) {
                ratio = rootView!!.getWidth().toFloat() / rootView!!.getHeight().toFloat()
            } else {
                ratio = 1.0f
            }
        }

        visualizerFrame!!.setAspectRatio(ratio)
    }

    fun getThemeElements(): RootElement? {
        return surfaceViewVisualizer?.getThemeElements()
    }

    companion object {
        @JvmField
        var onSurfaceCreated: WeakEvent1<VisualizerViewCore?> = WeakEvent1<VisualizerViewCore?>()
        @JvmField
        var onRequestShowVideoContentState: WeakEventR<Boolean?> = WeakEventR<Boolean?>()
        @JvmField
        var onToggleVideoScalingMode: WeakEvent = WeakEvent()
        @JvmField
        var onRequestVideoScalingMode: WeakEventR<Int?> = WeakEventR<Int?>()
        @JvmField
        var onRequestVideoWidthHeightRatio: WeakEventR<Float?> = WeakEventR<Float?>()
        @JvmField
        var onToggleVisualPreferShowContent: WeakEvent = WeakEvent()
        @JvmField
        var onToggleMediaControls: WeakEvent = WeakEvent()
        @JvmField
        var onVideoSurfaceHolderCreated: WeakEvent1<SurfaceHolder?> = WeakEvent1<SurfaceHolder?>()
        @JvmField
        var onVideoSurfaceHolderDestroyed: WeakEvent = WeakEvent()
        @JvmField
        var onRequestUIComponentNeedChangedValue: WeakEventR<Boolean?> = WeakEventR<Boolean?>()
        @JvmField
        var onVideoElementInteracted: WeakEvent = WeakEvent()
        @JvmField
        var onUIComponentNeedChanged: WeakEvent1<Boolean?> = WeakEvent1<Boolean?>()
        @JvmField
        var onCustomizeAction: WeakEvent = WeakEvent()
        @JvmField
        var onPickElementAction: WeakEvent4<ContextData?, Int?, CustomizationList?, Int?> =
            WeakEvent4<ContextData?, Int?, CustomizationList?, Int?>()
        @JvmField
        var onResetAction: WeakEvent3<ContextData?, Int?, CustomizationList?> =
            WeakEvent3<ContextData?, Int?, CustomizationList?>()

        @JvmStatic
        fun newInstance(): FragmentVisualizer {
            val fragment = FragmentVisualizer()
            val args = Bundle()
            fragment.setArguments(args)
            return fragment
        }

        private fun setStatusBarDimensions(view: View?) {
            if (view == null) return
            val params = view.getLayoutParams()
            params.height = UtilsUI.getStatusBarHeight(view.getContext())
        }
    }
}
