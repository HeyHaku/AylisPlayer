package com.aylis.comp.visual.core.Elements

import android.graphics.Bitmap
import android.graphics.Color
import com.aylis.Common.Vec2f
import com.aylis.comp.AlbumArt.AlbumArtRequest
import com.aylis.comp.AlbumArt.ImageLoadedListener
import com.aylis.comp.visual.core.Graphic.AtlasTexture
import com.aylis.comp.visual.core.Graphic.RenderState
import com.aylis.comp.visual.core.Graphic.VTexture
import com.aylis.comp.visual.core.Elements.Particles.ParticlePool
import com.aylis.comp.visual.core.Elements.Particles.ParticlePhysics
import com.aylis.comp.visual.core.Elements.Particles.ParticleAreaFactory
import com.aylis.comp.visual.core.Elements.Particles.ParticleArea
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer
import com.aylis.comp.visual.core.Graphic.SafeMipmapHelper
import com.aylis.comp.visual.core.Elements.Base.MVariableFloat
import kotlin.math.max
import com.aylis.comp.visual.core.Elements.Base.Transform2D

class ParticlesElement : Element(), ImageLoadedListener {

    companion object {
        const val PRECOMP_PREFIX = "precomp:"
    }

    private var customImagePath = ""
    private var albumArtRequest = AlbumArtRequest("", "", "", "")
    private var bitmap: Bitmap? = null
    private var bitmapLoading = false
    private var bitmapLoadedIn = false
    private var imageLoadStrongReference: Any? = null

    private var tex2: Texture? = null
    private var atlasTex2: AtlasTexture? = null

    private var particlesLowCount = 1000
    private var pool = ParticlePool(particlesLowCount)
    private var emittingTimeAcc = 0.0f
    private var areaField: ParticleArea? = null
    private var everySec = MVariableFloat.createConstantFloat(0.05f)
    private var color1 = 0xffffffff.toInt()
    private var color2 = 0xffffffff.toInt()
    private var startSize = MVariableFloat.createConstantFloat(1.0f)
    private var midSize = MVariableFloat.createConstantFloat(1.0f)
    private var endSize = MVariableFloat.createConstantFloat(1.0f)

    private var areaType = "HorizontalLine"
    private var behaviorType = "FloatingUp"
    private var particleSpeed = MVariableFloat.createConstantFloat(0.05f)
    private var friction = MVariableFloat.createConstantFloat(0.0f)
    private var spinSpeed = MVariableFloat.createConstantFloat(0.0f)
    private var gravityX = 0.0f
    private var gravityY = 100.0f
    private var particleLifetime = 3.0f
    private var particleTrail = MVariableFloat.createConstantFloat(0.0f)
    private var bassReaction = 1.0f
    private var currentBass = 0.0f
    private var smoothedTrail = 0.0f
    private val randomGenerator = kotlin.random.Random
    private var perspectiveDepth = 0.0f
    private var mirrorX = false
    private var mirrorY = false
    private var sizeRandomness = 0.5f
    private var lifeRandomness = 0.5f
    private var speedRandomness = 0.5f
    
    private val elementTransform = Transform2D()
    private val transformResult = FloatArray(8)

    init {
        useAnimatorMeasures = false
    }

    private fun updateAreaField() {
        areaField = ParticleAreaFactory.create(areaType)
    }

    fun getAreaTypeName(): String {
        return areaType
    }

