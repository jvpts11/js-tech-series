#version 150

// A picture with its colour taken out by Amount: 0 leaves it as it is, 1 leaves only how bright each pixel reads
// (the same weights as the tube's phosphor).

uniform sampler2D Sampler0;
uniform float Amount;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec3 colour = texture(Sampler0, texCoord0).rgb;
    float lit = dot(colour, vec3(0.2126, 0.7152, 0.0722));
    fragColor = vec4(mix(colour, vec3(lit), clamp(Amount, 0.0, 1.0)), 1.0);
}
