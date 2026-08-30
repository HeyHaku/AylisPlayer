

package com.aylis.comp.AppTips;
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
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.R;

public class TipReorderDialog extends DialogFragment {

    public static TipReorderDialog createAndShowTipReorderDialog(FragmentManager fragmentManager) {
        TipReorderDialog dialog = new TipReorderDialog();
        dialog.show(fragmentManager, "TipReorderDialog");
        return dialog;
    }

    @Override
    public void onAttach(android.content.Context context) {
        super.onAttach(context);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity());
        View rootView = View.inflate(getActivity(), R.layout.dialog_tip_reorder, null);
        builder.setView(rootView);
        builder.setTitle(R.string.dialog_tip);

        builder.setPositiveButton(R.string.dialog_hideTip, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                AppPreferences.createOrGetInstance().setBool(AppPreferences.PREF_Bool_tipShow_reorder, false);
            }
        });

        builder.setNeutralButton(R.string.dialog_close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

            }
        });

        return builder.create();
    }

}
