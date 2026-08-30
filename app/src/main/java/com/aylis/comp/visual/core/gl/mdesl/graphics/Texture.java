

package com.aylis.comp.visual.core.gl.mdesl.graphics;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Texture implements ITexture {

	protected int id;
	protected int width;
	protected int height;

	public static int toPowerOfTwo(int n) {
        return 1 << (32 - Integer.numberOfLeadingZeros(n-1));
    }

    public static boolean isPowerOfTwo(int n) {
        return (n & -n) == n;
    }

	public static final int LINEAR = GLES20.GL_LINEAR;
	public static final int NEAREST = GLES20.GL_NEAREST;
	public static final int LINEAR_MIPMAP_LINEAR = GLES20.GL_LINEAR_MIPMAP_LINEAR;
	public static final int LINEAR_MIPMAP_NEAREST = GLES20.GL_LINEAR_MIPMAP_NEAREST;
	public static final int NEAREST_MIPMAP_NEAREST = GLES20.GL_NEAREST_MIPMAP_NEAREST;
	public static final int NEAREST_MIPMAP_LINEAR = GLES20.GL_NEAREST_MIPMAP_LINEAR;

	public static final int CLAMP_TO_EDGE = GLES20.GL_CLAMP_TO_EDGE;
	public static final int REPEAT = GLES20.GL_REPEAT;

	public static final int DEFAULT_FILTER = NEAREST;
	public static final int DEFAULT_WRAP = REPEAT;

	protected Texture() {

	}

	public Texture(int width, int height) {
		this(width, height, DEFAULT_FILTER);
	}

	public Texture(int width, int height, int filter) {
		this(width, height, filter, DEFAULT_WRAP);
	}

	public Texture(int width, int height, int filter, int wrap) {
		GLES20.glEnable(getTarget());
		int[] id_container = new int[1];
		GLES20.glGenTextures(1, id_container, 0);
		id = id_container[0];
		this.width = width;
		this.height = height;
		bind();

		setFilter(filter);
		setWrap(wrap);

		ByteBuffer buf = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
		upload(GLES20.GL_RGBA, buf);
	}

	public int getTarget() {
		return GLES20.GL_TEXTURE_2D;
	}

	public int getID() {
		return id;
	}

	protected void setUnpackAlignment() {
		GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
		GLES20.glPixelStorei(GLES20.GL_PACK_ALIGNMENT, 1);
	}

	public void upload(int dataFormat, ByteBuffer data) {
		bind();
		setUnpackAlignment();
		GLES20.glTexImage2D(getTarget(), 0, GLES20.GL_RGBA, width, height, 0, dataFormat, GLES20.GL_UNSIGNED_BYTE, data);
	}

	public void upload(int x, int y, int width, int height, int dataFormat, ByteBuffer data) {
		bind();
		setUnpackAlignment();
		GLES20.glTexSubImage2D(getTarget(), 0, x, y, width, height, dataFormat, GLES20.GL_UNSIGNED_BYTE, data);
	}

	public void setFilter(int filter) {
		setFilter(filter, filter);
	}

	public void setFilter(int minFilter, int magFilter) {
		bind();
		GLES20.glTexParameteri(getTarget(), GLES20.GL_TEXTURE_MIN_FILTER, minFilter);
		GLES20.glTexParameteri(getTarget(), GLES20.GL_TEXTURE_MAG_FILTER, magFilter);
	}

	public void setWrap(int wrap) {
		bind();
		GLES20.glTexParameteri(getTarget(), GLES20.GL_TEXTURE_WRAP_S, wrap);
		GLES20.glTexParameteri(getTarget(), GLES20.GL_TEXTURE_WRAP_T, wrap);
	}

	public void bind() {
		if (!valid())
			throw new IllegalStateException("trying to bind a texture that was disposed");
		GLES20.glBindTexture(getTarget(), id);
	}

	public void dispose() {
		if (valid()) {
			GLES20.glDeleteTextures(1, new int[]{id}, 0);
			id = 0;
		}
	}

	public boolean valid() {
		return id!=0;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public Texture getTexture() {
		return this;
	}

	@Override
	public float getU() {
		return 0f;
	}

	@Override
	public float getV() {
		return 0f;
	}

	@Override
	public float getU2() {
		return 1f;
	}

	@Override
	public float getV2() {
		return 1f;
	}
}
