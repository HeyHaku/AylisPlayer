
package com.aylis.comp.visual.ui;

import static com.aylis.R.id.txtItemDescription;

import android.app.Dialog;
import android.content.DialogInterface;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.aylis.R;
import com.aylis.comp.AppPreferences.AppPreferences;
import com.aylis.comp.visual.core.Elements.Element;
import com.aylis.comp.visual.scene.VisualizerScene;
import com.aylis.comp.visual.scene.SceneElement;
import com.aylis.comp.visual.scene.SceneProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.widget.ImageView;

public class CustomizeMain1 {

    private final CustomizeVisDialog dialog;
    private final List<ElementNode> treeNodes = new ArrayList<>();
    private final List<Integer> validIndices = new ArrayList<>();
    private final List<VisualizerItem> displayedItems = new ArrayList<>();
    private static final Set<String> expandedPreCompIds = new HashSet<>();
    private static int expandedMenuElementIndex = -1;
    private ItemTouchHelper itemTouchHelper;

    private void rebuildValidIndices() {
        validIndices.clear();
        if (dialog.customizationDataList == null)
            return;
        for (int i = 0; i < dialog.customizationDataList.dataCount(); i++) {
            Element.CustomizationData data = dialog.customizationDataList.getData(i);
            if (data == null)
                continue;
            String name = data.getCustomizationName();
            if (name == null || name.isEmpty())
                continue;
            validIndices.add(i);
        }
    }

    public static class ElementNode {
        public int index;
        public SceneElement sceneElement;
        public int parentIndex;
        public boolean isPreComp;
    }

    public CustomizeMain1(CustomizeVisDialog dialog) {
        this.dialog = dialog;
    }

    private void buildTreeInfo(List<ElementNode> nodes, List<SceneElement> elements, int parentIndex, int[] nextIndex) {
        for (int i = 0; i < elements.size(); i++) {
            SceneElement el = elements.get(i);
            ElementNode node = new ElementNode();
            node.index = nextIndex[0];
            node.sceneElement = el;
            node.parentIndex = parentIndex;
            node.isPreComp = "PreCompElement".equals(el.getType());
            nodes.add(node);
            nextIndex[0]++;

            if (el.getChildren() != null && !el.getChildren().isEmpty()) {
                buildTreeInfo(nodes, el.getChildren(), node.index, nextIndex);
            }
        }
    }

    private boolean canMoveUp(int index) {
        if (index <= 2 || index >= treeNodes.size())
            return false;
        if (treeNodes.get(index).isPreComp)
            return false;
        if (treeNodes.get(index - 1).isPreComp)
            return false;
        return treeNodes.get(index).parentIndex == treeNodes.get(index - 1).parentIndex;
    }

    private boolean canMoveDown(int index) {
        if (index < 0 || index >= treeNodes.size() - 1)
            return false;
        if (treeNodes.get(index).isPreComp)
            return false;
        if (treeNodes.get(index + 1).isPreComp)
            return false;
        return treeNodes.get(index).parentIndex == treeNodes.get(index + 1).parentIndex;
    }

    void showElementList() {
        if (dialog.getActivity() == null)
            return;
        if (dialog.customizationDataList == null)
            return;

        dialog.expandedGroups.clear();
        dialog.hideBottomEditor();
        dialog.txtElementTitle.setText(R.string.visualizer_customization);
        if (dialog.btnBack != null)
            dialog.btnBack.setVisibility(View.GONE);

        if (dialog.layoutCustomizeMain1 != null)
            dialog.layoutCustomizeMain1.setVisibility(View.VISIBLE);
        if (dialog.layoutCustomizeMain2 != null)
            dialog.layoutCustomizeMain2.setVisibility(View.GONE);

        rebuildValidIndices();
        treeNodes.clear();
        ElementNode rootNode = new ElementNode();
        rootNode.index = 0;
        rootNode.sceneElement = null;
        rootNode.parentIndex = -1;
        rootNode.isPreComp = false;
        treeNodes.add(rootNode);

        int[] nextIndex = new int[] { 1 };
        VisualizerScene scene = AppPreferences.createOrGetInstance().getPrefThemeScene(dialog.rootIdentifier);
        if (scene != null) {
            buildTreeInfo(treeNodes, scene.getElements(), 0, nextIndex);
        }

        List<VisualizerItem> tree = PreCompModelBuilder.buildTree(treeNodes, dialog.customizationDataList,
                expandedPreCompIds);
        displayedItems.clear();
        displayedItems.addAll(PreCompModelBuilder.flattenItems(tree));

        if (dialog.recyclerViewElements == null)
            return;

        if (dialog.recyclerViewElements.getLayoutManager() == null) {
            dialog.recyclerViewElements.setLayoutManager(new LinearLayoutManager(dialog.getActivity()));
        }

        ElementsAdapter adapter = (ElementsAdapter) dialog.recyclerViewElements.getAdapter();
        if (adapter == null) {
            adapter = new ElementsAdapter();
            dialog.recyclerViewElements.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }

        setupDragAndDrop();
    }