    override fun onApplyCustomization(customizationData: CustomizationData) {
        super.onApplyCustomization(customizationData)
        color1 = customizationData.getPropertyInt("color", color1)
        color2 = customizationData.getPropertyInt("colorEnd", color2)
        val oldScale = customizationData.getPropertyMVariableFloat("particleScale", startSize)
        startSize = customizationData.getPropertyMVariableFloat("startSize", oldScale)
        midSize = customizationData.getPropertyMVariableFloat("midSize", startSize)
        endSize = customizationData.getPropertyMVariableFloat("endSize", midSize)
        everySec = customizationData.getPropertyMVariableFloat("spawnTime", everySec)
        
        val newLimit = customizationData.getPropertyInt("particlesLimit", particlesLowCount)
        if (newLimit != particlesLowCount) {
            particlesLowCount = newLimit
            pool.resize(particlesLowCount)
        }

        setCustomImagePath(customizationData.getPropertyString("customImage", ""))

        val areaCustom = customizationData.getChild("areaType")
        areaType = areaCustom.childTypeValue ?: "HorizontalLine"
        updateAreaField()

        val behaviorCustom = customizationData.getChild("behaviorType")
        behaviorType = behaviorCustom.childTypeValue ?: "FloatingUp"

        particleSpeed = customizationData.getPropertyMVariableFloat("particleSpeed", particleSpeed)
        friction = customizationData.getPropertyMVariableFloat("friction", friction)
        spinSpeed = customizationData.getPropertyMVariableFloat("spinSpeed", spinSpeed)
        particleTrail = customizationData.getPropertyMVariableFloat("particleTrail", particleTrail)
        bassReaction = customizationData.getPropertyFloat("bassReaction", bassReaction)

        val grav = customizationData.getPropertyVec2f("gravity", Vec2f(gravityX, gravityY))
        gravityX = grav.x
        gravityY = grav.y

        particleLifetime = customizationData.getPropertyFloat("particleLifetime", particleLifetime)
        
        perspectiveDepth = customizationData.getPropertyFloat("perspectiveDepth", perspectiveDepth)
        mirrorX = customizationData.getPropertyBool("mirrorX", mirrorX)
        mirrorY = customizationData.getPropertyBool("mirrorY", mirrorY)
        sizeRandomness = customizationData.getPropertyFloat("sizeRandomness", sizeRandomness)
        lifeRandomness = customizationData.getPropertyFloat("lifeRandomness", lifeRandomness)
        speedRandomness = customizationData.getPropertyFloat("speedRandomness", speedRandomness)
    }

    override fun onReadCustomization(outCustomizationData: CustomizationData) {
        super.onReadCustomization(outCustomizationData)

        outCustomizationData.setCustomizationName("Particles")
        outCustomizationData.putPropertyString("customImage", customImagePath, "img", "1_particles")
        outCustomizationData.putPropertyInt("color", color1, "crgba", "1_particles")
        outCustomizationData.putPropertyInt("colorEnd", color2, "crgba", "1_particles")
        outCustomizationData.putPropertyMVariableFloat("startSize", startSize, "1_particles", 0.0f, 20.0f)
        outCustomizationData.putPropertyMVariableFloat("midSize", midSize, "1_particles", 0.0f, 20.0f)
        outCustomizationData.putPropertyMVariableFloat("endSize", endSize, "1_particles", 0.0f, 20.0f)
        outCustomizationData.putPropertyMVariableFloat("spawnTime", everySec, "1_particles", 0.01f, 1.0f)
        outCustomizationData.putPropertyInt("particlesLimit", particlesLowCount, "i 10 2000", "1_particles")

        outCustomizationData.putChild("areaType", getAreaTypeName(), arrayOf("HorizontalLine", "VerticalLine", "Rectangle", "Circle", "Point"), "2_motion")
        outCustomizationData.putChild("behaviorType", behaviorType, arrayOf("FloatingUp", "OutwardBlast", "Rising", "Falling", "Turbulence", "Vortex"), "2_motion")

        outCustomizationData.putPropertyMVariableFloat("particleSpeed", particleSpeed, "2_motion", 0.0f, 10.0f)
        outCustomizationData.putPropertyMVariableFloat("friction", friction, "2_motion", 0.0f, 20.0f)
        outCustomizationData.putPropertyMVariableFloat("spinSpeed", spinSpeed, "2_motion", -1000.0f, 1000.0f)
        outCustomizationData.putPropertyMVariableFloat("particleTrail", particleTrail, "2_motion", 0.0f, 10.0f)
        outCustomizationData.putPropertyFloat("bassReaction", bassReaction, "f 0.0 10.0", "2_motion")
        outCustomizationData.putPropertyVec2f("gravity", Vec2f(gravityX, gravityY), "f2 -500.0 500.0", "2_motion")
        outCustomizationData.putPropertyFloat("particleLifetime", particleLifetime, "f 0.5 20.0", "2_motion")
        outCustomizationData.putPropertyFloat("sizeRandomness", sizeRandomness, "f 0.0 1.0", "1_particles")
        outCustomizationData.putPropertyFloat("lifeRandomness", lifeRandomness, "f 0.0 1.0", "1_particles")
        outCustomizationData.putPropertyFloat("speedRandomness", speedRandomness, "f 0.0 1.0", "2_motion")
        
        outCustomizationData.putPropertyFloat("perspectiveDepth", perspectiveDepth, "f 0.0 1000.0", "1_overall")
        outCustomizationData.putPropertyBool("mirrorX", mirrorX, "1_overall")
        outCustomizationData.putPropertyBool("mirrorY", mirrorY, "1_overall")
    }

