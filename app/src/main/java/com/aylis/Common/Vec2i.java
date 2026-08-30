

package com.aylis.Common;

public class Vec2i {

    public static Vec2i invalid = new Vec2i(-Integer.MAX_VALUE, -Integer.MAX_VALUE);
    public static Vec2i zero = new Vec2i(0, 0);
    public static Vec2i one = new Vec2i(1, 1);

    public int x, y;

    public Vec2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

}
