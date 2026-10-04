#version 150

// A picture as a monitor's tube shows it. Mode 0 copies it as it is, mode 1 lights the phosphor by how bright each
// pixel reads (the weights of Tube.apply on the Java side), mode 2 takes the nearest of the sixteen colours.

uniform sampler2D Sampler0;
uniform vec3 Phosphor;
uniform int Mode;

in vec2 texCoord0;

out vec4 fragColor;

const vec3 SIXTEEN[16] = vec3[16](
    vec3(0.0, 0.0, 0.0), vec3(0.0, 0.0, 0.667), vec3(0.0, 0.667, 0.0), vec3(0.0, 0.667, 0.667),
    vec3(0.667, 0.0, 0.0), vec3(0.667, 0.0, 0.667), vec3(0.667, 0.333, 0.0), vec3(0.667, 0.667, 0.667),
    vec3(0.333, 0.333, 0.333), vec3(0.333, 0.333, 1.0), vec3(0.333, 1.0, 0.333), vec3(0.333, 1.0, 1.0),
    vec3(1.0, 0.333, 0.333), vec3(1.0, 0.333, 1.0), vec3(1.0, 1.0, 0.333), vec3(1.0, 1.0, 1.0)
);

void main() {
    vec3 colour = texture(Sampler0, texCoord0).rgb;
    if (Mode == 1) {
        float lit = dot(colour, vec3(0.2126, 0.7152, 0.0722));
        colour = Phosphor * lit;
    } else if (Mode == 2) {
        vec3 best = SIXTEEN[0];
        float nearest = 10.0;
        for (int i = 0; i < 16; i++) {
            vec3 d = colour - SIXTEEN[i];
            float distance = dot(d, d);
            if (distance < nearest) {
                nearest = distance;
                best = SIXTEEN[i];
            }
        }
        colour = best;
    }
    fragColor = vec4(colour, 1.0);
}
