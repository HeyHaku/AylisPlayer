

package com.aylis.comp.GlobalSearch;

import android.os.AsyncTask;
import com.aylis.Common.Events.WeakEvent2;
import junit.framework.Assert;

public class SearchTaskManager {

    public static WeakEvent2<Integer  , String  > onUISearchQueryTextChangeWithIndex = new WeakEvent2<>();

    private int taskIndex = -1;
    private AsyncTask asyncTask = null;

    public void setTask(AsyncTask tsk, int tskIndex) {
        clearTask(this.taskIndex == tskIndex);
        this.taskIndex = tskIndex;
        this.asyncTask = tsk;
    }

    public boolean compareTask(AsyncTask tsk, int tskIndex) {
        return taskIndex == tskIndex && asyncTask != null && !tsk.isCancelled() && (tsk == asyncTask);
    }

    public void clearTaskIfMatch(int tskIndex) {
        if (taskIndex == tskIndex)
            clearTask(true);
    }

    protected void clearTask(boolean samePageSlot) {
        if (asyncTask == null) return;

            final int taskIndexFinal = taskIndex;

            Assert.assertNotNull(asyncTask);
            asyncTask.cancel(false);
            asyncTask = null;
            taskIndex = -1;

            if (!samePageSlot) {

                onUISearchQueryTextChangeWithIndex.invoke(taskIndexFinal, "");
            }
    }

}
