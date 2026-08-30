

package com.aylis.Common;

import android.util.Log;

public class tlog {

    static final boolean LOG = false;

    private static String getLogTagWithMethod(String prefix) {
        Throwable stack = new Throwable().fillInStackTrace();
        StackTraceElement[] trace = stack.getStackTrace();
        return prefix + trace[2].getFileName() + "." + trace[2].getMethodName() + ":" + trace[2].getLineNumber();
    }

    public static void w(String msg) {
        if(LOG) Log.w(getLogTagWithMethod("###"), ":" + msg);
    }

    public static void d(String msg) {
        if(LOG) Log.d(getLogTagWithMethod("###"), ":" + msg);
    }

    public static void e(String msg) {
        if(LOG) Log.e(getLogTagWithMethod("###"), ":" + msg);
    }

    public static void notice(String msg) {
        if(LOG) Log.w(getLogTagWithMethod("###"), ":" + msg);
    }

}

