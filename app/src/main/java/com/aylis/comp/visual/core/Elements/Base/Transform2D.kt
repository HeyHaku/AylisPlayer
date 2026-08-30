package com.aylis.comp.visual.core.Elements.Base

import android.graphics.Matrix
import android.graphics.RectF

class Transform2D {
    private val matrix = Matrix()
    private val tempPoints = FloatArray(8)

    fun reset() {
        matrix.reset()
    }

    fun translate(dx: Float, dy: Float) {
        matrix.postTranslate(dx, dy)
    }

    fun rotate(degrees: Float, px: Float, py: Float) {
        matrix.postRotate(degrees, px, py)
    }

    fun rotate(degrees: Float) {
        matrix.postRotate(degrees)
    }

    fun scale(sx: Float, sy: Float, px: Float, py: Float) {
        matrix.postScale(sx, sy, px, py)
    }

    fun scale(sx: Float, sy: Float) {
        matrix.postScale(sx, sy)
    }

    /**
     * Maps a rectangle into 4 vertices (8 floats) representing the 4 corners:
     * top-left, top-right, bottom-left, bottom-right.
     * x0, y0 = top-left
     * x1, y1 = top-right
     * x2, y2 = bottom-left
     * x3, y3 = bottom-right
     */
    fun mapRectToVertices(rect: RectF, outVertices: FloatArray) {
        tempPoints[0] = rect.left
        tempPoints[1] = rect.top
        
        tempPoints[2] = rect.right
        tempPoints[3] = rect.top
        
        tempPoints[4] = rect.left
        tempPoints[5] = rect.bottom
        
        tempPoints[6] = rect.right
        tempPoints[7] = rect.bottom

        matrix.mapPoints(outVertices, tempPoints)
    }

    fun mapPoint(x: Float, y: Float, outPoint: FloatArray) {
        tempPoints[0] = x
        tempPoints[1] = y
        matrix.mapPoints(outPoint, 0, tempPoints, 0, 1)
    }

    fun mapPoints(pts: FloatArray) {
        matrix.mapPoints(pts)
    }
}
