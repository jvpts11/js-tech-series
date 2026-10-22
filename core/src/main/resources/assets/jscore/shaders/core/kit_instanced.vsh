#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

// The Core's models drawn many at once: the faces of one model, each with the bone it belongs to, drawn once for
// every thing drawn with it. Each thing's numbers are in Instances, KitStride texels apiece: first its light and its
// overlay, then the rows of each bone's place, three texels a bone. A hidden bone's place is all zero.

in vec3 Position;
in vec2 UV0;
in vec3 Normal;
in vec2 Part;

uniform samplerBuffer Instances;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
uniform int KitStride;
uniform vec2 KitFrame;
uniform float KitGlow;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    int base = gl_InstanceID * KitStride;
    vec4 lit = texelFetch(Instances, base);
    int bone = base + 1 + int(Part.x + 0.5) * 3;
    vec4 row0 = texelFetch(Instances, bone);
    vec4 row1 = texelFetch(Instances, bone + 1);
    vec4 row2 = texelFetch(Instances, bone + 2);
    vec4 local = vec4(Position, 1.0);
    vec3 placed = vec3(dot(row0, local), dot(row1, local), dot(row2, local));
    // The rows' turn, transposed; its inverse turns the normals as the faces are turned, stretched or not.
    mat3 turned = mat3(row0.xyz, row1.xyz, row2.xyz);
    vec3 normal = determinant(turned) == 0.0 ? Normal : normalize(inverse(turned) * Normal);
    gl_Position = ProjMat * ModelViewMat * vec4(placed, 1.0);

    vertexDistance = fog_distance(placed, FogShape);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, normal, vec4(1.0));
    // A glowing part is at full light; the glowing texture laid over a model takes no light at all.
    if (KitGlow > 0.5) {
        lightMapColor = vec4(1.0);
    } else if (Part.y > 0.5) {
        lightMapColor = texelFetch(Sampler2, ivec2(15, 15), 0);
    } else {
        lightMapColor = texelFetch(Sampler2, ivec2(lit.xy) / 16, 0);
    }
    overlayColor = texelFetch(Sampler1, ivec2(lit.zw), 0);
    texCoord0 = vec2(UV0.x, UV0.y * KitFrame.x + KitFrame.y);
}
