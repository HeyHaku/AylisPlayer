

package com.aylis.comp.Playlists.Dialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TabHost;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.Events.WeakEvent5;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.MultiList;
import com.aylis.Common.Tuple2;
import com.aylis.Common.UtilsMusic;
import com.aylis.Common.UtilsUI;
import com.aylis.ContextData;
import com.aylis.comp.playback.Song.PlaylistSong;
import com.aylis.R;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PlaylistPickerDialog extends DialogFragment {

    public static WeakEvent5<Context  , Long  , long[]  , List<String>  , Boolean  > onLibraryQueueUI_SubmitSongSendToPlaylist = new WeakEvent5<>();
    public static WeakEvent5<String  , String  , List<String>  , Boolean  , Boolean  > onLibraryQueueUI_SubmitSongSendToStandalonePlaylist = new WeakEvent5<>();
    public static WeakEventR<MultiList<String, String>> onRequestStandalonePlaylists = new WeakEventR<>();
    public static WeakEvent3<long[]  , List<String>  , ContextData  > onActionCreatePlaylist = new WeakEvent3<>();

    private static final String arg1 = "arg1";
    private static final String arg2 = "arg2";
    private static final String arg3 = "arg3";

    public static PlaylistPickerDialog createAndShowPlaylistPickerDialog(FragmentManager fragmentManager, List<PlaylistSong> songs, Boolean showOptions) {
        PlaylistPickerDialog dialog = PlaylistPickerDialog.newInstance(songs, showOptions);
        dialog.show(fragmentManager, "PlaylistPickerDialog");
        return dialog;
    }

    private static PlaylistPickerDialog newInstance(List<PlaylistSong> songs, boolean showOptions) {
        PlaylistPickerDialog dialog = new PlaylistPickerDialog();

        long[] songDataSourceNativePL = new long[songs.size()];
        ArrayList<String> songDataSourcePL = new ArrayList<>(songs.size());

        int i = 0;
        for (PlaylistSong s : songs) {
            songDataSourcePL.add(s.getDataSourceForPlaylist());
            songDataSourceNativePL[i] = s.getDataSourceForNativePlaylist();
            i++;
        }

        Bundle args = new Bundle();
        args.putInt(arg1, showOptions ? 1 : 0);
        args.putStringArrayList(arg2, songDataSourcePL);
        args.putLongArray(arg3, songDataSourceNativePL);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(android.content.Context context) {
        super.onAttach(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = this.getArguments();
        int type = args.getInt(arg1);

        final boolean showOptions = type != 0;
        final ArrayList<String> songDataSourcePL = args.getStringArrayList(arg2);
        final long[] songDataSourceNativePL = args.getLongArray(arg3);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity());
        builder.setTitle(R.string.dialog_send_to_playlist_title);

        View rootView = View.inflate(getActivity(), R.layout.dialog_choose_playlist, null);
        builder.setView(rootView);

        TabHost host = (TabHost) rootView.findViewById(R.id.tabHost);
        host.setup();

        TabHost.TabSpec spec = host.newTabSpec("Tab One");
        spec.setContent(R.id.tab1);
        spec.setIndicator(getResources().getString(R.string.section_playlist_system));
        host.addTab(spec);

        spec = host.newTabSpec("Tab Two");
        spec.setContent(R.id.tab2);
        spec.setIndicator(getResources().getString(R.string.section_playlist_standalone));
        host.addTab(spec);

        final CheckBox checkBoxOverwrite = (CheckBox) rootView.findViewById(R.id.checkBoxOverwrite);

        final CheckBox checkBoxRelative = (CheckBox) rootView.findViewById(R.id.checkBoxRelative);
        checkBoxRelative.setChecked(true);

        if (showOptions) {
            checkBoxOverwrite.setVisibility(View.VISIBLE);
            checkBoxOverwrite.setChecked(true);
        } else {
            checkBoxOverwrite.setVisibility(View.GONE);
        }

        ListView lv1 = (ListView) rootView.findViewById(R.id.listViewPlaylist1);
        lv1.setTextFilterEnabled(true);
        ArrayAdapter<String> adapter1 = new ArrayAdapter<>(this.getActivity(), R.layout.bgreco_list_item);
        lv1.setAdapter(adapter1);

        ListView lv2 = (ListView) rootView.findViewById(R.id.listViewPlaylist2);
        lv2.setTextFilterEnabled(true);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this.getActivity(), R.layout.bgreco_list_item);
        lv2.setAdapter(adapter2);

        final List<Long> playlistNativeIds = new ArrayList<>();
        {
            List<String> names = new ArrayList<>();
            UtilsMusic.getPlayLists(getActivity(), playlistNativeIds, names);

            updateAdapter(adapter1, names);
        }

        final MultiList<String, String> playlistIdhashAndPath = onRequestStandalonePlaylists.invoke(new MultiList<String, String>());
        {
            List<String> standaloneNames = new ArrayList<>(playlistIdhashAndPath.size());

            for (Tuple2<String, String> pl : playlistIdhashAndPath)
                standaloneNames.add(pl.obj2);

            updateAdapter(adapter2, standaloneNames);
        }

        lv1.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean overwritePL = (showOptions && checkBoxOverwrite.isChecked());
                UtilsUI.dismissSafe(PlaylistPickerDialog.this);

                if (position >= 0 && position < playlistNativeIds.size()) {
                    long playlistId = playlistNativeIds.get(position);
                    onLibraryQueueUI_SubmitSongSendToPlaylist.invoke(getActivity(), playlistId, songDataSourceNativePL, songDataSourcePL, overwritePL);
                }
            }
        });

        lv2.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final boolean overwritePL = (showOptions && checkBoxOverwrite.isChecked());
                final boolean useRelativePaths = checkBoxRelative.isChecked();

                UtilsUI.dismissSafe(PlaylistPickerDialog.this);

                if (position >= 0 && position < playlistIdhashAndPath.size()) {
                    final Tuple2<String, String> idhashAndPath = playlistIdhashAndPath.get(position);
                    onLibraryQueueUI_SubmitSongSendToStandalonePlaylist.invoke(idhashAndPath.obj1, idhashAndPath.obj2, songDataSourcePL, overwritePL, useRelativePaths);

                }
            }
        });

        builder.setPositiveButton(R.string.dialog_send_to_playlist_add_to_new, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                onActionCreatePlaylist.invoke(songDataSourceNativePL, songDataSourcePL, new ContextData(getActivity()));
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        return builder.create();
    }

    private void updateAdapter(ArrayAdapter<String> adapter, String[] names) {
        if (names.length < 1)
            names = new String[]{getResources().getString(R.string.playlist_empty_placeholder)};

        adapter.clear();
        adapter.addAll(names);
        adapter.notifyDataSetChanged();
    }

    private void updateAdapter(ArrayAdapter<String> adapter, Collection<String> newNames) {
        if (newNames == null || newNames.size() < 1) {
            ArrayList<String> names = new ArrayList<>();
            names.add(getResources().getString(R.string.playlist_empty_placeholder));
            newNames = names;
        }

        adapter.clear();
        adapter.addAll(newNames);
        adapter.notifyDataSetChanged();
    }
}