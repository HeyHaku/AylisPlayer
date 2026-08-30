package com.aylis.comp.MediaControlsUI

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnLayout
import com.aylis.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.imageview.ShapeableImageView
import kotlin.math.max
import kotlin.math.min

class PlayerSheetController(private val rootView: View) {

    private val layoutMiniControls = rootView.findViewById<View>(R.id.layoutMiniControls)
    private val layoutExpandedControls = rootView.findViewById<View>(R.id.layoutExpandedControls)
    private val imgAlbumArtUnified = rootView.findViewById<ShapeableImageView>(R.id.imgAlbumArtUnified)
    private val placeholderMini = rootView.findViewById<View>(R.id.albumArtPlaceholderMini)
    private val placeholderExpanded = rootView.findViewById<View>(R.id.albumArtPlaceholderExpanded)
    
    // Starting translation coordinates (Mini Player) - initialized with fallbacks
    private var miniTransX = dpToPx(20f)
    private var miniTransY = dpToPx(18f)
    
    // Expanded translation coordinates - initialized with fallbacks
    private var expandedTransX = 0f
    private var expandedTransY = 0f
    
    private var targetScale = 56f / 320f

    private var currentOffset = 0f
    private var isCalculated = false

    init {
        // Fallback calculations to prevent first-frame glitch
        val density = rootView.resources.displayMetrics.density
        val screenWidth = rootView.resources.displayMetrics.widthPixels.toFloat()
        miniTransX = 20f * density
        miniTransY = 18f * density
        expandedTransX = (screenWidth - 320f * density) / 2f
        expandedTransY = 88f * density
        
        // Apply initial collapsed state immediately
        applyTransformations(0f, screenWidth)

        // Dynamically calculate the actual coordinates of the mini placeholder
        // so we don't rely on hardcoded margins/paddings if the XML changes.
        rootView.doOnLayout {
            calculateDynamicBounds()
            applyTransformations(currentOffset, rootView.width.toFloat())
        }
    }

    private fun calculateDynamicBounds() {
        if (placeholderMini == null || placeholderExpanded == null || imgAlbumArtUnified == null) return

        val rootLoc = IntArray(2)
        rootView.getLocationInWindow(rootLoc)

        // Mini placeholder
        val miniLoc = IntArray(2)
        placeholderMini.getLocationInWindow(miniLoc)
        val relMiniX = (miniLoc[0] - rootLoc[0]).toFloat()
        val relMiniY = (miniLoc[1] - rootLoc[1]).toFloat()

        if (relMiniX >= 0 && relMiniY >= 0) {
            miniTransX = relMiniX
            miniTransY = relMiniY
        }

        // Expanded placeholder
        val expLoc = IntArray(2)
        placeholderExpanded.getLocationInWindow(expLoc)
        val relExpX = (expLoc[0] - rootLoc[0]).toFloat() - (layoutExpandedControls?.translationX ?: 0f)
        val relExpY = (expLoc[1] - rootLoc[1]).toFloat() - (layoutExpandedControls?.translationY ?: 0f)

        if (relExpX >= 0 && relExpY >= 0) {
            expandedTransX = relExpX
            expandedTransY = relExpY
        }

        val miniSize = placeholderMini.width.toFloat()
        val expSize = placeholderExpanded.width.toFloat()
        if (expSize > 0f && miniSize > 0f) {
            targetScale = miniSize / expSize
            
            // Adjust imgAlbumArtUnified layout parameters to match expanded placeholder size dynamically
            val params = imgAlbumArtUnified.layoutParams
            params.width = expSize.toInt()
            params.height = placeholderExpanded.height
            imgAlbumArtUnified.layoutParams = params
            
            isCalculated = true
        }
    }

    fun getBottomSheetCallback(): BottomSheetBehavior.BottomSheetCallback {
        return object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        layoutExpandedControls?.visibility = View.GONE
                        layoutMiniControls?.visibility = View.VISIBLE
                        layoutMiniControls?.alpha = 1f
                        applyTransformations(0f, bottomSheet.width.toFloat())
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        layoutExpandedControls?.visibility = View.VISIBLE
                        layoutExpandedControls?.alpha = 1f
                        layoutMiniControls?.visibility = View.GONE
                        applyTransformations(1f, bottomSheet.width.toFloat())
                    }
                    else -> {
                        layoutExpandedControls?.visibility = View.VISIBLE
                        layoutMiniControls?.visibility = View.VISIBLE
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                currentOffset = max(0f, min(1f, slideOffset))
                applyTransformations(currentOffset, bottomSheet.width.toFloat())
            }
        }
    }

    private fun applyTransformations(offset: Float, sheetWidth: Float) {
        if (imgAlbumArtUnified == null) return

        // 1. Scale Interpolation
        val currentScale = targetScale + (1f - targetScale) * offset
        
        imgAlbumArtUnified.pivotX = 0f
        imgAlbumArtUnified.pivotY = 0f
        imgAlbumArtUnified.scaleX = currentScale
        imgAlbumArtUnified.scaleY = currentScale

        // 2. Translation Interpolation
        val transX = miniTransX + (expandedTransX - miniTransX) * offset
        val transY = miniTransY + (expandedTransY - miniTransY) * offset
        
        imgAlbumArtUnified.translationX = transX
        imgAlbumArtUnified.translationY = transY

        // 3. Corner Radius Interpolation
        val expandedRadius = 24f * imgAlbumArtUnified.context.resources.displayMetrics.density
        val miniRadius = Math.max(imgAlbumArtUnified.width.toFloat(), imgAlbumArtUnified.layoutParams.width.toFloat()) / 2f
        val currentRadius = miniRadius + (expandedRadius - miniRadius) * offset
        
        imgAlbumArtUnified.shapeAppearanceModel = imgAlbumArtUnified.shapeAppearanceModel
            .toBuilder()
            .setAllCornerSizes(currentRadius)
            .build()

        // 3. Fading Logic
        if (layoutMiniControls != null) {
            val miniAlpha = max(0f, 1f - (offset / 0.2f))
            layoutMiniControls.alpha = miniAlpha
            layoutMiniControls.visibility = if (miniAlpha == 0f) View.GONE else View.VISIBLE
        }

        if (layoutExpandedControls != null) {
            val expandedAlpha = max(0f, (offset - 0.5f) / 0.5f)
            layoutExpandedControls.alpha = expandedAlpha
            
            // Only set visibility to GONE if we have already calculated bounds, 
            // otherwise it will never be measured during the first layout pass!
            if (isCalculated || expandedAlpha > 0f) {
                layoutExpandedControls.visibility = if (expandedAlpha == 0f) View.GONE else View.VISIBLE
            }
            
            layoutExpandedControls.translationY = dpToPx(50f) * (1f - expandedAlpha)
        }

        // Hide Bottom Navigation dynamically
        val bottomNavCard = rootView.rootView.findViewById<View>(R.id.bottomNavCard)
        if (bottomNavCard != null) {
            val targetAlpha = max(0f, 1f - (offset / 0.3f))
            bottomNavCard.alpha = targetAlpha
            bottomNavCard.visibility = if (targetAlpha == 0f) View.GONE else View.VISIBLE
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * rootView.resources.displayMetrics.density
    }
}
