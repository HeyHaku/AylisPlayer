package com.aylis.comp.visual.core.Elements.Particles

import com.aylis.Common.Vec2f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max
import kotlin.random.Random

object ParticlePhysics {

    fun spawnVelocity(
        behaviorType: String,
        outVec: Vec2f,
        speedBase: Float,
        audioBurstMult: Float = 1f
    ) {
        val speed = (speedBase * Random.nextFloat()) + (speedBase * 0.3f) * audioBurstMult

        when (behaviorType) {
            "FloatingUp" -> {
                val angle = (Random.nextFloat() * 2f - 1f) * 0.5f
                outVec.x = sin(angle) * speed
                outVec.y = -cos(angle) * speed
            }
            "Rising" -> {
                outVec.x = 0f
                outVec.y = -speed
            }
            "Falling" -> {
                outVec.x = 0f
                outVec.y = speed
            }
            "OutwardBlast" -> {
                outVec.x *= speed
                outVec.y *= speed
            }
            "Turbulence", "Vortex" -> {
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                outVec.x = cos(angle) * speed
                outVec.y = sin(angle) * speed
            }
            else -> {
                outVec.x *= speed
                outVec.y *= speed
            }
        }
    }

    fun applyPhysics(
        data: FloatArray,
        baseIndex: Int,
        dt: Float,
        behaviorType: String,
        speedMultiplier: Float = 1f,
        cx: Float = 0f,
        cy: Float = 0f
    ) {
        val posX = data[baseIndex + ParticlePool.POS_X]
        val posY = data[baseIndex + ParticlePool.POS_Y]
        var velX = data[baseIndex + ParticlePool.VEL_X]
        var velY = data[baseIndex + ParticlePool.VEL_Y]
        val gravX = data[baseIndex + ParticlePool.GRAVITY_X]
        val gravY = data[baseIndex + ParticlePool.GRAVITY_Y]
        val friction = data[baseIndex + ParticlePool.FRICTION]
        val angularVel = data[baseIndex + ParticlePool.ANGULAR_VELOCITY]

        if (behaviorType == "Vortex") {
            val dx = posX - cx
            val dy = posY - cy
            val dist = kotlin.math.sqrt(dx * dx + dy * dy) + 0.001f
            
            val dirX = dx / dist
            val dirY = dy / dist
            
            // Берем текущую скорость частицы (задается юзером)
            val currentSpeed = kotlin.math.sqrt(velX * velX + velY * velY)
            
            // Задаем касательную (вихрь) и небольшое притяжение к центру
            val targetVelX = dirY * currentSpeed - dirX * currentSpeed * 0.2f
            val targetVelY = -dirX * currentSpeed - dirY * currentSpeed * 0.2f
            
            // Плавно интерполируем текущую скорость к скорости вихря
            velX = velX * (1f - dt * 5f) + targetVelX * dt * 5f
            velY = velY * (1f - dt * 5f) + targetVelY * dt * 5f
        } else if (behaviorType == "Turbulence") {
            // Плавный шум Перлина (упрощенная симуляция)
            val freq = 0.05f
            velX += kotlin.math.sin(posY * freq + dt * 10f) * 50f * dt
            velY += kotlin.math.cos(posX * freq + dt * 10f) * 50f * dt
        }

        // Apply friction
        velX *= max(0f, 1.0f - friction * dt)
        velY *= max(0f, 1.0f - friction * dt)

        data[baseIndex + ParticlePool.VEL_X] = velX
        data[baseIndex + ParticlePool.VEL_Y] = velY

        data[baseIndex + ParticlePool.POS_X] = posX + (velX * speedMultiplier + gravX) * dt
        data[baseIndex + ParticlePool.POS_Y] = posY + (velY * speedMultiplier + gravY) * dt

        // Apply angular velocity to rotation
        data[baseIndex + ParticlePool.ROTATION] = data[baseIndex + ParticlePool.ROTATION] + angularVel * dt
    }
}
