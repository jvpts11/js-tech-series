#version 150

#moj_import <fog.glsl>

// As the game draws a mob, solid with holes; or, for the glowing texture laid over a model, faded into the fog
// rather than coloured by it, as the game draws an emissive one.

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float KitGlow;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    if (KitGlow > 0.5) {
        fragColor = color * linear_fog_fade(vertexDistance, FogStart, FogEnd);
    } else {
        color *= lightMapColor;
        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    }
}