    override fun onRender(renderData: RenderState, resultFB: FrameBuffer?) {
        super.onRender(renderData, resultFB)

        if (areaField == null) {
            updateAreaField()
        }

        val drawRect = measureDrawRect(renderData.res.meter)
        val dt = renderData.frameTimeSmooth

        val rms = renderData.res.meter.frameDataRmsValue
        // Jelly-like smoothing: smoothly approach the target RMS
        currentBass += (rms - currentBass) * dt * 15.0f
        
        emittingTimeAcc += dt
        
        // Evaluate variables
        val evalSpawnTime = max(0.005f, everySec.getValueAsFloat(renderData.res.meter, 0f))
        val evalParticleSpeed = particleSpeed.getValueAsFloat(renderData.res.meter, 0f) * 2000.0f
        val evalFriction = max(0f, friction.getValueAsFloat(renderData.res.meter, 0f))
        val evalSpinSpeed = spinSpeed.getValueAsFloat(renderData.res.meter, 0f)

        // Modulate spawn interval so strong bass spawns particles faster
        val bassSpawnMultiplier = 1f - (currentBass * bassReaction * 0.8f)
        val spawnInterval = max(0.005f, evalSpawnTime * bassSpawnMultiplier)

        val pathPointOut = Vec2f(0f,0f)
        val pathPointVecOut = Vec2f(0f,0f)

        var animSprite = atlasTex2
        if (animSprite == null) {
            if (customImagePath.startsWith(PRECOMP_PREFIX)) {
                val preCompName = customImagePath.substring(PRECOMP_PREFIX.length)
                val precomp = PreCompManager.get(preCompName)
                if (precomp != null) {
                    val preCompTex = precomp.texture
                    if (SafeMipmapHelper.isTextureReady(preCompTex)) {
                        animSprite = AtlasTexture(preCompTex)
                    }
                }
            }
        }
        if (animSprite == null) {
            animSprite = renderData.res.atlasTexParticle0
        }

        while (emittingTimeAcc > spawnInterval) {
            emittingTimeAcc -= spawnInterval

            areaField?.getRandomPointInArea(drawRect, pathPointOut, pathPointVecOut)
            val speedRandMult = 1f - randomGenerator.nextFloat() * speedRandomness
            val currentParticleSpeed = evalParticleSpeed * speedRandMult
            ParticlePhysics.spawnVelocity(behaviorType, pathPointVecOut, currentParticleSpeed, 1f + currentBass * bassReaction * 1.5f)

            val alphaMult = 0.3f + randomGenerator.nextFloat() * 0.7f
            val startA = (android.graphics.Color.alpha(color1) * alphaMult).toInt()
            val initColor = android.graphics.Color.argb(startA, android.graphics.Color.red(color1), android.graphics.Color.green(color1), android.graphics.Color.blue(color1))
            val endA = (android.graphics.Color.alpha(color2) * alphaMult).toInt()
            val initColorEnd = android.graphics.Color.argb(endA, android.graphics.Color.red(color2), android.graphics.Color.green(color2), android.graphics.Color.blue(color2))
            
            val initRot = randomGenerator.nextFloat() * 360f
            val initFrameOffset = randomGenerator.nextFloat()
            
            val lifeRandMult = 1f - randomGenerator.nextFloat() * lifeRandomness
            val evalLifetime = particleLifetime * lifeRandMult
            
            val sizeRandMult = 1f - randomGenerator.nextFloat() * sizeRandomness
            val evalSizeMult = 5.5f * sizeRandMult

            pool.spawn(
                posX = pathPointOut.x,
                posY = pathPointOut.y,
                posZ = perspectiveDepth,
                velX = pathPointVecOut.x,
                velY = pathPointVecOut.y,
                lifetime = evalLifetime,
                sizeMult = evalSizeMult,
                gravityX = gravityX,
                gravityY = gravityY,
                rotation = initRot,
                colorArgb = initColor,
                frameOffset = initFrameOffset,
                angularVelocity = evalSpinSpeed,
                friction = evalFriction,
                colorEndArgb = initColorEnd
            )
        }

        render(renderData, animSprite, drawRect)
    }

