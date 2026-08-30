

package com.aylis.comp.visual.core.gl.mdesl.graphics.glutils;

import android.opengl.GLES20;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.List;
import org.lwjgl.LWJGLException;

public class ShaderProgram {

	private static FloatBuffer fbuf16;
	private static IntBuffer ibuf4;

	protected static class Attrib {
		String name = null;

		int location = -1;
	}

	public static final int VERTEX_SHADER = GLES20.GL_VERTEX_SHADER;

	public static final int FRAGMENT_SHADER = GLES20.GL_FRAGMENT_SHADER;
	private static boolean strict = false;

	public static boolean isSupported() {
		return true;

	}

	public static void setStrictMode(boolean enabled) {
		strict = enabled;
	}

	public static boolean isStrictMode() {
		return strict;
	}

	protected int program;

	protected String log = "";

	protected HashMap<String, Integer> uniforms = new HashMap<String, Integer>();

	protected Attrib[] attributes;

	protected String vertShaderSource;

	protected String fragShaderSource;

	protected int vert;

	protected int frag;

	public ShaderProgram(String vertexShaderSource, String fragShaderSource,
			List<VertexAttrib> attribLocations) throws LWJGLException {
		if (vertexShaderSource == null || fragShaderSource == null)
			throw new IllegalArgumentException("shader source must be non-null");
		if (!isSupported())
			throw new LWJGLException("no shader support found; shaders require OpenGL 2.0");
		this.vertShaderSource = vertexShaderSource;
		this.fragShaderSource = fragShaderSource;
		vert = compileShader(VERTEX_SHADER, vertexShaderSource);
		frag = compileShader(FRAGMENT_SHADER, fragShaderSource);
		program = createProgram();
		try {
			linkProgram(attribLocations);
		} catch (LWJGLException e) {
			dispose();
			throw e;
		}

	}

	public ShaderProgram(String vertexShaderSource, String fragShaderSource) throws LWJGLException {
		this(vertexShaderSource, fragShaderSource, null);
	}

	protected ShaderProgram() {
	}

	protected int createProgram() throws LWJGLException {
		int program = GLES20.glCreateProgram();
		if (program == 0)
			throw new LWJGLException("could not create program; check ShaderProgram.isSupported()");
		return program;
	}

	private String shaderTypeString(int type) {
		if (type == FRAGMENT_SHADER)
			return "FRAGMENT_SHADER";
		else if (type == VERTEX_SHADER)
			return "VERTEX_SHADER";

		else
			return "shader";
	}

