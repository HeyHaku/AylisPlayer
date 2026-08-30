package com.aylis.comp.export;

import android.opengl.EGLSurface;
import android.view.Surface;

public class WindowSurface {
    private EglCore mEglCore;
    private EGLSurface mEGLSurface;
    private Surface mSurface;
    private boolean mReleaseSurface;

    public WindowSurface(EglCore eglCore, Surface surface, boolean releaseSurface) {
        mEglCore = eglCore;
        mEGLSurface = mEglCore.createWindowSurface(surface);
        mSurface = surface;
        mReleaseSurface = releaseSurface;
    }

    public void release() {
        mEglCore.releaseSurface(mEGLSurface);
        if (mSurface != null && mReleaseSurface) {
            mSurface.release();
        }
        mEGLSurface = null;
        mSurface = null;
    }

    public void makeCurrent() {
        mEglCore.makeCurrent(mEGLSurface);
    }

    public boolean swapBuffers() {
        return mEglCore.swapBuffers(mEGLSurface);
    }

    public void setPresentationTime(long nsecs) {
        mEglCore.setPresentationTime(mEGLSurface, nsecs);
    }
}
