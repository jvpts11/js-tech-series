#version 150

// The world as drawn, bent: the air shimmering above up to four hot things (each x, y on the screen from 0 to 1,
// its reach as a share of the screen's height, its strength), and the ring of a shockwave going out (x, y, its
// radius, its strength). Extent is how much of the copied picture the screen fills.

uniform sampler2D Sampler0;
uniform float Time;
uniform float Aspect;
uniform vec2 Extent;
uniform vec4 Haze0;
uniform vec4 Haze1;
uniform vec4 Haze2;
uniform vec4 Haze3;
uniform vec4 Shock;

in vec2 texCoord0;

out vec4 fragColor;

vec2 haze(vec4 hot, vec2 uv) {
    if (hot.w <= 0.0) {
        return vec2(0.0);
    }
    vec2 apart = (uv - hot.xy) * vec2(Aspect, 1.0);
    // Hot air rises: the shimmer is over the hot thing, little below it.
    float above = smoothstep(-0.3 * hot.z, 0.5 * hot.z, uv.y - hot.y);
    float near = 1.0 - smoothstep(0.0, hot.z, length(apart));
    float strength = hot.w * above * near;
    return vec2(sin(uv.y * 95.0 + Time * 7.0), cos(uv.x * 70.0 + Time * 5.3)) * 0.0035 * strength;
}

void main() {
    vec2 uv = texCoord0 / Extent;
    vec2 bend = haze(Haze0, uv) + haze(Haze1, uv) + haze(Haze2, uv) + haze(Haze3, uv);
    if (Shock.w > 0.0) {
        vec2 apart = (uv - Shock.xy) * vec2(Aspect, 1.0);
        float away = length(apart);
        float ring = exp(-pow((away - Shock.z) / 0.035, 2.0));
        bend += apart / max(away, 0.0001) / vec2(Aspect, 1.0) * 0.03 * Shock.w * ring;
    }
    fragColor = vec4(texture(Sampler0, clamp(uv + bend, 0.0, 1.0) * Extent).rgb, 1.0);
}
