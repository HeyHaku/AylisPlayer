#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 v_texCoord;
uniform samplerExternalOES u_texture;

void main() {
    gl_FragColor = texture2D(u_texture, v_texCoord);
}
