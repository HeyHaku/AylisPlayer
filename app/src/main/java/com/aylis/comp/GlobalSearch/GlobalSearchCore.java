

package com.aylis.comp.GlobalSearch;

import com.aylis.Common.Events.WeakEvent4;
import com.aylis.comp.Common.IGeneralItemContainerIdentifier;
import com.aylis.comp.Common.ISearchEntry;
import java.lang.ref.WeakReference;

public class GlobalSearchCore {

    public static WeakEvent4<Integer  , Integer  , ISearchEntry  , Boolean  > ICompositeSearch_onCurrentSearchEntryChanged = new WeakEvent4<>();

    private static final Object createInstanceLock = new Object();
    private static volatile WeakReference<GlobalSearchCore> instanceWeak = new WeakReference<>(null);
    private int currentIndex = -1;
    private SearchEntry[] entrys = new SearchEntry[2];

    public GlobalSearchCore() {
        for (int i = 0; i < entrys.length; i++) {
            entrys[i] = new SearchEntry(i);
        }
        notifyEntry(currentIndex, true);
    }

    public static GlobalSearchCore createInstance() {
        GlobalSearchCore inst0 = instanceWeak.get();
        if (inst0 != null) return inst0;

        synchronized (createInstanceLock) {
            GlobalSearchCore inst = instanceWeak.get();
            if (inst == null) {
                inst = new GlobalSearchCore();
                instanceWeak = new WeakReference<>(inst);
            }

            return inst;
        }
    }

    public static GlobalSearchCore getInstance() {
        return instanceWeak.get();
    }

    public SearchEntry getSearchEntry(int index) {
        if (index < 0 || index >= entrys.length)
            return null;
        return entrys[index];
    }

    public ISearchEntry getCurrentSearchEntry() {
        return getSearchEntry(currentIndex);
    }

    public void onUpdateSearchOptions(int index, boolean enabled, String hint, IGeneralItemContainerIdentifier containerIdentifier) {
        if (index < 0 || index >= entrys.length) return;

        SearchEntry currentEntry = entrys[index];

        if (currentEntry.containerIdentifier == null || !currentEntry.containerIdentifier.equals(containerIdentifier)) {
            entrys[index].query = "";
        }

        entrys[index].enabled = enabled;
        entrys[index].hint = hint;
        entrys[index].containerIdentifier = containerIdentifier;

        notifyEntry(index, true);
    }

    public void onSearchQueryTextChange(String query) {
        onSearchQueryTextChange(currentIndex, query);
    }

    public void onSearchQueryTextChange(int index, String query) {
        if (index < 0 || index >= entrys.length) return;

        if (!entrys[index].query.equals(query)) {
            entrys[index].query = (query == null ? "" : new String(query));
            notifyEntry(index, true);
        }
    }

    public void onSetCurrentSearchIndex(int index) {
        if (index < 0 || index >= entrys.length)
            currentIndex = -1;
        else
            currentIndex = index;

        notifyEntry(currentIndex, false);
    }

    void notifyEntry(int index, final boolean queryChangedToo) {
        if (index < 0 || index >= entrys.length)
            index = -1;

        SearchEntry resultSearchEntry = index >= 0 ? entrys[index] : null;
        ICompositeSearch_onCurrentSearchEntryChanged.invoke(currentIndex, index, resultSearchEntry, queryChangedToo);
    }

}
