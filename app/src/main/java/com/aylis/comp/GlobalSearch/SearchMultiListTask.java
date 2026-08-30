

package com.aylis.comp.GlobalSearch;

import android.content.Context;
import android.os.AsyncTask;
import com.aylis.Common.MultiList;
import com.aylis.Common.Tuple2;
import com.aylis.Common.tlog;
import java.lang.ref.WeakReference;

public class SearchMultiListTask<T1, T2> extends AsyncTask<String, Object, Void> {

    public interface FilterComparable<T> {
        String preProcessQuery(String query);
        void preProcessItem(T item);
        boolean compare(String query, T item);
    }

    private MultiList<T1, T2> originalList;
    private FilterComparable<T1> searchFilter;
    private WeakReference<IResultReceiver<T1, T2>> receiver;
    private int fileCounter = 0;

    SearchMultiListTask.IResultReceiverInternal<T1, T2> resultReceiver0 = new IResultReceiverInternal<T1, T2>() {

        @Override
        public void onItemDirFound(final Tuple2<T1, T2> itemDir) {

        }

        @Override
        public void onItemFileFound(final Tuple2<T1, T2> itemFile) {
            fileCounter++;
            SearchMultiListTask.this.publishProgress(itemFile);
        }

        @Override
        public boolean isCancelled() {
            return SearchMultiListTask.this.isCancelled();
        }
    };

    public SearchMultiListTask(Context context,
                               MultiList<T1, T2> originalList,
                               FilterComparable<T1> searchFilter,
                               WeakReference<IResultReceiver<T1, T2>> receiver) {

        this.originalList = originalList;
        this.searchFilter = searchFilter;
        this.receiver = receiver;
    }

    @Override
    public void onPreExecute() {
        IResultReceiver rcv = receiver.get();
        if (rcv != null) rcv.onSearchStarted(SearchMultiListTask.this);
    }

    @Override
    public void onPostExecute(Void result) {
        IResultReceiver rcv = receiver.get();
        if (rcv != null) rcv.onSearchFinished(SearchMultiListTask.this, true);
    }

    @Override
    protected void onProgressUpdate(Object... values) {
        IResultReceiver<T1, T2> rcv = receiver.get();
        if (rcv != null) rcv.onItemFileFound(SearchMultiListTask.this, (Tuple2<T1, T2>)values[0]);
    }

    @Override
    protected void onCancelled(Void aVoid) {
        IResultReceiver rcv = receiver.get();
        if (rcv != null) rcv.onSearchFinished(SearchMultiListTask.this, false);
    }

    @Override
    protected Void doInBackground(String... params) {
        String _query = params[0];
        String query = null;
        if (searchFilter == null) return null;

        try {
            if (_query != null && !_query.isEmpty()) {
                query = searchFilter.preProcessQuery(_query);
            }

            for (Tuple2<T1, T2> item : originalList) {
                if (resultReceiver0.isCancelled()) break;
                searchFilter.preProcessItem(item.obj1);
            }

            for (Tuple2<T1, T2> item : originalList) {
                if (resultReceiver0.isCancelled()) break;
                if (searchFilter.compare(query, item.obj1))
                    resultReceiver0.onItemFileFound(item);
            }
        } catch (Exception e)
        {
            tlog.w("doInBackground Exception: "+e.getMessage());
        }

        return null;
    }

    public interface IResultReceiver<T1, T2> {
        void onSearchStarted(AsyncTask task);

        void onSearchFinished(AsyncTask task, boolean allFinished);

        void onItemDirFound(AsyncTask task, Tuple2<T1, T2> itemDir);

        void onItemFileFound(AsyncTask task, Tuple2<T1, T2> itemFile);
    }

    private interface IResultReceiverInternal<T1, T2> {
        void onItemDirFound(Tuple2<T1, T2> itemDir);

        void onItemFileFound(Tuple2<T1, T2> itemFile);

        boolean isCancelled();
    }

}