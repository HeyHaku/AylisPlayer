

package com.aylis.comp.visual.core.Graphic;

import android.opengl.GLES20;
import com.aylis.Common.tlog;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.ShaderProgram;

public class FullscreenQuad {

    private static final int COORDS_PER_VERTEX = 3;
    private static float squareCoords[] = {
            -1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            1.0f, 1.0f, 0.0f};

    private static float squareFlippedCoords[] = {
            0.5f, 0.5f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f};

    private static final String vertexShaderCode =
            "const vec2 madd=vec2(0.5,0.5);" +
                    "attribute vec2 vertexIn;" +
                    "varying vec2 textureCoord;" +
                    "void main() {" +
                    "textureCoord = vertexIn.xy*madd+madd;" +
                    "gl_Position = vec4(vertexIn.xy,0.0,1.0);" +
                    "}";

    private static final String fragmentShaderCode =
            "precision mediump float;" +
                    "varying vec2 textureCoord;" +
                    "uniform sampler2D s_texture;" +
                    "void main() {" +
                    "vec4 color1 = texture2D(s_texture,textureCoord);" +
                    "gl_FragColor = color1;" +
                    "}";

    private final FloatBuffer vertexBuffer, vertexBufferFlipped;
    private final ShortBuffer drawListBuffer;
    private final int program;
    private final short drawOrder[] = {0, 1, 2, 0, 2, 3};
    private final int vertexStride = COORDS_PER_VERTEX * 4;
    private int positionHandle;

    public FullscreenQuad() {

        ByteBuffer bb = ByteBuffer.allocateDirect(

                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(

                squareFlippedCoords.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        vertexBufferFlipped = bb2.asFloatBuffer();
        vertexBufferFlipped.put(squareFlippedCoords);
        vertexBufferFlipped.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(

                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        int vertexShader = GraphicsUtils.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = GraphicsUtils.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            tlog.w("Could not link program: ");
            tlog.w(GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
        }
    }

    public void drawFlipped() {

        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vertexIn");

        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glVertexAttribPointer(
                positionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBufferFlipped);

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    public void draw() {

        GLES20.glUseProgram(program);

        positionHandle = GLES20.glGetAttribLocation(program, "vertexIn");

        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glVertexAttribPointer(
                positionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

    public void drawShader(ShaderProgram blurShader, String position) {

        int positionHandle = blurShader.getAttributeLocation(position);

        GLES20.glEnableVertexAttribArray(positionHandle);

        GLES20.glVertexAttribPointer(
                positionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(positionHandle);
    }

}

