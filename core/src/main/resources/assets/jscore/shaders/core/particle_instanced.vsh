#version 150

#moj_import <fog.glsl>

// The Core's particles drawn many at once: one square, its corners from -1 to 1, drawn once for every particle and
// turned to face the camera on its plane, KitLeft and KitUp. Each particle's numbers are four texels of Instances:
// where it is from the camera and half its size; its picture's corners on the sheet; its colour; and its angle with
// its light.

in vec2 Corner;

uniform samplerBuffer Instances;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform vec3 KitLeft;
uniform vec3 KitUp;

out float vertexDistance;
out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    int base = gl_InstanceID * 4;
    vec4 place = texelFetch(Instances, base);
    vec4 picture = texelFetch(Instances, base + 1);
    vec4 colour = texelFetch(Instances, base + 2);
    vec4 turn = texelFetch(Instances, base + 3);
    float across = cos(turn.x) * place.w;
    float up = sin(turn.x) * place.w;
    // The square's two half-diagonals, turned by the particle's angle.
    vec3 a = KitLeft * across + KitUp * up;
    vec3 b = KitUp * across - KitLeft * up;
    vec3 position = place.xyz + Corner.x * a + Corner.y * b;
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);

    vertexDistance = fog_distance(position, FogShape);
    texCoord0 = vec2(Corner.x < 0.0 ? picture.z : picture.x, Corner.y < 0.0 ? picture.w : picture.y);
    vertexColor = colour * texelFetch(Sampler2, ivec2(turn.yz) / 16, 0);
}
