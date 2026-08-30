#version 300 es
precision highp float;
precision mediump int;

in vec4 vColor;
in vec2 vTexCoord;

uniform vec2 iResolution;
uniform float iTime;
uniform vec4 iMouse;
uniform float u_bass;
uniform float u_hue;
uniform float u_saturation;
uniform float u_brightness;

out vec4 fragColor;

float hash1(vec2 v) {
  vec3 v3 = vec3(v.x, v.y, v.x);
  v3 = fract(v3 * 0.1031);
  v3 += dot(v3, v3.yzx + 33.33);
  return fract((v3.x + v3.y) * v3.z);
}

vec2 hash2(vec2 v) {
  vec3 v3 = vec3(v.x, v.y, v.x);
  v3 = v3 * vec3(0.1031, 0.103, 0.0973);
  v3 += dot(v3, v3.yzx + 33.33);
  return fract((v3.xx + v3.yz) * v3.zy);
}

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

float noisemix2(float a, float b, float c, float d, vec2 f) {
  vec2 u = f * f * (3.0 - 2.0 * f);
  return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float noise_value(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  vec2 I = floor(i + 1.0);
  float a = hash1(i);
  float b = hash1(vec2(I.x, i.y));
  float c = hash1(vec2(i.x, I.y));
  float d = hash1(I);
  return noisemix2(a, b, c, d, f);
}

float noise_gradient(vec2 p) {
  vec2 i = floor(p);
  vec2 f = fract(p);
  vec2 I = floor(i + 1.0);
  vec2 F = f - 1.0;
  float a = dot(-0.5 + hash2(i), f);
  float b = dot(-0.5 + hash2(vec2(I.x, i.y)), vec2(F.x, f.y));
  float c = dot(-0.5 + hash2(vec2(i.x, I.y)), vec2(f.x, F.y));
  float d = dot(-0.5 + hash2(I), F);
  return 0.5 + noisemix2(a, b, c, d, f);
}

vec3 tonemap_aces(vec3 col) {
  return clamp((col * (2.51 * col + 0.03)) / (col * (2.43 * col + 0.59) + 0.14), 0.0, 1.0);
}

vec3 saturate(vec3 col, float sat) {
  float grey = dot(col, vec3(0.2125, 0.7154, 0.0721));
  return grey + sat * (col - grey);
}

vec3 colorgrade_tone(vec3 col, vec3 gain, vec3 lift, vec3 invgamma) {
  col = pow(col, invgamma);
  return (gain - lift) * col + lift;
}

vec3 gamma_correction(vec3 col) {
  return 1.12661 * sqrt(col) - 0.12661 * col;
}

vec3 vignette(vec3 col, vec2 coord, float strength, float amount) {
  return col * ((1.0 - amount) + amount * pow(16.0 * coord.x * coord.y * (1.0 - coord.x) * (1.0 - coord.y), strength));
}

vec3 camera_perspective(vec3 lookfrom, vec3 lookat, float tilt, float vfov, vec2 uv) {
  vec2 sc = vec2(sin(tilt), cos(tilt));
  vec3 vup = vec3(sc.x, sc.y, 0.0);
  vec3 w = normalize(lookat - lookfrom);
  vec3 u = normalize(cross(w, vup));
  vec3 v = cross(u, w);
  float wf = 1.0 / tan(vfov * 0.00872664626);
  return normalize(uv.x * u + uv.y * v + wf * w);
}

float fbm_terrain(vec2 p) {
  float a = 1.0;
  float t = 0.0;
  t += a * noise_value(p);
  a *= 0.5; p *= 2.0;
  t += a * noise_value(p);
  a *= 0.5; p *= 2.0;
  t += a * noise_value(p);
  a *= 0.5; p *= 2.0;
  t += a * noise_value(p);
  a *= 0.5; p *= 2.0;
  t += a * noise_value(p);
  return t;
}

float map(vec3 p) {
  vec2 q = p.xz;
  float h = fbm_terrain(q) * 0.5 * (1.0 + u_bass * 0.2);
  float d = p.y + h * 0.75 + 0.0;
  return d * 0.5;
}

float ray_march(vec3 ro, vec3 rd) {
  float t = 0.0;
  for(int i = 1; i <= 256; i++) {
    vec3 p = ro + t * rd;
    float d = map(p);
    if(d < 0.003 * t || t >= 25.0) {
      break;
    }
    t += d;
  }
  return t;
}

void main() {
  vec2 frag_coord = vTexCoord * iResolution; 
  vec2 res = iResolution;
  vec2 mouse = iMouse.xy / res;
  vec2 uv = vTexCoord; 
  vec2 coord = 2.0 * (frag_coord - res * 0.5) / res.y;
  coord.y = -coord.y;
  
  float z = iTime * 1.0;
  vec2 sc = vec2(sin(iTime * 0.5), cos(iTime * 0.5));
  float y = 0.0;
  
  vec3 lookat = vec3(sc.x * 0.5, y, z);
  vec3 ro = vec3(-sc.x * 0.5, y, z - 2.0);
  vec3 rd = camera_perspective(ro, lookat, 0.0, 45.0, coord);
  
  vec3 col = vec3(0.0);
  vec3 sun_dir = normalize(vec3(0.3, 0.07, 1.0));
  
  vec3 hor_col = vec3(0.7 + u_bass * 0.3, 0.05, 0.01);
  vec3 sun_col = vec3(0.9, 0.8, 0.7);
  vec3 bou_col = vec3(0.8, 0.3, 0.1);
  
  float t = ray_march(ro, rd);
  vec3 back_col;
  
  back_col = mix(hor_col, hor_col * 0.3, smoothstep(0.0, 0.25, rd.y));
  back_col = mix(back_col, bou_col, max(0.1 - rd.y, 0.0));
  float sun_lightness = max(dot(rd, sun_dir), 0.0);
  back_col += sun_col * pow(sun_lightness, 2000.0);
  back_col += 0.3 * sun_col * pow(sun_lightness, 100.0);
  back_col += vec3(0.3, 0.2, 0.1) * pow(sun_lightness, 4.0);
  
  if(abs(coord.y) > 0.75) {
    col = vec3(0.0);
  } else if(t < 25.0) {
    float decay = 1.0 - exp(-0.12 * t);
    col = mix(col, back_col, decay);
  } else {
    col = back_col;
    float clouds_altitude = 1000.0;
    float clouds_dist = (1.0 - ro.y / clouds_altitude) / rd.y;
    if(clouds_dist > 0.0) {
      float clouds_zoom = 1.0;
      vec2 clouds_pos = ro.xz + rd.xz * clouds_dist * clouds_zoom;
      float clouds_lightness = max(noise_gradient(clouds_pos) - 0.3, 0.0);
      float clouds_decay = smoothstep(0.0, 0.3, rd.y);
      vec3 clouds_col = 2.0 * col;
      col = mix(col, clouds_col, clouds_lightness * clouds_decay);
    }
    col = clamp(col, 0.0, 1.0);
  }
  
  col = tonemap_aces(col);
  col = gamma_correction(col);
  col = colorgrade_tone(col, vec3(1.3, 0.9, 0.7), vec3(0.5, 0.1, 0.1) * 0.1, vec3(3.0, 2.0, 1.2));
  
  if (u_hue != 0.0) {
      vec3 hsv = rgb2hsv(col);
      hsv.x = fract(hsv.x + u_hue);
      col = hsv2rgb(hsv);
  }
  
  col = saturate(col, u_saturation);
  col *= u_brightness;
  
  col = vignette(col, uv, 0.25, 0.7);
  col = clamp(col + hash1(frag_coord) * 0.01, 0.0, 1.0); 
  
  fragColor = vec4(col, 1.0);
}
