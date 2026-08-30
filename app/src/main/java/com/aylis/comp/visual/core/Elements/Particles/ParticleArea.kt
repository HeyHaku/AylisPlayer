package com.aylis.comp.visual.core.Elements.Particles

import android.graphics.RectF
import com.aylis.Common.Vec2f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

interface ParticleArea {
    fun getRandomPointInArea(drawRect: RectF, outPos: Vec2f, outVec: Vec2f)
}

class HorizontalLineArea : ParticleArea {
    override fun getRandomPointInArea(drawRect: RectF, outPos: Vec2f, outVec: Vec2f) {
        outPos.x = drawRect.left + (drawRect.width() * Random.nextFloat())
        outPos.y = drawRect.centerY()
        outVec.x = 0f
        outVec.y = -1f
    }
}

class VerticalLineArea : ParticleArea {
    override fun getRandomPointInArea(drawRect: RectF, outPos: Vec2f, outVec: Vec2f) {
        outPos.x = drawRect.centerX()
        outPos.y = drawRect.top + (drawRect.height() * Random.nextFloat())
        outVec.x = 1f
        outVec.y = 0f
    }
}

class RectArea : ParticleArea {
    override fun getRandomPointInArea(drawRect: RectF, outPos: Vec2f, outVec: Vec2f) {
        outPos.x = drawRect.left + (drawRect.width() * Random.nextFloat())
        outPos.y = drawRect.top + (drawRect.height() * Random.nextFloat())
        outVec.x = 0f
        outVec.y = -1f
    }
}

class CircleArea(var radiusMult: Float = 1.0f) : ParticleArea {
    override fun getRandomPointInArea(drawRect: RectF, outPos: Vec2f, outVec: Vec2f) {
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        val r = (drawRect.width() / 2f) * radiusMult * Random.nextFloat()
        outVec.x = cos(angle)
        outVec.y = sin(angle)
        outPos.x = drawRect.centerX() + outVec.x * r
        outPos.y = drawRect.centerY() + outVec.y * r
    }
}

class PointArea : ParticleArea {
    override fun getRandomPointInArea(drawRect: RectF, outPos: Vec2f, outVec: Vec2f) {
        outPos.x = drawRect.centerX()
        outPos.y = drawRect.centerY()
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        outVec.x = cos(angle)
        outVec.y = sin(angle)
    }
}

object ParticleAreaFactory {
    fun create(type: String): ParticleArea {
        return when (type) {
            "VerticalLine" -> VerticalLineArea()
            "Rectangle" -> RectArea()
            "Circle" -> CircleArea()
            "Point" -> PointArea()
            "HorizontalLine" -> HorizontalLineArea()
            else -> HorizontalLineArea()
        }
    }
}
