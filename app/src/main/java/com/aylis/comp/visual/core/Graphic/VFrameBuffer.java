

package com.aylis.comp.visual.core.Graphic;

import android.opengl.GLES20;
import com.aylis.Common.tlog;
import org.lwjgl.LWJGLException;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer;

public class VFrameBuffer extends FrameBuffer {

    public static VFrameBuffer createSafe(int width, int height, int filter, int wrap, boolean genMipmap) {
        VTexture texture = new VTexture(width, height, filter, wrap, genMipmap).checkIfValid();
        if (texture == null) return null;

        try {
            return new VFrameBuffer(texture, true);
        } catch (Exception ex) {
            tlog.w("exception " + ex.getMessage());
            return null;
        }
    }

    private VFrameBuffer(VTexture texture, boolean ownsTexture) throws LWJGLException {
        super(texture, ownsTexture);
    }

    public boolean isValid() {
        return id!=0;
    }

    public VFrameBuffer checkIfValid() {
        if (!this.isValid()) {
            this.dispose();
            tlog.w("FrameBuffer is not valid");
            return null;
        }

        return this;
    }

    public void begin() {
        if (!isValid()) return;

        GLES20.glViewport(0, 0, getWidth(), getHeight());
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id);
    }

    public void end() {
        if (!isValid()) return;

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }
}

