

package com.aylis.comp.GlobalSearch;

import com.aylis.comp.Common.IGeneralItemContainerIdentifier;
import com.aylis.comp.Common.ISearchEntry;

public class SearchEntry extends SearchEntryOptions implements ISearchEntry {
    private final int index;
    String query = "";

    SearchEntry(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getHint() {
        return hint;
    }

    @Override
    public IGeneralItemContainerIdentifier getContainerIdentifier() {
        return containerIdentifier;
    }
}
