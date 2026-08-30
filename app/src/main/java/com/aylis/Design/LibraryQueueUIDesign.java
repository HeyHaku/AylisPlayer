

package com.aylis.Design;

import android.os.Handler;

import androidx.fragment.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import com.aylis.comp.ContextualActionBar.ItemSelection;
import com.aylis.comp.LibraryQueueUI.FragmentLibrary;
import com.aylis.comp.LibraryQueueUI.Dialog.AddLinkDialog;

import com.aylis.comp.LibraryQueueUI.Dialog.SongDetailsDialog;
import com.aylis.comp.LibraryQueueUI.LibraryQueueFragmentBase;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.Events.WeakEventR1;
import com.aylis.Common.MultiList;
import com.aylis.comp.AlbumArt.AlbumArtCore;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.AppTips.TipReorderDialog;
import com.aylis.comp.Common.IGeneralItemContainerIdentifier;
import com.aylis.comp.Common.IItemIdentifier;
import com.aylis.comp.ContextualActionBar.ContextualActionBar;
import com.aylis.comp.playback.MediaPlaybackServiceDefs;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.comp.PlaybackQueue.IPlaylistSongContainerIdentifier;
import com.aylis.comp.PlaybackQueue.QueueCore;
import com.aylis.comp.Playlists.Dialog.PlaylistPickerDialog;
import com.aylis.ContextData;
import com.aylis.MainActivity;
import com.aylis.PlayerCore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class LibraryQueueUIDesign {

    private List<Object> listenerRefHolder = new LinkedList<>();

    public LibraryQueueUIDesign() {

        LibraryQueueFragmentBase.onRequestQueueList.subscribeWeak(new WeakEventR.Handler<MultiList<PlaylistSong, IItemIdentifier>>() {
            @Override
            public MultiList<PlaylistSong, IItemIdentifier> invoke() {

                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    return playbackQueue.getQueue();

                return new MultiList<>();
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestSongContainerIdentifier.subscribeWeak(new WeakEventR.Handler<IPlaylistSongContainerIdentifier>() {
            @Override
            public IPlaylistSongContainerIdentifier invoke() {

                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    return playbackQueue.getSongContainerIdentifier();

                return null;
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestShuffleMode.subscribeWeak(new WeakEventR.Handler<Integer>() {
            @Override
            public Integer invoke() {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    return playbackQueue.getShuffleMode();
                return MediaPlaybackServiceDefs.SHUFFLE_NONE;
            }
        }, listenerRefHolder);

        AddLinkDialog.onSubmitAddByLink.subscribeWeak(new WeakEvent2.Handler<ContextData, String>() {
            @Override
            public void invoke(ContextData contextData, String value) {
                Uri songUri = Uri.parse(value);
                PlaylistSong newsong = new PlaylistSong(-1, songUri);
                final List<PlaylistSong> list = new ArrayList<>();
                list.add(newsong);

                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.open(list, 0, MediaPlaybackServiceDefs.FIRST, null);

            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onNavigateLibraryAddress.subscribeWeak(new WeakEvent1.Handler<String>() {
            @Override
            public void invoke(String address) {
                FragmentLibrary FragmentLibrary = MainActivity.getFragmentLibraryInstance();
                if (FragmentLibrary != null) FragmentLibrary.navigateForwardLibraryAddress(null, address);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onAction.subscribeWeak(new WeakEvent2.Handler<ContextData, Integer>() {
            @Override
            public void invoke(ContextData contextData, Integer action) {

                Context context = PlayerCore.s().getAppContext();
                if (context == null) return;

                FragmentManager fragmentManager = contextData.getFragmentManager();

                switch (action) {

                    case LibraryQueueFragmentBase.ACTION_AddByLink:
                        if (fragmentManager != null) {
                            AddLinkDialog.createAndShowDialog(fragmentManager);
                        }
                        break;

                    case LibraryQueueFragmentBase.ACTION_ClearQueue:

                        QueueCore playbackQueue = QueueCore.createOrGetInstance();
                        if (playbackQueue != null)
                            playbackQueue.open(new ArrayList<PlaylistSong>(), 0, MediaPlaybackServiceDefs.CLEAR, null);
                        break;

                    case LibraryQueueFragmentBase.ACTION_SaveAs: {
                        List<PlaylistSong> songs;
                        QueueCore playbackQueue2 = QueueCore.createOrGetInstance();
                        if (playbackQueue2 != null) {
                            songs = playbackQueue2.getQueue().unmodifiableList1();

                            if (fragmentManager != null)
                                PlaylistPickerDialog.createAndShowPlaylistPickerDialog(fragmentManager, songs, true);
                        }
                        break;
                    }

                    case LibraryQueueFragmentBase.ACTION_Shuffle:
                        toggleShuffle();
                        break;

                    case LibraryQueueFragmentBase.ACTION_FollowCurrent:
                        AppPreferences.createOrGetInstance().toggleBool(AppPreferences.PREF_Bool_followCurrentState);
                        break;

                }
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onUIRequestFollowCurrentValue.subscribeWeak(new WeakEventR.Handler<Boolean>() {
            @Override
            public Boolean invoke() {
                return AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_followCurrentState);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestAlbumArtSimple.subscribeWeak(new WeakEvent2.Handler<String, ImageView>() {
            @Override
            public void invoke(String uri, ImageView imageView) {
                AlbumArtCore albumArtCore = AlbumArtCore.getInstance();
                if (albumArtCore != null) {
                    albumArtCore.loadAlbumArt(uri, imageView);
                }
            }
        }, listenerRefHolder);

        SongDetailsDialog.onRequestAlbumArt.subscribeWeak(new WeakEvent4.Handler<AlbumArtRequest, ImageView, Boolean, Boolean>() {
            @Override
            public void invoke(AlbumArtRequest albumArtRequest, ImageView imageView, Boolean fitCenterInside, Boolean preferLarge) {
                AlbumArtCore albumArtCore = AlbumArtCore.getInstance();
                if (albumArtCore != null) {
                    albumArtCore.loadAlbumArt(albumArtRequest.videoThumbDataSource,
                            albumArtRequest.path0,
                            albumArtRequest.path1,
                            albumArtRequest.genStr,
                            imageView,
                            fitCenterInside,
                            preferLarge);
                }
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestAlbumArt.subscribeWeak(new WeakEvent4.Handler<AlbumArtRequest, ImageView, Boolean, Boolean>() {
            @Override
            public void invoke(AlbumArtRequest albumArtRequest, ImageView imageView, Boolean fitCenterInside, Boolean preferLarge) {
                AlbumArtCore albumArtCore = AlbumArtCore.getInstance();
                if (albumArtCore != null) {
                    albumArtCore.loadAlbumArt(albumArtRequest.videoThumbDataSource,
                            albumArtRequest.path0,
                            albumArtRequest.path1,
                            albumArtRequest.genStr,
                            imageView,
                            fitCenterInside,
                            preferLarge);

                }
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestShowAlbumArtValue.subscribeWeak(new WeakEventR.Handler<Boolean>() {
            @Override
            public Boolean invoke() {
                return AppPreferences.createOrGetInstance().getBool(AppPreferences.PREF_Bool_showAlbumArtInstead);
            }
        }, listenerRefHolder);

        AppPreferences.onBoolPreferenceChanged.subscribeWeak(new WeakEvent2.Handler<Integer, Boolean>() {
            @Override
            public void invoke(Integer preference, Boolean value) {
                if (preference == AppPreferences.PREF_Bool_followCurrentState) {
                    LibraryQueueFragmentBase.onFollowCurrentValueChanged(value);
                } else if (preference == AppPreferences.PREF_Bool_showAlbumArtInstead) {
                    LibraryQueueFragmentBase.onShowAlbumArtValueChanged(value);

                    updateLibraryItems();
                }
            }
        }, listenerRefHolder);

        ContextualActionBar.onItemSelectionChanged.subscribeWeak(new WeakEvent2.Handler<ItemSelection.One<Object>, Boolean>() {
            @Override
            public void invoke(ItemSelection.One<Object> itemSelection, Boolean select) {
                updateContainerItems(itemSelection.getContainerIdentifier());
            }
        }, listenerRefHolder);

        ContextualActionBar.onContainerItemsDeselected.subscribeWeak(new WeakEvent1.Handler<IGeneralItemContainerIdentifier>() {
            @Override
            public void invoke(IGeneralItemContainerIdentifier containerIdentifier) {
                updateContainerItems(containerIdentifier);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onEnqueue.subscribeWeak(new WeakEvent2.Handler<Collection<PlaylistSong>, Integer>() {
            @Override
            public void invoke(Collection<PlaylistSong> list, Integer action) {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.enqueue(list, action);
            }
        }, listenerRefHolder);
        LibraryQueueFragmentBase.onMoveQueueItems.subscribeWeak(new WeakEvent3.Handler<Integer, Integer, List<Integer>>() {
            @Override
            public void invoke(Integer from, Integer to, List<Integer> itemOffsets) {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.moveQueueItems(from, to, itemOffsets);
            }
        }, listenerRefHolder);
        LibraryQueueFragmentBase.onOpen2.subscribeWeak(new WeakEvent3.Handler<List<PlaylistSong>, Integer, IPlaylistSongContainerIdentifier>() {
            @Override
            public void invoke(List<PlaylistSong> list, Integer startPlayPosition, IPlaylistSongContainerIdentifier songContainerIdentifier) {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.open(list, startPlayPosition, MediaPlaybackServiceDefs.CLEAR, songContainerIdentifier);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRemoveQueueItems.subscribeWeak(new WeakEvent1.Handler<List<IItemIdentifier>>() {
            @Override
            public void invoke(List<IItemIdentifier> itemIdentifiers) {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.removeQueueItems(itemIdentifiers);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onQueuePositionChanged.subscribeWeak(new WeakEvent1.Handler<Integer>() {
            @Override
            public void invoke(Integer position) {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.setQueuePosition(position);
            }
        }, listenerRefHolder);
        LibraryQueueFragmentBase.onSetCurrentQueueItem.subscribeWeak(new WeakEvent1.Handler<IItemIdentifier>() {
            @Override
            public void invoke(IItemIdentifier item) {
                QueueCore playbackQueue = QueueCore.createOrGetInstance();
                if (playbackQueue != null)
                    playbackQueue.setQueueItem(item);
            }
        }, listenerRefHolder);

        QueueCore.onQueueStateChanged.subscribeWeak(new WeakEvent2.Handler<MultiList<PlaylistSong, IItemIdentifier>, IPlaylistSongContainerIdentifier>() {
            @Override
            public void invoke(MultiList<PlaylistSong, IItemIdentifier> list, IPlaylistSongContainerIdentifier songContainerIdentifier) {

            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestShowTipState.subscribeWeak(new WeakEventR1.Handler<Integer, Boolean>() {
            @Override
            public Boolean invoke(Integer tipId) {
                return AppPreferences.createOrGetInstance().getBool(tipId);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onActionShowReorderTip.subscribeWeak(new WeakEvent1.Handler<ContextData>() {
            @Override
            public void invoke(ContextData contextData) {
                FragmentManager fragmentManager = contextData.getFragmentManager();
                if (fragmentManager == null) return;

                TipReorderDialog.createAndShowTipReorderDialog(fragmentManager);
            }
        }, listenerRefHolder);
    }

    private void toggleShuffle() {

        int mode = MediaPlaybackServiceDefs.SHUFFLE_NONE;
        QueueCore playbackQueue = QueueCore.createOrGetInstance();
        if (playbackQueue != null)
            mode = playbackQueue.getShuffleMode();

        int shuffleMode = mode;

        if (mode == MediaPlaybackServiceDefs.SHUFFLE_NONE)
            shuffleMode = MediaPlaybackServiceDefs.SHUFFLE_NORMAL;
        else if (mode == MediaPlaybackServiceDefs.SHUFFLE_NORMAL)
            shuffleMode = MediaPlaybackServiceDefs.SHUFFLE_NONE;

        if (playbackQueue != null)
            playbackQueue.setShuffleMode(shuffleMode, true);
    }

    private void updateContainerItems(IGeneralItemContainerIdentifier containerIdentifier) {

        FragmentLibrary FragmentLibrary = MainActivity.getFragmentLibraryInstance();
        if (FragmentLibrary != null)
            FragmentLibrary.refreshAdapter(containerIdentifier);

    }

    private void updateLibraryItems() {
        FragmentLibrary FragmentLibrary = MainActivity.getFragmentLibraryInstance();
        if (FragmentLibrary != null) FragmentLibrary.updateLibraryItems();
    }

}

