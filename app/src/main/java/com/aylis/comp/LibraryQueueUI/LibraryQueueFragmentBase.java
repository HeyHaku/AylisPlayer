package com.aylis.comp.LibraryQueueUI;

import android.content.Context;
import android.widget.ImageView;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.Events.WeakEventR1;
import com.aylis.Common.Events.WeakEventR2;
import com.aylis.Common.Events.WeakEventR3;
import com.aylis.Common.MultiList;
import com.aylis.comp.AlbumArt.AlbumArtRequest;
import com.aylis.comp.Common.IItemIdentifier;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.comp.PlaybackQueue.IPlaylistSongContainerIdentifier;
import com.aylis.ContextData;
import java.util.Collection;
import java.util.List;

public class LibraryQueueFragmentBase {

    public static final int ACTION_AddByLink = 110;
    public static final int ACTION_ClearQueue = 111;
    public static final int ACTION_SaveAs = 112;
    public static final int ACTION_Shuffle = 113;
    public static final int ACTION_FollowCurrent = 114;
    public static final int ACTION_ShowAlbumArt = 115;
    public static final int ACTION_AddFolder = 120;

    public static WeakEventR<Integer> onRequestShuffleMode = new WeakEventR<>();
    public static WeakEvent1<String> onNavigateLibraryAddress = new WeakEvent1<>();

    public static WeakEventR<IPlaylistSongContainerIdentifier> onRequestSongContainerIdentifier = new WeakEventR<>();
    public static WeakEventR<MultiList<PlaylistSong, IItemIdentifier>> onRequestQueueList = new WeakEventR<>();
    public static WeakEvent1<Integer> onQueuePositionChanged = new WeakEvent1<>();
    public static WeakEvent2<Collection<PlaylistSong>, Integer> onEnqueue = new WeakEvent2<>();
    public static WeakEvent3<List<PlaylistSong>, Integer, IPlaylistSongContainerIdentifier> onOpen2 = new WeakEvent3<>();

    public static WeakEvent1<IItemIdentifier> onSetCurrentQueueItem = new WeakEvent1<>();
    public static WeakEvent1<List<IItemIdentifier>> onRemoveQueueItems = new WeakEvent1<>();
    public static WeakEventR1<Integer, Boolean> onRequestShowTipState = new WeakEventR1<>();
    public static WeakEvent1<ContextData> onActionShowReorderTip = new WeakEvent1<>();

    public static WeakEvent2<ContextData, Integer> onAction = new WeakEvent2<>();
    public static WeakEventR<Boolean> onUIRequestFollowCurrentValue = new WeakEventR<>();

    public static WeakEvent3<Integer, Integer, List<Integer>> onMoveQueueItems = new WeakEvent3<>();
    public static WeakEvent2<String, ImageView> onRequestAlbumArtSimple = new WeakEvent2<>();
    public static WeakEvent4<AlbumArtRequest, ImageView, Boolean, Boolean> onRequestAlbumArt = new WeakEvent4<>();
    public static WeakEventR<Boolean> onRequestShowAlbumArtValue = new WeakEventR<>();

    private static WeakEvent1<Integer> internalOnShuffleModeChanged = new WeakEvent1<>();
    private static WeakEvent1<Boolean> internalOnFollowCurrentValueChanged = new WeakEvent1<>();
    private static WeakEvent1<Boolean> internalOnShowAlbumArtValueChanged = new WeakEvent1<>();

    public static void onShuffleModeChanged(int shuffleMode) {
        internalOnShuffleModeChanged.invoke(shuffleMode);
    }

    public static void onFollowCurrentValueChanged(boolean followCurrent) {
        internalOnFollowCurrentValueChanged.invoke(followCurrent);
    }

    public static void onShowAlbumArtValueChanged(boolean followCurrent) {
        internalOnShowAlbumArtValueChanged.invoke(followCurrent);
    }

    public static WeakEventR3<Integer, com.aylis.comp.Common.IGeneralItemContainerIdentifier, java.io.File, Boolean> onRequestFilterFileResult = new WeakEventR3<>();
    public static WeakEventR2<Integer, com.aylis.comp.Common.IGeneralItemContainerIdentifier, String> onRequestSearchQuery = new WeakEventR2<>();
    public static WeakEvent4<Integer, Boolean, String, com.aylis.comp.Common.IGeneralItemContainerIdentifier> onUpdateSearchOptions = new WeakEvent4<>();
    public static WeakEventR2<android.os.AsyncTask, Integer, Boolean> onCompareSearchTask = new WeakEventR2<>();
    public static WeakEvent3<android.os.AsyncTask, Integer, Object> onStartingSearchTask = new WeakEvent3<>();
    public static WeakEvent1<Integer> onContainerDataSetChanged = new WeakEvent1<>();
    public static WeakEvent3<com.aylis.comp.ContextualActionBar.ActionListenerBase[], Boolean, com.aylis.comp.ContextualActionBar.ItemSelection.One<Object>> onItemSelected = new WeakEvent3<>();
    public static WeakEventR<Boolean> onRequestIsSelectingEnabled = new WeakEventR<>();
    public static WeakEventR1<com.aylis.comp.ContextualActionBar.ItemSelection.One, Boolean> onRequestContainsItemSelection = new WeakEventR1<>();
    
    public static WeakEvent1<ContextData> onLibraryQueue2UI_ActionScanStandalonePlaylist = new WeakEvent1<>();
    public static WeakEvent4<Context, String, String, ContextData> onLibraryQueue2UI_ActionRemoveStandalonePlaylist = new WeakEvent4<>();
    public static WeakEvent3<long[], List<String>, ContextData> onActionCreatePlaylist = new WeakEvent3<>();
    public static WeakEvent4<Context, List<PlaylistSong>, Boolean, ContextData> onLibraryQueueUI_ActionSongSendToPlaylist = new WeakEvent4<>();
    public static WeakEvent4<Context, Long, String, ContextData> onLibraryQueueUI_ActionRenamePlaylist = new WeakEvent4<>();
    public static WeakEvent4<Context, Long, String, ContextData> onLibraryQueueUI_ActionDeletePlaylist = new WeakEvent4<>();
    
    public static WeakEventR1<Class<?>, Boolean> onRequestSectionOpenedState = new WeakEventR1<>();
    public static WeakEvent2<Boolean, Class<?>> onSetSectionOpened = new WeakEvent2<>();
    public static WeakEvent1<ContextData> onActionChooseSortFiles = new WeakEvent1<>();
    public static WeakEvent1<ContextData> onActionChooseSort = new WeakEvent1<>();
    public static WeakEventR2<Integer, com.aylis.comp.Common.IGeneralItemContainerIdentifier, com.aylis.Design.SortDesign.SortDesc> onRequestCurrentSortDesc = new WeakEventR2<>();
    public static WeakEvent3<Context, String, String> onActionRemoveFolder = new WeakEvent3<>();

}