    private fun render(renderData: RenderState, texture: AtlasTexture?, drawRect: android.graphics.RectF) {
        val dt = renderData.frameTimeSmooth
        val data = pool.data
        val capacity = pool.capacity
        val renderer = renderData.res.bufferRenderer
        
        val activeTex = texture ?: renderData.res.atlasTexWhite
        
        val evaluatedTrailTarget = particleTrail.getValueAsFloat(renderData.res.meter, 0f) * 10.0f
        smoothedTrail += (evaluatedTrailTarget - smoothedTrail) * dt * 10f
        val trailCount = max(1, smoothedTrail.toInt() + 1)
        val trailStretch = 1f + smoothedTrail * 0.5f
        val evalStartSize = kotlin.math.max(0.001f, startSize.getValueAsFloat(renderData.res.meter, 0f))
        val evalMidSize = kotlin.math.max(0.001f, midSize.getValueAsFloat(renderData.res.meter, 0f))
        val evalEndSize = kotlin.math.max(0.001f, endSize.getValueAsFloat(renderData.res.meter, 0f))

        val cx = drawRect.centerX()
        val cy = drawRect.centerY()
        val elementRot = measureDrawRot(renderData.res.meter)
        
        elementTransform.reset()
        elementTransform.rotate(elementRot * 360.0f, cx, cy)

        for (i in 0 until capacity) {
            val baseIndex = i * pool.PARTICLE_SIZE
            if (data[baseIndex + ParticlePool.ALIVE] == 0f) continue

            var currLife = data[baseIndex + ParticlePool.CURR_LIFETIME]
            val lifeTime = data[baseIndex + ParticlePool.LIFETIME]
            currLife += dt
            if (currLife >= lifeTime) {
                data[baseIndex + ParticlePool.ALIVE] = 0f
                continue
            }
            data[baseIndex + ParticlePool.CURR_LIFETIME] = currLife

            // Jelly bass: size and speed modulates smoothly based on bass
            val jellyScale = 1f + currentBass * bassReaction * 1.5f
            val speedPulse = 1f + currentBass * bassReaction * 5.0f

            ParticlePhysics.applyPhysics(data, baseIndex, dt, behaviorType, speedPulse, cx, cy)

            val life10 = currLife / lifeTime
            var posX = data[baseIndex + ParticlePool.POS_X]
            var posY = data[baseIndex + ParticlePool.POS_Y]
            val posZ = data[baseIndex + ParticlePool.POS_Z]
            val velX = data[baseIndex + ParticlePool.VEL_X]
            val velY = data[baseIndex + ParticlePool.VEL_Y]
            
            val currentScale = if (life10 < 0.5f) {
                evalStartSize + (evalMidSize - evalStartSize) * (life10 * 2f)
            } else {
                evalMidSize + (evalEndSize - evalMidSize) * ((life10 - 0.5f) * 2f)
            }
            
            var size = data[baseIndex + ParticlePool.SIZE_MULT] * currentScale * jellyScale
            
            // Pseudo 3D Perspective (Star Wars effect)
            if (perspectiveDepth > 0f) {
                val currentZ = posZ * (1f - life10)
                val focalLength = 1000f
                val scaleZ = focalLength / (focalLength + currentZ)
                posX = cx + (posX - cx) * scaleZ
                posY = cy + (posY - cy) * scaleZ
                size *= scaleZ
            }
            
            val rot = data[baseIndex + ParticlePool.ROTATION]
            
            val alphaFade = if (life10 < 0.1f) life10 / 0.1f else if (life10 > 0.8f) (1f - life10) / 0.2f else 1f
            
            val cStart = data[baseIndex + ParticlePool.COLOR_ARGB].toRawBits()
            val cEnd = data[baseIndex + ParticlePool.COLOR_END_ARGB].toRawBits()
            val startA = android.graphics.Color.alpha(cStart)
            val startR = android.graphics.Color.red(cStart)
            val startG = android.graphics.Color.green(cStart)
            val startB = android.graphics.Color.blue(cStart)
            val endA = android.graphics.Color.alpha(cEnd)
            val endR = android.graphics.Color.red(cEnd)
            val endG = android.graphics.Color.green(cEnd)
            val endB = android.graphics.Color.blue(cEnd)
            
            val interpA = (startA + (endA - startA) * life10).toInt()
            val interpR = (startR + (endR - startR) * life10).toInt()
            val interpG = (startG + (endG - startG) * life10).toInt()
            val interpB = (startB + (endB - startB) * life10).toInt()
            
            val baseAlpha = (interpA * alphaFade).toInt()
            val finalColorObj = android.graphics.Color.argb(baseAlpha, interpR, interpG, interpB)

            val effectiveVelX = velX * speedPulse
            val effectiveVelY = velY * speedPulse

            val actualCount = trailCount * 3
            val timeStep = (0.015f * trailStretch) / 3f
            
            val mxCount = if (mirrorX) 2 else 1
            val myCount = if (mirrorY) 2 else 1
            
            for (mXi in 0 until mxCount) {
                val fx = if (mXi == 1) -1f else 1f
                for (mYi in 0 until myCount) {
                    val fy = if (mYi == 1) -1f else 1f
                    
                    val mPosX = cx + (posX - cx) * fx
                    val mPosY = cy + (posY - cy) * fy
                    val mVelX = effectiveVelX * fx
                    val mVelY = effectiveVelY * fy
                    val mRot = if (fx * fy < 0) -rot else rot
                    
                    if (actualCount <= 1 || (mVelX == 0f && mVelY == 0f) || smoothedTrail < 0.1f) {
                        val colorFinal = android.graphics.Color.argb(baseAlpha, interpR, interpG, interpB)
                        
                        val cosR = kotlin.math.cos(mRot.toDouble()).toFloat()
                        val sinR = kotlin.math.sin(mRot.toDouble()).toFloat()
                        
                        val dirX0_x = -size * cosR - size * sinR
                        val dirX0_y = -size * sinR + size * cosR
                        val dirX1_x = size * cosR - size * sinR
                        val dirX1_y = size * sinR + size * cosR
                        
                        transformResult[0] = mPosX + dirX0_x
                        transformResult[1] = mPosY + dirX0_y
                        transformResult[2] = mPosX + dirX1_x
                        transformResult[3] = mPosY + dirX1_y
                        transformResult[4] = mPosX - dirX1_x
                        transformResult[5] = mPosY - dirX1_y
                        transformResult[6] = mPosX - dirX0_x
                        transformResult[7] = mPosY - dirX0_y
                        
                        elementTransform.mapPoints(transformResult)
                        renderer.drawRectangle(
                            renderData,
                            transformResult[0], transformResult[1], 
                            transformResult[2], transformResult[3], 
                            transformResult[4], transformResult[5], 
                            transformResult[6], transformResult[7],
                            0f, colorFinal, Vec2f.zero, Vec2f.one, activeTex
                        )
                    } else {
                        // Оптимизированный трейл из кружков
                        val maxIterations = 15
                        val iterations = kotlin.math.min(actualCount, maxIterations)
                        
                        // Вычисляем длину трейла и шаг между кружками
                        val totalTrailLen = (actualCount - 1).toFloat() * timeStep
                        val renderTimeStep = if (iterations > 1) totalTrailLen / (iterations - 1).toFloat() else 0f
                        
                        // ВЫНОСИМ ТЯЖЕЛЫЕ МАТЕМАТИЧЕСКИЕ ОПЕРАЦИИ И АЛЛОКАЦИИ ИЗ ЦИКЛА!
                        val cosR = kotlin.math.cos(mRot.toDouble()).toFloat()
                        val sinR = kotlin.math.sin(mRot.toDouble()).toFloat()
                        
                        val dirX0_x = -size * cosR - size * sinR
                        val dirX0_y = -size * sinR + size * cosR
                        val dirX1_x = size * cosR - size * sinR
                        val dirX1_y = size * sinR + size * cosR

                        for (t in 0 until iterations) {
                            val tScale = if (iterations <= 1) 0f else t.toFloat() / (iterations - 1).toFloat()
                            val tPosX = mPosX - mVelX * renderTimeStep * t.toFloat()
                            val tPosY = mPosY - mVelY * renderTimeStep * t.toFloat()
                            
                            val tAlpha = baseAlpha * (1f - tScale)
                            val colorFinal = android.graphics.Color.argb(tAlpha.toInt(), interpR, interpG, interpB)
                            
                            transformResult[0] = tPosX + dirX0_x
                            transformResult[1] = tPosY + dirX0_y
                            transformResult[2] = tPosX + dirX1_x
                            transformResult[3] = tPosY + dirX1_y
                            transformResult[4] = tPosX - dirX1_x
                            transformResult[5] = tPosY - dirX1_y
                            transformResult[6] = tPosX - dirX0_x
                            transformResult[7] = tPosY - dirX0_y
                            
                            elementTransform.mapPoints(transformResult)
                            renderer.drawRectangle(
                                renderData,
                                transformResult[0], transformResult[1], 
                                transformResult[2], transformResult[3], 
                                transformResult[4], transformResult[5], 
                                transformResult[6], transformResult[7],
                                0f, colorFinal, Vec2f.zero, Vec2f.one, activeTex
                            )
                        }
                    }
                }
            }
        }
    }

