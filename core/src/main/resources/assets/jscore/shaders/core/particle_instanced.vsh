#version 150

#moj_import <fog.glsl>

// The Core's particles drawn many at once: one square, its corners from -1 to 1, drawn once for every particle and
// turned to face the camera on its plane, KitLeft and KitUp. Each particle's numbers are five texels of Instances:
// where it is from the camera and half its size; its picture's corners on the sheet; its colour; its angle with its
// light; and how far it is drawn out either way along the way it goes, for a spark's streak, none for a square.

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
    int base = gl_InstanceID * 5;
    vec4 place = texelFetch(Instances, base);
    vec4 picture = texelFetch(Instances, base + 1);
    vec4 colour = texelFetch(Instances, base + 2);
    vec4 turn = texelFetch(Instances, base + 3);
    vec3 stretch = texelFetch(Instances, base + 4).xyz;
    vec3 a;
    vec3 b;
    float drawn = length(stretch);
    vec3 side = drawn > 0.0001 ? cross(stretch / drawn, normalize(place.xyz)) : vec3(0.0);
    if (length(side) > 0.0001) {
        // Drawn out along the way it goes, turned about that line to face the camera.
        vec3 along = stretch / drawn;
        a = normalize(side) * place.w;
        b = along * (place.w + drawn);
    } else {
        float across = cos(turn.x) * place.w;
        float up = sin(turn.x) * place.w;
        // The square's two half-diagonals, turned by the particle's angle.
        a = KitLeft * across + KitUp * up;
        b = KitUp * across - KitLeft * up;
    }
    vec3 position = place.xyz + Corner.x * a + Corner.y * b;
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);

    vertexDistance = fog_distance(position, FogShape);
    texCoord0 = vec2(Corner.x < 0.0 ? picture.z : picture.x, Corner.y < 0.0 ? picture.w : picture.y);
    vertexColor = colour * texelFetch(Sampler2, ivec2(turn.yz) / 16, 0);
}
