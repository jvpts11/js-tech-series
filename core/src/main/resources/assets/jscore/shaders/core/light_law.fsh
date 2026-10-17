#version 150

// The world as drawn, under laws of light of its own: bent round a mass (Lens: x, y on the screen from 0 to 1, its
// radius as a share of the screen's height, its strength), its middle a black horizon; shimmering as in a mirage;
// its colours shifted towards red (Shift above 0) or blue (below), as by the Doppler effect, and seen through Tint;
// darkened or lit with nothing to make it so. Extent is how much of the copied picture the screen fills.

uniform sampler2D Sampler0;
uniform float Time;
uniform float Aspect;
uniform vec2 Extent;
uniform vec3 Tint;
uniform float Shift;
uniform vec4 Lens;
uniform float Mirage;
uniform float Dark;
uniform float Glow;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec2 uv = texCoord0 / Extent;
    vec2 bend = vec2(0.0);
    float horizon = 1.0;
    if (Lens.w > 0.0) {
        vec2 apart = (uv - Lens.xy) * vec2(Aspect, 1.0);
        float away = max(length(apart), 0.0001);
        // Light passing near the mass bends round it: what lies behind is seen pushed out, a ring at its radius.
        float pull = Lens.w * Lens.z * Lens.z / away;
        bend -= apart / away / vec2(Aspect, 1.0) * min(pull, away * 0.9);
        horizon = smoothstep(Lens.z * 0.3, Lens.z * 0.42, away);
    }
    if (Mirage > 0.0) {
        bend += vec2(sin(uv.y * 160.0 + Time * 3.0) * 0.004, sin(uv.x * 40.0 + Time * 2.0) * 0.006 * (1.0 - uv.y))
                * Mirage;
    }
    vec3 colour = texture(Sampler0, clamp(uv + bend, 0.0, 1.0) * Extent).rgb * horizon;
    if (Shift > 0.0) {
        colour = vec3(colour.r + (colour.g * 0.5 + colour.b * 0.3) * Shift,
                      colour.g * (1.0 - 0.4 * Shift) + colour.b * 0.3 * Shift,
                      colour.b * (1.0 - 0.8 * Shift));
    } else if (Shift < 0.0) {
        float s = -Shift;
        colour = vec3(colour.r * (1.0 - 0.8 * s),
                      colour.g * (1.0 - 0.4 * s) + colour.r * 0.3 * s,
                      colour.b + (colour.g * 0.5 + colour.r * 0.3) * s);
    }
    colour *= Tint;
    colour *= 1.0 - Dark;
    colour += (vec3(1.0) - colour) * Glow * 0.6;
    fragColor = vec4(clamp(colour, 0.0, 1.0), 1.0);
}