	protected int compileShader(int type, String source) throws LWJGLException {
		int shader = GLES20.glCreateShader(type);
		if (shader == 0)
			throw new LWJGLException(
					"could not create shader object; check ShaderProgram.isSupported()");
		GLES20.glShaderSource(shader, source);
		GLES20.glCompileShader(shader);

		int comp;
		int[] comp_container = new int[1];
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, comp_container, 0);
		comp = comp_container[0];
		String t = shaderTypeString(type);
		String err = GLES20.glGetShaderInfoLog(shader);
		if (err != null && err.length() != 0)
			log += t + " compile log:\n" + err + "\n";
		if (comp == GLES20.GL_FALSE)
			throw new LWJGLException(log.length()!=0 ? log : "Could not compile "+shaderTypeString(type));
		return shader;
	}

	protected void attachShaders() {
		GLES20.glAttachShader(getID(), vert);
		GLES20.glAttachShader(getID(), frag);
	}

	protected void linkProgram(List<VertexAttrib> attribLocations) throws LWJGLException {
		if (!valid())
			throw new LWJGLException("trying to link an invalid (i.e. released) program");

		uniforms.clear();

		if (attribLocations != null) {
			for (VertexAttrib a : attribLocations) {
				if (a != null)
					GLES20.glBindAttribLocation(program, a.location, a.name);
			}
		}

		attachShaders();
		GLES20.glLinkProgram(program);
		int comp;
		int[] comp_container = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, comp_container, 0);
		comp = comp_container[0];
		String err = GLES20.glGetProgramInfoLog(program);
		if (err != null && err.length() != 0)
			log = err + "\n" + log;
		if (log != null)
			log = log.trim();
		if (comp == GLES20.GL_FALSE)
			throw new LWJGLException(log.length()!=0 ? log : "Could not link program");

		fetchUniforms();
		fetchAttributes();
	}

	public String getLog() {
		return log;
	}

	public void use() {
		if (!valid())
			throw new IllegalStateException("trying to enable a program that is not valid");
		GLES20.glUseProgram(program);
	}

	public void disposeShaders() {
		if (vert != 0) {
			GLES20.glDetachShader(getID(), vert);
			GLES20.glDeleteShader(vert);
			vert = 0;
		}
		if (frag != 0) {
			GLES20.glDetachShader(getID(), frag);
			GLES20.glDeleteShader(frag);
			frag = 0;
		}
	}

	public void dispose() {
		if (program != 0) {
			disposeShaders();
			GLES20.glDeleteProgram(program);
			program = 0;
		}
	}

	public int getVertexShaderID() {
		return vert;
	}

	public int getFragmentShaderID() {
		return frag;
	}

	public String getVertexShaderSource() {
		return vertShaderSource;
	}

	public String getFragmentShaderSource() {
		return fragShaderSource;
	}

	public int getID() {
		return program;
	}

	public boolean valid() {
		return program != 0;
	}

	private void fetchUniforms() {
		int len;
		int[] len_container = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_UNIFORMS, len_container, 0);
		len = len_container[0];

		final int NAME_CONTAINER_SIZE = 64;
		final int[] length_container = new int[1];
		final int[] size_container = new int[1];
		final int[] type_container = new int[1];
		final byte[] name_container = new byte[NAME_CONTAINER_SIZE];

		for (int i = 0; i < len; i++) {

			GLES20.glGetActiveUniform(program, i, NAME_CONTAINER_SIZE, length_container, 0, size_container, 0, type_container, 0, name_container, 0);

			int length = length_container[0];

			if (length == 0) {
				while (length < NAME_CONTAINER_SIZE && name_container[length] != '\0') {
					length++;
				}
			}

			String name = new String(name_container, 0, length);

			if (name.contains(" ")) {
				name = name.substring(0, name.indexOf(" "));
			}

			int id = GLES20.glGetUniformLocation(program, name);
			uniforms.put(name, id);
		}
	}

	private void fetchAttributes() {
		int len;
		int[] len_container = new int[1];
		GLES20.glGetProgramiv(program, GLES20.GL_ACTIVE_ATTRIBUTES, len_container, 0);
		len = len_container[0];

		final int NAME_CONTAINER_SIZE = 64;
		final int[] length_container = new int[1];
		final int[] size_container = new int[1];
		final int[] type_container = new int[1];
		final byte[] name_container = new byte[NAME_CONTAINER_SIZE];

		attributes = new Attrib[len];
		for (int i = 0; i < len; i++) {
			Attrib a = new Attrib();

			GLES20.glGetActiveAttrib(program, i, NAME_CONTAINER_SIZE, length_container, 0, size_container, 0, type_container, 0, name_container, 0);

			a.name = new String(name_container, 0, length_container[0]);

			a.location = GLES20.glGetAttribLocation(program, a.name);
			attributes[i] = a;
		}
	}

	public int getUniformLocation(String name) {
		int location = -1;
		Integer locI = uniforms.get(name);
		if (locI == null) {
			location = GLES20.glGetUniformLocation(program, name);
			uniforms.put(name, location);
		} else
			location = locI.intValue();

		if (location == -1 && strict)
			throw new IllegalArgumentException("no active uniform by name '" + name + "' "
					+ "(disable strict compiling to suppress warnings)");
		return location;
	}

	Attrib attrib(String name) {
		for (int i = 0; i < attributes.length; i++) {
			if (name.equals(attributes[i].name))
				return attributes[i];
		}

		if (strict)
			throw new IllegalArgumentException("no active attribute by name '" + name + "' "
					+ "(disable strict compiling to suppress warnings)");
		return null;
	}

	public int getAttributeLocation(String name) {
		Attrib a = attrib(name);
		return a != null ? a.location : -1;
	}

	public String[] getAttributeNames() {
		String[] s = new String[attributes.length];
		for (int i = 0; i < attributes.length; i++) {
			s[i] = attributes[i].name;
		}
		return s;
	}

	public String[] getUniformNames() {
		return uniforms.keySet().toArray(new String[uniforms.size()]);
	}

	public boolean hasUniform(String name) {
		return uniforms.containsKey(name);
	}

	public boolean hasAttribute(String name) {
		for (int i = 0; i < attributes.length; i++)
			if (name.equals(attributes[i].name))
				return true;
		return false;
	}

	private FloatBuffer uniformf(int loc) {
		if (fbuf16 == null)
			fbuf16 = ByteBuffer.allocateDirect(16 << 2).order(ByteOrder.nativeOrder()).asFloatBuffer();
		fbuf16.clear();
		if (loc == -1)
			return fbuf16;
		getUniform(loc, fbuf16);
		return fbuf16;
	}

	private IntBuffer uniformi(int loc) {
		if (ibuf4 == null)
			ibuf4 = ByteBuffer.allocateDirect(4 << 2).order(ByteOrder.nativeOrder()).asIntBuffer();
		ibuf4.clear();
		if (loc == -1)
			return ibuf4;
		getUniform(loc, ibuf4);
		return ibuf4;
	}

	public void getUniform(int loc, FloatBuffer buf) {
		GLES20.glGetUniformfv(program, loc, buf);
	}

	public void getUniform(int loc, IntBuffer buf) {
		GLES20.glGetUniformiv(program, loc, buf);
	}

	public boolean getUniform(String name, FloatBuffer buf) {
		int id = getUniformLocation(name);
		if (id == -1)
			return false;
		getUniform(id, buf);
		return true;
	}

	public boolean getUniform(String name, IntBuffer buf) {
		int id = getUniformLocation(name);
		if (id == -1)
			return false;
		getUniform(id, buf);
		return true;
	}

	public int getUniform1i(int loc) {
		return uniformi(loc).get(0);
	}

	public int getUniform1i(String name) {
		return getUniform1i(getUniformLocation(name));
	}

	public int[] getUniform2i(int loc) {
		IntBuffer buf = uniformi(loc);
		return new int[] { buf.get(0), buf.get(1) };
	}

	public int[] getUniform2i(String name) {
		return getUniform2i(getUniformLocation(name));
	}

	public int[] getUniform3i(int loc) {
		IntBuffer buf = uniformi(loc);
		return new int[] { buf.get(0), buf.get(1), buf.get(2) };
	}

	public int[] getUniform3i(String name) {
		return getUniform3i(getUniformLocation(name));
	}

	public int[] getUniform4i(int loc) {
		IntBuffer buf = uniformi(loc);
		return new int[] { buf.get(0), buf.get(1), buf.get(2), buf.get(3) };
	}

	public int[] getUniform4i(String name) {
		return getUniform4i(getUniformLocation(name));
	}

	public float getUniform1f(int loc) {
		return uniformf(loc).get(0);
	}

	public float getUniform1f(String name) {
		return getUniform1f(getUniformLocation(name));
	}

	public float[] getUniform2f(int loc) {
		FloatBuffer buf = uniformf(loc);
		return new float[] { buf.get(0), buf.get(1) };
	}

	public float[] getUniform2f(String name) {
		return getUniform2f(getUniformLocation(name));
	}

	public float[] getUniform3f(int loc) {
		FloatBuffer buf = uniformf(loc);
		return new float[] { buf.get(0), buf.get(1), buf.get(2) };
	}

	public float[] getUniform3f(String name) {
		return getUniform3f(getUniformLocation(name));
	}

	public float[] getUniform4f(int loc) {
		FloatBuffer buf = uniformf(loc);
		return new float[] { buf.get(0), buf.get(1), buf.get(2), buf.get(3) };
	}

	public float[] getUniform4f(String name) {
		return getUniform4f(getUniformLocation(name));
	}

	public void setUniformf(int loc, float f) {
		if (loc==-1) return;
		GLES20.glUniform1f(loc, f);
	}

	public void setUniformf(int loc, float a, float b) {
		if (loc==-1) return;
		GLES20.glUniform2f(loc, a, b);
	}

	public void setUniformf(int loc, float a, float b, float c) {
		if (loc==-1) return;
		GLES20.glUniform3f(loc, a, b, c);
	}

	public void setUniformf(int loc, float a, float b, float c, float d) {
		if (loc==-1) return;
		GLES20.glUniform4f(loc, a, b, c, d);
	}

	public void setUniformi(int loc, int i) {
		if (loc==-1) return;
		GLES20.glUniform1i(loc, i);
	}

	public void setUniformi(int loc, int a, int b) {
		if (loc==-1) return;
		GLES20.glUniform2i(loc, a, b);
	}

	public void setUniformi(int loc, int a, int b, int c) {
		if (loc==-1) return;
		GLES20.glUniform3i(loc, a, b, c);
	}

	public void setUniformi(int loc, int a, int b, int c, int d) {
		if (loc==-1) return;
		GLES20.glUniform4i(loc, a, b, c, d);
	}

	public void setUniformf(String name, float f) {
		setUniformf(getUniformLocation(name), f);
	}

	public void setUniformf(String name, float a, float b) {
		setUniformf(getUniformLocation(name), a, b);
	}

	public void setUniformf(String name, float a, float b, float c) {
		setUniformf(getUniformLocation(name), a, b, c);
	}

	public void setUniformf(String name, float a, float b, float c, float d) {
		setUniformf(getUniformLocation(name), a, b, c, d);
	}

	public void setUniformi(String name, int i) {
		setUniformi(getUniformLocation(name), i);
	}

	public void setUniformi(String name, int a, int b) {
		setUniformi(getUniformLocation(name), a, b);
	}

	public void setUniformi(String name, int a, int b, int c) {
		setUniformi(getUniformLocation(name), a, b, c);
	}

	public void setUniformi(String name, int a, int b, int c, int d) {
		setUniformi(getUniformLocation(name), a, b, c, d);
	}

}
