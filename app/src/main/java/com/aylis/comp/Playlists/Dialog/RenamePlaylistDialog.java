

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
import android.widget.EditText;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.R;

public class RenamePlaylistDialog extends DialogFragment {

    public static WeakEvent3<Context  , Long  , String  > onSubmitRenamePlaylist = new WeakEvent3<>();

    private static final String arg1 = "arg1";
    private static final String arg2 = "arg2";

    public static RenamePlaylistDialog createAndShowCreateRenamePlaylistDialog(FragmentManager fragmentManager, Long playlistId, String defaultValue) {
        RenamePlaylistDialog dialog = RenamePlaylistDialog.newInstanceRename(playlistId, defaultValue);
        dialog.show(fragmentManager, "RenamePlaylistDialog");
        return dialog;
    }

    private static RenamePlaylistDialog newInstanceRename(long playlistId, String defaultValue) {
        RenamePlaylistDialog dialog = new RenamePlaylistDialog();
        Bundle args = new Bundle();
        args.putLong(arg1, playlistId);
        args.putString(arg2, defaultValue);
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

        final long playlistId = args.getLong(arg1);
        final String defaultValue = args.getString(arg2);

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity());

        View rootView = View.inflate(getActivity(), R.layout.dialog_rename_playlist, null);
        builder.setView(rootView);

        final EditText editTxtPlaylistName = (EditText) rootView.findViewById(R.id.editTxtPlaylistName);
        if (defaultValue == null)
            editTxtPlaylistName.setText(R.string.dialog_add_playlist_default_value);
        else
            editTxtPlaylistName.setText(defaultValue);

        builder.setTitle(R.string.dialog_rename_playlist_title);

        builder.setPositiveButton(R.string.dialog_rename, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                onSubmitRenamePlaylist.invoke(editTxtPlaylistName.getContext(), playlistId, editTxtPlaylistName.getText().toString());
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                RenamePlaylistDialog.this.getDialog().cancel();
            }
        });

        return builder.create();
    }
}