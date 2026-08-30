
package com.aylis.comp.visual.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import androidx.fragment.app.DialogFragment;
import android.view.WindowManager;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.Common.tlog;
import com.aylis.R;
import com.aylis.comp.visual.core.Elements.Element;
import android.content.Intent;

public class CustomizeVisDialog extends DialogFragment {

    private static final String arg1 = "arg1";
    private static final String arg2 = "arg2";
    private static final String arg3 = "arg3";
    public static WeakEvent4<Integer, Element.CustomizationList, Integer, WeakEvent2<Integer, Element.CustomizationList>> onPickedColor = new WeakEvent4<>();
    public static WeakEvent3<Integer, Element.CustomizationList, Integer> onFinishedPickingColor = new WeakEvent3<>();
    public static WeakEvent1<Integer> onRequestSaveScene = new WeakEvent1<>();

    private WeakEvent2<Integer, Element.CustomizationList> onCustomStructureChanged = new WeakEvent2<>();
    private Object handlerRefHolder;

    int rootIdentifier;
    int customizationIndex;
    int lastSelectedElementIndex = -1;
    Element.CustomizationList customizationDataList;

    TextView txtElementTitle;
    View btnBack;
    boolean eventsFromUser = false;

    View layoutCustomizeMain1;
    View layoutCustomizeMain2;
    androidx.recyclerview.widget.RecyclerView recyclerViewElements;
    ViewGroup linearLayoutPropertiesContent;

    View bottomEditorPanel;
    TextView editorTitle;
    android.widget.EditText editorValue;
    boolean isUpdatingProgrammatically = false;
    android.text.TextWatcher activeTextWatcher = null;
    ViewGroup editorContentContainer;
    View btnEditorDone;

    final java.util.Set<String> expandedGroups = new java.util.HashSet<>();

    private CustomizeMain1 customizeMain1;
    private CustomizeMain2 customizeMain2;

    public CustomizeVisDialog() {
        handlerRefHolder = onCustomStructureChanged
                .subscribeHoldWeak(new WeakEvent2.Handler<Integer, Element.CustomizationList>() {
                    @Override
                    public void invoke(Integer rootIdent, Element.CustomizationList customizationList) {
                        if (rootIdentifier != rootIdent) {
                            tlog.w("rootIdentifiers doesn't match");
                            return;
                        }
                        eventsFromUser = false;
                        parseCustomizationData(customizationList);
                        eventsFromUser = true;
                    }
                });
    }

    public static CustomizeVisDialog createAndShowCustomizeVisDialog(
            FragmentManager fragmentManager,
            Integer rootIdentifier,
            Element.CustomizationList customization,
            Integer customizationIndex) {

        CustomizeVisDialog dialog = new CustomizeVisDialog();
        Bundle args = new Bundle();
        args.putInt(arg1, rootIdentifier);
        args.putInt(arg2, customizationIndex);
        args.putString(arg3, customization.serialize());
        dialog.setArguments(args);

        dialog.show(fragmentManager, "CustomizeVisDialog");
        return dialog;
    }

