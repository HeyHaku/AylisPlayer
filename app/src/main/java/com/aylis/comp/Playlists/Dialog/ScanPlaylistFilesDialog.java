

package com.aylis.comp.Playlists.Dialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.os.Handler;

import android.content.Context;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.aylis.Common.Events.WeakEvent;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.UtilsUI;
import com.aylis.Design.PlaylistsDesign;
import com.aylis.R;
import java.util.LinkedList;
import java.util.List;

public class ScanPlaylistFilesDialog extends DialogFragment {

    public static WeakEventR<PlaylistsDesign.PlaylistScanningStatus  > onRequestPlaylistScanStatus = new WeakEventR<>();
    public static WeakEvent onStopPlaylistScan = new WeakEvent();

    private static final String arg1 = "arg1";
    private TextView txtInfo;
    private List<Object> listenerRefHolder = new LinkedList<>();
    private static WeakEvent1<PlaylistsDesign.PlaylistScanningStatus  > internalOnPlaylistScanStatusUpdated = new WeakEvent1<>();

    public static void updatePlaylistScanStatus(PlaylistsDesign.PlaylistScanningStatus status)
    {
        internalOnPlaylistScanStatusUpdated.invoke(status);
    }

    public static ScanPlaylistFilesDialog createAndShowScanPlaylistFilesDialog(FragmentManager fragmentManager) {
        ScanPlaylistFilesDialog dialog = new ScanPlaylistFilesDialog();
        dialog.show(fragmentManager, "ScanPlaylistFilesDialog");
        return dialog;
    }

    private static ScanPlaylistFilesDialog newInstance(int mode) {
        ScanPlaylistFilesDialog dialog = new ScanPlaylistFilesDialog();
        Bundle args = new Bundle();
        args.putInt(arg1, mode);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onAttach(android.content.Context context) {
        super.onAttach(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity());

        View rootView = View.inflate(getActivity(), R.layout.dialog_scan_playlists, null);
        builder.setView(rootView);

        txtInfo = (TextView) rootView.findViewById(R.id.txtInfo);

        builder.setTitle(R.string.dialog_scan_playlists);

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                onStopPlaylistScan.invoke();
            }
        });

        {
            internalOnPlaylistScanStatusUpdated.subscribeWeak(new WeakEvent1.Handler<PlaylistsDesign.PlaylistScanningStatus>() {
                @Override
                public void invoke(PlaylistsDesign.PlaylistScanningStatus playlistScanningStatus) {
                    updateScanStatus(playlistScanningStatus);
                }
            }, listenerRefHolder);

            PlaylistsDesign.PlaylistScanningStatus scanStatus = onRequestPlaylistScanStatus.invoke(null);
            if (scanStatus != null)
                updateScanStatus(scanStatus);
        }

        return builder.create();

    }

    private void updateScanStatus(PlaylistsDesign.PlaylistScanningStatus scanStatus) {
        if (scanStatus.active)
            txtInfo.setText(scanStatus.text);
        else {
            txtInfo.setText("..");
            UtilsUI.dismissSafe(this);
        }
    }
}
