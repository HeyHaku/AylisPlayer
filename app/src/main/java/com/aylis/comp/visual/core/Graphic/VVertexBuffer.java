

package com.aylis.comp.visual.core.Graphic;

import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.VertexArray;
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.VertexAttrib;

public class VVertexBuffer extends VertexArray {

    public VVertexBuffer(int vertCount, VertexAttrib... attributes) {
        super(vertCount, attributes);
    }

    public void dispose() {

    }

    public int remaining() {
        return buffer.remaining();
    }
}

