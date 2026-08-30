

package com.aylis.Design;

import android.os.Handler;

import android.app.Activity;
import com.aylis.comp.LibraryQueueUI.LibraryQueueFragmentBase;
import com.aylis.Common.Events.WeakEvent;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.Events.WeakEventR1;
import com.aylis.comp.Common.IGeneralItemContainerIdentifier;
import com.aylis.comp.ContextualActionBar.ActionListenerBase;
import com.aylis.comp.ContextualActionBar.ContextualActionBar;
import com.aylis.comp.ContextualActionBar.ItemSelection;
import com.aylis.ContextData;
import com.aylis.MainActivity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class ContextualActionModeDesign {

    private boolean selectingEnabled = false;

    private HashMap<IGeneralItemContainerIdentifier, ItemSelection<Object>> itemSelectionContainers = new HashMap<>();
    private List<Object> listenerRefHolder = new LinkedList<>();

    public ContextualActionModeDesign() {

        ContextualActionBar.onSelectModeChanged.subscribeWeak(new WeakEvent1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean selectingEnabled) {
                ContextualActionModeDesign.this.selectingEnabled = selectingEnabled;
            }
        }, listenerRefHolder);

        ContextualActionBar.onItemSelectionChanged.subscribeWeak(new WeakEvent2.Handler<ItemSelection.One<Object>, Boolean>() {
            @Override
            public void invoke(ItemSelection.One<Object> newItemSelection, Boolean select) {
                ItemSelection<Object> containerItemSel = itemSelectionContainers.get(newItemSelection.getContainerIdentifier());

                if (containerItemSel == null) {
                    containerItemSel = new ItemSelection<>(newItemSelection.getContainerIdentifier());
                    itemSelectionContainers.put(newItemSelection.getContainerIdentifier(), containerItemSel);
                }

                if (select)
                    containerItemSel.addSelection(newItemSelection);
                else
                    containerItemSel.subtractSelection(newItemSelection);
            }
        }, listenerRefHolder);

        ContextualActionBar.onContainerItemsDeselected.subscribeWeak(new WeakEvent1.Handler<IGeneralItemContainerIdentifier>() {
            @Override
            public void invoke(IGeneralItemContainerIdentifier containerIdentifier) {
                itemSelectionContainers.remove(containerIdentifier);
            }
        }, listenerRefHolder);

        ContextualActionBar.onAllItemsDeselected.subscribeWeak(new WeakEvent.Handler() {
            @Override
            public void invoke() {
                Collection<IGeneralItemContainerIdentifier> itemSelectionContainersCopy = new ArrayList<>(itemSelectionContainers.keySet());

                for (final IGeneralItemContainerIdentifier containerId : itemSelectionContainersCopy) {
                    ContextualActionBar.onContainerItemsDeselected.invoke(containerId);
                }

                itemSelectionContainers.clear();
            }
        }, listenerRefHolder);

        MainActivity.onCreate.subscribeWeak(new WeakEvent1.Handler<Activity>() {
            @Override
            public void invoke(Activity activity) {

                ContextualActionBar contextualActionBar = ContextualActionBar.getInstance();
                if (contextualActionBar != null)
                    contextualActionBar.updateMenu();

            }
        }, listenerRefHolder);

        MainActivity.onDestroy.subscribeWeak(new WeakEvent1.Handler<ContextData>() {
            @Override
            public void invoke(ContextData contextData) {
                ContextualActionBar contextualActionBar = ContextualActionBar.getInstance();
                if (contextualActionBar != null)
                    contextualActionBar.onActivityDestroyed();
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onItemSelected.subscribeWeak(new WeakEvent3.Handler<ActionListenerBase[], Boolean, ItemSelection.One<Object>>() {

            @Override
            public void invoke(ActionListenerBase[] itemActions, Boolean select, ItemSelection.One<Object> itemSelection) {
                ContextualActionBar contextualActionBar = ContextualActionBar.getInstance();
                if (contextualActionBar != null)
                    contextualActionBar.onItemSelected(itemActions, select, itemSelection);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestIsSelectingEnabled.subscribeWeak(new WeakEventR.Handler<Boolean>() {
            @Override
            public Boolean invoke() {
                return selectingEnabled;
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestContainsItemSelection.subscribeWeak(new WeakEventR1.Handler<ItemSelection.One, Boolean>() {
            @Override
            public Boolean invoke(ItemSelection.One itemSelection) {
                return containsItemSelection(itemSelection);
            }
        }, listenerRefHolder);
    }

    boolean containsItemSelection(ItemSelection.One itemSelection) {
        ItemSelection<Object> container = itemSelectionContainers.get(itemSelection.getContainerIdentifier());
        return container != null && container.containsItem(itemSelection.getItemIdentifier());
    }
}
