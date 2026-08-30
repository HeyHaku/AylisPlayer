package com.aylis.comp.visual.core.Graphic

import android.graphics.Bitmap
import android.opengl.GLES20
import com.aylis.Common.tlog
import com.aylis.comp.visual.core.gl.mdesl.graphics.Texture
import com.aylis.comp.visual.core.gl.mdesl.graphics.glutils.FrameBuffer

/**
 * Утилитный класс для безопасной генерации мипмапов в OpenGL ES 2.0/3.0.
 *
 * Решает проблемы:
 * - glGenerateMipmap на неинициализированных текстурах
 * - glGenerateMipmap на null/recycled Bitmap
 * - glGenerateMipmap на NPOT-текстурах без CLAMP_TO_EDGE
 * - glGenerateMipmap на FBO-текстурах до первого рендер-прохода
 *
 * Все методы предназначены для вызова из GL-потока.
 */
object SafeMipmapHelper {

    private const val TAG = "SafeMipmapHelper"

    // ==================== Валидация источников ====================

    /**
     * Проверяет валидность Bitmap для генерации мипмапов.
     * @return true если bitmap != null, не recycled, и имеет размеры > 0
     */
    @JvmStatic
    fun isBitmapValid(bitmap: Bitmap?): Boolean {
        if (bitmap == null) return false
        if (bitmap.isRecycled) return false
        if (bitmap.width <= 0 || bitmap.height <= 0) return false
        return true
    }

    /**
     * Проверяет готовность текстуры FBO (пре-композиции) для генерации мипмапов.
     * FBO считается готовым если:
     * - FrameBuffer не null и имеет валидный ID
     * - Внутренняя текстура не null, валидна, и имеет размеры > 0
     *
     * @return true если FBO полностью инициализирован и готов к использованию
     */
    @JvmStatic
    fun isFboTextureReady(fb: FrameBuffer?): Boolean {
        if (fb == null) return false
        if (fb.id <= 0) return false

        val tex = fb.texture ?: return false
        if (!tex.valid()) return false
        if (tex.width <= 0 || tex.height <= 0) return false

        return true
    }

    /**
     * Проверяет, готова ли текстура (из любого источника) для использования.
     * @return true если текстура не null, валидна и имеет размеры > 0
     */
    @JvmStatic
    fun isTextureReady(texture: Texture?): Boolean {
        if (texture == null) return false
        if (!texture.valid()) return false
        if (texture.width <= 0 || texture.height <= 0) return false
        return true
    }

    // ==================== NPOT-безопасность ====================

    /**
     * Принудительно выставляет CLAMP_TO_EDGE для текущей забинденной текстуры.
     * Необходимо для NPOT (Non-Power-of-Two) текстур, т.к. многие GPU
     * не поддерживают GL_REPEAT с мипмапами для NPOT-текстур.
     */
    @JvmStatic
    fun applyNpotSafeWrap() {
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    // ==================== Безопасная генерация мипмапов ====================

    /**
     * Безопасная обёртка над glGenerateMipmap.
     *
     * 1. Очищает предыдущие GL-ошибки
     * 2. Вызывает glGenerateMipmap
     * 3. Проверяет GL-ошибку после вызова
     * 4. При ошибке — логирует и откатывает фильтрацию на GL_LINEAR
     *
     * @param target GL-таргет текстуры (обычно GL_TEXTURE_2D)
     * @param callerTag имя вызывающего класса для логирования
     * @return true если мипмапы сгенерированы успешно, false при ошибке
     */
    @JvmStatic
    fun generateMipmapSafe(target: Int, callerTag: String): Boolean {
        // Очистка предыдущих ошибок GL (с ограничением итераций для защиты от багов драйвера)
        var clearAttempts = 0
        while (GLES20.glGetError() != GLES20.GL_NO_ERROR && clearAttempts < 10) {
            clearAttempts++
        }

        GLES20.glGenerateMipmap(target)

        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            tlog.w("$TAG [$callerTag]: glGenerateMipmap failed with GL error 0x${
                Integer.toHexString(error)
            }. Falling back to GL_LINEAR filter.")

            // Фолбек: откатываем фильтрацию на базовую GL_LINEAR
            fallbackToLinearFilter(target)
            return false
        }

        return true
    }

    /**
     * Полный безопасный пайплайн генерации мипмапов для Bitmap-текстуры.
     *
     * Выполняет все проверки, применяет NPOT-безопасную обёртку,
     * генерирует мипмапы, проверяет ошибки GL.
     *
     * @param bitmap исходный Bitmap (может быть null)
     * @param target GL-таргет текстуры
     * @param callerTag имя вызывающего класса для логирования
     * @return true если мипмапы сгенерированы успешно
     */
    @JvmStatic
    fun generateMipmapForBitmap(bitmap: Bitmap?, target: Int, callerTag: String): Boolean {
        if (!isBitmapValid(bitmap)) {
            tlog.w("$TAG [$callerTag]: Skipping mipmap generation — bitmap is invalid " +
                    "(null=${bitmap == null}, recycled=${bitmap?.isRecycled}, " +
                    "size=${bitmap?.width}x${bitmap?.height})")
            fallbackToLinearFilter(target)
            return false
        }

        // NPOT-безопасность: принудительный CLAMP_TO_EDGE
        applyNpotSafeWrap()

        return generateMipmapSafe(target, callerTag)
    }

    /**
     * Полный безопасный пайплайн генерации мипмапов для FBO-текстуры (пре-композиции).
     *
     * @param fb FrameBuffer (может быть null)
     * @param target GL-таргет текстуры
     * @param callerTag имя вызывающего класса для логирования
     * @return true если мипмапы сгенерированы успешно
     */
    @JvmStatic
    fun generateMipmapForFbo(fb: FrameBuffer?, target: Int, callerTag: String): Boolean {
        if (!isFboTextureReady(fb)) {
            tlog.w("$TAG [$callerTag]: Skipping mipmap generation — FBO texture not ready " +
                    "(fb=${fb != null}, id=${fb?.id ?: 0})")
            fallbackToLinearFilter(target)
            return false
        }

        applyNpotSafeWrap()

        return generateMipmapSafe(target, callerTag)
    }

    // ==================== Фолбеки ====================

    /**
     * Откат фильтрации на GL_LINEAR (без мипмапов).
     * Безопасная базовая фильтрация, работающая на всех GPU.
     */
    @JvmStatic
    fun fallbackToLinearFilter(target: Int) {
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(target, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    /**
     * Биндит текстуру-заглушку (чёрный 2×2 пиксель) для предотвращения
     * ошибок шейдера при пустом текстурном юните.
     *
     * @param renderData текущий RenderState для доступа к системным текстурам
     * @return забинденная текстура-заглушка (AtlasTexture), или null если RenderState недоступен
     */
    @JvmStatic
    fun bindFallbackTexture(renderData: RenderState): AtlasTexture {
        val fallback = renderData.res.atlasTexBlack
        val tex = fallback?.texture2D?.texture
        tex?.bind()
        return fallback
    }
}
