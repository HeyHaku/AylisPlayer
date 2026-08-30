

package com.aylis.comp.visual.core.gl.mdesl.graphics.glutils;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

public class VertexArray implements VertexData {

	protected VertexAttrib[] attributes;

	private int totalNumComponents;
	private int stride;
	protected FloatBuffer buffer;
	private int vertCount;

	public VertexArray(int vertCount, VertexAttrib ... attributes) {
		this.attributes = attributes;
		for (VertexAttrib a : attributes)
			totalNumComponents += a.numComponents;
		this.vertCount = vertCount;

		this.buffer = ByteBuffer.allocateDirect((vertCount * totalNumComponents) << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
	}

	public VertexArray(int vertCount, List<VertexAttrib> attributes) {
		this(vertCount, attributes.toArray(new VertexAttrib[attributes.size()]));
	}

	public VertexArray flip() {
		buffer.flip();
		return this;
	}

	public VertexArray clear() {
		buffer.clear();
		return this;
	}

	public VertexArray put(float[] verts, int offset, int length) {
		buffer.put(verts, offset, length);
		return this;
	}

	public VertexArray put(float f) {
		buffer.put(f);
		return this;
	}

	public FloatBuffer buffer() {
		return buffer;
	}

	public int getTotalNumComponents() {
		return totalNumComponents;
	}

	public int getVertexCount() {
		return vertCount;
	}

	public void bind() {
		int offset = 0;

		int stride = totalNumComponents * 4;

		for (int i=0; i<attributes.length; i++) {
			VertexAttrib a = attributes[i];
			buffer.position(offset);
			GLES20.glEnableVertexAttribArray(a.location);
			GLES20.glVertexAttribPointer(a.location, a.numComponents, GLES20.GL_FLOAT, false, stride, buffer);
			offset += a.numComponents;
		}
	}

	public void draw(int geom, int first, int count) {
		GLES20.glDrawArrays(geom, first, count);
	}

	public void unbind() {
		for (int i=0; i<attributes.length; i++) {
			VertexAttrib a = attributes[i];
			GLES20.glDisableVertexAttribArray(a.location);
		}
	}
}

