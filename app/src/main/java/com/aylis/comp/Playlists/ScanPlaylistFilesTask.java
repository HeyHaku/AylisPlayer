

package com.aylis.comp.Playlists;

import android.content.Context;
import android.os.AsyncTask;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ScanPlaylistFilesTask extends AsyncTask<String, Object, Void> {

    private Context context;
    private File rootFile;
    private FilterComparable<File> searchFilter;
    private WeakReference<IResultReceiver> resultReceiver;
    private String searchQuery;
    private List<String> resultItems = new ArrayList<>();

    IResultReceiverInternal resultReceiverInternal = new IResultReceiverInternal() {
        @Override
        public void onItemDirFound(File itemDir) {
        }

        @Override
        public void onItemFileFound(File itemFile) {
            try {
                resultItems.add(itemFile.getCanonicalPath());
            } catch (IOException ignored) {
            }
        }

        @Override
        public void onDir(File file) {
            String str = null;
            try {
                str = file.getCanonicalPath();
            } catch (IOException ignored) {
            }

            if (str != null)
                ScanPlaylistFilesTask.this.publishProgress(str);
        }

        @Override
        public boolean isCancelled() {
            return ScanPlaylistFilesTask.this.isCancelled();
        }
    };

    public ScanPlaylistFilesTask(Context context, File rootFile, FilterComparable<File> searchFilter, WeakReference<IResultReceiver> resultReceiver) {
        this.context = context;
        this.rootFile = rootFile;
        this.searchFilter = searchFilter;
        this.resultReceiver = resultReceiver;
        searchQuery = "";
    }

    public static ScanPlaylistFilesTask createScanPlaylistFilesTask(Context context, File rootFile, ScanPlaylistFilesTask.FilterComparable<File> searchFilter, WeakReference<ScanPlaylistFilesTask.IResultReceiver> resultReceiver) {
        return new ScanPlaylistFilesTask(context,
                rootFile,
                searchFilter,
                resultReceiver
        );
    }

    static boolean getItemsSearch(Context context, IResultReceiverInternal result, File f, boolean excludeDirs, String query, FilterComparable<File> searchFilter) {
        String queryProcessed = searchFilter.preProcessQuery(query);
        return getItemsRecursive(context, result, f, excludeDirs, queryProcessed, searchFilter);
    }

    static boolean getItemsRecursive(Context context,
                                     IResultReceiverInternal result,
                                     File f,
                                     boolean excludeDirs,
                                     String query,
                                     FilterComparable<File> searchFilter) {

        File[] dirs = f.listFiles();

        try {
            for (File ff : dirs) {
                if (result.isCancelled()) return false;

                if (ff.isDirectory()) {

                    result.onDir(ff);
                    if (!excludeDirs && !getItemsRecursive(context, result, ff, false, query, searchFilter))
                        return false;

                } else {
                    boolean include = true;

                    if (searchFilter != null)
                        include = searchFilter.compare(query, ff);

                    if (include)
                        result.onItemFileFound(ff);
                }
            }
        } catch (Exception ignored) {
        }

        return true;
    }

    public void start() {
        execute(searchQuery);
    }

    @Override
    public void onPreExecute() {
        IResultReceiver rcv = resultReceiver.get();
        if (rcv != null) rcv.onStarted(this);
    }

    @Override
    public void onPostExecute(Void result) {
        IResultReceiver rcv = resultReceiver.get();
        if (rcv != null) rcv.onFinished(this, true, resultItems);

    }

    @Override
    protected void onProgressUpdate(Object... values) {
        IResultReceiver rcv = resultReceiver.get();
        if (rcv != null) rcv.onStatusUpdate(this, (String) values[0]);
    }

    @Override
    protected void onCancelled(Void aVoid) {
        IResultReceiver rcv = resultReceiver.get();
        if (rcv != null) rcv.onFinished(this, false, null);
    }

    @Override
    protected Void doInBackground(String... params) {
        String searchQuery = params[0];
        getItemsSearch(context, resultReceiverInternal, rootFile, false, searchQuery, searchFilter);
        return null;
    }

    public interface FilterComparable<T1> {
        String preProcessQuery(String text);

        void preProcessItem(T1 item);

        boolean compare(String text, T1 item);
    }

    public interface IResultReceiver {
        void onStarted(AsyncTask task);

        void onFinished(AsyncTask task, boolean allFinished, List<String> resultItems);

        void onItem(AsyncTask task, String itemPath);

        void onStatusUpdate(AsyncTask task, String statusText);
    }

    private interface IResultReceiverInternal {
        void onItemDirFound(File itemDir);

        void onItemFileFound(File itemFile);

        void onDir(File itemDir);

        boolean isCancelled();
    }

}
