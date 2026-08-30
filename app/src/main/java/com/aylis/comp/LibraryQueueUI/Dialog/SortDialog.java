

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
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Space;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.Common.Events.WeakEventR2;
import com.aylis.Common.Tuple2;
import com.aylis.comp.Common.IGeneralItemContainerIdentifier;
import com.aylis.Design.SortDesign;
import com.aylis.R;
import java.util.ArrayList;
import java.util.List;

public class SortDialog extends DialogFragment implements RadioGroup.OnCheckedChangeListener {

    public static WeakEventR2<Integer  , IGeneralItemContainerIdentifier  , SortDesign.SortOptions> onRequestSortOptions = new WeakEventR2<>();
    public static WeakEvent4<Integer  , IGeneralItemContainerIdentifier  , Integer  , Integer  > onSubmitSortOptions = new WeakEvent4<>();

    private static final String arg1 = "arg1";
    private List<RadioButton> radioBtnList = null;
    private List<CheckBox> chkBoxList = null;

    public SortDialog() {
    }

    public static SortDialog createAndShowDialog(FragmentManager fragmentManager, int mode) {
        SortDialog dialog = newInstance(mode);
        dialog.show(fragmentManager, "SortDialog");
        return dialog;
    }

    private static SortDialog newInstance(int mode) {
        SortDialog dialog = new SortDialog();
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
        Bundle args = this.getArguments();
        int mode = args.getInt(arg1);

        final int pageIndex = -1;
        final IGeneralItemContainerIdentifier containerIdentifier = null;

        SortDesign.SortOptions optionsRaw = onRequestSortOptions.invoke(pageIndex, containerIdentifier, null);

        SortDesign.SortOptions options;
        if (mode == 0) {
            options = optionsRaw;
        } else {

            options = optionsRaw.getRefined(SortDesign.Sort_Mode_Title,
                    SortDesign.Sort_Mode_Path,
                    SortDesign.Sort_Mode_DateModified,
                    SortDesign.Sort_Mode_Size);
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(getActivity());

        View rootView = View.inflate(getActivity(), R.layout.dialog_sort, null);
        builder.setView(rootView);

        LinearLayout layoutCheckOptions = (LinearLayout) rootView.findViewById(R.id.layoutCheckOptions);
        RadioGroup radioGroupOptions = (RadioGroup) rootView.findViewById(R.id.radioGroupOptions);

        Space space = (Space) rootView.findViewById(R.id.space);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        radioBtnList = new ArrayList<>(options == null ? 0 : options.radioOptions.size());
        chkBoxList = new ArrayList<>(options == null ? 0 : options.checkOptions.size());

        if (options != null) {
            for (Tuple2<Integer, String> item : options.radioOptions) {
                RadioButton radioBtn = new RadioButton(this.getActivity());
                radioBtn.setTag((Integer) item.obj1);
                radioBtn.setText(item.obj2);
                radioGroupOptions.addView(radioBtn, params);

                if (item.obj1 == options.selectedRadioOption) {
                    radioGroupOptions.check(radioBtn.getId());
                }

                radioBtnList.add(radioBtn);
            }

            if (options.checkOptions.size() > 0)
                space.setVisibility(View.VISIBLE);
            else
                space.setVisibility(View.GONE);

            for (Tuple2<Integer, String> item : options.checkOptions) {
                CheckBox chkBox = new CheckBox(this.getActivity());
                chkBox.setTag((Integer) item.obj1);
                chkBox.setText(item.obj2);
                if ((item.obj1 & options.maskCheckOptions) != 0) chkBox.setChecked(true);
                layoutCheckOptions.addView(chkBox, params);

                chkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        com.aylis.utils.HapticManager.INSTANCE.performClick(buttonView);
                        notifyOptionChange(pageIndex, containerIdentifier);
                    }
                });

                chkBoxList.add(chkBox);
            }
        }

        radioGroupOptions.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                com.aylis.utils.HapticManager.INSTANCE.performClick(group);
                notifyOptionChange(pageIndex, containerIdentifier);
            }
        });

        builder.setTitle(R.string.dialog_sort_title);

        builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                notifyOptionChange(pageIndex, containerIdentifier);
            }
        });

        return builder.create();
    }

    void notifyOptionChange(final int pageIndex, final IGeneralItemContainerIdentifier containerIdentifier) {
        if (radioBtnList == null || chkBoxList == null) return;

        int sortRadioOption = 0;
        int sortMaskOptions = 0;

        for (RadioButton radioBtn : radioBtnList) {
            if (radioBtn.isChecked())
                sortRadioOption = (int) radioBtn.getTag();
        }

        for (CheckBox chkBox : chkBoxList) {
            if (chkBox.isChecked())
                sortMaskOptions |= (int) chkBox.getTag();
        }

        final int sortRadioOptionFinal = sortRadioOption;
        final int sortMaskOptionsFinal = sortMaskOptions;
        onSubmitSortOptions.invoke(pageIndex, containerIdentifier, sortRadioOptionFinal, sortMaskOptionsFinal);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }
}
