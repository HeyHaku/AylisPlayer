package com.aylis.comp.visual.core.Elements;

public class LyricsLayout {
    
    public static class LayoutResult {
        public float offsetX;
        public float offsetY;
        public float fade;
        public float scale;
        public float rotation;
        
        public LayoutResult(float offsetX, float offsetY, float fade, float scale, float rotation) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.fade = fade;
            this.scale = scale;
            this.rotation = rotation;
        }
    }
    
    public static LayoutResult calculate(
            String mode, 
            int i, 
            float indexRelative, 
            float lineHeight, 
            float fadeTopDist, 
            float fadeBottomDist, 
            float activeScale, 
            float inactiveScale) {
            
        float offsetX = 0;
        float offsetY = 0;
        
        if ("Horizontal_Left".equals(mode)) {
            offsetX = indexRelative * (lineHeight * 5f);
        } else if ("Horizontal_Right".equals(mode)) {
            offsetX = -indexRelative * (lineHeight * 5f);
        } else if ("Scatter".equals(mode)) {
            float angle;
            if (indexRelative > 0) {
                angle = (i * 2.39996f) % ((float)Math.PI * 2f); 
            } else {
                angle = ((i + 1337) * 2.39996f) % ((float)Math.PI * 2f);
            }
            
            float radius = Math.abs(indexRelative) * (lineHeight * 4.0f);
            offsetX = (float)Math.sin(angle) * radius;
            offsetY = (float)Math.cos(angle) * radius;
        } else if ("Carousel".equals(mode)) {
            float angle = indexRelative * (float)(Math.PI / 5.0); // 36 degrees per line
            
            float radiusX = lineHeight * 8.0f;
            offsetX = (float)Math.sin(angle) * radiusX;
            
            float radiusY = lineHeight * 2.5f;
            offsetY = ((float)Math.cos(angle) - 1.0f) * -radiusY;
            
            float z = (float)Math.cos(angle);
            
            float maxDist = (indexRelative < 0) ? fadeTopDist : fadeBottomDist;
            float absDist = Math.abs(indexRelative);
            
            if (absDist > maxDist + 1.0f) {
                return new LayoutResult(offsetX, offsetY, 0f, inactiveScale, 0f);
            }
            
            float fade = 1.0f;
            if (absDist > maxDist) {
                fade = 0f;
            } else if (maxDist > 0f) {
                float t = absDist / maxDist;
                if (t > 1.0f) t = 1.0f;
                fade = 1.0f - t;
            }
            
            float depthScale = (z + 1.0f) / 2.0f; // 1.0 (front) to 0.0 (back)
            if (depthScale < 0.1f) depthScale = 0.1f;
            
            float targetScale = inactiveScale * depthScale;
            float scaleT = absDist;
            if (scaleT > 1.0f) scaleT = 1.0f;
            float currentScale = activeScale + (targetScale - activeScale) * scaleT;
            
            return new LayoutResult(offsetX, offsetY, fade * depthScale, currentScale, 0f);
        } else if ("Circle".equals(mode)) {
            // Circle radius
            float radius = lineHeight * 7.0f;
            
            // Step angle (so lines are evenly spaced on a static circle)
            float stepAngle = (float)(Math.PI / 6.0);
            
            // Angle based on absolute index (so the circle DOES NOT SPIN)
            float angle = i * stepAngle;
            
            // Fixed orbit position
            float circleX = (float)Math.sin(angle) * radius;
            float circleY = -(float)Math.cos(angle) * radius;
            
            // indexRelative determines position on the flight path:
            // > 1.0 : waiting at the static circle position
            // 0.0   : active in the center
            // < 0.0 : flying away past the center to the opposite side
            float flightProgress = indexRelative;
            if (flightProgress > 1.0f) flightProgress = 1.0f;
            
            offsetX = circleX * flightProgress;
            offsetY = circleY * flightProgress;
            
            float absDist = Math.abs(indexRelative);
            float maxDist = (indexRelative < 0) ? fadeTopDist : fadeBottomDist;
            
            if (absDist > maxDist + 1.0f) {
                return new LayoutResult(offsetX, offsetY, 0f, inactiveScale, 0f);
            }
            
            float fade = 1.0f;
            if (absDist > maxDist) {
                fade = 0f;
            } else if (maxDist > 0f) {
                float tFade = absDist / maxDist;
                if (tFade > 1.0f) tFade = 1.0f;
                fade = 1.0f - tFade;
            }
            
            // Scale logic (normal size in center, smaller on circle)
            float scaleT = absDist;
            if (scaleT > 1.0f) scaleT = 1.0f;
            float currentScale = activeScale + (inactiveScale - activeScale) * scaleT;
            
            // 0f rotation so text is always horizontal and readable
            return new LayoutResult(offsetX, offsetY, fade, currentScale, 0f);
        } else { // "Vertical"
            offsetY = indexRelative * lineHeight;
        }
        
        float maxDist = (indexRelative < 0) ? fadeTopDist : fadeBottomDist;
        float absDist = Math.abs(indexRelative);
        
        if (absDist > maxDist + 1.0f) {
            return new LayoutResult(offsetX, offsetY, 0f, inactiveScale, 0f);
        }
        
        float fade = 1.0f;
        if (absDist > maxDist) {
            fade = 0f;
        } else if (maxDist > 0f) {
            float t = absDist / maxDist;
            if (t > 1.0f) t = 1.0f;
            fade = 1.0f - t;
        }
        
        float scaleT = absDist;
        if (scaleT > 1.0f) scaleT = 1.0f;
        float currentScale = activeScale + (inactiveScale - activeScale) * scaleT;
        
        return new LayoutResult(offsetX, offsetY, fade, currentScale, 0f);
    }
}
