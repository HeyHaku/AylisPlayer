

package com.aylis.comp.MediaControlsUI;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import com.aylis.Common.UtilsUI;
import com.aylis.comp.playback.MediaPlaybackServiceDefs;
import com.aylis.R;

class ThreeDotPopupWindow extends PopupWindow {

    private Handler handler;
    private ImageButton shuffleButton, repeatOnceButton, repeatAllButton;
    private ImageButton musicSys0Button, musicSys1Button;

    public ThreeDotPopupWindow(View anchor) {
        super(anchor.getContext(), null, 0, R.style.MyListPopupWindowDarkStyle);

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == 0) {
                    UtilsUI.dismissSafe(ThreeDotPopupWindow.this);
                }
                return false;
            }
        });

        View rootView = View.inflate(anchor.getContext(), R.layout.player_media_controls_overflow_dialog, null);

        shuffleButton = (ImageButton) rootView.findViewById(R.id.btnShuffle);
        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int mode = MediaControlsUI.onRequestShuffleMode.invoke(MediaPlaybackServiceDefs.SHUFFLE_NONE);
                int shuffleMode = mode;

                if (mode == MediaPlaybackServiceDefs.SHUFFLE_NONE)
                    shuffleMode = MediaPlaybackServiceDefs.SHUFFLE_NORMAL;
                else if (mode == MediaPlaybackServiceDefs.SHUFFLE_NORMAL)
                    shuffleMode = MediaPlaybackServiceDefs.SHUFFLE_NONE;

                MediaControlsUI.onSetShuffleMode.invoke(shuffleMode);

            }
        });

        repeatOnceButton = (ImageButton) rootView.findViewById(R.id.btnRepeatOnce);
        repeatOnceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatMode = MediaControlsUI.onRequestRepeatMode.invoke(0);

                if (repeatMode != MediaPlaybackServiceDefs.REPEAT_CURRENT)
                    repeatMode = MediaPlaybackServiceDefs.REPEAT_CURRENT;
                else
                    repeatMode = MediaPlaybackServiceDefs.REPEAT_NONE;

                MediaControlsUI.onSetRepeatMode.invoke(repeatMode);
            }
        });

        repeatAllButton = (ImageButton) rootView.findViewById(R.id.btnRepeatAll);
        repeatAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int repeatMode = MediaControlsUI.onRequestRepeatMode.invoke(0);

                if (repeatMode != MediaPlaybackServiceDefs.REPEAT_ALL)
                    repeatMode = MediaPlaybackServiceDefs.REPEAT_ALL;
                else
                    repeatMode = MediaPlaybackServiceDefs.REPEAT_NONE;

                MediaControlsUI.onSetRepeatMode.invoke(repeatMode);
            }
        });

        musicSys0Button = (ImageButton) rootView.findViewById(R.id.btnPlaybackEngine0);
        musicSys0Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControlsUI.onSelectMusicSysAction.invoke(0);
            }
        });

        musicSys1Button = (ImageButton) rootView.findViewById(R.id.btnPlaybackEngine1);
        musicSys1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MediaControlsUI.onSelectMusicSysAction.invoke(1);
            }
        });

        android.widget.SeekBar seekBarVolume = (android.widget.SeekBar) rootView.findViewById(R.id.seekBarVolume);
        com.aylis.Common.Tuple2<Integer, Integer> volumeState = MediaControlsUI.onRequestAudioVolumeState.invoke(new com.aylis.Common.Tuple2<>(0, 100));
        seekBarVolume.setMax(volumeState.obj2);
        seekBarVolume.setProgress(volumeState.obj1);

        seekBarVolume.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MediaControlsUI.onSetAudioVolume.invoke(progress, seekBar.getMax());
                }
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });

        this.setContentView(rootView);
        this.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        setWidth(WindowManager.LayoutParams.WRAP_CONTENT);

        setOutsideTouchable(true);
        setFocusable(true);

        int musicSysIndex = MediaControlsUI.onRequestMusicSystemIndex.invoke(-1);
        onMusicSysChanged(musicSysIndex);
        onRepeatModeChanged(MediaControlsUI.onRequestRepeatMode.invoke(0));
        onShuffleModeChanged(MediaControlsUI.onRequestShuffleMode.invoke(MediaPlaybackServiceDefs.SHUFFLE_NONE));

        int yoffset = (int) anchor.getResources().getDimension(R.dimen.player_controls_volume_popup_offset);
        int xoffset = (int) anchor.getResources().getDimension(R.dimen.player_controls_volume_popup_offset_x);
        Rect displayFrame = new Rect();
        anchor.getWindowVisibleDisplayFrame(displayFrame);
        int displayHeight = displayFrame.height();
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);

        this.showAtLocation(anchor, Gravity.BOTTOM | Gravity.START, location[0] - xoffset, (displayHeight - location[1]) + yoffset);

    }

    public void onRepeatModeChanged(int repeatMode) {
        int activeColor = UtilsUI.getAttrColor(repeatAllButton, com.google.android.material.R.attr.colorPrimary);
        int inactiveColor = android.graphics.Color.parseColor("#757575");

        if (repeatMode == MediaPlaybackServiceDefs.REPEAT_CURRENT) {
            repeatOnceButton.setColorFilter(activeColor);
            repeatAllButton.setColorFilter(inactiveColor);
        } else if (repeatMode == MediaPlaybackServiceDefs.REPEAT_ALL) {
            repeatOnceButton.setColorFilter(inactiveColor);
            repeatAllButton.setColorFilter(activeColor);
        } else {
            repeatOnceButton.setColorFilter(inactiveColor);
            repeatAllButton.setColorFilter(inactiveColor);
        }
    }

    public void onShuffleModeChanged(int shuffleMode) {
        int activeColor = UtilsUI.getAttrColor(shuffleButton, com.google.android.material.R.attr.colorPrimary);
        int inactiveColor = android.graphics.Color.parseColor("#757575");

        if (shuffleMode != MediaPlaybackServiceDefs.SHUFFLE_NONE)
            shuffleButton.setColorFilter(activeColor);
        else
            shuffleButton.setColorFilter(inactiveColor);
    }

    public void onMusicSysChanged(int musicSysIndex) {
        int activeColor = UtilsUI.getAttrColor(musicSys0Button, com.google.android.material.R.attr.colorPrimary);
        int inactiveColor = android.graphics.Color.parseColor("#757575");

        if (musicSysIndex == 0) {
            musicSys0Button.setColorFilter(activeColor);
            musicSys1Button.setColorFilter(inactiveColor);
        } else {
            musicSys0Button.setColorFilter(inactiveColor);
            musicSys1Button.setColorFilter(activeColor);
        }
    }
}
