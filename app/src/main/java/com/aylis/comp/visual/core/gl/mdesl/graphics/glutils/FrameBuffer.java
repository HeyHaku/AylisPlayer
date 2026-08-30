

package com.aylis.comp.visual.core.gl.mdesl.graphics.glutils;

import android.opengl.GLES20;
import com.aylis.comp.visual.core.gl.mdesl.graphics.ITexture;
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture;
import org.lwjgl.LWJGLException;

public class FrameBuffer implements ITexture {

	public static boolean isSupported() {
		return true;

	}

	protected int id;
	protected Texture texture;
	protected boolean ownsTexture;

	protected FrameBuffer(Texture texture, boolean ownsTexture) throws LWJGLException {
		this.texture = texture;
		this.ownsTexture = ownsTexture;
		if (!isSupported()) {
			throw new LWJGLException("FBO extension not supported in hardware");
		}
		texture.bind();
		int[] id_container = new int[1];
		GLES20.glGenFramebuffers(1, id_container, 0);
		id = id_container[0];
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
							 	  texture.getTarget(), texture.getID(), 0);
		int result = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (result!=GLES20.GL_FRAMEBUFFER_COMPLETE) {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			GLES20.glDeleteFramebuffers(1, new int[]{id}, 0);
			throw new LWJGLException("exception "+result+" when checking FBO status");
		}
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}

	public FrameBuffer(Texture texture) throws LWJGLException {
		this(texture, false);
	}

	public FrameBuffer(int width, int height, int filter, int wrap) throws LWJGLException {
		this(new Texture(width, height, filter, wrap), true);
	}

	public FrameBuffer(int width, int height, int filter) throws LWJGLException {
		this(width, height, filter, Texture.DEFAULT_WRAP);
	}

	public FrameBuffer(int width, int height) throws LWJGLException {
		this(width, height, Texture.DEFAULT_FILTER, Texture.DEFAULT_WRAP);
	}

	public int getID() {
		return id;
	}

	public int getWidth() {
		return texture.getWidth();
	}

	public int getHeight() {
		return texture.getHeight();
	}

	public Texture getTexture() {
		return texture;
	}

	public void begin() {
		if (id == 0)
			throw new IllegalStateException("can't use FBO as it has been destroyed..");
		GLES20.glViewport(0, 0, getWidth(), getHeight());
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, id);

	}

	public void end() {
		if (id==0)
			return;
		GLES20.glViewport(0, 0, getWidth(), getHeight());
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	}

	public void dispose() {
		if (id==0)
			return;
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glDeleteFramebuffers(1, new int[]{id}, 0);
		if (ownsTexture)
			texture.dispose();
		id = 0;

	}

	@Override
	public float getU() {
		return 0;
	}

	@Override
	public float getV() {
		return 1f;
	}

	@Override
	public float getU2() {
		return 1f;
	}

	@Override
	public float getV2() {
		return 0;
	}
}

