package com.aylis.comp.visual.core.Elements.Particles

import kotlin.math.max

class ParticlePool(capacity: Int) {
    val PARTICLE_SIZE = 17
    
    var data: FloatArray = FloatArray(max(1, capacity) * PARTICLE_SIZE)
        private set
        
    var spawnHead = 0
        private set
        
    val capacity: Int
        get() = data.size / PARTICLE_SIZE

    companion object {
        const val ALIVE = 0
        const val POS_X = 1
        const val POS_Y = 2
        const val POS_Z = 3
        const val VEL_X = 4
        const val VEL_Y = 5
        const val CURR_LIFETIME = 6
        const val LIFETIME = 7
        const val SIZE_MULT = 8
        const val GRAVITY_X = 9
        const val GRAVITY_Y = 10
        const val ROTATION = 11
        const val COLOR_ARGB = 12
        const val FRAME_OFFSET = 13
        const val ANGULAR_VELOCITY = 14
        const val FRICTION = 15
        const val COLOR_END_ARGB = 16
    }

    fun resize(newCapacity: Int) {
        if (newCapacity == capacity) return
        val cap = max(1, newCapacity)
        val newData = FloatArray(cap * PARTICLE_SIZE)
        val elementsToCopy = Math.min(data.size, newData.size)
        System.arraycopy(data, 0, newData, 0, elementsToCopy)
        data = newData
        if (spawnHead >= cap) {
            spawnHead = 0
        }
    }

    fun spawn(
        posX: Float, posY: Float, posZ: Float,
        velX: Float, velY: Float,
        lifetime: Float, sizeMult: Float,
        gravityX: Float, gravityY: Float,
        rotation: Float, colorArgb: Int, frameOffset: Float,
        angularVelocity: Float, friction: Float, colorEndArgb: Int
    ) {
        val baseIndex = spawnHead * PARTICLE_SIZE
        data[baseIndex + ALIVE] = 1f
        data[baseIndex + POS_X] = posX
        data[baseIndex + POS_Y] = posY
        data[baseIndex + POS_Z] = posZ
        data[baseIndex + VEL_X] = velX
        data[baseIndex + VEL_Y] = velY
        data[baseIndex + CURR_LIFETIME] = 0f
        data[baseIndex + LIFETIME] = lifetime
        data[baseIndex + SIZE_MULT] = sizeMult
        data[baseIndex + GRAVITY_X] = gravityX
        data[baseIndex + GRAVITY_Y] = gravityY
        data[baseIndex + ROTATION] = rotation
        data[baseIndex + COLOR_ARGB] = Float.fromBits(colorArgb)
        data[baseIndex + FRAME_OFFSET] = frameOffset
        data[baseIndex + ANGULAR_VELOCITY] = angularVelocity
        data[baseIndex + FRICTION] = friction
        data[baseIndex + COLOR_END_ARGB] = Float.fromBits(colorEndArgb)

        spawnHead = (spawnHead + 1) % capacity
    }
}