    @androidx.annotation.NonNull
    @Override
    public android.app.Dialog onCreateDialog(@androidx.annotation.Nullable Bundle savedInstanceState) {
        return new android.app.Dialog(getActivity(), getTheme()) {
            @Override
            public boolean dispatchTouchEvent(@androidx.annotation.NonNull android.view.MotionEvent ev) {
                if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    if (bottomEditorPanel != null && bottomEditorPanel.getVisibility() == android.view.View.VISIBLE) {
                        android.graphics.Rect outRect = new android.graphics.Rect();
                        bottomEditorPanel.getGlobalVisibleRect(outRect);
                        if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                            hideBottomEditor();
                        }
                    }
                }
                return super.dispatchTouchEvent(ev);
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.setCanceledOnTouchOutside(false);
            dialog.getWindow().setDimAmount(0.0f);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(dialog.getWindow(), false);
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            dialog.getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            dialog.getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                dialog.getWindow().setNavigationBarContrastEnforced(false);
                dialog.getWindow().setStatusBarContrastEnforced(false);
            }
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            dialog.setOnKeyListener(new android.content.DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(android.content.DialogInterface dialog, int keyCode, android.view.KeyEvent event) {
                    if (keyCode == android.view.KeyEvent.KEYCODE_BACK
                            && event.getAction() == android.view.KeyEvent.ACTION_UP) {
                        if (bottomEditorPanel != null
                                && bottomEditorPanel.getVisibility() == android.view.View.VISIBLE) {
                            hideBottomEditor();
                            return true;
                        } else if (customizationIndex >= 0) {
                            customizationIndex = -1;
                            showElementList();
                            onPropertyChanged();
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        animateControlsVisibility(false);
    }

    private void animateControlsVisibility(final boolean show) {
        if (getActivity() == null)
            return;
        final View mediaControls = getActivity().findViewById(R.id.media_controls_root);
        final View fragButtons = getActivity().findViewById(R.id.layoutButtons);

        int animRes = show ? R.anim.fade_in : R.anim.fade_out;
        android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(getActivity(),
                animRes);
        if (anim == null)
            return;

        anim.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
                if (show) {
                    if (mediaControls != null) {
                        boolean isHidden = com.aylis.comp.AppPreferences.AppPreferences.createOrGetInstance()
                                .getBool(com.aylis.comp.AppPreferences.AppPreferences.PREF_Bool_mediaControlsHidden);
                        if (!isHidden) {
                            mediaControls.setVisibility(View.VISIBLE);
                        }
                    }
                    if (fragButtons != null)
                        fragButtons.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                if (!show) {
                    if (mediaControls != null)
                        mediaControls.setVisibility(View.GONE);
                    if (fragButtons != null)
                        fragButtons.setVisibility(View.GONE);
                }
            }

            @Override
            public void onAnimationRepeat(android.view.animation.Animation animation) {
            }
        });

        if (mediaControls != null) {
            mediaControls.clearAnimation();
            boolean isHidden = com.aylis.comp.AppPreferences.AppPreferences.createOrGetInstance()
                    .getBool(com.aylis.comp.AppPreferences.AppPreferences.PREF_Bool_mediaControlsHidden);
            if (!isHidden) {
                mediaControls.startAnimation(anim);
            }
        }
        if (fragButtons != null) {
            fragButtons.clearAnimation();
            fragButtons.startAnimation(anim);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        Bundle args = this.getArguments();

        int layoutId = com.aylis.comp.visual.ui.LayoutModeManager
                .getLayout(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_main));
        View rootView = inflater.inflate(layoutId, container, false);

        txtElementTitle = (TextView) rootView.findViewById(R.id.txtElementTitle);
        btnBack = rootView.findViewById(R.id.btnBack);

        layoutCustomizeMain1 = rootView.findViewById(R.id.layoutCustomizeMain1);
        layoutCustomizeMain2 = rootView.findViewById(R.id.layoutCustomizeMain2);

        if (layoutCustomizeMain1 != null) {
            recyclerViewElements = layoutCustomizeMain1.findViewById(R.id.recyclerViewElements);
        }
        if (layoutCustomizeMain2 != null) {
            linearLayoutPropertiesContent = layoutCustomizeMain2.findViewById(R.id.linearLayoutPropertiesContent);
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView,
                new androidx.core.view.OnApplyWindowInsetsListener() {
                    @Override
                    public androidx.core.view.WindowInsetsCompat onApplyWindowInsets(View v,
                            androidx.core.view.WindowInsetsCompat insets) {
                        androidx.core.graphics.Insets insetsToApply = insets
                                .getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()
                                        | androidx.core.view.WindowInsetsCompat.Type.ime());
                        if (layoutCustomizeMain1 != null) {
                            layoutCustomizeMain1.setPadding(0, 0, 0, insetsToApply.bottom);
                        }
                        if (layoutCustomizeMain2 != null) {
                            layoutCustomizeMain2.setPadding(0, 0, 0, insetsToApply.bottom);
                        }
                        return insets;
                    }
                });

        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lastSelectedElementIndex = customizationIndex;
                    customizationIndex = -1;
                    showElementList();
                    onPropertyChanged();
                }
            });
        }

        bottomEditorPanel = rootView.findViewById(R.id.bottomEditorPanel);
        editorTitle = (TextView) rootView.findViewById(R.id.editorTitle);
        editorValue = (android.widget.EditText) rootView.findViewById(R.id.editorValue);
        editorContentContainer = (ViewGroup) rootView.findViewById(R.id.editorContentContainer);
        btnEditorDone = rootView.findViewById(R.id.btnEditorDone);

        if (btnEditorDone != null) {
            btnEditorDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideBottomEditor();
                }
            });
        }

        rootIdentifier = args.getInt(arg1);
        customizationIndex = args.getInt(arg2);
        customizationDataList = Element.CustomizationList.deserialize(args.getString(arg3));

        customizeMain1 = new CustomizeMain1(this);
        customizeMain2 = new CustomizeMain2(this);

        eventsFromUser = false;
        if (customizationIndex < 0) {
            showElementList();
        } else {
            parseCustomizationData(customizationDataList);
        }
        eventsFromUser = true;

        return rootView;
    }

    void showElementList() {
        customizeMain1.showElementList();
    }

    void parseCustomizationData(Element.CustomizationList customList) {
        customizeMain2.parseCustomizationData(customList);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onFinishedPickingColor.invoke(rootIdentifier, customizationDataList, customizationIndex);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        animateControlsVisibility(true);
        onFinishedPickingColor.invoke(rootIdentifier, customizationDataList, customizationIndex);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        customizeMain2.onActivityResult(requestCode, resultCode, data);
    }

    void showBottomEditor(String title, View editorView) {
        if (bottomEditorPanel == null)
            return;

        editorTitle.setText(title);
        editorValue.setOnClickListener(null);
        if (activeTextWatcher != null) {
            editorValue.removeTextChangedListener(activeTextWatcher);
            activeTextWatcher = null;
        }
        editorValue.setOnEditorActionListener(null);
        editorContentContainer.removeAllViews();
        if (editorView != null) {
            editorContentContainer.addView(editorView);
        }
        editorValue.setVisibility(View.VISIBLE);

        if (bottomEditorPanel.getVisibility() != View.VISIBLE) {
            bottomEditorPanel.setVisibility(View.VISIBLE);
            bottomEditorPanel.setAlpha(0.0f);
            float translationY = bottomEditorPanel.getHeight() > 0 ? bottomEditorPanel.getHeight()
                    : (300f * getResources().getDisplayMetrics().density);
            bottomEditorPanel.setTranslationY(translationY);
            bottomEditorPanel.animate()
                    .alpha(1.0f)
                    .translationY(0f)
                    .setDuration(250)
                    .start();
        }
    }

    void hideBottomEditor() {
        if (bottomEditorPanel == null || bottomEditorPanel.getVisibility() != View.VISIBLE)
            return;

        if (editorValue != null) {
            editorValue.clearFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getActivity()
                    .getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(editorValue.getWindowToken(), 0);
            }
        }

        bottomEditorPanel.animate()
                .alpha(0.0f)
                .translationY(bottomEditorPanel.getHeight())
                .setDuration(200)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        bottomEditorPanel.setVisibility(View.GONE);
                        editorContentContainer.removeAllViews();

                        eventsFromUser = false;
                        parseCustomizationData(customizationDataList);
                        eventsFromUser = true;
                    }
                })
                .start();
    }

    void setEditorValueText(String text) {
        if (editorValue == null)
            return;
        isUpdatingProgrammatically = true;
        editorValue.setText(text);
        isUpdatingProgrammatically = false;
    }

    void updateEditorValueText(int color) {
        String hexText = String.format(java.util.Locale.US, "#%08X", color);
        isUpdatingProgrammatically = true;
        editorValue.setText(hexText);
        isUpdatingProgrammatically = false;
    }

    void onChildPropertyChanged() {
        if (!eventsFromUser)
            return;
        if (customizationDataList == null)
            return;
        onPickedColor.invoke(rootIdentifier, customizationDataList, customizationIndex, onCustomStructureChanged);
    }

    void onPropertyChanged() {
        if (!eventsFromUser)
            return;
        if (customizationDataList == null)
            return;
        onPickedColor.invoke(rootIdentifier, customizationDataList, customizationIndex, null);
    }
}
