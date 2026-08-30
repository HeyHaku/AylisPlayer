

package com.aylis.comp.ContextualActionBar;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import android.view.View;
import com.aylis.ContextData;
import java.util.ArrayList;
import java.util.List;

public abstract class ItemActionBase {

    private final int actionId;
    private final int iconResId;
    private final int nameStrResId;
    private final boolean allowMultiple, allowSingle;

    public ItemActionBase(int actionId, boolean allowMultiple, @DrawableRes int iconResId, @StringRes int nameStrResId) {
        this(actionId, allowMultiple, true, iconResId, nameStrResId);
    }

    public ItemActionBase(int actionId, boolean allowMultiple, boolean allowSingle, @DrawableRes int iconResId, @StringRes int nameStrResId) {
        this.allowMultiple = allowMultiple;
        this.allowSingle = allowSingle;
        this.actionId = actionId;
        this.iconResId = iconResId;
        this.nameStrResId = nameStrResId;
    }

    public void executeBase(ContextData contextData, Object item, ActionListenerBase listener) {
        List<Object> items = new ArrayList<>();
        List<ActionListenerBase> listeners = new ArrayList<>();

        items.add(item);
        listeners.add(listener);
        executeListBase(contextData, items, listeners);
    }

    public abstract void executeListBase(ContextData contextData, List<Object> items, List<ActionListenerBase> listeners);

    public boolean getShouldShow() {
        return true;
    }

    public interface OnClickListener {
        void onClick(View v, androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder);
    }

    public int getActionId() {
        return actionId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getNameStrResId() {
        return nameStrResId;
    }

    public boolean isAllowMultiple() {
        return allowMultiple;
    }

    public boolean isAllowSingle() {
        return allowSingle;
    }
}
