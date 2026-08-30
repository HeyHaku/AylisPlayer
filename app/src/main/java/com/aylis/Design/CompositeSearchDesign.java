

package com.aylis.Design;

import android.os.Handler;

import android.os.AsyncTask;
import com.aylis.Common.Events.WeakEventR2;

import com.aylis.comp.LibraryQueueUI.LibraryQueueFragmentBase;
import com.aylis.Common.Events.WeakEvent1;
import com.aylis.Common.Events.WeakEvent2;
import com.aylis.Common.Events.WeakEvent3;
import com.aylis.Common.Events.WeakEvent4;
import com.aylis.Common.Events.WeakEventR;
import com.aylis.Common.Utils;
import com.aylis.comp.Common.IGeneralItemContainerIdentifier;
import com.aylis.comp.Common.ISearchEntry;
import com.aylis.comp.GlobalSearch.GlobalSearchCore;
import com.aylis.comp.GlobalSearch.SearchEntry;
import com.aylis.comp.GlobalSearch.SearchEntryOptions;
import com.aylis.comp.GlobalSearch.SearchTaskManager;
import com.aylis.comp.LibraryQueueUI.FragmentLibrary;
import com.aylis.MainActivity;
import java.util.LinkedList;
import java.util.List;

public class CompositeSearchDesign {

    private static SearchTaskManager searchTaskManager = new SearchTaskManager();
    private List<Object> listenerRefHolder = new LinkedList<>();

    public CompositeSearchDesign() {

        GlobalSearchCore.ICompositeSearch_onCurrentSearchEntryChanged.subscribeWeak(new WeakEvent4.Handler<Integer, Integer, ISearchEntry, Boolean>() {
            @Override
            public void invoke(Integer currentIndex, Integer index, ISearchEntry
                    searchEntry, Boolean queryChangedToo) {

                if (searchEntry == null || searchEntry.getQuery() == null || searchEntry.getQuery().isEmpty())
                    searchTaskManager.clearTaskIfMatch(index);

                MainActivity mainActivity = MainActivity.getInstance();
                if (mainActivity == null) return;

                if (currentIndex.equals(index))
                    mainActivity.updateSearchView(searchEntry, false);

                if (queryChangedToo &&searchEntry != null) {
                    if (index == MainActivity.LIBRARY_PAGE_INDEX) {
                        FragmentLibrary FragmentLibrary = MainActivity.getFragmentLibraryInstance();
                        if (FragmentLibrary != null)
                            FragmentLibrary.updateSearchQuery(mainActivity, searchEntry.getQuery());
                    }
                }
            }
        }
        , listenerRefHolder);

        MainActivity.onUISearchQueryTextChange.subscribeWeak(new WeakEvent2.Handler<Integer, java.lang.String>() {
            @Override
            public void invoke(Integer index, String query) {
                GlobalSearchCore globalSearchCore = GlobalSearchCore.getInstance();
                if (globalSearchCore == null) return;

                globalSearchCore.onSearchQueryTextChange(query);
            }
        }, listenerRefHolder);

        SearchTaskManager.onUISearchQueryTextChangeWithIndex.subscribeWeak(new WeakEvent2.Handler<Integer, java.lang.String>() {
            @Override
            public void invoke(Integer index, String query) {
                GlobalSearchCore globalSearchCore = GlobalSearchCore.getInstance();
                if (globalSearchCore == null) return;

                globalSearchCore.onSearchQueryTextChange(index, query);
            }
        }, listenerRefHolder);

        MainActivity.onUISearchQueryStateChange.subscribeWeak(new WeakEvent1.Handler<Boolean>() {
            @Override
            public void invoke(Boolean enabled) {
                GlobalSearchCore globalSearchCore = GlobalSearchCore.getInstance();
                if (globalSearchCore == null) return;

                globalSearchCore.onSearchQueryTextChange(null);
            }
        }, listenerRefHolder);

        MainActivity.onSetCurrentSearchIndex.subscribeWeak(new WeakEvent1.Handler<Integer>() {
            @Override
            public void invoke(final Integer index) {

                GlobalSearchCore globalSearchCore = GlobalSearchCore.getInstance();
                if (globalSearchCore == null) return;

                SearchEntryOptions searchOptions = SearchEntryOptions.refuse;

                if (index == MainActivity.LIBRARY_PAGE_INDEX) {
                    FragmentLibrary FragmentLibrary = MainActivity.getFragmentLibraryInstance();
                    if (FragmentLibrary != null) searchOptions = FragmentLibrary.getSearchEntryOptions();
                }

                if (searchOptions != SearchEntryOptions.refuse) {
                    if (searchOptions != null)
                        globalSearchCore.onUpdateSearchOptions(index, searchOptions.enabled, searchOptions.hint, searchOptions.containerIdentifier);
                    else
                        globalSearchCore.onUpdateSearchOptions(index, false, "", null);
                }

                globalSearchCore.onSetCurrentSearchIndex(index);
            }
        }, listenerRefHolder);

        MainActivity.onRequestCurrentSearchEntry.subscribeWeak(new WeakEventR.Handler<ISearchEntry>() {
            @Override
            public ISearchEntry invoke() {
                GlobalSearchCore globalSearchCore = GlobalSearchCore.getInstance();
                if (globalSearchCore == null) return null;

                return globalSearchCore.getCurrentSearchEntry();
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onRequestSearchQuery.subscribeWeak(new WeakEventR2.Handler<Integer, IGeneralItemContainerIdentifier, String>() {
            @Override
            public String invoke(Integer pageIndex, IGeneralItemContainerIdentifier containerIdentifier) {
                GlobalSearchCore globalSearchCore = GlobalSearchCore.getInstance();
                if (globalSearchCore == null) return null;

                SearchEntry entry = globalSearchCore.getSearchEntry(pageIndex);
                if (entry == null) return null;

                if (Utils.compareNullEqual(entry.getContainerIdentifier(), containerIdentifier))
                    return entry.getQuery();
                else
                    return null;
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onUpdateSearchOptions.subscribeWeak(new WeakEvent4.Handler<Integer, Boolean, String, IGeneralItemContainerIdentifier>() {
            @Override
            public void invoke(Integer index, Boolean enabled, String hint, IGeneralItemContainerIdentifier containerIdentifier) {
                GlobalSearchCore globalSearchCore = GlobalSearchCore.getInstance();
                if (globalSearchCore == null) return;

                globalSearchCore.onUpdateSearchOptions(index, enabled, hint, containerIdentifier);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onCompareSearchTask.subscribeWeak(new WeakEventR2.Handler<AsyncTask, Integer, Boolean>() {
            @Override
            public Boolean invoke(AsyncTask task, Integer pageIndex) {
                return searchTaskManager.compareTask(task, pageIndex);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onStartingSearchTask.subscribeWeak(new WeakEvent3.Handler<AsyncTask, Integer, Object>() {
            @Override
            public void invoke(AsyncTask task, Integer pageIndex, Object param) {
                searchTaskManager.setTask(task, pageIndex);
            }
        }, listenerRefHolder);

        LibraryQueueFragmentBase.onContainerDataSetChanged.subscribeWeak(new WeakEvent1.Handler<Integer>() {
            @Override
            public void invoke(Integer pageIndex) {
                searchTaskManager.clearTaskIfMatch(pageIndex);
            }
        }, listenerRefHolder);
    }

}

