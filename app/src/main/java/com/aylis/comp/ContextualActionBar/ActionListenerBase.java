

package com.aylis.comp.ContextualActionBar;

import com.aylis.ContextData;

public abstract class ActionListenerBase {
    private final ItemActionBase actionBase;

    public ActionListenerBase(ItemActionBase actionBase) {
        this.actionBase = actionBase;
    }

    public ItemActionBase getItemActionBase() {
        return actionBase;
    }

    public void execute(ContextData contextData, Object item) {
        actionBase.executeBase(contextData, item, this);
    }
}
