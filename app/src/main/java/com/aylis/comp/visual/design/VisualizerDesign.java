

package com.aylis.comp.visual.design;

import android.os.Message;

import com.aylis.Design.PlaybackDesign;
import com.aylis.Design.PlaybackControlsDesign;
import android.app.Activity;
import androidx.fragment.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Handler;
import android.view.SurfaceHolder;
import com.aylis.AppPermissions;
import com.aylis.Common.Events.WeakEvent;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.Events.WeakEventR1;
import com.aylis.Common.Events.WeakEventR2;
import com.aylis.Common.Events.WeakEventR3;
import com.aylis.Common.Events.WeakEventR4;
import com.aylis.Common.Tuple2;
import com.aylis.Common.Utils;
import com.aylis.comp.AlbumArt.AlbumArtCore;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AlbumArt.ImageLoadedListener;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.Common.IItemIdentifier;
import com.aylis.comp.visual.ui.CustomizeVisDialog;
import com.aylis.comp.visual.core.playback.AudioFrameData;
import com.aylis.comp.playback.EventsPlaybackService;
import com.aylis.comp.playback.IMediaPlayerCore;
import com.aylis.comp.playback.MediaPlaybackService;
import com.aylis.comp.playback.PlayingMediaInfo;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.comp.visual.ui.FragmentVisualizer;
import com.aylis.comp.visual.ui.ChooseVisualizerDialog;
import com.aylis.comp.visual.ui.VisualizerThemeInfo;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.core.Elements.RootElement;
import com.aylis.comp.visual.core.VisualizerViewCore;
import com.aylis.ContextData;
import com.aylis.EventsGlobal.EventsGlobalTextNotifier;
import com.aylis.MainActivity;
import com.aylis.PlayerCore;
import com.aylis.R;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class VisualizerDesign {

    private volatile WeakReference<SurfaceHolder> surfaceHolder = new WeakReference<>(null);
    private WeakReference<VisualizerViewCore> visualizerSurfaceView = new WeakReference<>(null);
    private volatile float videoWidthHeightRatio = 1.0f;
    private boolean uiNeedShowVisual = true;
    private List<Object> listenerRefHolder = new LinkedList<>();
    private Handler threadHandler = new Handler();

    private static VisualizerDesign instance = null;
    public static VisualizerDesign s() {
        return instance;
    }

    public RootElement getActiveThemeObject() {
        int themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId);
        return getActiveThemeObject(themeId);
    }

    private RootElement getActiveThemeObject(int themeId) {
        com.aylis.comp.visual.scene.VisualizerScene scene = AppPreferences.createOrGetInstance().getPrefThemeScene(themeId);
        if (scene != null) {
            return com.aylis.comp.visual.scene.SceneBuilder.INSTANCE.buildFromScene(themeId, scene);
        }
        RootElement root = VisualizerThemes.s().getThemeObject(themeId);
        if(root != null) {
            Element.CustomizationList customizationList = AppPreferences.createOrGetInstance().getPrefThemeCustomizationData(root.getIdentifier());
            applyThemeCustomizationData(root, customizationList);
        }
        return root;
    }

    private final HandheldMotion handheldMotionSmooth = new HandheldMotion(HandheldMotion.Jarles_Presets_MoreMovement_Smoothest);
    private final HandheldMotion handheldMotionLotOfShake = new HandheldMotion(HandheldMotion.Jarles_Presets_LotsOshake);
    private PointF shakePointSmooth = new PointF(1f, 1f);
    private com.aylis.comp.visual.core.Elements.Base.MeasureLogic measureLogic;

    public VisualizerDesign() {
        measureLogic = new com.aylis.comp.visual.core.Elements.Base.MeasureLogic(
                handheldMotionSmooth,
                handheldMotionLotOfShake,
                shakePointSmooth
        );

        instance = this;

        MediaPlaybackService.onDisplayMetaDataStateChanged.subscribeWeak(new WeakEvent4.Handler<PlaylistSong, IItemIdentifier, PlaylistSong.Data, PlayingMediaInfo>() {
            @Override
            public void invoke(PlaylistSong currentTrack, IItemIdentifier _currentItemIdent, PlaylistSong.Data currentTrackData, PlayingMediaInfo playingMediaInfo) {

            }
        }, listenerRefHolder);
        MediaPlaybackService.onRequestVideoScalingMode.subscribeWeak(new WeakEventR.Handler<Integer>() {
            @Override
            public Integer invoke() {
                return getPlayerbackVideoScalingMode();
            }
        }, listenerRefHolder);
        MediaPlaybackService.onRequestVideoSurfaceHolder.subscribeWeak(new WeakEventR.Handler<SurfaceHolder>() {
            @Override
            public SurfaceHolder invoke() {
                return surfaceHolder.get();
            }
        }, listenerRefHolder);
        MediaPlaybackService.onVideoSizeChanged.subscribeWeak(new WeakEvent3.Handler<Integer, Integer, Float>() {
            @Override
            public void invoke(Integer width, Integer height, Float widthHeightRatio) {
                videoWidthHeightRatio = widthHeightRatio;
            }
        }, listenerRefHolder);

        FragmentVisualizer.onSurfaceCreated.subscribeWeak(new WeakEvent1.Handler<VisualizerViewCore>() {
            @Override
            public void invoke(VisualizerViewCore visualizerView) {
                visualizerSurfaceView = new WeakReference<>(visualizerView);

                int themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId);

                RootElement visualizerThemeElementRoot = getActiveThemeObject(themeId);

                if(visualizerThemeElementRoot != null) {
                    visualizerView.setThemeElements(visualizerThemeElementRoot);
                }
            }
        }, listenerRefHolder);

        VisualizerViewCore.onRequestsSoundVisualizationData.subscribeWeak(new WeakEventR1.Handler<AudioFrameData, AudioFrameData>() {
            @Override
            public AudioFrameData invoke(final AudioFrameData outResult) {
                boolean useGlobalSession = AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_visualizerUseGlobalSession);
                return EventsPlaybackService.Receive.getVisualizationData.invoke(outResult, useGlobalSession, null);
            }
        }, listenerRefHolder);

        VisualizerViewCore.onRequestMeasureText.subscribeWeak(new WeakEventR2.Handler<String, VisualizerViewCore, String>() {
            @Override
            public String invoke(String val, VisualizerViewCore visualizerViewCore) {
                if (val == null) return "";
                if (val.length() > 0 && val.charAt(0) != '$') return val;

                if ("$timeCurrent".equals(val)) {
                    long trackPosition = com.aylis.Design.PlaybackDesign.trackPosition;
                    return Utils.getDurationStringHMSS((int) (trackPosition / 1000));
                } else if ("$trackPositionMs".equals(val)) {
                    return String.valueOf(com.aylis.Design.PlaybackDesign.trackPosition);
                } else if ("$timeLength".equals(val)) {
                    long duration = (com.aylis.Design.PlaybackDesign.playingMediaInfo != null) ? com.aylis.Design.PlaybackDesign.playingMediaInfo.duration : 0;
                    return Utils.getDurationStringHMSS((int) (duration / 1000));
                } else if ("$durationSec".equals(val)) {
                    long duration = (com.aylis.Design.PlaybackDesign.playingMediaInfo != null) ? com.aylis.Design.PlaybackDesign.playingMediaInfo.duration : 0;
                    return String.valueOf(duration / 1000);
                } else if ("$artist".equals(val)) {
                    PlaylistSong.Data songData = com.aylis.Design.PlaybackControlsDesign.fieldsongData;
                    return songData.hasValidArtistName() ? songData.artistName : "";
                } else if ("$title".equals(val)) {
                    return com.aylis.Design.PlaybackControlsDesign.fieldsongData.trackName;
                } else if ("$album".equals(val)) {
                    return com.aylis.Design.PlaybackControlsDesign.fieldsongData.albumName;
                } else if ("$fps".equals(val)) {
                    return "" + visualizerViewCore.getFps();
                } else if ("$frametime".equals(val)) {
                    return "" + visualizerViewCore.getFrameTimeMs();
                }

                return val;
            }
        }, listenerRefHolder);

        VisualizerViewCore.onRequestMeasureVec2f.subscribeWeak(new WeakEventR4.Handler<String, PointF, PointF, Float, PointF>() {
            @Override
            public PointF invoke(String val, PointF argVec, PointF lastMeasured, Float frameDataRmsValue) {
                VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                if (surfaceView != null) {
                    measureLogic.updatePlaybackState(
                        com.aylis.Design.PlaybackDesign.trackPosition / 1000.0f,
                        (com.aylis.Design.PlaybackDesign.playingMediaInfo != null) ? (com.aylis.Design.PlaybackDesign.playingMediaInfo.duration / 1000.0f) : 1.0f,
                        com.aylis.Design.PlaybackDesign.isPlaying
                    );
                    return measureLogic.process(val, argVec, lastMeasured, frameDataRmsValue);
                }
                return argVec != null ? argVec : new PointF(0.0f, 0.0f);
            }
        }, listenerRefHolder);

        VisualizerViewCore.onRequestsAlbumArtPath.subscribeWeak(new WeakEventR.Handler<AlbumArtRequest>() {
            @Override
            public AlbumArtRequest invoke() {
                PlaylistSong.Data songData = com.aylis.Design.PlaybackControlsDesign.fieldsongData;
                if (songData == PlaylistSong.emptyData)
                    return null;

                return new AlbumArtRequest(songData.getVideoThumbDataSourceAsStr(), songData.getAlbumArtPath0Str(), songData.getAlbumArtPath1Str(), songData.getAlbumArtGenerateStr());
            }
        }, listenerRefHolder);

        VisualizerViewCore.onRequestAlbumArtPathAndBitmap.subscribeWeak(new WeakEvent4.Handler<ImageLoadedListener, Integer, Integer, AlbumArtRequest>() {
            @Override
            public void invoke(
                    final ImageLoadedListener loadedListener,
                    final Integer targetBoundsWidth,
                    final Integer targetBoundsHeight,
                    final AlbumArtRequest albumArtRequest) {

                ImageLoadedListener loadedListener2 = new ImageLoadedListener() {

                    Object object1;

                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, final String url00, final String url0, final String url1) {

                        VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                        if (surfaceView != null)
                            surfaceView.queueEvent(new Runnable() {
                                @Override
                                public void run() {
                                    loadedListener.onBitmapLoaded(bitmap, url00, url0, url1);
                                }
                            });
                    }

                    @Override
                    public void setUserObject1(Object obj1) {
                        object1 = obj1;
                    }
                };

                loadedListener.setUserObject1(loadedListener2);

                AlbumArtCore albumArtCore = AlbumArtCore.getInstance();
                if (albumArtCore != null) {
                    albumArtCore.loadASyncAlbumArtLarge(
                            albumArtRequest.videoThumbDataSource,
                            albumArtRequest.path0,
                            albumArtRequest.path1,
                            albumArtRequest.genStr,
                            loadedListener2,
                            targetBoundsWidth,
                            targetBoundsHeight
                    );
                }

            }
        }, listenerRefHolder);

        ChooseVisualizerDialog.Companion.getOnRequestSkinThemePresetList().subscribeWeak(new WeakEventR1.Handler<List<VisualizerThemeInfo>, Integer>() {
            @Override
            public Integer invoke(List<VisualizerThemeInfo> listOut) {
                List<Tuple2<VisualizerThemeInfo, VisualizerThemes.IVisualizerFactory>> themes = VisualizerThemes.s().getThemesList();

                for (Tuple2<VisualizerThemeInfo, VisualizerThemes.IVisualizerFactory> t : themes)
                    listOut.add(t.obj1);

                return AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId);
            }
        }, listenerRefHolder);

        ChooseVisualizerDialog.Companion.getOnSkinThemePresetSelected().subscribeWeak(new WeakEvent1.Handler<VisualizerThemeInfo>() {
            @Override
            public void invoke(VisualizerThemeInfo presetInfo) {
                AppPreferences.createOrGetInstance().setInt(AppPreferences.PREF_Int_visualizerThemeId, presetInfo.id, true);
                AppPreferences.createOrGetInstance().setBool(AppPreferences.PREF_Bool_visualPreferShowVideoContent, false);
            }
        }, listenerRefHolder);

        VisualizerViewCore.onRequestSelectedSkinThemePreset.subscribeWeak(new WeakEventR.Handler<RootElement>() {
            @Override
            public RootElement invoke() {
                int themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId);
                return getActiveThemeObject(themeId);
            }
        }, listenerRefHolder);

        ChooseVisualizerDialog.Companion.getOnShowVideoContentAction().subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                AppPreferences.createOrGetInstance().setInt(AppPreferences.PREF_Int_visualizerThemeId,
                        AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId), true);

                AppPreferences.createOrGetInstance().setBool(AppPreferences.PREF_Bool_visualPreferShowVideoContent, true);
            }
        }, listenerRefHolder);

        FragmentVisualizer.onRequestShowVideoContentState.subscribeWeak(new WeakEventR.Handler<Boolean>() {
            @Override
            public Boolean invoke() {
                return AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_visualPreferShowVideoContent);
            }
        }, listenerRefHolder);

        FragmentVisualizer.onToggleVideoScalingMode.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                int mode = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_videoScalingMode);

                if (mode == 1) mode = 2;
                else if (mode == 2) mode = 3;
                else if (mode == 3) mode = 1;
                else mode = 1;

                AppPreferences.createOrGetInstance().setInt(AppPreferences.PREF_Int_videoScalingMode, mode);

                Context context = PlayerCore.s().getAppContext();
                if (context != null) {

                    String msg;

                    if (mode == 1) {
                        msg = context.getResources().getString(R.string.video_scaling_fit);
                    } else if (mode == 2) {
                        msg = context.getResources().getString(R.string.video_scaling_crop);
                    } else {
                        msg = context.getResources().getString(R.string.video_scaling_stretch);
                    }

                    EventsGlobalTextNotifier.onTextMsg.invoke(msg);
                }

            }
        }, listenerRefHolder);

        FragmentVisualizer.onRequestVideoScalingMode.subscribeWeak(new WeakEventR.Handler<Integer>() {
            @Override
            public Integer invoke() {
                return AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_videoScalingMode);
            }
        }, listenerRefHolder);

        FragmentVisualizer.onRequestVideoWidthHeightRatio.subscribeWeak(new WeakEventR.Handler<Float>() {
            @Override
            public Float invoke() {
                return getSurfaceVideoSize(videoWidthHeightRatio);
            }
        }, listenerRefHolder);

        FragmentVisualizer.onToggleVisualPreferShowContent.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                AppPreferences.createOrGetInstance().setInt(AppPreferences.PREF_Int_visualizerThemeId,
                        AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId), true);
                AppPreferences.createOrGetInstance().toggleBool(AppPreferences.PREF_Bool_visualPreferShowVideoContent);

                Context context = PlayerCore.s().getAppContext();
                if (context != null) {
                    if (AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_visualPreferShowVideoContent)) {
                        final String message = context.getResources().getString(R.string.switched_to_video);
                        EventsGlobalTextNotifier.onTextMsg.invoke(message);

                    } else {
                        final String message = context.getResources().getString(R.string.switched_to_visualizer);
                        EventsGlobalTextNotifier.onTextMsg.invoke(message);

                    }
                }
            }
        }, listenerRefHolder);

        FragmentVisualizer.onVideoSurfaceHolderCreated.subscribeWeak(new WeakEvent1.Handler<SurfaceHolder>() {
            @Override
            public void invoke(final SurfaceHolder holder) {
                surfaceHolder = new WeakReference<>(holder);

                EventsPlaybackService.Receive.setVideoSurfaceHolder.invoke(holder);
            }
        }, listenerRefHolder);

        FragmentVisualizer.onVideoSurfaceHolderDestroyed.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                surfaceHolder = new WeakReference<>(null);
            }
        }, listenerRefHolder);

        MainActivity.onViewPagerPageSelected.subscribeWeak(new WeakEvent2.Handler<Integer, Activity>() {
            @Override
            public void invoke(Integer page, Activity activity) {
                if (page == MainActivity.VISUAL_PAGE_INDEX) {

                    AppPermissions.is_RecordAudio_PermissionGranted(activity, activity);

                    uiNeedShowVisual = true;

                    boolean showVideoContent = AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_visualPreferShowVideoContent);
                    FragmentVisualizer frag2 = MainActivity.getFragmentVisualizerInstance();
                    if (frag2 != null) frag2.updateSurfaceVisibility(true, showVideoContent);

                } else {
                    uiNeedShowVisual = false;

                    boolean showVideoContent = AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_visualPreferShowVideoContent);
                    FragmentVisualizer frag2 = MainActivity.getFragmentVisualizerInstance();
                    if (frag2 != null) frag2.updateSurfaceVisibility(false, showVideoContent);
                }

            }
        }, listenerRefHolder);

        FragmentVisualizer.onUIComponentNeedChanged.subscribeWeak(new WeakEvent1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean need) {
                uiNeedShowVisual = need;
            }
        }, listenerRefHolder);

        FragmentVisualizer.onRequestUIComponentNeedChangedValue.subscribeWeak(new WeakEventR.Handler<Boolean>() {
            @Override
            public Boolean invoke() {
                return uiNeedShowVisual;
            }
        }, listenerRefHolder);

        FragmentVisualizer.onVideoElementInteracted.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {

                MainActivity mainActivity = MainActivity.getInstance();
                if (mainActivity != null) {
                    if (mainActivity.currentFragmentPage == MainActivity.VISUAL_PAGE_INDEX) {
                        mainActivity.toggleShowControls(mainActivity.currentFragmentPage);

                    }
                }
            }
        }, listenerRefHolder);

        FragmentVisualizer.onCustomizeAction.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                final VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                if (surfaceView != null)
                    surfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {

                            final Element.CustomizationList customization = new Element.CustomizationList();
                            final int rootIdentifier = surfaceView.readThemeCustomizationData(customization);

                            surfaceView.post(new Runnable() {
                                @Override
                                public void run() {

                                    if (rootIdentifier >= 0) {
                                        FragmentVisualizer FragmentVisualizer = MainActivity.getFragmentVisualizerInstance();
                                        if (FragmentVisualizer != null)
                                            FragmentVisualizer.showCustomizationMenu(new Tuple2<>(rootIdentifier, customization));
                                    }
                                }
                            });
                        }
                    });
            }
        }, listenerRefHolder);

        FragmentVisualizer.onPickElementAction.subscribeWeak(new WeakEvent4.Handler<ContextData, Integer, Element.CustomizationList, Integer>() {
            @Override
            public void invoke(ContextData contextData, Integer rootIdentifier, Element.CustomizationList customizationList, Integer colorIndex) {
                FragmentManager fragmentManager = contextData.getFragmentManager();
                if (fragmentManager != null) {
                    VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                    if (surfaceView != null) {
                        RootElement root = surfaceView.getThemeElements();
                        if (root != null) {
                            com.aylis.comp.visual.scene.VisualizerScene scene = com.aylis.comp.visual.scene.SceneBuilder.INSTANCE.exportToScene(root);
                            AppPreferences.createOrGetInstance().savePrefThemeScene(rootIdentifier, scene);
                        }
                    }
                    CustomizeVisDialog.createAndShowCustomizeVisDialog(fragmentManager, rootIdentifier, customizationList, colorIndex);
                }
            }
        }, listenerRefHolder);

        FragmentVisualizer.onResetAction.subscribeWeak(new WeakEvent3.Handler<ContextData, Integer, Element.CustomizationList>() {
            @Override
            public void invoke(ContextData contextData, Integer rootIdentifier, Element.CustomizationList customizationList) {

                int themeId = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_visualizerThemeId);

                AppPreferences.createOrGetInstance().savePrefThemeScene(themeId, null);

                RootElement visualizerThemeElementRoot = VisualizerThemes.s().getThemeObject(themeId);

                final Element.CustomizationList readCustomization = new Element.CustomizationList();
                final int readRootIdentifier = visualizerThemeElementRoot.readThemeCustomizationData(readCustomization);

                if(visualizerThemeElementRoot != null) {

                    VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                    if (surfaceView != null)
                        surfaceView.setThemeElements(visualizerThemeElementRoot);
                }

                AppPreferences.createOrGetInstance().savePrefThemeCustomizationData(readRootIdentifier, readCustomization);

            }
        }, listenerRefHolder);

        CustomizeVisDialog.onPickedColor.subscribeWeak(new WeakEvent4.Handler<Integer, Element.CustomizationList, Integer, WeakEvent2<Integer, Element.CustomizationList>>() {
            @Override
            public void invoke(final Integer rootIdentifier, final Element.CustomizationList customizationList, Integer colorIndex, final WeakEvent2<Integer  , Element.CustomizationList  > onCustomStructureChanged) {

                final VisualizerViewCore surfaceView = visualizerSurfaceView.get();

                if (surfaceView != null) {

                    final Element.CustomizationList customizationClone = customizationList.createClone();
                    final int selectedIndex = colorIndex != null ? colorIndex : -1;

                    surfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {

                            surfaceView.setThemeCustomizationData(rootIdentifier, customizationClone, selectedIndex);

                            final Element.CustomizationList readCustomization = new Element.CustomizationList();
                            final int rootIdentifier = surfaceView.readThemeCustomizationData(readCustomization);
                            if (rootIdentifier >= 0) {
                                surfaceView.post(new Runnable() {
                                    @Override
                                    public void run() {

                                        if (onCustomStructureChanged != null)
                                            onCustomStructureChanged.invoke(rootIdentifier, readCustomization);
                                    }
                                });
                            }
                        }
                    });
                }
            }
        }, listenerRefHolder);

        CustomizeVisDialog.onFinishedPickingColor.subscribeWeak(new WeakEvent3.Handler<Integer, Element.CustomizationList, Integer>() {
            @Override
            public void invoke(Integer rootIdentifier, Element.CustomizationList customizationList, Integer colorIndex) {
                AppPreferences.createOrGetInstance().savePrefThemeCustomizationData(rootIdentifier, customizationList);

                VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                if (surfaceView != null) {
                    surfaceView.triggerSaveIndicator();
                    RootElement root = surfaceView.getThemeElements();
                    if (root != null) {
                        com.aylis.comp.visual.scene.VisualizerScene scene = com.aylis.comp.visual.scene.SceneBuilder.INSTANCE.exportToScene(root);
                        AppPreferences.createOrGetInstance().savePrefThemeScene(rootIdentifier, scene);
                    }
                }
            }
        }, listenerRefHolder);

        CustomizeVisDialog.onRequestSaveScene.subscribeWeak(new WeakEvent1.Handler<Integer>() {
            @Override
            public void invoke(Integer rootIdentifier) {
                VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                if (surfaceView != null) {
                    RootElement root = surfaceView.getThemeElements();
                    if (root != null) {
                        com.aylis.comp.visual.scene.VisualizerScene scene = com.aylis.comp.visual.scene.SceneBuilder.INSTANCE.exportToScene(root);
                        AppPreferences.createOrGetInstance().savePrefThemeScene(rootIdentifier, scene);
                    }
                }
            }
        }, listenerRefHolder);

        AppPreferences.onThemeSceneChanged.subscribeWeak(new WeakEvent1.Handler<Integer>() {
            @Override
            public void invoke(Integer themeId) {
                RootElement visualizerThemeElementRoot = getActiveThemeObject(themeId);
                if(visualizerThemeElementRoot != null) {
                    VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                    if (surfaceView != null)
                        surfaceView.setThemeElements(visualizerThemeElementRoot);
                }
            }
        }, listenerRefHolder);

        AppPreferences.onIntPreferenceChanged.subscribeWeak(new WeakEvent3.Handler<Integer, Integer, Boolean>() {
            @Override
            public void invoke(Integer preference, Integer value, Boolean userForce) {
                if (preference == AppPreferences.PREF_Int_visualizerThemeId && userForce) {

                    int themeId = value;

                    RootElement visualizerThemeElementRoot = getActiveThemeObject(themeId);
                    if(visualizerThemeElementRoot != null) {
                        VisualizerViewCore surfaceView = visualizerSurfaceView.get();
                        if (surfaceView != null)
                            surfaceView.setThemeElements(visualizerThemeElementRoot);
                    }

                } else if (preference == AppPreferences.PREF_Int_videoScalingMode) {

                    final int videoScaling = getPlayerbackVideoScalingMode();
                    EventsPlaybackService.Receive.setVideoScalingMode.invoke(videoScaling);
                }
            }
        }, listenerRefHolder);

        AppPreferences.onBoolPreferenceChanged.subscribeWeak(new WeakEvent2.Handler<Integer, Boolean>() {
            @Override
            public void invoke(Integer preference, Boolean value) {
                if (preference == AppPreferences.PREF_Bool_visualPreferShowVideoContent) {

                    FragmentVisualizer FragmentVisualizer = MainActivity.getFragmentVisualizerInstance();
                    if (FragmentVisualizer != null) {
                        FragmentVisualizer.setShowVideoContentState(value);
                    }
                }
            }
        }, listenerRefHolder);

        MainActivity.onFullscreenChanged.subscribeWeak(new WeakEvent1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean fullScreen) {

                FragmentVisualizer FragmentVisualizer = MainActivity.getFragmentVisualizerInstance();
                if (FragmentVisualizer != null)
                    FragmentVisualizer.animateShow(!fullScreen);
            }
        }, listenerRefHolder);
    }

    private int getPlayerbackVideoScalingMode() {
        int modePref = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_videoScalingMode);

        if (modePref == 1)
            return IMediaPlayerCore.MP_VIDEO_SCALING_MODE_SCALE_TO_FIT;
        else if (modePref == 2)
            return IMediaPlayerCore.MP_VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING;
        else if (modePref == 3) return IMediaPlayerCore.MP_VIDEO_SCALING_MODE_SCALE_TO_FIT;
        else return IMediaPlayerCore.MP_VIDEO_SCALING_MODE_SCALE_TO_FIT;
    }

    private float getSurfaceVideoSize(float videoWidthHeightRatio) {

        int modePref = AppPreferences.createOrGetInstance().getInt(AppPreferences.PREF_Int_videoScalingMode);

        if (modePref == 1)
            return videoWidthHeightRatio;
        else if (modePref == 2) return 0.0f;
        else if (modePref == 3) return 0.0f;
        else return videoWidthHeightRatio;

    }

    private void applyThemeCustomizationData(RootElement visualizerThemeElementRoot, Element.CustomizationList customizationList) {
        visualizerThemeElementRoot.setCustomization(customizationList);
    }

}