    fun setCustomImagePath(path: String) {
        var p = path
        if (p == null) p = ""
        if (this.customImagePath == p) return
        this.customImagePath = p
        if (!p.startsWith(PRECOMP_PREFIX)) {
            this.albumArtRequest = AlbumArtRequest(p, p, "", "")
        }
        this.markNeedReCreateGLResources()
    }

    override fun setUserObject1(obj1: Any?) {
        imageLoadStrongReference = obj1
    }

    override fun markNeedReCreateGLResources() {
        bitmap = null
        bitmapLoading = false
        bitmapLoadedIn = false
        super.markNeedReCreateGLResources()
    }

    override fun reCreateGLResources(renderData: RenderState) {
        this.markNeedReCreateGLResources()
        super.reCreateGLResources(renderData)
    }

    override fun onCreateGLResources(renderData: RenderState) {
        super.onCreateGLResources(renderData)

        if (customImagePath.startsWith(PRECOMP_PREFIX)) {
            tex2 = null
            atlasTex2 = null
            bitmap = null
        } else if (customImagePath.isNotEmpty()) {
            if (!bitmapLoading) {
                bitmapLoading = true
                val targetBoundsWidth = renderData.fullscreenWidth
                val targetBoundsHeight = renderData.fullscreenHeight

                renderData.res.visualizationData.onRequestAlbumArtPathAndBitmap(
                    this,
                    targetBoundsWidth,
                    targetBoundsHeight,
                    albumArtRequest.makeCopy()
                )
            }

            if (!bitmapLoadedIn) {
                bitmapLoadedIn = true
                onAlbumArtCreateGLResources(bitmap)
            }
        } else {
            tex2 = null
            atlasTex2 = null
        }
        bitmap = null
    }

    protected fun onAlbumArtCreateGLResources(bitmap: Bitmap?) {
        tex2 = null
        atlasTex2 = null
        if (bitmap == null) return

        tex2 = VTexture(
            bitmap,
            VTexture.DEFAULT_FILTER,
            VTexture.DEFAULT_FILTER,
            VTexture.DEFAULT_WRAP,
            false
        )
        atlasTex2 = AtlasTexture(tex2)
    }

    override fun onBitmapLoaded(bitmap: Bitmap?, url00: String?, url0: String?, url1: String?) {
        if (com.aylis.Common.Utils.compareNullStrings(url00, albumArtRequest.videoThumbDataSource)) {
            if (com.aylis.Common.Utils.compareNullStrings(url0, albumArtRequest.path0)) {
                if (com.aylis.Common.Utils.compareNullStrings(url1, albumArtRequest.path1)) {
                    this.bitmap = bitmap
                    bitmapLoadedIn = false
                    super.markNeedReCreateGLResources()
                }
            }
        }
    }
}
