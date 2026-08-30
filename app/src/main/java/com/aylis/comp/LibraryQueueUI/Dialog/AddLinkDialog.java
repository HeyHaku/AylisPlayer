

package com.aylis.comp.LibraryQueueUI.Dialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.content.Context;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.ContextData;
import com.aylis.R;

public class AddLinkDialog extends DialogFragment {

    public static WeakEvent2<ContextData, String> onSubmitAddByLink = new WeakEvent2<>();

    private int currentSample = 0;

    public static AddLinkDialog createAndShowDialog(FragmentManager fragmentManager) {
        AddLinkDialog dialog = new AddLinkDialog();
        dialog.show(fragmentManager, "AddLinkDialog");
        return dialog;
    }

    public AddLinkDialog() {
    }

    @Override
    public void onAttach(android.content.Context context) {
        super.onAttach(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity());

        View rootView = View.inflate(getActivity(), R.layout.dialog_add_link, null);
        builder.setView(rootView);

        final EditText et1 = (EditText) rootView.findViewById(R.id.editTxtFolderName);
        et1.setText(R.string.dialog_add_link_default_value);

        TextView txtUnder = (TextView) rootView.findViewById(R.id.txtUnder);
        txtUnder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentSample == 0)
                    et1.setText(R.string.dialog_add_link_sample_0);
                else if (currentSample == 1)
                    et1.setText(R.string.dialog_add_link_sample_1);
                else if (currentSample == 2)
                    et1.setText(R.string.dialog_add_link_sample_2);
                else if (currentSample == 3)
                    et1.setText(R.string.dialog_add_link_sample_3);
                else if (currentSample == 4)
                    et1.setText("http://yt-dash-mse-test.commondatastorage.googleapis.com/media/oops-20120802-manifest.mpd");
                else if (currentSample == 5)
                    et1.setText("http://wams.edgesuite.net/media/MPTExpressionData02/BigBuckBunny_1080p24_IYUV_2ch.ism/manifest(format=mpd-time-csf)");
                else if (currentSample == 6)
                    et1.setText("http://playready.directtaps.net/smoothstreaming/TTLSS720VC1/To_The_Limit_720.ism/Manifest");
                else if (currentSample == 7)
                    et1.setText("http://playready.directtaps.net/smoothstreaming/TTLSS720VC1/To_The_Limit_720_688.ismv");
                else if (currentSample == 8)
                    et1.setText("http://techslides.com/demos/sample-videos/small.flv");

                currentSample = ((currentSample + 1) % 9);
            }
        });

        builder.setTitle(R.string.dialog_add_link_title);

        builder.setPositiveButton(R.string.dialog_add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                onSubmitAddByLink.invoke(new ContextData(et1), et1.getText().toString());
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                AddLinkDialog.this.getDialog().cancel();
            }
        });

        return builder.create();
    }
}
