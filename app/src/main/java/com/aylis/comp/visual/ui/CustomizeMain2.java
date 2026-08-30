
package com.aylis.comp.visual.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.aylis.comp.visual.core.Elements.Images.ImageElement;
import com.google.android.material.slider.Slider;

import com.aylis.Common.Utils;
import com.aylis.Common.Vec2f;
import com.aylis.Common.tlog;
import com.aylis.R;
import com.aylis.comp.visual.core.Elements.Element;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class CustomizeMain2 {

    private static final int PICK_IMAGE_REQUEST = 123;
    private static final int PICK_FONT_REQUEST = 124;
    private final CustomizeVisDialog dialog;
    private String activeImagePropertyKey;
    private Element.CustomizationData activeImageCustomData;
    private String activeBottomNavCategory = null;
    private int lastCustomizationIndex = -1;

    public CustomizeMain2(CustomizeVisDialog dialog) {
        this.dialog = dialog;
    }

    void parseCustomizationData(Element.CustomizationList customList) {
        if (dialog.getActivity() == null)
            return;

        dialog.hideBottomEditor();
        dialog.customizationDataList = customList;

        com.aylis.comp.visual.core.Elements.PreCompManager.clear();
        for (int i = 0; i < customList.dataCount(); i++) {
            Element.CustomizationData d = customList.getData(i);
            if (d != null) {
                String type = d.getPropertyString("__type", "");
                if ("PreCompElement".equals(type)) {
                    String name = d.getPropertyString("preCompName", "");
                    if (!name.isEmpty()) {
                        com.aylis.comp.visual.core.Elements.PreCompManager.register(name, null);
                    }
                }
            }
        }

        Element.CustomizationData customizationData = customList.getData(dialog.customizationIndex);
        if (customizationData == null) {
            tlog.w("customizationData is null");
            return;
        }

        if (lastCustomizationIndex != dialog.customizationIndex) {
            activeBottomNavCategory = null;
            lastCustomizationIndex = dialog.customizationIndex;
        }

        dialog.txtElementTitle.setText(customizationData.getCustomizationName());
        if (dialog.btnBack != null)
            dialog.btnBack.setVisibility(View.VISIBLE);

        if (dialog.layoutCustomizeMain1 != null)
            dialog.layoutCustomizeMain1.setVisibility(View.GONE);
        if (dialog.layoutCustomizeMain2 != null)
            dialog.layoutCustomizeMain2.setVisibility(View.VISIBLE);

        if (dialog.linearLayoutPropertiesContent == null)
            return;
        dialog.linearLayoutPropertiesContent.removeAllViews();

        final java.util.LinkedHashMap<String, LinearLayout> groups = new java.util.LinkedHashMap<>();
        final java.util.List<String> sortedGroupTags = scanAndSortGroupTags(customizationData);
        for (String tag : sortedGroupTags) {
            createGroupView(dialog.linearLayoutPropertiesContent, tag, groups);
        }

        parseDataRecursive(customizationData, dialog.linearLayoutPropertiesContent, groups);

        if (dialog.layoutCustomizeMain2 != null) {
            androidx.recyclerview.widget.RecyclerView recyclerBottomNav = dialog.layoutCustomizeMain2
                    .findViewById(R.id.recyclerBottomNav);
            if (recyclerBottomNav != null) {
                recyclerBottomNav.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                        dialog.getActivity(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));

                if (activeBottomNavCategory == null && !sortedGroupTags.isEmpty()) {
                    activeBottomNavCategory = sortedGroupTags.get(0);
                }

                CustomizeBottomNavAdapter adapter = new CustomizeBottomNavAdapter(sortedGroupTags,
                        activeBottomNavCategory, new kotlin.jvm.functions.Function1<String, kotlin.Unit>() {
                            @Override
                            public kotlin.Unit invoke(String tag) {
                                activeBottomNavCategory = tag;
                                for (java.util.Map.Entry<String, LinearLayout> entry : groups.entrySet()) {
                                    boolean isMatch = entry.getKey().equals(tag);
                                    entry.getValue().setVisibility(isMatch ? View.VISIBLE : View.GONE);
                                    View parent = (View) entry.getValue().getParent();
                                    if (parent != null)
                                        parent.setVisibility(isMatch ? View.VISIBLE : View.GONE);
                                }
                                return kotlin.Unit.INSTANCE;
                            }
                        });
                recyclerBottomNav.setAdapter(adapter);

                if (activeBottomNavCategory != null) {
                    for (java.util.Map.Entry<String, LinearLayout> entry : groups.entrySet()) {
                        boolean isMatch = entry.getKey().equals(activeBottomNavCategory);
                        entry.getValue().setVisibility(isMatch ? View.VISIBLE : View.GONE);
                        View parent = (View) entry.getValue().getParent();
                        if (parent != null)
                            parent.setVisibility(isMatch ? View.VISIBLE : View.GONE);
                    }
                }
            }
        }
    }

    void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null
                && data.getData() != null) {
            Uri uri = data.getData();
            try {
                dialog.getActivity().getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
            if (activeImageCustomData != null && activeImagePropertyKey != null) {
                String finalUriString = uri.toString();
                try {
                    java.io.File destDir = new java.io.File(dialog.getActivity().getFilesDir(),
                            "custom_images/user_picked");
                    if (!destDir.exists())
                        destDir.mkdirs();

                    String fileName = "image_" + System.currentTimeMillis() + ".jpg";
                    android.database.Cursor returnCursor = dialog.getActivity().getContentResolver().query(uri, null,
                            null, null, null);
                    if (returnCursor != null && returnCursor.moveToFirst()) {
                        int nameIndex = returnCursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (nameIndex >= 0) {
                            String realName = returnCursor.getString(nameIndex);
                            if (realName != null && realName.contains(".")) {
                                fileName = "image_" + System.currentTimeMillis()
                                        + realName.substring(realName.lastIndexOf('.'));
                            }
                        }
                        returnCursor.close();
                    }
                    java.io.File destFile = new java.io.File(destDir, fileName);
                    java.io.InputStream in = dialog.getActivity().getContentResolver().openInputStream(uri);
                    if (in != null) {
                        java.io.OutputStream out = new java.io.FileOutputStream(destFile);
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                        out.close();
                        in.close();
                        finalUriString = Uri.fromFile(destFile).toString();
                    }
                } catch (Exception e) {
                    tlog.w("Failed to copy image to local storage: " + e.getMessage());
                }

                activeImageCustomData.putPropertyString(activeImagePropertyKey, finalUriString);
                dialog.onPropertyChanged();
                parseCustomizationData(dialog.customizationDataList);
            }
        } else if (requestCode == PICK_FONT_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            java.util.List<Uri> uris = new ArrayList<>();
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    uris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                uris.add(data.getData());
            }

            if (!uris.isEmpty()) {
                String fontsFolder = com.aylis.comp.visual.core.CustomFontManager.getFontsFolder();
                for (Uri uri : uris) {
                    try {
                        String fileName = "font_" + System.currentTimeMillis() + ".ttf";
                        // Try to get original file name
                        android.database.Cursor returnCursor = dialog.getActivity().getContentResolver().query(uri,
                                null, null, null, null);
                        if (returnCursor != null && returnCursor.moveToFirst()) {
                            int nameIndex = returnCursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                            if (nameIndex >= 0) {
                                fileName = returnCursor.getString(nameIndex);
                            }
                            returnCursor.close();
                        }

                        java.io.File destFile = new java.io.File(fontsFolder, fileName);
                        java.io.InputStream in = dialog.getActivity().getContentResolver().openInputStream(uri);
                        java.io.OutputStream out = new java.io.FileOutputStream(destFile);
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                        out.close();
                        in.close();
                    } catch (Exception e) {
                        tlog.w("Failed to import font: " + e.getMessage());
                    }
                }
                com.aylis.comp.visual.core.CustomFontManager.scanFonts();
                dialog.onPropertyChanged();
                parseCustomizationData(dialog.customizationDataList);
            }
        }
    }

    private static String formatPropertyDisplayName(String name) {
        int index = name.indexOf('_');
        if (index >= 0 && index < name.length() - 1) {
            boolean onlyDigitsBefore = true;
            for (int i = 0; i < index; i++) {
                if (!Character.isDigit(name.charAt(i))) {
                    onlyDigitsBefore = false;
                    break;
                }
            }
            if (onlyDigitsBefore) {
                name = name.substring(index + 1);
            }
        }

        StringBuilder sb = new StringBuilder();
        boolean lastLower = false;

        if (name.length() > 0) {
            char c = Character.toUpperCase(name.charAt(0));
            sb.append(c);
            lastLower = Character.isDigit(c);
        }

        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean upper = Character.isUpperCase(c) || Character.isDigit(c);

            if (lastLower && upper)
                sb.append(' ');

            sb.append(c);
            lastLower = !upper;
        }

        return sb.toString();
    }

    private int getGroupWeight(String groupTag) {
        if (groupTag == null)
            return 100;
        String lower = groupTag.toLowerCase(Locale.US).trim();
        if (lower.startsWith("0_") || "general".equals(lower))
            return 0;
        if (lower.startsWith("1_"))
            return 1;
        if (lower.startsWith("2_"))
            return 2;
        if (lower.startsWith("3_"))
            return 3;
        if (lower.startsWith("4_"))
            return 4;
        if (lower.startsWith("5_"))
            return 5;
        switch (lower) {
            case "performance":
                return 10;
            case "spectrum":
                return 11;
            case "spectrum hz":
                return 12;
            case "beat":
                return 13;
            default:
                return 100;
        }
    }

    private java.util.List<String> scanAndSortGroupTags(Element.CustomizationData customData) {
        final java.util.Set<String> tags = new java.util.HashSet<>();
        collectGroupTagsRecursive(customData, tags, true);
        java.util.List<String> sortedTags = new ArrayList<>(tags);
        java.util.Collections.sort(sortedTags, new java.util.Comparator<String>() {
            @Override
            public int compare(String g1, String g2) {
                int w1 = getGroupWeight(g1);
                int w2 = getGroupWeight(g2);
                if (w1 != w2) {
                    return Integer.compare(w1, w2);
                }
                return g1.compareToIgnoreCase(g2);
            }
        });
        return sortedTags;
    }

    private void collectGroupTagsRecursive(Element.CustomizationData customData, java.util.Set<String> outTags,
            boolean isRootOrHoisted) {
        if (customData == null)
            return;
        Iterator<String> keys = customData.GetAllPropertiesSortedByOrder();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith("_"))
                continue;

            String type = customData.getPropertyType(key);
            String[] typeParts = Element.CustomizationData.getPropertyTypeParts(type);
            if (typeParts[0].equals("_child")) {
                if ("00_sampleProvider".equals(key) || "sampleProvider".equals(key)) {
                    Element.CustomizationData childData = customData.getChild(key);
                    collectGroupTagsRecursive(childData, outTags, true);
                    continue;
                }
            }

            if (isRootOrHoisted) {
                String groupTag = customData.getPropertyGroupTag(key);
                if (groupTag == null || groupTag.isEmpty() || "None".equals(groupTag) || "General".equals(groupTag)) {
                    groupTag = "General";
                }
                outTags.add(groupTag);
            }
        }
    }

    private void createGroupView(ViewGroup parent, String groupTag, java.util.Map<String, LinearLayout> groups) {
        if (dialog.getActivity() == null)
            return;

        View containerView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_group_container), null);
        final LinearLayout groupContent = (LinearLayout) containerView.findViewById(R.id.groupContainer);

        groupContent.setVisibility(View.GONE);
        containerView.setVisibility(View.GONE);

        parent.addView(containerView);
        groups.put(groupTag, groupContent);
    }

    private void parseDataRecursive(Element.CustomizationData customData, ViewGroup contentView,
            java.util.Map<String, LinearLayout> groups) {
        if (dialog.getActivity() == null)
            return;
        Iterator<String> keys = customData.GetAllPropertiesSortedByOrder();
        while (keys.hasNext()) {
            String key = keys.next();
            if (key.startsWith("_"))
                continue;

            String groupTag = "General";
            if (groups != null) {
                groupTag = customData.getPropertyGroupTag(key);
                if (groupTag == null || groupTag.isEmpty() || "None".equals(groupTag) || "General".equals(groupTag)) {
                    groupTag = "General";
                }
            } else {
                groupTag = "None";
            }

            LinearLayout targetView;
            if ("None".equals(groupTag)) {
                targetView = (LinearLayout) contentView;
            } else {
                targetView = groups.get(groupTag);
                if (targetView == null) {
                    createGroupView(contentView, groupTag, groups);
                    targetView = groups.get(groupTag);
                }
            }

            String type = customData.getPropertyType(key);
            String[] typeParts = Element.CustomizationData.getPropertyTypeParts(type);
            String hint = customData.getPropertyHint(key);
            String displayName = (hint != null && !hint.isEmpty()) ? hint : formatPropertyDisplayName(key);

            if (typeParts[0].equals("i") && typeParts.length >= 3) {
                int min = Utils.strToIntSafe(typeParts[1], 0);
                int max = Utils.strToIntSafe(typeParts[2], 100);
                createPropertyViewInt(customData, targetView, displayName, key, min, max);
            } else if (typeParts[0].equals("b")) {
                createPropertyViewBool(customData, targetView, displayName, key);
            } else if (typeParts[0].equals("crgb")) {
                createPropertyViewRGBA(customData, targetView, false, displayName, key);
            } else if (typeParts[0].equals("crgba")) {
                createPropertyViewRGBA(customData, targetView, true, displayName, key);
            } else if (typeParts[0].equals("f") && typeParts.length >= 3) {
                float min = Utils.strToFloatSafe(typeParts[1], 0.0f);
                float max = Utils.strToFloatSafe(typeParts[2], 100.0f);
                createPropertyViewFloat(customData, targetView, displayName, key, min, max, (max - min) / 20.0f);
            } else if (typeParts[0].equals("mvarf") && typeParts.length >= 3) {
                float min = Utils.strToFloatSafe(typeParts[1], 0.0f);
                float max = Utils.strToFloatSafe(typeParts[2], 100.0f);
                CustomizeMVarHelper.createPropertyViewMVarFloat(dialog, customData, targetView, displayName, key, min,
                        max, (max - min) / 20.0f);
            } else if (typeParts[0].equals("mvar") && typeParts.length >= 3) {
                float min = Utils.strToFloatSafe(typeParts[1], 0.0f);
                float max = Utils.strToFloatSafe(typeParts[2], 100.0f);
                CustomizeMVarHelper.createPropertyViewMeasuredVar(dialog, customData, targetView, displayName, key, min,
                        max, (max - min) / 20.0f);
            } else if (typeParts[0].equals("f2") && typeParts.length >= 3) {
                float min = Utils.strToFloatSafe(typeParts[1], 0.0f);
                float max = Utils.strToFloatSafe(typeParts[2], 100.0f);
                createPropertyViewVec2f(customData, targetView, displayName, key, min, max, (max - min) / 20.0f);
            } else if (typeParts[0].equals("txt")) {
                createPropertyViewText(customData, targetView, displayName, key);
            } else if (typeParts[0].equals("align")) {
                createPropertyViewAlign(customData, targetView, displayName, key);
            } else if (typeParts[0].equals("shader_code")) {
                createPropertyViewShader(customData, targetView, displayName, key);
            } else if (typeParts[0].equals("img")) {
                createPropertyViewImage(customData, targetView, displayName, key, false);
            } else if (typeParts[0].equals("vid")) {
                createPropertyViewImage(customData, targetView, displayName, key, true);
            } else if (typeParts[0].equals("lbl")) {
                createPropertyViewLabel(customData, targetView, displayName, key);
            } else if (typeParts[0].equals("sel") || typeParts[0].equals("s")) {
                String[] validValues = new String[typeParts.length - 1];
                System.arraycopy(typeParts, 1, validValues, 0, validValues.length);
                createPropertyViewSelect(customData, targetView, displayName, key, validValues);
            } else if (typeParts[0].equals("font")) {
                String[] validValues = new String[typeParts.length - 1];
                System.arraycopy(typeParts, 1, validValues, 0, validValues.length);
                createPropertyViewFont(customData, targetView, displayName, key, validValues);
            } else if (typeParts[0].equals("action")) {
                createPropertyViewAction(customData, targetView, displayName, key);
            } else if (typeParts[0].equals("_child")) {
                String[] validValues = new String[typeParts.length - 1];
                System.arraycopy(typeParts, 1, validValues, 0, validValues.length);

                final Element.CustomizationData childData = customData.getChild(key);
                createChildView(childData, targetView, (ViewGroup) contentView, groups, displayName, key, validValues,
                        groupTag);
            }
        }
    }

    private void createPropertyViewLabel(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name) {
        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_child), null);
        itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);
        // hide right side controls since this is just a label
        View btnExpand = itemView.findViewById(R.id.btnExpand);
        if (btnExpand != null)
            btnExpand.setVisibility(View.GONE);
        contentView.addView(itemView);
    }

    private void createPropertyViewSelect(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name, final String[] validValues) {
        String value = customData.getPropertyString(name, "");

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_child), null);
        itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        TextView spinnerTypes = (TextView) itemView.findViewById(R.id.spinnerType);
        {
            int selection = 0;
            for (int i = 0; i < validValues.length; i++) {
                if (value.equals(validValues[i])) {
                    selection = i;
                    break;
                }
            }

            spinnerTypes.setText(validValues[selection]);
            spinnerTypes.setBackgroundResource(R.drawable.bg_popup_rounded);

            spinnerTypes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.widget.ListPopupWindow popup = new android.widget.ListPopupWindow(dialog.getActivity());
                    popup.setAnchorView(v);
                    popup.setAdapter(
                            new ArrayAdapter<>(dialog.getActivity(), android.R.layout.simple_list_item_1, validValues));
                    popup.setBackgroundDrawable(dialog.getActivity().getDrawable(R.drawable.bg_popup_rounded));
                    popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < validValues.length) {
                                if (!customData.getPropertyString(name, "").equals(validValues[position])) {
                                    customData.putPropertyString(name, validValues[position]);
                                    spinnerTypes.setText(validValues[position]);
                                    dialog.onPropertyChanged();
                                }
                            }
                            popup.dismiss();
                        }
                    });
                    popup.show();
                }
            });
        }
        // removed OnItemSelectedListener as it's handled by ListPopupWindow

        contentView.addView(itemView);
    }

    private void createPropertyViewImage(final Element.CustomizationData customData, ViewGroup targetView,
            String displayName, final String name, final boolean isVideo) {
        final String value = customData.getPropertyString(name, "");

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_image), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.txtPropertyName);
        txtTitle.setText(displayName);

        final ImageView imgIcon = (ImageView) itemView.findViewById(R.id.imgPropertyIcon);

        if (value != null && value.startsWith(ImageElement.PRECOMP_PREFIX)) {
            imgIcon.setImageResource(R.drawable.ic_pick_img);
        } else if (value != null && !value.isEmpty()) {
            if (value.startsWith("file:///android_asset/")) {
                try {
                    String assetPath = value.substring("file:///android_asset/".length());
                    android.graphics.Bitmap bm = android.graphics.BitmapFactory
                            .decodeStream(imgIcon.getContext().getAssets().open(assetPath));
                    imgIcon.setImageBitmap(bm);
                } catch (Exception e) {
                    imgIcon.setImageResource(R.drawable.placeholderart4);
                }
            } else {
                try {
                    imgIcon.setImageURI(Uri.parse(value));
                } catch (Exception e) {
                    imgIcon.setImageResource(R.drawable.placeholderart4);
                }
            }
        } else {
            imgIcon.setImageResource(R.drawable.placeholderart4);
        }

        final TextView txtCurrentValue = itemView.findViewById(R.id.txtCurrentImageValue);
        if (txtCurrentValue != null) {
            if (value != null && value.startsWith(ImageElement.PRECOMP_PREFIX)) {
                txtCurrentValue.setText(value.substring(ImageElement.PRECOMP_PREFIX.length()));
                txtCurrentValue.setVisibility(View.VISIBLE);
            } else if (value != null && value.startsWith("file:///android_asset/")) {
                txtCurrentValue.setText(value.substring("file:///android_asset/".length()));
                txtCurrentValue.setVisibility(View.VISIBLE);
            } else {
                txtCurrentValue.setVisibility(View.GONE);
            }
        }

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageEditor(displayName, customData, name, isVideo);
            }
        };
        imgIcon.setOnClickListener(clickListener);
        itemView.setOnClickListener(clickListener);

        targetView.addView(itemView);
    }

    private void showImageEditor(final String displayName, final Element.CustomizationData customData,
            final String name, final boolean isVideo) {
        android.app.Activity activity = dialog.getActivity();
        if (activity == null)
            return;

        View view = View.inflate(activity,
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_image_picker), null);
        final TextView txtSelectedPath = (TextView) view.findViewById(R.id.txtSelectedPath);
        final ViewGroup layoutItems = (ViewGroup) view.findViewById(R.id.layoutItems);
        final ViewGroup layoutDownloadedItems = (ViewGroup) view.findViewById(R.id.layoutDownloadedItems);
        View btnPickImage = view.findViewById(R.id.btnPickImage);
        View btnUseDefault = view.findViewById(R.id.btnUseDefault);

        final String currentValue = customData.getPropertyString(name, "");
        if (currentValue == null || currentValue.isEmpty()) {
            txtSelectedPath.setText("Default/Empty");
        } else if (currentValue.startsWith(ImageElement.PRECOMP_PREFIX)) {
            txtSelectedPath.setText(currentValue.substring(ImageElement.PRECOMP_PREFIX.length()));
        } else if (currentValue.startsWith("file:///android_asset/")) {
            txtSelectedPath.setText(currentValue.substring("file:///android_asset/".length()));
        } else {
            txtSelectedPath.setText(currentValue);
        }

        final java.util.List<View> itemViews = new java.util.ArrayList<>();

        float density = activity.getResources().getDisplayMetrics().density;
        final int itemSizePx = (int) (72 * density);
        final int itemMarginPx = (int) (6 * density);

        String[] rawNames = com.aylis.comp.visual.core.Elements.PreCompManager.getPreCompNames();
        final java.util.List<String> preCompNames = new java.util.ArrayList<>();
        for (String n : rawNames) {
            if (!"None".equals(n))
                preCompNames.add(n);
        }

        for (final String pcName : preCompNames) {
            final String itemValue = ImageElement.PRECOMP_PREFIX + pcName;
            final View itemView = LayoutInflater.from(activity).inflate(
                    com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_image_picker_item),
                    layoutItems, false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemSizePx, itemSizePx);
            params.setMargins(itemMarginPx, itemMarginPx, itemMarginPx, itemMarginPx);
            itemView.setLayoutParams(params);

            ImageView imgPreview = (ImageView) itemView.findViewById(R.id.imgPreview);
            TextView txtTitle = (TextView) itemView.findViewById(R.id.txtTitle);

            imgPreview.setImageResource(R.drawable.ic_pick_img);
            txtTitle.setText(pcName);
            txtTitle.setVisibility(View.VISIBLE);

            itemView.setSelected(currentValue.equals(itemValue));
            itemViews.add(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    customData.putPropertyString(name, itemValue);
                    dialog.onPropertyChanged();
                    txtSelectedPath.setText(pcName);
                    for (View iv : itemViews) {
                        iv.setSelected(iv == itemView);
                    }
                }
            });

            layoutItems.addView(itemView);
        }

        java.util.List<String> assetFiles = new java.util.ArrayList<>();
        try {
            String[] files = activity.getAssets().list("");
            if (files != null) {
                for (String f : files) {
                    if (f.toLowerCase(java.util.Locale.US).endsWith(".png")
                            || f.toLowerCase(java.util.Locale.US).endsWith(".jpg")) {
                        assetFiles.add(f);
                    }
                }
            }
        } catch (Exception e) {
            tlog.w("Failed to list assets: " + e.getMessage());
        }

        for (final String assetFile : assetFiles) {
            final String itemValue = "file:///android_asset/" + assetFile;
            final View itemView = LayoutInflater.from(activity).inflate(
                    com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_image_picker_item),
                    layoutItems, false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemSizePx, itemSizePx);
            params.setMargins(itemMarginPx, itemMarginPx, itemMarginPx, itemMarginPx);
            itemView.setLayoutParams(params);

            ImageView imgPreview = (ImageView) itemView.findViewById(R.id.imgPreview);

            try {
                java.io.InputStream is = activity.getAssets().open(assetFile);
                android.graphics.Bitmap bm = android.graphics.BitmapFactory.decodeStream(is);
                imgPreview.setImageBitmap(bm);
                is.close();
            } catch (Exception e) {
                imgPreview.setImageResource(R.drawable.placeholderart4);
            }

            itemView.setSelected(currentValue.equals(itemValue));
            itemViews.add(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    customData.putPropertyString(name, itemValue);
                    dialog.onPropertyChanged();
                    txtSelectedPath.setText(assetFile);
                    for (View iv : itemViews) {
                        iv.setSelected(iv == itemView);
                    }
                }
            });

            layoutItems.addView(itemView);
        }

        java.io.File picturesDir = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "OpenPlayer");
        if (layoutDownloadedItems != null && picturesDir.exists() && picturesDir.isDirectory()) {
            java.io.File[] downloadedFiles = picturesDir.listFiles();
            if (downloadedFiles != null) {
                for (final java.io.File file : downloadedFiles) {
                    if (file.isFile() && (file.getName().toLowerCase(java.util.Locale.US).endsWith(".jpg") || file.getName().toLowerCase(java.util.Locale.US).endsWith(".png"))) {
                        final String itemValue = android.net.Uri.fromFile(file).toString();
                        final View itemView = LayoutInflater.from(activity).inflate(
                                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_image_picker_item),
                                layoutDownloadedItems, false);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemSizePx, itemSizePx);
                        params.setMargins(itemMarginPx, itemMarginPx, itemMarginPx, itemMarginPx);
                        itemView.setLayoutParams(params);

                        ImageView imgPreview = (ImageView) itemView.findViewById(R.id.imgPreview);
                        
                        try {
                            imgPreview.setImageURI(android.net.Uri.fromFile(file));
                        } catch (Exception e) {
                            imgPreview.setImageResource(R.drawable.placeholderart4);
                        }

                        itemView.setSelected(currentValue.equals(itemValue));
                        itemViews.add(itemView);

                        itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                customData.putPropertyString(name, itemValue);
                                dialog.onPropertyChanged();
                                txtSelectedPath.setText(file.getName());
                                for (View iv : itemViews) {
                                    iv.setSelected(iv == itemView);
                                }
                            }
                        });

                        layoutDownloadedItems.addView(itemView);
                    }
                }
            }
        }
        
        if (layoutDownloadedItems != null && layoutDownloadedItems.getChildCount() == 0) {
            ((View) layoutDownloadedItems.getParent()).setVisibility(View.GONE);
        }

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeImagePropertyKey = name;
                activeImageCustomData = customData;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType(isVideo ? "video/*" : "image/*");
                dialog.startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        btnUseDefault.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customData.putPropertyString(name, "");
                dialog.onPropertyChanged();
                txtSelectedPath.setText("Default/Empty");
                for (View iv : itemViews) {
                    iv.setSelected(false);
                }
            }
        });

        dialog.showBottomEditor(displayName, view);
    }

    private void showFontEditor(final String displayName, final Element.CustomizationData customData,
            final String name, final TextView spinnerTypes) {
        android.app.Activity activity = dialog.getActivity();
        if (activity == null)
            return;

        View view = View.inflate(activity,
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_font_picker), null);
        
        final TextView txtSelectedFontPath = (TextView) view.findViewById(R.id.txtSelectedFontPath);
        final ViewGroup layoutFonts = (ViewGroup) view.findViewById(R.id.layoutFonts);
        View btnImportFont = view.findViewById(R.id.btnImportFont);

        final String currentValue = customData.getPropertyString(name, "");
        if (currentValue == null || currentValue.isEmpty()) {
            txtSelectedFontPath.setText("Default/Empty");
        } else {
            txtSelectedFontPath.setText(currentValue);
        }

        final java.util.List<View> itemViews = new java.util.ArrayList<>();
        java.util.List<String> fonts = com.aylis.comp.visual.core.CustomFontManager.getAvailableFontNames();

        for (final String font : fonts) {
            final View itemView = LayoutInflater.from(activity).inflate(
                    R.layout.item_font_selection_entry,
                    layoutFonts, false);

            TextView txtFontName = (TextView) itemView.findViewById(R.id.txtFontName);
            ImageView imgSelected = (ImageView) itemView.findViewById(R.id.imgSelected);

            txtFontName.setText(font);
            try {
                android.graphics.Typeface tf = com.aylis.comp.visual.core.CustomFontManager.getTypeface(font);
                if (tf != null) {
                    txtFontName.setTypeface(tf);
                }
            } catch (Exception e) {}

            boolean isSelected = font.equals(currentValue);
            imgSelected.setVisibility(isSelected ? View.VISIBLE : View.GONE);

            itemViews.add(itemView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    customData.putPropertyString(name, font);
                    
                    String prevText = font.length() > 3 ? font.substring(0, 3) : font;
                    spinnerTypes.setText(prevText);
                    try {
                        android.graphics.Typeface tf = com.aylis.comp.visual.core.CustomFontManager.getTypeface(font);
                        if (tf != null) {
                            spinnerTypes.setTypeface(tf);
                        }
                    } catch (Exception e) {}
                    
                    dialog.onPropertyChanged();
                    txtSelectedFontPath.setText(font);
                    
                    for (int i = 0; i < itemViews.size(); i++) {
                        View iv = itemViews.get(i);
                        ImageView ivSelected = (ImageView) iv.findViewById(R.id.imgSelected);
                        ivSelected.setVisibility(iv == itemView ? View.VISIBLE : View.GONE);
                    }
                }
            });

            layoutFonts.addView(itemView);
        }

        btnImportFont.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                String[] mimetypes = {"font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                activeImagePropertyKey = name;
                activeImageCustomData = customData;
                dialog.startActivityForResult(intent, PICK_IMAGE_REQUEST);
            }
        });

        dialog.showBottomEditor(displayName, view);
    }

    private void createPropertyViewShader(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name) {
        String value = customData.getPropertyString(name, "");

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_shader), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        final android.widget.EditText input = (android.widget.EditText) itemView.findViewById(R.id.editTextInput);
        input.setText(value);

        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String newText = s.toString();
                customData.putPropertyString(name, newText);
                dialog.onPropertyChanged();
            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) dialog
                        .getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        contentView.addView(itemView);
    }

    private void createPropertyViewText(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name) {
        String value = customData.getPropertyString(name, "");

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_text), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        final android.widget.EditText input = (android.widget.EditText) itemView.findViewById(R.id.editTextInput);
        input.setText(value);

        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String newText = s.toString();
                customData.putPropertyString(name, newText);
                dialog.onPropertyChanged();
            }
        });

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) dialog
                        .getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        contentView.addView(itemView);
    }

    private void createPropertyViewAlign(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name) {
        final String value = customData.getPropertyString(name, "Center");

        View itemView = View.inflate(dialog.getActivity(), com.aylis.comp.visual.ui.LayoutModeManager
                .getLayout(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_align)), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        final View btnLeft = itemView.findViewById(R.id.btnAlignLeft);
        final View btnCenter = itemView.findViewById(R.id.btnAlignCenter);
        final View btnRight = itemView.findViewById(R.id.btnAlignRight);

        Runnable updateSelection = new Runnable() {
            @Override
            public void run() {
                String curVal = customData.getPropertyString(name, "Center");
                btnLeft.setAlpha("Left".equals(curVal) ? 1.0f : 0.3f);
                btnCenter.setAlpha("Center".equals(curVal) ? 1.0f : 0.3f);
                btnRight.setAlpha("Right".equals(curVal) ? 1.0f : 0.3f);

                btnLeft.setBackgroundResource("Left".equals(curVal) ? R.drawable.bg_button_selected_round : 0);
                btnCenter.setBackgroundResource("Center".equals(curVal) ? R.drawable.bg_button_selected_round : 0);
                btnRight.setBackgroundResource("Right".equals(curVal) ? R.drawable.bg_button_selected_round : 0);
            }
        };

        updateSelection.run();

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newValue = "Center";
                if (v == btnLeft)
                    newValue = "Left";
                else if (v == btnRight)
                    newValue = "Right";

                customData.putPropertyString(name, newValue);
                updateSelection.run();
                dialog.onPropertyChanged();
            }
        };

        btnLeft.setOnClickListener(clickListener);
        btnCenter.setOnClickListener(clickListener);
        btnRight.setOnClickListener(clickListener);

        contentView.addView(itemView);
    }

    private void createPropertyViewFont(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name, final String[] validValues) {
        String value = customData.getPropertyString(name, "");

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_font), null);
        itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // txtTitle initialization removed to rely strictly on XML cosmetics
        // TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        // txtTitle.setText(displayName);

        TextView spinnerTypes = (TextView) itemView.findViewById(R.id.spinnerType);
        String previewText = value.length() > 3 ? value.substring(0, 3) : value;
        spinnerTypes.setText(previewText);
        try {
            android.graphics.Typeface tf = com.aylis.comp.visual.core.CustomFontManager.getTypeface(value);
            if (tf != null) {
                spinnerTypes.setTypeface(tf);
            }
        } catch (Exception e) {}

        View fontSelectionContainer = itemView.findViewById(R.id.fontSelectionContainer);
        if (fontSelectionContainer == null)
            fontSelectionContainer = spinnerTypes;

        fontSelectionContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFontEditor(displayName, customData, name, spinnerTypes);
            }
        });

        contentView.addView(itemView);
    }

    private void createChildView(final Element.CustomizationData childData, ViewGroup parentContentView,
            ViewGroup rootContentView, java.util.Map<String, LinearLayout> groups, String displayName,
            final String name, final String[] validValues, String groupTag) {
        String value = childData.getChildTypeValue();

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_child), null);

        ViewGroup contentView = (ViewGroup) itemView.findViewById(R.id.linearLayoutContent);

        itemView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        if ("None".equals(groupTag)) {
            contentView.setPadding(0, 0, 0, 0);
        }

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        TextView spinnerTypes = (TextView) itemView.findViewById(R.id.spinnerType);
        if (validValues == null || validValues.length <= 1) {
            spinnerTypes.setVisibility(View.GONE);
        } else {
            int selection = 0;
            for (int i = 0; i < validValues.length; i++) {
                if (value.equals(validValues[i])) {
                    selection = i;
                    break;
                }
            }

            spinnerTypes.setText(validValues[selection]);
            spinnerTypes.setBackgroundResource(R.drawable.bg_popup_rounded);

            spinnerTypes.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    android.widget.ListPopupWindow popup = new android.widget.ListPopupWindow(dialog.getActivity());
                    popup.setAnchorView(v);
                    popup.setAdapter(
                            new ArrayAdapter<>(dialog.getActivity(), android.R.layout.simple_list_item_1, validValues));
                    popup.setBackgroundDrawable(dialog.getActivity().getDrawable(R.drawable.bg_popup_rounded));
                    popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            if (position >= 0 && position < validValues.length) {
                                if (!childData.getChildTypeValue().equals(validValues[position])) {
                                    childData.putChildTypeValue(validValues[position]);
                                    spinnerTypes.setText(validValues[position]);
                                    dialog.onChildPropertyChanged();
                                }
                            }
                            popup.dismiss();
                        }
                    });
                    popup.show();
                }
            });
            // removed OnItemSelectedListener as it's handled by ListPopupWindow
        }

        parentContentView.addView(itemView);

        if ("00_sampleProvider".equals(name) || "sampleProvider".equals(name)) {
            parseDataRecursive(childData, rootContentView, groups);
        } else {
            parseDataRecursive(childData, contentView, null);
        }
    }

    private void createPropertyViewAction(final Element.CustomizationData customData, ViewGroup contentView,
            String displayName, final String name) {
        View itemView = View.inflate(dialog.getActivity(), com.aylis.comp.visual.ui.LayoutModeManager
                .getLayout(com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_action)), null);

        android.widget.Button btnAction = (android.widget.Button) itemView.findViewById(R.id.btnAction);
        btnAction.setText(displayName);
        btnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = customData.getPropertyAction(name, 0);
                customData.putPropertyAction(name, count + 1, customData.getPropertyHint(name)); // hint is used for
                                                                                                 // group tag sometimes,
                                                                                                 // but let's just pass
                                                                                                 // null or retrieve it
                                                                                                 // properly. Actually
                                                                                                 // putPropertyAction
                                                                                                 // does not require a
                                                                                                 // hint in this scope,
                                                                                                 // we can just omit
                                                                                                 // group or leave it.
                                                                                                 // Wait,
                                                                                                 // CustomizationData's
                                                                                                 // putPropertyAction
                                                                                                 // has groupTag. We can
                                                                                                 // just use "General"
                                                                                                 // as fallback.
                customData.putPropertyAction(name, count + 1, "0_general");
                dialog.onPropertyChanged();
            }
        });

        contentView.addView(itemView);
    }

    private void createPropertyViewBool(final Element.CustomizationData customData, ViewGroup contentView,
            String displayName, final String name) {
        boolean value = customData.getPropertyBool(name, false);

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_toggle), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        CheckBox checkbox = (CheckBox) itemView.findViewById(R.id.checkbox);
        checkbox.setChecked(value);

        checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                com.aylis.utils.HapticManager.INSTANCE.performClick(buttonView);
                customData.putPropertyBool(name, isChecked);
                dialog.onPropertyChanged();
            }
        });

        contentView.addView(itemView);
    }

    private void createPropertyViewInt(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name, final int min, final int max) {
        final int value = customData.getPropertyInt(name, min);

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_seekbar), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        final TextView txtValue = (TextView) itemView.findViewById(R.id.txtValue);
        txtValue.setText("" + value);

        final ProgressBar progressPreview = (ProgressBar) itemView.findViewById(R.id.progressPreview);
        progressPreview.setMax(max - min);
        progressPreview.setProgress(value - min);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View editorView = View.inflate(dialog.getActivity(),
                        com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_editor_seekbar), null);
                Slider seekBar = (Slider) editorView.findViewById(R.id.materialSlider);

                int currVal = customData.getPropertyInt(name, min);

                final int stepSize;
                final int actualMin;
                final int actualMax;
                if ((max - min) >= 20) {
                    stepSize = 5;
                    actualMin = (int) (Math.floor((double) Math.min(min, currVal) / 5.0) * 5);
                    actualMax = (int) (Math.ceil((double) Math.max(max, currVal) / 5.0) * 5);
                    currVal = Math.round((float) currVal / 5.0f) * 5;
                } else {
                    stepSize = 1;
                    actualMin = Math.min(min, currVal);
                    actualMax = Math.max(max, currVal);
                }

                float valRange = (actualMax - actualMin) > 0 ? (actualMax - actualMin) : 1f;

                seekBar.setValueFrom(0.0f);
                seekBar.setValueTo(valRange);
                seekBar.setStepSize((float) stepSize);
                seekBar.setValue(currVal - actualMin);

                dialog.showBottomEditor(displayName, editorView);
                dialog.editorValue.setInputType(
                        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
                dialog.setEditorValueText("" + currVal);

                seekBar.addOnChangeListener(new Slider.OnChangeListener() {
                    @Override
                    public void onValueChange(@NonNull Slider slider, float valFloat, boolean fromUser) {
                        if (fromUser) {
                            int progress = (int) valFloat;
                            int val = progress + actualMin;
                            val = Math.max(min, val);
                            dialog.setEditorValueText("" + val);
                            txtValue.setText("" + val);
                            progressPreview.setProgress(val - min);
                            customData.putPropertyInt(name, val);
                            dialog.onPropertyChanged();
                        }
                    }
                });

                dialog.activeTextWatcher = new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        if (dialog.isUpdatingProgrammatically)
                            return;
                        try {
                            String str = s.toString().trim();
                            if (str.isEmpty() || str.equals("-"))
                                return;
                            int val = Integer.parseInt(str);
                            val = Math.max(min, val);

                            txtValue.setText("" + val);
                            progressPreview.setProgress(val - min);

                            int progress = val - actualMin;
                            if (stepSize > 1) {
                                progress = Math.round((float) progress / stepSize) * stepSize;
                            }
                            dialog.isUpdatingProgrammatically = true;
                            if (progress > seekBar.getValueTo()) {
                                seekBar.setValueTo(progress);
                            }
                            seekBar.setValue(progress);
                            dialog.isUpdatingProgrammatically = false;

                            customData.putPropertyInt(name, val);
                            dialog.onPropertyChanged();
                        } catch (Exception e) {
                        }
                    }
                };
                dialog.editorValue.addTextChangedListener(dialog.activeTextWatcher);

                dialog.editorValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                            dialog.editorValue.clearFocus();
                            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) dialog
                                    .getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(dialog.editorValue.getWindowToken(), 0);
                            }
                            return true;
                        }
                        return false;
                    }
                });
            }
        });

        contentView.addView(itemView);
    }

    private void createPropertyViewFloat(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name, final float min, final float max, final float step) {
        float value = customData.getPropertyFloat(name, min);

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_seekbar), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        final TextView txtValue = (TextView) itemView.findViewById(R.id.txtValue);
        txtValue.setText(String.format(Locale.US, "%.3f", value));

        final ProgressBar progressPreview = (ProgressBar) itemView.findViewById(R.id.progressPreview);

        final float actualStep;
        float range = max - min;
        if (range < 50.0f) {
            actualStep = 0.005f;
        } else if (range < 200.0f) {
            actualStep = 0.05f;
        } else {
            actualStep = 0.5f;
        }

        int progressMax = (int) ((max - min) / actualStep);
        progressPreview.setMax(progressMax);
        progressPreview.setProgress((int) ((value - min) / actualStep));

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View editorView = View.inflate(dialog.getActivity(),
                        com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_editor_seekbar), null);
                Slider seekBar = (Slider) editorView.findViewById(R.id.materialSlider);

                float currVal = customData.getPropertyFloat(name, min);
                final float actualMin = (float) (Math.floor(Math.min(min, currVal) / actualStep) * actualStep);
                final float actualMax = (float) (Math.ceil(Math.max(max, currVal) / actualStep) * actualStep);

                int progressMaxVal = (int) ((actualMax - actualMin) / actualStep);
                float valRange = progressMaxVal > 0 ? progressMaxVal : 1f;
                seekBar.setValueFrom(0.0f);
                seekBar.setValueTo(valRange);
                seekBar.setStepSize(1.0f);
                seekBar.setValue((int) ((currVal - actualMin) / actualStep));

                dialog.showBottomEditor(displayName, editorView);
                dialog.editorValue.setInputType(
                        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                                | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
                dialog.setEditorValueText(String.format(Locale.US, "%.3f", currVal));

                seekBar.addOnChangeListener(new Slider.OnChangeListener() {
                    @Override
                    public void onValueChange(@NonNull Slider slider, float valFloat, boolean fromUser) {
                        if (fromUser) {
                            int progress = (int) valFloat;
                            float val = (progress * actualStep) + actualMin;
                            val = Math.max(min, val);
                            dialog.setEditorValueText(String.format(Locale.US, "%.3f", val));
                            txtValue.setText(String.format(Locale.US, "%.3f", val));
                            progressPreview.setProgress((int) ((val - min) / actualStep));
                            customData.putPropertyFloat(name, val);
                            dialog.onPropertyChanged();
                        }
                    }
                });

                dialog.activeTextWatcher = new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        if (dialog.isUpdatingProgrammatically)
                            return;
                        try {
                            String str = s.toString().trim().replace(',', '.');
                            if (str.isEmpty() || str.equals("-") || str.equals(".") || str.endsWith("."))
                                return;
                            float val = Float.parseFloat(str);
                            val = Math.max(min, val);

                            txtValue.setText(String.format(Locale.US, "%.3f", val));
                            progressPreview.setProgress((int) ((val - min) / actualStep));

                            int sliderProgress = (int) Math.round((val - actualMin) / actualStep);
                            dialog.isUpdatingProgrammatically = true;
                            if (sliderProgress > seekBar.getValueTo()) {
                                seekBar.setValueTo(sliderProgress);
                            }
                            seekBar.setValue(sliderProgress);
                            dialog.isUpdatingProgrammatically = false;

                            customData.putPropertyFloat(name, val);
                            dialog.onPropertyChanged();
                        } catch (Exception e) {
                        }
                    }
                };
                dialog.editorValue.addTextChangedListener(dialog.activeTextWatcher);

                dialog.editorValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                            dialog.editorValue.clearFocus();
                            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) dialog
                                    .getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(dialog.editorValue.getWindowToken(), 0);
                            }
                            return true;
                        }
                        return false;
                    }
                });
            }
        });

        contentView.addView(itemView);
    }

    private void createPropertyViewVec2f(final Element.CustomizationData customData, ViewGroup contentView,
            final String displayName, final String name, final float min, final float max, final float step) {
        Vec2f value = customData.getPropertyVec2f(name, new Vec2f(min, min));

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_seekbar_xy), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        final TextView txtValue = (TextView) itemView.findViewById(R.id.txtValue);
        txtValue.setText(String.format(Locale.US, "%.3f   %.3f", value.x, value.y));

        final ProgressBar progressPreviewX = (ProgressBar) itemView.findViewById(R.id.progressPreviewX);
        final ProgressBar progressPreviewY = (ProgressBar) itemView.findViewById(R.id.progressPreviewY);

        final float actualStep;
        float range = max - min;
        if (range < 50.0f) {
            actualStep = 0.005f;
        } else if (range < 200.0f) {
            actualStep = 0.05f;
        } else {
            actualStep = 0.5f;
        }

        int progressMax = (int) ((max - min) / actualStep);
        progressPreviewX.setMax(progressMax);
        progressPreviewX.setProgress((int) ((value.x - min) / actualStep));
        progressPreviewY.setMax(progressMax);
        progressPreviewY.setProgress((int) ((value.y - min) / actualStep));

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View editorView = View.inflate(dialog.getActivity(),
                        com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_editor_seekbar_xy),
                        null);
                final Slider seekBarX = (Slider) editorView.findViewById(R.id.seekBarX);
                final Slider seekBarY = (Slider) editorView.findViewById(R.id.seekBarY);

                Vec2f currVal = customData.getPropertyVec2f(name, new Vec2f(min, min));
                final float actualMinX = (float) (Math.floor(Math.min(min, currVal.x) / actualStep) * actualStep);
                final float actualMaxX = (float) (Math.ceil(Math.max(max, currVal.x) / actualStep) * actualStep);
                final float actualMinY = (float) (Math.floor(Math.min(min, currVal.y) / actualStep) * actualStep);
                final float actualMaxY = (float) (Math.ceil(Math.max(max, currVal.y) / actualStep) * actualStep);

                int progressMaxX = (int) ((actualMaxX - actualMinX) / actualStep);
                float valRangeX = progressMaxX > 0 ? progressMaxX : 1f;
                seekBarX.setValueFrom(0.0f);
                seekBarX.setValueTo(valRangeX);
                seekBarX.setStepSize(1.0f);

                int progressMaxY = (int) ((actualMaxY - actualMinY) / actualStep);
                float valRangeY = progressMaxY > 0 ? progressMaxY : 1f;
                seekBarY.setValueFrom(0.0f);
                seekBarY.setValueTo(valRangeY);
                seekBarY.setStepSize(1.0f);

                seekBarX.setValue((int) ((currVal.x - actualMinX) / actualStep));
                seekBarY.setValue((int) ((currVal.y - actualMinY) / actualStep));

                dialog.showBottomEditor(displayName, editorView);
                dialog.editorValue.setInputType(
                        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                dialog.setEditorValueText(String.format(Locale.US, "%.3f   %.3f", currVal.x, currVal.y));

                Slider.OnChangeListener listener = new Slider.OnChangeListener() {
                    @Override
                    public void onValueChange(@NonNull Slider slider, float valFloat, boolean fromUser) {
                        if (fromUser) {
                            float valX = (seekBarX.getValue() * actualStep) + actualMinX;
                            float valY = (seekBarY.getValue() * actualStep) + actualMinY;
                            valX = Math.max(min, valX);
                            valY = Math.max(min, valY);
                            Vec2f newVal = new Vec2f(valX, valY);

                            dialog.setEditorValueText(String.format(Locale.US, "%.3f   %.3f", valX, valY));
                            txtValue.setText(String.format(Locale.US, "%.3f   %.3f", valX, valY));

                            progressPreviewX.setProgress((int) ((valX - min) / actualStep));
                            progressPreviewY.setProgress((int) ((valY - min) / actualStep));

                            customData.putPropertyVec2f(name, newVal);
                            dialog.onPropertyChanged();
                        }
                    }
                };

                seekBarX.addOnChangeListener(listener);
                seekBarY.addOnChangeListener(listener);

                dialog.activeTextWatcher = new android.text.TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(android.text.Editable s) {
                        if (dialog.isUpdatingProgrammatically)
                            return;
                        try {
                            String str = s.toString().trim();
                            String[] parts = str.split("[\\s,]+");
                            if (parts.length == 0 || parts[0].isEmpty())
                                return;

                            Vec2f currentVec = customData.getPropertyVec2f(name, new Vec2f(min, min));
                            float valX = currentVec.x;
                            float valY = currentVec.y;

                            String partX = parts[0].replace(',', '.');
                            if (!partX.equals("-") && !partX.equals(".") && !partX.endsWith(".")) {
                                valX = Float.parseFloat(partX);
                                valX = Math.max(min, valX);
                            }

                            if (parts.length >= 2) {
                                String partY = parts[1].replace(',', '.');
                                if (!partY.equals("-") && !partY.equals(".") && !partY.endsWith(".")) {
                                    valY = Float.parseFloat(partY);
                                    valY = Math.max(min, valY);
                                }
                            }

                            Vec2f newVal = new Vec2f(valX, valY);

                            txtValue.setText(String.format(Locale.US, "%.3f   %.3f", valX, valY));
                            progressPreviewX.setProgress((int) ((valX - min) / actualStep));
                            progressPreviewY.setProgress((int) ((valY - min) / actualStep));

                            int progressX = (int) Math.round((valX - actualMinX) / actualStep);
                            int progressY = (int) Math.round((valY - actualMinY) / actualStep);

                            dialog.isUpdatingProgrammatically = true;
                            if (progressX > seekBarX.getValueTo()) {
                                seekBarX.setValueTo(progressX);
                            }
                            if (progressY > seekBarY.getValueTo()) {
                                seekBarY.setValueTo(progressY);
                            }
                            seekBarX.setValue(progressX);
                            seekBarY.setValue(progressY);
                            dialog.isUpdatingProgrammatically = false;

                            customData.putPropertyVec2f(name, newVal);
                            dialog.onPropertyChanged();
                        } catch (Exception e) {
                        }
                    }
                };
                dialog.editorValue.addTextChangedListener(dialog.activeTextWatcher);

                dialog.editorValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                            dialog.editorValue.clearFocus();
                            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) dialog
                                    .getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(dialog.editorValue.getWindowToken(), 0);
                            }
                            return true;
                        }
                        return false;
                    }
                });
            }
        });

        contentView.addView(itemView);
    }

    private void setOpacityBarGradient(SeekBar opacityBar, int baseColor) {
        int startColor = baseColor & 0x00FFFFFF;
        int endColor = baseColor | 0xFF000000;

        float density = opacityBar.getContext().getResources().getDisplayMetrics().density;
        int trackHeight = (int) (6f * density);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { startColor, endColor });
        gd.setCornerRadius(trackHeight / 2f);
        gd.setSize(-1, trackHeight);

        android.graphics.drawable.GradientDrawable progress = new android.graphics.drawable.GradientDrawable();
        progress.setColor(Color.TRANSPARENT);
        progress.setSize(-1, trackHeight);

        android.graphics.drawable.Drawable[] layers = new android.graphics.drawable.Drawable[2];
        layers[0] = gd;
        layers[1] = progress;

        android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(layers);
        ld.setId(0, android.R.id.background);
        ld.setId(1, android.R.id.progress);

        opacityBar.setProgressDrawable(ld);
    }

    private void setBrightnessBarGradient(SeekBar brightnessBar, float[] hsv) {
        float[] startHsv = new float[] { hsv[0], hsv[1], 0f };
        float[] endHsv = new float[] { hsv[0], hsv[1], 1f };

        int startColor = Color.HSVToColor(255, startHsv);
        int endColor = Color.HSVToColor(255, endHsv);

        float density = brightnessBar.getContext().getResources().getDisplayMetrics().density;
        int trackHeight = (int) (6f * density);

        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] { startColor, endColor });
        gd.setCornerRadius(trackHeight / 2f);
        gd.setSize(-1, trackHeight);

        android.graphics.drawable.GradientDrawable progress = new android.graphics.drawable.GradientDrawable();
        progress.setColor(Color.TRANSPARENT);
        progress.setSize(-1, trackHeight);

        android.graphics.drawable.Drawable[] layers = new android.graphics.drawable.Drawable[2];
        layers[0] = gd;
        layers[1] = progress;

        android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(layers);
        ld.setId(0, android.R.id.background);
        ld.setId(1, android.R.id.progress);

        brightnessBar.setProgressDrawable(ld);
    }

    private void showColorEditor(String displayName, final Element.CustomizationData customData, final String name,
            final boolean showOpacityBar, final View colorPreview, final TextView txtHexValue) {
        int initialColor = customData.getPropertyInt(name, 0xffffffff);

        View editorView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_editor_color), null);
        final ColorWheelView wheel = (ColorWheelView) editorView.findViewById(R.id.colorWheel);
        final SeekBar brightnessBar = (SeekBar) editorView.findViewById(R.id.brightnessBar);
        View layoutOpacity = editorView.findViewById(R.id.layoutOpacity);
        final SeekBar opacityBar = (SeekBar) editorView.findViewById(R.id.opacityBar);

        wheel.setColor(initialColor);

        final float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);

        brightnessBar.setMax(255);
        brightnessBar.setProgress((int) (hsv[2] * 255f));

        brightnessBar.setProgressTintList(null);
        brightnessBar.setProgressBackgroundTintList(null);
        brightnessBar.setThumbTintList(null);

        setBrightnessBarGradient(brightnessBar, hsv);

        float density = dialog.getActivity().getResources().getDisplayMetrics().density;
        android.graphics.drawable.GradientDrawable thumb1 = new android.graphics.drawable.GradientDrawable();
        thumb1.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        int thumbSize = (int) (18f * density);
        thumb1.setSize(thumbSize, thumbSize);
        thumb1.setColor(Color.WHITE);
        brightnessBar.setThumb(thumb1);
        brightnessBar.setPadding((int) (12f * density), (int) (10f * density), (int) (12f * density),
                (int) (10f * density));

        opacityBar.setProgressTintList(null);
        opacityBar.setProgressBackgroundTintList(null);
        opacityBar.setThumbTintList(null);

        if (showOpacityBar) {
            layoutOpacity.setVisibility(View.VISIBLE);
            opacityBar.setMax(255);
            opacityBar.setProgress(Color.alpha(initialColor));
            setOpacityBarGradient(opacityBar, initialColor);

            android.graphics.drawable.GradientDrawable thumb2 = new android.graphics.drawable.GradientDrawable();
            thumb2.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            thumb2.setSize(thumbSize, thumbSize);
            thumb2.setColor(Color.WHITE);
            opacityBar.setThumb(thumb2);
            opacityBar.setPadding((int) (12f * density), (int) (10f * density), (int) (12f * density),
                    (int) (10f * density));
        } else {
            layoutOpacity.setVisibility(View.GONE);
        }

        ColorWheelView.OnColorSelectedListener colorWheelListener = new ColorWheelView.OnColorSelectedListener() {
            @Override
            public void onColorSelected(int color) {
                if (dialog.isUpdatingProgrammatically)
                    return;
                float[] newHsv = new float[3];
                Color.colorToHSV(color, newHsv);
                hsv[0] = newHsv[0];
                hsv[1] = newHsv[1];

                int finalColor = Color.HSVToColor(showOpacityBar ? opacityBar.getProgress() : 255, hsv);
                customData.putPropertyInt(name, finalColor);

                setBrightnessBarGradient(brightnessBar, hsv);
                if (showOpacityBar) {
                    setOpacityBarGradient(opacityBar, finalColor);
                }

                dialog.updateEditorValueText(finalColor);
                txtHexValue.setText(String.format(Locale.US, "%08X", finalColor));
                colorPreview.setBackgroundColor(finalColor);
                dialog.onPropertyChanged();
            }
        };
        wheel.setOnColorSelectedListener(colorWheelListener);

        brightnessBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (dialog.isUpdatingProgrammatically)
                    return;
                if (fromUser) {
                    float brightness = progress / 255f;
                    hsv[2] = brightness;
                    wheel.setBrightness(brightness);

                    int finalColor = Color.HSVToColor(showOpacityBar ? opacityBar.getProgress() : 255, hsv);
                    customData.putPropertyInt(name, finalColor);

                    if (showOpacityBar) {
                        setOpacityBarGradient(opacityBar, finalColor);
                    }

                    dialog.updateEditorValueText(finalColor);
                    txtHexValue.setText(String.format(Locale.US, "%08X", finalColor));
                    colorPreview.setBackgroundColor(finalColor);
                    dialog.onPropertyChanged();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
            }
        });

        if (showOpacityBar) {
            opacityBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                    if (dialog.isUpdatingProgrammatically)
                        return;
                    if (fromUser) {
                        int finalColor = Color.HSVToColor(progress, hsv);
                        customData.putPropertyInt(name, finalColor);

                        dialog.updateEditorValueText(finalColor);
                        txtHexValue.setText(String.format(Locale.US, "%08X", finalColor));
                        colorPreview.setBackgroundColor(finalColor);
                        dialog.onPropertyChanged();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar sb) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar sb) {
                }
            });
        }

        dialog.updateEditorValueText(initialColor);
        dialog.showBottomEditor(displayName, editorView);
        dialog.editorValue.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        dialog.activeTextWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (dialog.isUpdatingProgrammatically)
                    return;
                try {
                    String hexStr = s.toString().trim();
                    if (hexStr.startsWith("#")) {
                        hexStr = hexStr.substring(1);
                    }
                    int parsedColor;
                    if (hexStr.length() == 6) {
                        parsedColor = (int) Long.parseLong("FF" + hexStr, 16);
                    } else if (hexStr.length() == 8) {
                        if (!showOpacityBar) {
                            parsedColor = (int) Long.parseLong("FF" + hexStr.substring(2), 16);
                        } else {
                            parsedColor = (int) Long.parseLong(hexStr, 16);
                        }
                    } else {
                        return;
                    }

                    customData.putPropertyInt(name, parsedColor);
                    Color.colorToHSV(parsedColor, hsv);

                    dialog.isUpdatingProgrammatically = true;
                    wheel.setColor(parsedColor);
                    if (brightnessBar != null) {
                        brightnessBar.setProgress((int) (hsv[2] * 255f));
                        setBrightnessBarGradient(brightnessBar, hsv);
                    }
                    if (showOpacityBar && opacityBar != null) {
                        opacityBar.setProgress(Color.alpha(parsedColor));
                        setOpacityBarGradient(opacityBar, parsedColor);
                    }
                    dialog.isUpdatingProgrammatically = false;

                    txtHexValue.setText(String.format(Locale.US, "%08X", parsedColor));
                    colorPreview.setBackgroundColor(parsedColor);
                    dialog.onPropertyChanged();
                } catch (Exception e) {
                }
            }
        };
        dialog.editorValue.addTextChangedListener(dialog.activeTextWatcher);

        dialog.editorValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, android.view.KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                    dialog.editorValue.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) dialog
                            .getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(dialog.editorValue.getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void createPropertyViewRGBA(final Element.CustomizationData customData, ViewGroup contentView,
            final boolean showOpacityBar, final String displayName, final String name) {
        int value = customData.getPropertyInt(name, 0xffffffff);

        View itemView = View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_color), null);

        TextView txtTitle = (TextView) itemView.findViewById(R.id.title);
        txtTitle.setText(displayName);

        final View colorPreview = itemView.findViewById(R.id.colorPreview);
        colorPreview.setBackgroundColor(value);

        final TextView txtHexValue = (TextView) itemView.findViewById(R.id.txtHexValue);
        txtHexValue.setText(String.format(Locale.US, "%08X", value));

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorEditor(displayName, customData, name, showOpacityBar, colorPreview, txtHexValue);
            }
        });

        contentView.addView(itemView);
    }
}
