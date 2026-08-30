

package com.aylis.comp.visual.core.Graphic;

import android.opengl.GLES20;
import org.lwjgl.LWJGLException;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.ShaderProgram;

public class VShaderProgram extends ShaderProgram {

    public VShaderProgram(String vertexShaderSource, String fragShaderSource) throws LWJGLException {
        super(vertexShaderSource, fragShaderSource);
    }

    public void setUniformMatrix(String name, boolean transpose, float[] m) {
        setUniformMatrix(getUniformLocation(name), transpose, m);
    }

    public void setUniformMatrix(int loc, boolean transpose, float[] m) {
        GLES20.glUniformMatrix4fv(loc, 1, transpose, m, 0);
    }
}