    private void showAddElementDialog(final int preCompIndex) {
        if (dialog.getActivity() == null)
            return;

        final java.util.List<String> types = new java.util.ArrayList<>(
                com.aylis.comp.visual.core.Elements.ElementsFactory.getAddableTypeNames());
        types.remove("PreCompElement");

        android.view.View dialogView = android.view.View.inflate(dialog.getActivity(),
                com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_dialog_add_element),
                null);
        android.widget.GridView gridView = (android.widget.GridView) dialogView.findViewById(R.id.elementGridView);

        final android.app.Dialog addDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(
                dialog.getActivity())
                .setTitle("Add Element")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .create();

        gridView.setAdapter(new android.widget.BaseAdapter() {
            @Override
            public int getCount() {
                return types.size();
            }

            @Override
            public Object getItem(int position) {
                return types.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public android.view.View getView(int position, android.view.View convertView,
                    android.view.ViewGroup parent) {
                if (convertView == null) {
                    convertView = android.view.View.inflate(dialog.getActivity(),
                            com.aylis.comp.visual.ui.LayoutModeManager
                                    .getLayout(R.layout.customize_item_add_element),
                            null);
                }

                android.widget.TextView txtName = convertView.findViewById(R.id.txtElementName);
                android.widget.ImageView imgPreview = convertView.findViewById(R.id.imgElementPreview);

                String type = types.get(position);

                String displayName = com.aylis.comp.visual.core.Elements.ElementsFactory.getElementDisplayName(type);
                txtName.setText(displayName);

                String cleanName = type;
                if (cleanName.endsWith("Element")) {
                    cleanName = cleanName.substring(0, cleanName.length() - 7);
                }
                if (cleanName.endsWith("DataProvider")) {
                    cleanName = cleanName.substring(0, cleanName.length() - 12);
                }
                if (cleanName.endsWith("Effect")) {
                    cleanName = cleanName.substring(0, cleanName.length() - 6);
                }

                if (cleanName.equals("3DBox")) {
                    cleanName = "box3d";
                }

                String resName = cleanName.toLowerCase();

                int imageResId = dialog.getActivity().getResources().getIdentifier(
                        resName,
                        "drawable",
                        dialog.getActivity().getPackageName());

                if (imageResId != 0) {

                    imgPreview.setVisibility(android.view.View.VISIBLE);
                    imgPreview.setImageResource(imageResId);
                } else {

                    imgPreview.setVisibility(android.view.View.VISIBLE);
                    imgPreview.setImageResource(R.drawable.placeholderart4);
                }

                return convertView;
            }
        });

        gridView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, android.view.View view, int position,
                    long id) {
                if (position >= 0 && position < types.size()) {
                    modifyScene(3, preCompIndex, types.get(position));
                    addDialog.dismiss();
                }
            }
        });

        addDialog.show();
    }

    private void setupDragAndDrop() {
        if (itemTouchHelper == null && dialog.recyclerViewElements != null) {
            ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                boolean wasDragged = false;

                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                        RecyclerView.ViewHolder target) {
                    int fromPos = viewHolder.getAdapterPosition();
                    int toPos = target.getAdapterPosition();

                    if (fromPos < 0 || fromPos >= displayedItems.size() || toPos < 0
                            || toPos >= displayedItems.size()) {
                        return false;
                    }

                    VisualizerItem fromItem = displayedItems.get(fromPos);
                    VisualizerItem toItem = displayedItems.get(toPos);

                    if (fromItem.isPreComp() || toItem.isPreComp()) {
                        return false;
                    }

                    if (fromItem.getDepth() != toItem.getDepth()) {
                        return false;
                    }

                    int fromIndex = fromItem.getIndex();
                    int toIndex = toItem.getIndex();

                    if (fromIndex <= 2 || toIndex <= 2) {
                        return false;
                    }

                    if (fromIndex >= treeNodes.size() || toIndex >= treeNodes.size()) {
                        return false;
                    }

                    if (treeNodes.get(fromIndex).parentIndex != treeNodes.get(toIndex).parentIndex) {
                        return false;
                    }

                    if (toPos < fromPos) {
                        if (!canMoveUp(fromIndex))
                            return false;
                        modifySceneMove(fromIndex, toIndex, true);
                    } else {
                        if (!canMoveDown(fromIndex))
                            return false;
                        modifySceneMove(fromIndex, toIndex, true);
                    }

                    wasDragged = true;
                    if (recyclerView.getAdapter() != null) {
                        recyclerView.getAdapter().notifyItemMoved(fromPos, toPos);

                        // Force drop the drag immediately after 1 step
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                dialog.showElementList();
                            }
                        });
                    }
                    com.aylis.utils.HapticManager.INSTANCE.performTick(recyclerView);
                    return true;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                }

                @Override
                public boolean isLongPressDragEnabled() {
                    return true;
                }

                @Override
                public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                    super.onSelectedChanged(viewHolder, actionState);
                    if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                        viewHolder.itemView.setAlpha(0.6f);
                        com.aylis.utils.HapticManager.INSTANCE.performLongPress(viewHolder.itemView);
                    }
                }

                @Override
                public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    if (viewHolder != null) {
                        viewHolder.itemView.setAlpha(1.0f);
                    }
                    if (wasDragged) {
                        wasDragged = false;
                        AppPreferences.onThemeSceneChanged.invoke(dialog.rootIdentifier);
                        dialog.onPropertyChanged();
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                showElementList();
                            }
                        });
                    }
                }
            };
            itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(dialog.recyclerViewElements);
        }
    }

    private class ElementsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_LIST_ITEM = 0;
        private static final int VIEW_TYPE_COMP_ITEM = 1;
        private static final int VIEW_TYPE_ADD_BUTTON = 2;

        @Override
        public int getItemViewType(int position) {
            if (position == displayedItems.size()) {
                return VIEW_TYPE_ADD_BUTTON;
            }
            VisualizerItem item = displayedItems.get(position);
            int index = item.getIndex();
            Element.CustomizationData data = dialog.customizationDataList.getData(index);
            String name = data != null ? data.getCustomizationName() : "";
            boolean isPreComp = item instanceof PreCompItem;
            if ("Master Scene".equals(name) || "Composition final".equals(name) || isPreComp) {
                return VIEW_TYPE_COMP_ITEM;
            }
            return VIEW_TYPE_LIST_ITEM;
        }

        @Override
        public int getItemCount() {
            return dialog.customizationDataList != null ? displayedItems.size() + 1 : 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(dialog.getActivity());
            if (viewType == VIEW_TYPE_ADD_BUTTON) {
                int layoutId = com.aylis.comp.visual.ui.LayoutModeManager.getLayout(
                        com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_item_element));
                View v = inflater.inflate(layoutId, parent, false);
                return new AddButtonViewHolder(v);
            } else if (viewType == VIEW_TYPE_COMP_ITEM) {
                int layoutId = com.aylis.comp.visual.ui.LayoutModeManager.getLayout(
                        com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_item_composition));
                View v = inflater.inflate(layoutId, parent, false);
                return new ElementViewHolder(v);
            } else {
                int layoutId = com.aylis.comp.visual.ui.LayoutModeManager.getLayout(
                        com.aylis.comp.visual.ui.LayoutModeManager.getLayout(R.layout.customize_item_element));
                View v = inflater.inflate(layoutId, parent, false);
                return new ElementViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == VIEW_TYPE_ADD_BUTTON) {
                AddButtonViewHolder addHolder = (AddButtonViewHolder) holder;
                addHolder.txtName.setText("New Scene");
                addHolder.txtName.setTextColor(0xFF2196F3);
                addHolder.itemView.setSelected(false);
                addHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        modifyScene(3, -2, "PreCompElement");
                    }
                });
            } else if (holder instanceof ElementViewHolder) {
                final VisualizerItem item = displayedItems.get(position);
                final int index = item.getIndex();
                Element.CustomizationData data = dialog.customizationDataList.getData(index);
                final String name = data != null ? data.getCustomizationName() : "";
                final boolean isPreComp = item instanceof PreCompItem;
                final boolean isCompositionFinal = "Master Scene".equals(name) || "Composition final".equals(name);

                ElementViewHolder elHolder = (ElementViewHolder) holder;
                final int fPosition = position;

                if (elHolder.indentContainer != null) {
                    elHolder.indentContainer.removeAllViews();
                    int indentWidth = (int) (12 * dialog.getActivity().getResources().getDisplayMetrics().density);
                    elHolder.indentContainer.setMinimumWidth(item.getDepth() * indentWidth);
                }

                if (elHolder.btnDragHandle != null) {
                    elHolder.btnDragHandle.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, android.view.MotionEvent event) {
                            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                                if (itemTouchHelper != null) {
                                    itemTouchHelper.startDrag(elHolder);
                                }
                            }
                            return false;
                        }
                    });
                }

                if (elHolder.imgIndicator != null) {
                    if (isPreComp) {
                        elHolder.imgIndicator.setVisibility(View.VISIBLE);
                        PreCompItem preComp = (PreCompItem) item;
                        elHolder.imgIndicator.setRotation(preComp.isExpanded() ? 0f : -90f);
                    } else {
                        elHolder.imgIndicator.setVisibility(View.GONE);
                    }
                }
                elHolder.txtName.setText(name);
                if (elHolder.txtName != null) {
                    if (clipboardElement != null && (isPreComp || isCompositionFinal)) {
                        elHolder.txtName.setTextColor(0xFF00E5FF);
                    } else {
                        elHolder.txtName.setTextColor(com.aylis.Common.UtilsUI.getAttrColor(elHolder.itemView,
                                android.R.attr.textColorPrimary));
                    }
                }

                if (elHolder.txtDescription != null) {
                    String description = data != null ? data.getPropertyString("description", "") : "";
                    if (description.isEmpty()) {
                        elHolder.txtDescription.setVisibility(View.GONE);
                    } else {
                        elHolder.txtDescription.setVisibility(View.VISIBLE);
                        elHolder.txtDescription.setText(description);
                    }
                }
                elHolder.itemView.setSelected(index == dialog.lastSelectedElementIndex);

                if (elHolder.btnToggleVisibility != null && elHolder.imgVisibility != null) {
                    final Element.CustomizationData fData = data;
                    boolean isElementVisible = fData != null ? fData.getPropertyBool("visible", true) : true;
                    elHolder.imgVisibility
                            .setImageResource(isElementVisible ? R.drawable.ic_vis_on : R.drawable.ic_vis_off);
                    elHolder.btnToggleVisibility.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (fData != null) {
                                boolean nextState = !fData.getPropertyBool("visible", true);
                                fData.putPropertyBool("visible", nextState);
                                dialog.onPropertyChanged();
                                notifyItemChanged(fPosition);
                            }
                        }
                    });
                }

                if (elHolder.btnAddInside != null) {
                    elHolder.btnAddInside.setVisibility((isPreComp || isCompositionFinal) ? View.VISIBLE : View.GONE);
                    elHolder.btnAddInside.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isCompositionFinal) {
                                showAddElementDialog(-1);
                            } else {
                                showAddElementDialog(index);
                            }
                        }
                    });
                }

                elHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        int[] location = new int[2];
                        v.getLocationOnScreen(location);
                        int screenHeight = dialog.getActivity().getResources().getDisplayMetrics().heightPixels;
                        int gravity = android.view.Gravity.END;
                        if (location[1] > screenHeight / 2) {
                            gravity |= android.view.Gravity.BOTTOM;
                        }
                        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(
                                dialog.getActivity(), v, gravity);

                        if (!isCompositionFinal) {
                            if (canMoveUp(index)) {
                                popup.getMenu().add(0, 10, 0, "Move Up");
                            }
                            if (canMoveDown(index)) {
                                popup.getMenu().add(0, 11, 1, "Move Down");
                            }
                            if (!isPreComp) {
                                popup.getMenu().add(0, 2, 3, "Copy");
                            }
                            popup.getMenu().add(0, 0, 4, "Delete");
                        }

                        if (clipboardElement != null && (isPreComp || isCompositionFinal)) {
                            popup.getMenu().add(0, 3, 5, "Paste");
                        }

                        if (popup.getMenu().size() == 0) {
                            return false;
                        }

                        popup.setOnMenuItemClickListener(
                                new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        int itemId = item.getItemId();
                                        if (itemId == 10) {
                                            modifySceneMove(index, index - 1);
                                            return true;
                                        } else if (itemId == 11) {
                                            modifySceneMove(index, index + 1);
                                            return true;
                                        } else if (itemId == 2) {
                                            copyToClipboard(index);
                                            return true;
                                        } else if (itemId == 3) {
                                            pasteFromClipboard(isCompositionFinal ? -1 : index);
                                            return true;
                                        } else if (itemId == 0) {
                                            modifyScene(0, index, null);
                                            return true;
                                        }
                                        return false;
                                    }
                                });
                        popup.show();
                        return true;
                    }
                });

                elHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (item instanceof PreCompItem) {
                            PreCompItem preComp = (PreCompItem) item;
                            preComp.setExpanded(!preComp.isExpanded());
                            if (preComp.isExpanded()) {
                                expandedPreCompIds.add(preComp.getName());
                            } else {
                                expandedPreCompIds.remove(preComp.getName());
                            }

                            List<VisualizerItem> tree = PreCompModelBuilder.buildTree(treeNodes,
                                    dialog.customizationDataList, expandedPreCompIds);
                            displayedItems.clear();
                            displayedItems.addAll(PreCompModelBuilder.flattenItems(tree));
                            notifyDataSetChanged();
                        } else {
                            dialog.customizationIndex = index;
                            dialog.lastSelectedElementIndex = index;
                            dialog.parseCustomizationData(dialog.customizationDataList);
                            dialog.onPropertyChanged();
                        }
                    }
                });
            }
        }
    }

    private static class AddButtonViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        TextView txtDescription;

        AddButtonViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtItemName);
            txtDescription = itemView.findViewById(txtItemDescription);
            View btnUp = itemView.findViewById(R.id.btnMoveUp);
            View btnDown = itemView.findViewById(R.id.btnMoveDown);
            View btnAddInside = itemView.findViewById(R.id.btnAddInside);
            if (btnUp != null)
                btnUp.setVisibility(View.GONE);
            if (btnDown != null)
                btnDown.setVisibility(View.GONE);
            if (btnAddInside != null)
                btnAddInside.setVisibility(View.GONE);
        }
    }

    private static class ElementViewHolder extends RecyclerView.ViewHolder {
        TextView txtName;
        TextView txtDescription;
        ImageView imgIndicator;
        View btnAddInside;
        View btnToggleVisibility;
        ImageView imgVisibility;
        View btnDragHandle;
        android.widget.LinearLayout indentContainer;

        ElementViewHolder(View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtItemName);
            txtDescription = itemView.findViewById(txtItemDescription);
            imgIndicator = itemView.findViewById(R.id.imgIndicator);
            btnAddInside = itemView.findViewById(R.id.btnAddInside);
            btnToggleVisibility = itemView.findViewById(R.id.btnToggleVisibility);
            imgVisibility = itemView.findViewById(R.id.imgVisibility);
            indentContainer = itemView.findViewById(R.id.indentContainer);
            btnDragHandle = itemView.findViewById(R.id.btnDragHandle);
        }
    }

    private SceneElement extractElement(List<SceneElement> elements, int[] counter, int targetIndex) {
        for (int i = 0; i < elements.size(); i++) {
            if (counter[0] == targetIndex) {
                return elements.remove(i);
            }
            counter[0]++;
            SceneElement child = elements.get(i);
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                List<SceneElement> subList = new ArrayList<>(child.getChildren());
                SceneElement found = extractElement(subList, counter, targetIndex);
                if (found != null) {
                    SceneElement newChild = new SceneElement(child.getId(), child.getType(), child.getProperties(),
                            subList);
                    elements.set(i, newChild);
                    return found;
                }
            }
        }
        return null;
    }

    private boolean insertElement(List<SceneElement> elements, int[] counter, int targetIndex,
            SceneElement elToInsert) {
        for (int i = 0; i < elements.size(); i++) {
            if (counter[0] == targetIndex) {
                elements.add(i, elToInsert);
                return true;
            }
            counter[0]++;
            SceneElement child = elements.get(i);
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                List<SceneElement> subList = new ArrayList<>(child.getChildren());
                if (insertElement(subList, counter, targetIndex, elToInsert)) {
                    SceneElement newChild = new SceneElement(child.getId(), child.getType(), child.getProperties(),
                            subList);
                    elements.set(i, newChild);
                    return true;
                }
            }
        }
        if (counter[0] == targetIndex) {
            elements.add(elToInsert);
            return true;
        }
        return false;
    }

    void modifySceneMove(int fromIndex, int toIndex) {
        modifySceneMove(fromIndex, toIndex, false);
    }

    void modifySceneMove(int fromIndex, int toIndex, boolean isDragging) {
        if (fromIndex <= 2 || toIndex <= 2)
            return;
        if (toIndex < fromIndex) {
            if (!canMoveUp(fromIndex))
                return;
        } else {
            if (!canMoveDown(fromIndex))
                return;
        }

        if (expandedMenuElementIndex == fromIndex) {
            expandedMenuElementIndex = toIndex;
        } else if (expandedMenuElementIndex == toIndex) {
            expandedMenuElementIndex = fromIndex;
        }

        int fromPos = -1;
        int toPos = -1;
        for (int i = 0; i < displayedItems.size(); i++) {
            if (displayedItems.get(i).getIndex() == fromIndex)
                fromPos = i;
            if (displayedItems.get(i).getIndex() == toIndex)
                toPos = i;
        }

        CustomizeVisDialog.onRequestSaveScene.invoke(dialog.rootIdentifier);
        VisualizerScene scene = AppPreferences.createOrGetInstance().getPrefThemeScene(dialog.rootIdentifier);
        if (scene == null)
            return;

        List<SceneElement> elements = new ArrayList<>(scene.getElements());

        int[] counter = new int[] { 1 };
        SceneElement extracted = extractElement(elements, counter, fromIndex);

        if (extracted != null) {
            int insertIndex = toIndex;
            counter[0] = 1;
            insertElement(elements, counter, insertIndex, extracted);
        }

        VisualizerScene newScene = new VisualizerScene(scene.getVersion(), elements);
        AppPreferences.createOrGetInstance().savePrefThemeScene(dialog.rootIdentifier, newScene);

        if (!isDragging) {
            AppPreferences.onThemeSceneChanged.invoke(dialog.rootIdentifier);
        }

        com.aylis.comp.visual.core.Elements.RootElement root = com.aylis.comp.visual.scene.SceneBuilder.INSTANCE
                .buildFromScene(dialog.rootIdentifier, newScene);
        Element.CustomizationList newList = new Element.CustomizationList();
        root.getCustomization(newList, 0);

        dialog.customizationDataList = newList;

        rebuildValidIndices();
        treeNodes.clear();
        ElementNode rootNode = new ElementNode();
        rootNode.index = 0;
        rootNode.sceneElement = null;
        rootNode.parentIndex = -1;
        rootNode.isPreComp = false;
        treeNodes.add(rootNode);

        int[] nextIndex = new int[] { 1 };
        if (newScene != null) {
            buildTreeInfo(treeNodes, newScene.getElements(), 0, nextIndex);
        }

        if (!isDragging) {
            dialog.onPropertyChanged();
        }

        if (dialog.recyclerViewElements != null) {
            final ElementsAdapter adapter = (ElementsAdapter) dialog.recyclerViewElements.getAdapter();
            if (adapter != null && fromPos >= 0 && toPos >= 0) {
                List<VisualizerItem> tree = PreCompModelBuilder.buildTree(treeNodes, dialog.customizationDataList,
                        expandedPreCompIds);
                displayedItems.clear();
                displayedItems.addAll(PreCompModelBuilder.flattenItems(tree));

                if (isDragging) {
                    return; // Skip notifyItemRangeChanged during drag
                }

                adapter.notifyItemMoved(fromPos, toPos);
                final int fFromPos = fromPos;
                final int fToPos = toPos;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        int start = Math.min(fFromPos, fToPos);
                        int count = Math.abs(fFromPos - fToPos) + 1;
                        adapter.notifyItemRangeChanged(start, count);
                    }
                });
                return;
            }
        }

        showElementList();
    }

    private boolean traverseAndModify(List<SceneElement> elements, int[] counter, int targetIndex, int action) {
        for (int i = 0; i < elements.size(); i++) {
            if (counter[0] == targetIndex) {
                if (action == 0) {
                    elements.remove(i);
                } else if (action == 1) {
                    SceneElement target = elements.get(i);
                    String newId = "el_" + System.currentTimeMillis();
                    java.util.Map<String, SceneProperty> copiedProps = new java.util.HashMap<>();
                    if (target.getProperties() != null) {
                        for (java.util.Map.Entry<String, SceneProperty> entry : target.getProperties().entrySet()) {
                            SceneProperty sp = entry.getValue();
                            copiedProps.put(entry.getKey(),
                                    new SceneProperty(sp.getValue(), sp.getType(), sp.getGroup(), sp.getProperties()));
                        }
                    }
                    List<SceneElement> copiedChildren = null;
                    if (target.getChildren() != null) {
                        copiedChildren = new ArrayList<>(target.getChildren());
                    }
                    SceneElement dup = new SceneElement(newId, target.getType(), copiedProps, copiedChildren);
                    elements.add(i + 1, dup);
                }
                return true;
            }
            counter[0]++;
            SceneElement child = elements.get(i);
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                List<SceneElement> subList = new ArrayList<>(child.getChildren());
                if (traverseAndModify(subList, counter, targetIndex, action)) {
                    SceneElement newChild = new SceneElement(child.getId(), child.getType(), child.getProperties(),
                            subList);
                    elements.set(i, newChild);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean addChildToPreComp(List<SceneElement> elements, int[] counter, int targetIndex, SceneElement newEl) {
        for (int i = 0; i < elements.size(); i++) {
            if (counter[0] == targetIndex) {
                SceneElement target = elements.get(i);
                if ("PreCompElement".equals(target.getType())) {
                    List<SceneElement> subList = new ArrayList<>();
                    if (target.getChildren() != null) {
                        subList.addAll(target.getChildren());
                    }
                    subList.add(newEl);
                    SceneElement newChild = new SceneElement(target.getId(), target.getType(), target.getProperties(),
                            subList);
                    elements.set(i, newChild);
                    return true;
                }
                return false;
            }
            counter[0]++;
            SceneElement child = elements.get(i);
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                List<SceneElement> subList = new ArrayList<>(child.getChildren());
                if (addChildToPreComp(subList, counter, targetIndex, newEl)) {
                    SceneElement newChild = new SceneElement(child.getId(), child.getType(), child.getProperties(),
                            subList);
                    elements.set(i, newChild);
                    return true;
                }
            }
        }
        return false;
    }

    void modifyScene(int action, int elementIndex, String newElementType) {
        if (action != 3 && elementIndex == 1)
            return;
        CustomizeVisDialog.onRequestSaveScene.invoke(dialog.rootIdentifier);
        VisualizerScene scene = AppPreferences.createOrGetInstance().getPrefThemeScene(dialog.rootIdentifier);
        if (scene == null)
            return;

        List<SceneElement> elements = new ArrayList<>(scene.getElements());

        if (action == 3) {
            java.util.Map<String, SceneProperty> props = new java.util.HashMap<>();
            if ("SolidCircleElement".equals(newElementType)) {
                props.put("color", new SceneProperty("-16711936", "crgba", "General"));
                props.put("shapeSides", new SceneProperty("6", "i 3 50", "General"));
            } else if ("TextElement".equals(newElementType)) {
                props.put("text", new SceneProperty("New Text", "txt", "General"));
                props.put("color", new SceneProperty("-1", "crgba", "General"));
            } else if ("AudioDataProviderElement".equals(newElementType)) {
            } else if ("BlurElement".equals(newElementType) || "BlurGroupElement".equals(newElementType)) {
                props.put("customImage", new SceneProperty("", "img", "1_image"));
                props.put("color", new SceneProperty("-1", "crgb", "1_image"));
            } else if ("ParticlesElement".equals(newElementType)) {
                props.put("customImage", new SceneProperty("", "img", "1_particles"));
                props.put("color", new SceneProperty("-1", "crgba", "1_particles"));
            } else if ("SegmentElement".equals(newElementType)) {
                props.put("color", new SceneProperty("-1", "crgba", "General"));
            } else if ("BackgroundElement".equals(newElementType)) {
            } else if ("PreCompElement".equals(newElementType)) {
                int preCompCount = countPreCompsInScene(elements);
                props.put("preCompName", new SceneProperty("Scene " + (preCompCount + 1), "txt", "General"));
            } else if ("FxaaGroupElement".equals(newElementType)) {
            } else if ("ImageElement".equals(newElementType)) {
                props.put("customImage", new SceneProperty("default", "img", "1_image"));
                props.put("color", new SceneProperty("-1", "crgba", "1_image"));
            }
            SceneElement newEl = new SceneElement("el_" + System.currentTimeMillis(), newElementType, props, null);
            boolean addedToPreComp = false;
            if (elementIndex >= 0) {
                int[] counter = new int[] { 1 };
                addedToPreComp = addChildToPreComp(elements, counter, elementIndex, newEl);
            }
            if (!addedToPreComp) {
                if (!elements.isEmpty() && "BackgroundElement".equals(elements.get(0).getType())) {
                    SceneElement bgEl = elements.get(0);
                    List<SceneElement> bgChildren = bgEl.getChildren();
                    List<SceneElement> newBgChildren = new java.util.ArrayList<>();
                    if (bgChildren != null) {
                        int insertPos = -1;
                        for (int i = 0; i < bgChildren.size(); i++) {
                            if ("PreCompElement".equals(bgChildren.get(i).getType())) {
                                insertPos = i;
                                break;
                            }
                        }
                        if (insertPos >= 0 && !"PreCompElement".equals(newElementType)) {
                            newBgChildren.addAll(bgChildren.subList(0, insertPos));
                            newBgChildren.add(newEl);
                            newBgChildren.addAll(bgChildren.subList(insertPos, bgChildren.size()));
                        } else {
                            newBgChildren.addAll(bgChildren);
                            newBgChildren.add(newEl);
                        }
                    } else {
                        newBgChildren.add(newEl);
                    }
                    elements.set(0,
                            new SceneElement(bgEl.getId(), bgEl.getType(), bgEl.getProperties(), newBgChildren));
                } else {
                    elements.add(newEl);
                }
            }
        } else {
            int[] counter = new int[] { 1 };
            traverseAndModify(elements, counter, elementIndex, action);
        }

        VisualizerScene newScene = new VisualizerScene(scene.getVersion(), elements);
        AppPreferences.createOrGetInstance().savePrefThemeScene(dialog.rootIdentifier, newScene);

        AppPreferences.onThemeSceneChanged.invoke(dialog.rootIdentifier);

        com.aylis.comp.visual.core.Elements.RootElement root = com.aylis.comp.visual.scene.SceneBuilder.INSTANCE
                .buildFromScene(dialog.rootIdentifier, newScene);
        Element.CustomizationList newList = new Element.CustomizationList();
        root.getCustomization(newList, 0);

        dialog.customizationDataList = newList;
        dialog.onPropertyChanged();
        showElementList();
    }

    private int countPreCompsInScene(List<SceneElement> elements) {
        int count = 0;
        for (SceneElement el : elements) {
            if ("PreCompElement".equals(el.getType()))
                count++;
            if (el.getChildren() != null)
                count += countPreCompsInScene(el.getChildren());
        }
        return count;
    }

    private SceneElement findLastPreComp(List<SceneElement> elements) {
        if (elements == null)
            return null;
        SceneElement last = null;
        for (SceneElement el : elements) {
            if ("PreCompElement".equals(el.getType()))
                last = el;
        }
        return last;
    }

    private static SceneElement clipboardElement = null;

    private void copyToClipboard(int index) {
        CustomizeVisDialog.onRequestSaveScene.invoke(dialog.rootIdentifier);
        VisualizerScene scene = AppPreferences.createOrGetInstance().getPrefThemeScene(dialog.rootIdentifier);
        if (scene == null)
            return;
        int[] counter = new int[] { 1 };
        clipboardElement = findElementInScene(scene.getElements(), counter, index);
        if (clipboardElement != null) {
            if (dialog.getActivity() != null) {
                android.widget.Toast
                        .makeText(dialog.getActivity(), "Copied to clipboard", android.widget.Toast.LENGTH_SHORT)
                        .show();
            }
            ElementsAdapter adapter = (ElementsAdapter) dialog.recyclerViewElements.getAdapter();
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private SceneElement findElementInScene(List<SceneElement> elements, int[] counter, int targetIndex) {
        for (int i = 0; i < elements.size(); i++) {
            if (counter[0] == targetIndex) {
                return elements.get(i);
            }
            counter[0]++;
            SceneElement child = elements.get(i);
            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                SceneElement found = findElementInScene(child.getChildren(), counter, targetIndex);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private SceneElement deepCopySceneElement(SceneElement element) {
        String newId = "el_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);

        java.util.Map<String, SceneProperty> copiedProps = new java.util.HashMap<>();
        if (element.getProperties() != null) {
            for (java.util.Map.Entry<String, SceneProperty> entry : element.getProperties().entrySet()) {
                SceneProperty sp = entry.getValue();
                java.util.Map<String, SceneProperty> nestedProps = null;
                if (sp.getProperties() != null) {
                    nestedProps = new java.util.HashMap<>();
                    for (java.util.Map.Entry<String, SceneProperty> nestEntry : sp.getProperties().entrySet()) {
                        SceneProperty nestSp = nestEntry.getValue();
                        nestedProps.put(nestEntry.getKey(), new SceneProperty(nestSp.getValue(), nestSp.getType(),
                                nestSp.getGroup(), nestSp.getProperties()));
                    }
                }
                copiedProps.put(entry.getKey(),
                        new SceneProperty(sp.getValue(), sp.getType(), sp.getGroup(), nestedProps));
            }
        }

        if (element.getType().equals("PreCompElement")) {
            SceneProperty nameProp = copiedProps.get("preCompName");
            if (nameProp != null) {
                String oldName = nameProp.getValue();
                String newName = oldName + " (Copy)";
                copiedProps.put("preCompName",
                        new SceneProperty(newName, nameProp.getType(), nameProp.getGroup(), nameProp.getProperties()));
            }
        }

        List<SceneElement> copiedChildren = null;
        if (element.getChildren() != null) {
            copiedChildren = new ArrayList<>();
            for (SceneElement child : element.getChildren()) {
                copiedChildren.add(deepCopySceneElement(child));
            }
        }

        return new SceneElement(newId, element.getType(), copiedProps, copiedChildren);
    }

    private void pasteFromClipboard(int targetIndex) {
        if (clipboardElement == null)
            return;
        CustomizeVisDialog.onRequestSaveScene.invoke(dialog.rootIdentifier);
        VisualizerScene scene = AppPreferences.createOrGetInstance().getPrefThemeScene(dialog.rootIdentifier);
        if (scene == null)
            return;

        List<SceneElement> elements = new ArrayList<>(scene.getElements());
        SceneElement newEl = deepCopySceneElement(clipboardElement);

        boolean addedToPreComp = false;
        if (targetIndex >= 0) {
            int[] counter = new int[] { 1 };
            addedToPreComp = addChildToPreComp(elements, counter, targetIndex, newEl);
        }
        if (!addedToPreComp) {
            if (!elements.isEmpty() && "BackgroundElement".equals(elements.get(0).getType())) {
                SceneElement bgEl = elements.get(0);
                List<SceneElement> bgChildren = bgEl.getChildren();
                List<SceneElement> newBgChildren = new java.util.ArrayList<>();
                if (bgChildren != null) {
                    int insertPos = -1;
                    for (int i = 0; i < bgChildren.size(); i++) {
                        if ("PreCompElement".equals(bgChildren.get(i).getType())) {
                            insertPos = i;
                            break;
                        }
                    }
                    if (insertPos >= 0 && !"PreCompElement".equals(newEl.getType())) {
                        newBgChildren.addAll(bgChildren.subList(0, insertPos));
                        newBgChildren.add(newEl);
                        newBgChildren.addAll(bgChildren.subList(insertPos, bgChildren.size()));
                    } else {
                        newBgChildren.addAll(bgChildren);
                        newBgChildren.add(newEl);
                    }
                } else {
                    newBgChildren.add(newEl);
                }
                elements.set(0, new SceneElement(bgEl.getId(), bgEl.getType(), bgEl.getProperties(), newBgChildren));
            } else {
                elements.add(newEl);
            }
        }

        VisualizerScene newScene = new VisualizerScene(scene.getVersion(), elements);
        AppPreferences.createOrGetInstance().savePrefThemeScene(dialog.rootIdentifier, newScene);

        AppPreferences.onThemeSceneChanged.invoke(dialog.rootIdentifier);

        com.aylis.comp.visual.core.Elements.RootElement root = com.aylis.comp.visual.scene.SceneBuilder.INSTANCE
                .buildFromScene(dialog.rootIdentifier, newScene);
        Element.CustomizationList newList = new Element.CustomizationList();
        root.getCustomization(newList, 0);

        dialog.customizationDataList = newList;
        dialog.onPropertyChanged();
        clipboardElement = null;
        showElementList();
    }
}
