package com.aylis.comp.MediaControlsUI

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListPopupWindow
import android.widget.SeekBar
import android.widget.TextView
import androidx.transition.TransitionManager
import com.aylis.Common.Events.WeakEvent
import com.aylis.Common.Events.WeakEvent1
import com.aylis.Common.Events.WeakEvent2
import com.aylis.Common.Events.WeakEventR
import com.aylis.Common.Tuple2
import com.aylis.Common.Utils
import com.aylis.Common.UtilsUI
import com.aylis.ContextData
import com.aylis.MainActivity
import com.aylis.R
import com.aylis.comp.AlbumArt.AlbumArtRequest
import com.aylis.comp.AlbumArt.ImageLoadedListener
import com.aylis.comp.playback.PlayingMediaInfo
import com.aylis.comp.playback.Song.PlaylistSong
import com.google.android.material.transition.MaterialContainerTransform
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.aylis.comp.AppPreferences.AppPreferences
import androidx.core.graphics.ColorUtils

import com.google.android.material.imageview.ShapeableImageView

class MediaControlsUI private constructor() {

    companion object {
        @JvmField val onRequestVolumeMuteState = WeakEventR<Boolean>()
        @JvmField val onRequestAudioEffectsActiveState = WeakEventR<Boolean>()
        @JvmField val onRequestShowState = WeakEventR<Int>()
        @JvmField val onPlaybackPrev = WeakEvent()
        @JvmField val onPlaybackNext = WeakEvent()
        @JvmField val onPlaybackTogglePause = WeakEvent()
        @JvmField val onRequestTrackPosition = WeakEventR<Long>()
        @JvmField val onRequestTrackInfo = WeakEventR<Tuple2<PlaylistSong.Data, PlayingMediaInfo>>()
        @JvmField val onRequestPlaystate = WeakEventR<Tuple2<Boolean, Boolean>>()

        @JvmField val onRequestAudioVolumeState = WeakEventR<Tuple2<Int, Int>>()
        @JvmField val onRequestAudioBalanceState = WeakEventR<Tuple2<Int, Int>>()
        @JvmField val onRequestAudioEffectVirtualizerState = WeakEventR<Tuple2<Int, Int>>()
        @JvmField val onRequestCrossFadeState = WeakEventR<Tuple2<Int, Int>>()
        @JvmField val onSetAudioVolume = WeakEvent2<Int, Int>()
        @JvmField val onSetAudioStereoBalance = WeakEvent2<Int, Int>()
        @JvmField val onSetCrossFade = WeakEvent2<Int, Int>()
        @JvmField val onRequestAudioViewExpandedState = WeakEventR<Boolean>()
        @JvmField val onSetAudioViewExpandedState = WeakEvent1<Boolean>()
        @JvmField val onToggleMuteAction = WeakEvent()
        @JvmField val onActionEq = WeakEvent1<ContextData>()
        @JvmField val onRequestEqState = WeakEventR<Boolean>()

        @JvmField val onRequestShuffleMode = WeakEventR<Int>()
        @JvmField val onSetShuffleMode = WeakEvent1<Int>()
        @JvmField val onRequestRepeatMode = WeakEventR<Int>()
        @JvmField val onSetRepeatMode = WeakEvent1<Int>()
        @JvmField val onRequestMusicSystemIndex = WeakEventR<Int>()
        @JvmField val onSelectMusicSysAction = WeakEvent1<Int>()
        @JvmField val onSetTrackPosition = WeakEvent1<Long>()

        private const val MSG_REFRESH = 1
        
        @Volatile
        private var instanceWeak: WeakReference<MediaControlsUI>? = null

        @JvmStatic
        fun createOrGetInstance(): MediaControlsUI {
            var inst = instanceWeak?.get()
            if (inst == null) {
                synchronized(this) {
                    inst = instanceWeak?.get()
                    if (inst == null) {
                        inst = MediaControlsUI()
                        instanceWeak = WeakReference(inst)
                    }
                }
            }
            return inst!!
        }

        @JvmStatic
        fun getInstance(): MediaControlsUI? = instanceWeak?.get()
    }

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_REFRESH) {
                val next = refreshNow()
                queueNextRefresh(next)
            }
        }
    }

    private var volumePopupWindowSingleton: WeakReference<VolumePopupWindow>? = null
    private var ctrlOverflowPopupWindowSingleton: WeakReference<ThreeDotPopupWindow>? = null
    private var overflowPopupWindowSingleton: WeakReference<ListPopupWindow>? = null

    private var layoutMediaControls: ViewGroup? = null
    private var playerUnifiedRoot: View? = null
    private var layoutMiniControls: View? = null
    private var playerSheetController: PlayerSheetController? = null

    // Mini Views
    private var btnPauseMini: ImageButton? = null
    private var btnPrevMini: ImageButton? = null
    private var btnNextMini: ImageButton? = null
    private var progressMini: SeekBar? = null
    private var titleMini: TextView? = null
    private var artistMini: TextView? = null

    // Expanded Views
    private var btnPauseExpanded: ImageButton? = null
    private var btnPrevExpanded: ImageButton? = null
    private var btnNextExpanded: ImageButton? = null
    private var btnCollapseExpanded: ImageButton? = null
    private var btnOverflowExpanded: ImageButton? = null
    private var progressExpanded: SeekBar? = null
    private var txtTimeElapsedExpanded: TextView? = null
    private var txtTimeTotalExpanded: TextView? = null
    private var titleExpanded: TextView? = null
    private var artistExpanded: TextView? = null
    private var imgAlbumArtUnified: ShapeableImageView? = null

    private var btnLikeExpanded: ImageButton? = null
    private var btnDownloadExpanded: ImageButton? = null
    private var txtDownloadProgressExpanded: TextView? = null
    private var btnDislikeExpanded: ImageButton? = null
    private var btnRepeatExpanded: ImageButton? = null
    private var btnShuffleExpanded: ImageButton? = null
    private var btnVolumeExpanded: ImageButton? = null
    private var currentSongData: PlaylistSong.Data? = null
    private var lastSwipeDirection: Int = 1

    private var bottomNavCard: View? = null

    private var duration: Long = 0
    private var posOverride: Long = -1
    private var lastSeekEventTime: Long = 0

    private var lastShowLevel = -1
    private var designHeight0 = 0f

    var isExpanded: Boolean = false
        private set

    private fun isViewCreated() = layoutMediaControls != null

    private val seekListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(bar: SeekBar) {
            lastSeekEventTime = 0
        }
        override fun onProgressChanged(bar: SeekBar, p: Int, fromuser: Boolean) {
            if (!fromuser) return
            
            posOverride = duration * p / 1000
            val now = SystemClock.elapsedRealtime()
            if (now - lastSeekEventTime > 250) {
                lastSeekEventTime = now
                onSetTrackPosition.invoke(posOverride)
                com.aylis.utils.HapticManager.performTick(bar)
            }
        }
        override fun onStopTrackingTouch(bar: SeekBar) {
            onSetTrackPosition.invoke(posOverride)
            posOverride = -1
        }
    }

    private var bottomSheetBehavior: com.google.android.material.bottomsheet.BottomSheetBehavior<View>? = null

    fun expand() {
        bottomSheetBehavior?.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
    }

    fun collapse() {
        bottomSheetBehavior?.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
    }

    fun onCreateView(view: View, viewBg: View) {
        designHeight0 = view.resources.getDimension(R.dimen.design_height_0)
        
        layoutMediaControls = view.rootView.findViewById(R.id.layoutMediaControls)
        bottomNavCard = view.rootView.findViewById(R.id.bottomNavCard)

        val bottomSheetView = layoutMediaControls?.findViewById<View>(R.id.player_bottom_sheet)
        if (bottomSheetView != null) {
            bottomSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheetView)
        }

        playerUnifiedRoot = layoutMediaControls?.findViewById(R.id.player_unified_root)
        if (playerUnifiedRoot != null) {
            playerSheetController = PlayerSheetController(playerUnifiedRoot!!)
        }

        // Setup BottomSheet Animation
        val callback = playerSheetController?.getBottomSheetCallback()
        if (callback != null) {
            bottomSheetBehavior?.addBottomSheetCallback(callback)
        }
        bottomSheetBehavior?.addBottomSheetCallback(object : com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                isExpanded = (newState == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED)
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })

        // Bind Mini
        layoutMiniControls = playerUnifiedRoot?.findViewById(R.id.layoutMiniControls)
        layoutMiniControls?.setOnClickListener { expand() }
        titleMini = playerUnifiedRoot?.findViewById(R.id.txtSongTitleMini)
        artistMini = playerUnifiedRoot?.findViewById(R.id.txtSongArtistMini)
        progressMini = playerUnifiedRoot?.findViewById(R.id.seekBarSongProgressMini)
        btnPrevMini = playerUnifiedRoot?.findViewById(R.id.btnPrevMini)
        btnPauseMini = playerUnifiedRoot?.findViewById(R.id.btnPauseMini)
        btnNextMini = playerUnifiedRoot?.findViewById(R.id.btnNextMini)

        // Bind Expanded
        btnCollapseExpanded = playerUnifiedRoot?.findViewById(R.id.btnCollapseExpanded)
        btnCollapseExpanded?.setOnClickListener { collapse() }

        imgAlbumArtUnified = playerUnifiedRoot?.findViewById(R.id.imgAlbumArtUnified)
        titleExpanded = playerUnifiedRoot?.findViewById(R.id.txtSongTitleExpanded)
        artistExpanded = playerUnifiedRoot?.findViewById(R.id.txtSongArtistExpanded)
        progressExpanded = playerUnifiedRoot?.findViewById(R.id.seekBarSongProgressExpanded)
        txtTimeElapsedExpanded = playerUnifiedRoot?.findViewById(R.id.txtTimeElapsedExpanded)
        txtTimeTotalExpanded = playerUnifiedRoot?.findViewById(R.id.txtTimeTotalExpanded)
        
        btnPrevExpanded = playerUnifiedRoot?.findViewById(R.id.btnPrevExpanded)
        btnPauseExpanded = playerUnifiedRoot?.findViewById(R.id.btnPauseExpanded)
        btnNextExpanded = playerUnifiedRoot?.findViewById(R.id.btnNextExpanded)
        btnOverflowExpanded = playerUnifiedRoot?.findViewById(R.id.btnOverflowExpanded)
        
        btnLikeExpanded = playerUnifiedRoot?.findViewById(R.id.btnLikeExpanded)
        btnDownloadExpanded = playerUnifiedRoot?.findViewById(R.id.btnDownloadExpanded)
        txtDownloadProgressExpanded = playerUnifiedRoot?.findViewById(R.id.txtDownloadProgressExpanded)
        btnDislikeExpanded = playerUnifiedRoot?.findViewById(R.id.btnDislikeExpanded)
        btnRepeatExpanded = playerUnifiedRoot?.findViewById(R.id.btnRepeatExpanded)
        btnShuffleExpanded = playerUnifiedRoot?.findViewById(R.id.btnShuffleExpanded)
        btnVolumeExpanded = playerUnifiedRoot?.findViewById(R.id.btnVolumeExpanded)

        // Setup Seekbars
        progressMini?.setOnSeekBarChangeListener(seekListener)
        progressExpanded?.setOnSeekBarChangeListener(seekListener)

        // Setup Actions
        val prevListener = View.OnClickListener { v ->
            com.aylis.utils.HapticManager.performClick(v)
            onPlaybackPrev.invoke() 
        }
        val nextListener = View.OnClickListener { v ->
            com.aylis.utils.HapticManager.performClick(v)
            onPlaybackNext.invoke()
        }
        
        val downloadListener = object : com.aylis.comp.online.managers.OnlineDownloadManager.DownloadListener {
            override fun onProgress(videoId: String, progress: Int) {
                val track = getCurrentOnlineTrack()
                if (track?.videoId == videoId) {
                    if (txtDownloadProgressExpanded?.visibility != View.VISIBLE) {
                        val ctx = txtDownloadProgressExpanded?.context
                        if (ctx != null) {
                            val fadeIn = android.view.animation.AnimationUtils.loadAnimation(ctx, R.anim.fade_in)
                            val fadeOut = android.view.animation.AnimationUtils.loadAnimation(ctx, R.anim.fade_out)
                            txtDownloadProgressExpanded?.startAnimation(fadeIn)
                            btnDownloadExpanded?.startAnimation(fadeOut)
                        }
                        txtDownloadProgressExpanded?.visibility = View.VISIBLE
                        btnDownloadExpanded?.visibility = View.INVISIBLE
                    }
                    txtDownloadProgressExpanded?.text = "$progress%"
                }
            }

            override fun onCompleted(videoId: String, success: Boolean, file: java.io.File?) {
                val track = getCurrentOnlineTrack()
                if (track?.videoId == videoId) {
                    if (txtDownloadProgressExpanded?.visibility == View.VISIBLE) {
                        val ctx = txtDownloadProgressExpanded?.context
                        if (ctx != null) {
                            val fadeIn = android.view.animation.AnimationUtils.loadAnimation(ctx, R.anim.fade_in)
                            val fadeOut = android.view.animation.AnimationUtils.loadAnimation(ctx, R.anim.fade_out)
                            btnDownloadExpanded?.startAnimation(fadeIn)
                            txtDownloadProgressExpanded?.startAnimation(fadeOut)
                        }
                        txtDownloadProgressExpanded?.visibility = View.GONE
                        btnDownloadExpanded?.visibility = View.VISIBLE
                    }
                    if (success) {
                        updateDownloadButtonUI(true)
                    }
                }
            }
        }
        com.aylis.comp.online.managers.OnlineDownloadManager.addListener(downloadListener)

        val pauseListener = View.OnClickListener { v ->
            com.aylis.utils.HapticManager.performClick(v)
            onPlaybackTogglePause.invoke() 
        }

        btnPrevMini?.setOnClickListener(prevListener)
        btnNextMini?.setOnClickListener(nextListener)
        btnPauseMini?.setOnClickListener(pauseListener)

        btnPrevExpanded?.setOnClickListener(prevListener)
        btnNextExpanded?.setOnClickListener(nextListener)
        btnPauseExpanded?.setOnClickListener(pauseListener)
        btnOverflowExpanded?.setOnClickListener { v ->
            // overflow 
        }

        btnDownloadExpanded?.setOnClickListener { v ->
            val track = getCurrentOnlineTrack() ?: return@setOnClickListener
            com.aylis.utils.HapticManager.performClick(v)
            val context = v.context
            if (com.aylis.comp.online.managers.OnlineDownloadManager.isTrackDownloaded(track.videoId)) {
                android.widget.Toast.makeText(context, "Уже загружено", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            com.aylis.comp.online.managers.OnlineDownloadManager.downloadTrack(context, track)
        }
        
        btnLikeExpanded?.setOnClickListener { v ->
            com.aylis.utils.HapticManager.performClick(v)
            onToggleMuteAction.invoke()
        }
        
        btnShuffleExpanded?.setOnClickListener { v ->
            com.aylis.utils.HapticManager.performClick(v)
            var currentMode = onRequestShuffleMode.invoke(0)
            if (currentMode == 0) currentMode = 1 else currentMode = 0
            onSetShuffleMode.invoke(currentMode)
        }
        
        btnRepeatExpanded?.setOnClickListener { v ->
            com.aylis.utils.HapticManager.performClick(v)
            var currentMode = onRequestRepeatMode.invoke(0)
            if (currentMode == 0) {
                currentMode = 2 // REPEAT_ALL
            } else if (currentMode == 2) {
                currentMode = 1 // REPEAT_CURRENT
            } else {
                currentMode = 0 // REPEAT_NONE
            }
            onSetRepeatMode.invoke(currentMode)
        }

        btnLikeExpanded?.setOnClickListener { v ->
            val track = getCurrentOnlineTrack() ?: return@setOnClickListener
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            val isNowLiked = com.aylis.comp.online.managers.LikedTracksManager.toggleLike(track)
            updateLikeButtonUI(isNowLiked)
            v.animate().scaleX(1.25f).scaleY(1.25f).setDuration(100).withEndAction {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }.start()
        }

        btnDislikeExpanded?.setOnClickListener { v ->
            val track = getCurrentOnlineTrack() ?: return@setOnClickListener
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            CoroutineScope(Dispatchers.IO).launch {
                com.aylis.comp.online.repository.OnlineMusicRepository.dislikeTrack(track.videoId)
                if (com.aylis.comp.online.managers.LikedTracksManager.isLiked(track.videoId)) {
                    com.aylis.comp.online.managers.LikedTracksManager.toggleLike(track)
                }
            }
            lastSwipeDirection = 1
            onPlaybackNext.invoke()
        }

        val gestureDetector = android.view.GestureDetector(playerUnifiedRoot?.context, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                if (Math.abs(deltaX) > 100 && Math.abs(velocityX) > 100) {
                    if (deltaX > 0) {
                        lastSwipeDirection = -1
                        onPlaybackPrev.invoke()
                    } else {
                        lastSwipeDirection = 1
                        onPlaybackNext.invoke()
                    }
                    return true
                }
                return false
            }
        })
        imgAlbumArtUnified?.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) || true }
        playerUnifiedRoot?.findViewById<View>(R.id.albumArtPlaceholderExpanded)?.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) || true }

        setupPillAnimations(btnPrevMini, btnPauseMini, btnNextMini, btnPrevExpanded, btnPauseExpanded, btnNextExpanded)

        applyThemeColors()

        val showState = onRequestShowState.invoke(2)
        lastShowLevel = -1
        animateShow(showState)

        val playState = onRequestPlaystate.invoke(Tuple2(false, false))
        updatePlaystate(playState.obj1, playState.obj2)
        val trackInfo = onRequestTrackInfo.invoke(Tuple2(PlaylistSong.emptyData, PlayingMediaInfo.empty))
        updateTrackInfo(trackInfo.obj1, trackInfo.obj2)

        val volumeMuteState = onRequestVolumeMuteState.invoke(false)
        val audioEffectsActiveState = onRequestAudioEffectsActiveState.invoke(false)
        onVolumeMuteChanged(volumeMuteState, audioEffectsActiveState)

        queueNextRefresh(100)
    }

    private fun updatePauseButtonImage(isPlaying: Boolean, wantsPlaying: Boolean) {
        val resId = if (wantsPlaying) R.drawable.ic_ctrl_pause_s else R.drawable.ic_ctrl_play_s
        btnPauseMini?.setImageResource(resId)
        btnPauseExpanded?.setImageResource(resId)
        applyThemeColors()
    }

    fun updatePlaystate(isPlaying: Boolean, wantsPlaying: Boolean) {
        if (!isViewCreated()) return
        updatePauseButtonImage(isPlaying, wantsPlaying)
    }

    fun updateTrackInfo(songData: PlaylistSong.Data, playingMediaInfo: PlayingMediaInfo) {
        if (!isViewCreated()) return

        duration = playingMediaInfo.duration

        currentSongData = songData
        val track = getCurrentOnlineTrack()
        if (track != null) {
            val isLiked = com.aylis.comp.online.managers.LikedTracksManager.isLiked(track.videoId)
            updateLikeButtonUI(isLiked)
            
            val isDownloaded = com.aylis.comp.online.managers.OnlineDownloadManager.isTrackDownloaded(track.videoId)
            updateDownloadButtonUI(isDownloaded)
            
            val progress = com.aylis.comp.online.managers.OnlineDownloadManager.getProgress(track.videoId)
            if (progress != null && !isDownloaded) {
                txtDownloadProgressExpanded?.visibility = View.VISIBLE
                txtDownloadProgressExpanded?.text = "$progress%"
                btnDownloadExpanded?.visibility = View.INVISIBLE
            } else {
                txtDownloadProgressExpanded?.visibility = View.GONE
                btnDownloadExpanded?.visibility = View.VISIBLE
            }
        } else {
            updateLikeButtonUI(false)
            updateDownloadButtonUI(false)
            txtDownloadProgressExpanded?.visibility = View.GONE
            btnDownloadExpanded?.visibility = View.VISIBLE
        }

        setAnimatedText(titleMini, songData.trackName)
        setAnimatedText(titleExpanded, songData.trackName)
        
        titleMini?.isSelected = true
        titleExpanded?.isSelected = true
        
        titleMini?.movementMethod = MyTitleScrollingMovementMethod()
        titleExpanded?.movementMethod = MyTitleScrollingMovementMethod()
        
        artistMini?.text = songData.artistName
        artistExpanded?.text = songData.artistName

        val imageLoadedListener = object : ImageLoadedListener {
            override fun onBitmapLoaded(bitmap: Bitmap?, url00: String?, url0: String?, url1: String?) {
                if (bitmap != null) {
                    val oldBitmap = (imgAlbumArtUnified?.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                    if (oldBitmap != null && oldBitmap != bitmap && imgAlbumArtUnified!!.alpha > 0 && isExpanded) {
                        val tempImg = ShapeableImageView(imgAlbumArtUnified!!.context)
                        tempImg.layoutParams = imgAlbumArtUnified!!.layoutParams
                        tempImg.scaleType = imgAlbumArtUnified!!.scaleType
                        tempImg.shapeAppearanceModel = imgAlbumArtUnified!!.shapeAppearanceModel
                        tempImg.elevation = imgAlbumArtUnified!!.elevation
                        tempImg.setImageBitmap(oldBitmap)
                        tempImg.pivotX = imgAlbumArtUnified!!.pivotX
                        tempImg.pivotY = imgAlbumArtUnified!!.pivotY
                        tempImg.translationX = imgAlbumArtUnified!!.translationX
                        tempImg.translationY = imgAlbumArtUnified!!.translationY
                        tempImg.scaleX = imgAlbumArtUnified!!.scaleX
                        tempImg.scaleY = imgAlbumArtUnified!!.scaleY
                        
                        val parent = imgAlbumArtUnified!!.parent as ViewGroup
                        parent.addView(tempImg, parent.indexOfChild(imgAlbumArtUnified))
                        
                        val slideDist = 200f * lastSwipeDirection
                        tempImg.animate()
                            .translationXBy(-slideDist)
                            .alpha(0f)
                            .setDuration(250)
                            .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                            .withEndAction {
                                parent.removeView(tempImg)
                            }.start()
                        
                        val targetScaleX = imgAlbumArtUnified!!.scaleX
                        val targetScaleY = imgAlbumArtUnified!!.scaleY
                        val targetTransX = imgAlbumArtUnified!!.translationX
                        
                        imgAlbumArtUnified?.setImageBitmap(bitmap)
                        imgAlbumArtUnified?.alpha = 0f
                        imgAlbumArtUnified?.scaleX = targetScaleX * 0.95f
                        imgAlbumArtUnified?.scaleY = targetScaleY * 0.95f
                        imgAlbumArtUnified?.translationX = targetTransX + slideDist
                        
                        imgAlbumArtUnified?.animate()
                            ?.translationX(targetTransX)
                            ?.alpha(1f)
                            ?.scaleX(targetScaleX)
                            ?.scaleY(targetScaleY)
                            ?.setDuration(250)
                            ?.setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                            ?.start()
                    } else {
                        imgAlbumArtUnified?.setImageBitmap(bitmap)
                        imgAlbumArtUnified?.invalidateOutline()
                    }
                    
                    com.aylis.comp.visual.ambient.AmbientManager.updateCover(bitmap)
                    
                    val context = playerUnifiedRoot?.context
                    val expandedLayout = playerUnifiedRoot?.findViewById<View>(R.id.layoutExpandedControls)
                    expandedLayout?.setBackgroundResource(R.drawable.bg_bottom_sheet)
                    
                    val typedValue = android.util.TypedValue()
                    context?.theme?.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
                    if (layoutMiniControls is com.google.android.material.card.MaterialCardView) {
                        (layoutMiniControls as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(typedValue.data)
                    }
                    if (bottomNavCard is com.google.android.material.card.MaterialCardView) {
                        (bottomNavCard as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(typedValue.data)
                    }
                } else {
                    imgAlbumArtUnified?.setImageResource(R.drawable.placeholderart4)
                    
                    com.aylis.comp.visual.ambient.AmbientManager.updateCover(null)
                    
                    val expandedLayout = playerUnifiedRoot?.findViewById<View>(R.id.layoutExpandedControls)
                    expandedLayout?.setBackgroundResource(R.drawable.bg_bottom_sheet)
                    
                    val context = playerUnifiedRoot?.context
                    val typedValue = android.util.TypedValue()
                    context?.theme?.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
                    if (layoutMiniControls is com.google.android.material.card.MaterialCardView) {
                        (layoutMiniControls as com.google.android.material.card.MaterialCardView).setCardBackgroundColor(typedValue.data)
                    }
                }
            }
            override fun setUserObject1(obj1: Any?) {}
        }

        MainActivity.onRequestAlbumArtLarge.invoke(
            AlbumArtRequest(
                songData.videoThumbDataSourceAsStr,
                songData.albumArtPath0Str,
                songData.albumArtPath1Str,
                songData.albumArtGenerateStr
            ),
            imageLoadedListener,
            600, 600
        )

        queueNextRefresh(100)
        applyThemeColors()
    }

    private fun setAnimatedText(textView: TextView?, newText: String) {
        if (textView == null) return
        val oldText = textView.text.toString()
        if (oldText == newText) return

        textView.animate().cancel()
        if (oldText.isEmpty() || oldText == "...") {
            textView.text = newText
            textView.alpha = 0.0f
            textView.animate().alpha(1.0f).setDuration(200).start()
            return
        }

        textView.animate().alpha(0.0f).setDuration(120).withEndAction {
            textView.text = newText
            textView.animate().alpha(1.0f).setDuration(150).start()
        }.start()
    }

    private fun queueNextRefresh(delay: Long) {
        val msg = handler.obtainMessage(MSG_REFRESH)
        handler.removeMessages(MSG_REFRESH)
        handler.sendMessageDelayed(msg, delay)
    }

    private fun refreshNow(): Long {
        val trackPosition = onRequestTrackPosition.invoke(-1L)

        if (trackPosition < 0) return 500

        try {
            val pos = if (posOverride < 0) trackPosition else posOverride
            if (pos >= 0 && duration >= 0) {
                val p = (1000 * pos / duration).toInt()
                
                // Do not update progress if actively dragging
                if (posOverride < 0) {
                    progressMini?.progress = p
                    progressExpanded?.progress = p
                }
                val el = com.aylis.Common.Utils.getDurationStringHMSS((pos / 1000).toInt())
                val tot = com.aylis.Common.Utils.getDurationStringHMSS((duration / 1000).toInt())
                
                txtTimeElapsedExpanded?.text = el
                txtTimeTotalExpanded?.text = tot
            } else {
                progressMini?.progress = 1000
                progressExpanded?.progress = 1000
                txtTimeElapsedExpanded?.text = "0:00"
                txtTimeTotalExpanded?.text = "0:00"
            }

            val remaining = 1000 - (pos % 1000)
            var width = progressExpanded?.width ?: 320
            if (width == 0) width = 320
            val smoothrefreshtime = duration / width

            return if (smoothrefreshtime > remaining) remaining
            else if (smoothrefreshtime < 20) 20 else smoothrefreshtime
        } catch (ignored: Exception) {}

        return 500
    }

    fun animateShow(showLevel: Int) {
        if (!isViewCreated()) return
        var sl = showLevel
        if (sl == 1) sl = 2

        if (lastShowLevel != sl) {
            UtilsUI.dismissSafe(volumePopupWindowSingleton?.get())
            volumePopupWindowSingleton?.clear()
            
            UtilsUI.dismissSafe(ctrlOverflowPopupWindowSingleton?.get())
            ctrlOverflowPopupWindowSingleton?.clear()

            UtilsUI.dismissSafe(overflowPopupWindowSingleton?.get())
        }
        lastShowLevel = sl

        val root = layoutMediaControls ?: return
        val shortAnimTime = root.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        if (sl == 0) {
            root.animate().translationY(root.height.toFloat()).alpha(0.0f).setDuration(shortAnimTime)
                .withEndAction { root.visibility = View.INVISIBLE }
        } else if (sl == 1 || sl == 2) {
            com.aylis.MainActivity.onHideBottomNav.invoke(false)
            root.visibility = View.VISIBLE
            root.animate().translationY(0f).alpha(1.0f).setDuration(shortAnimTime).withEndAction(null).start()
        }
    }

    fun onVolumeMuteChanged(volumeMuteState: Boolean) {
        if (!isViewCreated()) return
        val audioEffectsActiveState = onRequestAudioEffectsActiveState.invoke(false)
        onVolumeMuteChanged(volumeMuteState, audioEffectsActiveState)
    }

    fun onAudioEffectsActiveChanged(state: Boolean) {
        if (!isViewCreated()) return
        val muteState = onRequestVolumeMuteState.invoke(false)
        onVolumeMuteChanged(muteState, state)
    }

    private fun onVolumeMuteChanged(volumeMuteState: Boolean, audioEffectsActiveState: Boolean) {
        if (volumeMuteState) {
            btnVolumeExpanded?.setImageResource(R.drawable.ic_mute_s)
            btnVolumeExpanded?.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E53935"))
        } else {
            btnVolumeExpanded?.setImageResource(R.drawable.ic_volume)
            val context = playerUnifiedRoot?.context ?: return
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
            btnVolumeExpanded?.imageTintList = androidx.core.content.ContextCompat.getColorStateList(context, typedValue.resourceId)
        }
    }

    fun onRepeatModeChanged(repeatMode: Int) {
        val context = playerUnifiedRoot?.context ?: return
        val typedValue = android.util.TypedValue()
        
        when (repeatMode) {
            1 -> { // REPEAT_CURRENT (One)
                btnRepeatExpanded?.setImageResource(R.drawable.ic_repeat_one)
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            }
            2 -> { // REPEAT_ALL
                btnRepeatExpanded?.setImageResource(R.drawable.ic_repeat_pl)
                context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            }
            else -> { // REPEAT_NONE
                btnRepeatExpanded?.setImageResource(R.drawable.ic_repeat_pl)
                context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
            }
        }
        btnRepeatExpanded?.imageTintList = androidx.core.content.ContextCompat.getColorStateList(context, typedValue.resourceId)
    }

    fun onShuffleModeChanged(shuffleMode: Int) {
        val context = playerUnifiedRoot?.context ?: return
        val typedValue = android.util.TypedValue()
        
        if (shuffleMode != 0) { // SHUFFLE_NORMAL
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        } else { // SHUFFLE_NONE
            context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        }
        btnShuffleExpanded?.imageTintList = androidx.core.content.ContextCompat.getColorStateList(context, typedValue.resourceId)
    }

    fun onMusicSysChanged(musicSysIndex: Int) {
        val popup3 = ctrlOverflowPopupWindowSingleton?.get()
        if (popup3?.isShowing == true) popup3.onMusicSysChanged(musicSysIndex)
    }

    fun onEqStateChanged(eqState: Boolean) {
        val popup3 = volumePopupWindowSingleton?.get()
        if (popup3?.isShowing == true) popup3.onEqStateChanged(eqState)
    }

    fun applyThemeColors() {
        // Obsolete: dynamic colors are now fully managed by standard Material themes
    }

    private fun setupPillAnimations(vararg buttons: View?) {
        val touchListener = View.OnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    animateWeight(view, 1.5f)
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    animateWeight(view, 1.0f)
                }
            }
            false
        }
        buttons.forEach { it?.setOnTouchListener(touchListener) }
    }

    private fun animateWeight(view: View, targetWeight: Float) {
        val params = view.layoutParams as? android.widget.LinearLayout.LayoutParams ?: return
        val animator = android.animation.ValueAnimator.ofFloat(params.weight, targetWeight)
        animator.duration = 300
        animator.addUpdateListener { anim ->
            params.weight = anim.animatedValue as Float
            view.layoutParams = params
        }
        animator.start()
    }

    private fun getCurrentOnlineTrack(): com.aylis.comp.online.repository.OnlineTrack? {
        val data = currentSongData ?: return null
        val dataSourceStr = data.dataSource?.toString() ?: ""
        if (!dataSourceStr.startsWith("ytsearch://")) return null
        
        val videoId = dataSourceStr.removePrefix("ytsearch://")
        
        return com.aylis.comp.online.repository.OnlineTrack(
            videoId = videoId,
            title = data.trackName ?: "",
            artist = data.artistName ?: "",
            thumbnail = data.thumbnail ?: data.albumArtPath0Str ?: ""
        )
    }

    private fun updateLikeButtonUI(isLiked: Boolean) {
        val context = playerUnifiedRoot?.context ?: return
        if (isLiked) {
            val redColor = android.graphics.Color.parseColor("#E53935")
            btnLikeExpanded?.imageTintList = android.content.res.ColorStateList.valueOf(redColor)
            btnLikeExpanded?.setImageResource(R.drawable.ic_favorite)
        } else {
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            if (typedValue.resourceId != 0) {
                btnLikeExpanded?.imageTintList = androidx.core.content.ContextCompat.getColorStateList(context, typedValue.resourceId)
            } else {
                btnLikeExpanded?.imageTintList = android.content.res.ColorStateList.valueOf(typedValue.data)
            }
            btnLikeExpanded?.setImageResource(R.drawable.ic_favorite_border)
        }
    }
    
    private fun updateDownloadButtonUI(isDownloaded: Boolean) {
        val context = layoutMediaControls?.context ?: return
        val typedValue = android.util.TypedValue()
        if (isDownloaded) {
            context.theme.resolveAttribute(android.R.attr.colorAccent, typedValue, true)
            btnDownloadExpanded?.setImageResource(R.drawable.ic_check)
        } else {
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
            btnDownloadExpanded?.setImageResource(R.drawable.ic_download)
        }
        
        if (typedValue.type >= android.util.TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= android.util.TypedValue.TYPE_LAST_COLOR_INT) {
            btnDownloadExpanded?.imageTintList = android.content.res.ColorStateList.valueOf(typedValue.data)
        } else {
            btnDownloadExpanded?.imageTintList = androidx.core.content.ContextCompat.getColorStateList(context, typedValue.resourceId)
        }
    }
}
