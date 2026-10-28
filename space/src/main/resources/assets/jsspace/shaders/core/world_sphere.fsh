#version 150

// A world drawn as a real round body: each pixel's ray meets its sphere at the screen's own resolution, so its outline
// is smooth, while its surface keeps the texels of its maps, read from the copy whose texels are no smaller than a
// pixel and never blended. It is lit smoothly from its sun, as the game's smooth lighting shades blocks: the slopes of
// its height map lean the light, a slope turned from the sun falls to the world's shadow tone, and the terminator is a
// soft band into the tone of its night, warmed by dusk where there is air. Air hazes the limb and glows past it on the
// lit side. The Overworld's clouds are read from their own cover: white tops, a greyer edge on the side away from the
// sun, and their shadows on the ground a little beside them. The cover is laid once, in bands that each slide west at
// their own pace, where the weather's fronts, sliding east, let cloud be, so they move as smoothly as the world turns.

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
// The clouds' bands as they stand at the start, each texel's red the pace of its row in 64ths of a turn a turn; and
// where the weather lets cloud be, at the start. CloudTurn is how many turns the clouds have gone, modulo 64.
uniform sampler2D Sampler2;
uniform sampler2D Sampler4;
uniform float CloudTurn;

uniform vec3 Centre;
uniform float Radius;
uniform vec3 North;
uniform vec3 Middle;
uniform vec3 SunDir;
uniform float Relief;
uniform float Shadow;
uniform vec3 Night;
uniform vec3 Dusk;
uniform int HasDusk;
uniform vec3 Air;
uniform float AirThickness;
uniform int HasClouds;
// The fewest pixels a texel is drawn across: a world far off in a sky keeps texels as wide as a vanilla moon's.
uniform float TexelPixels;
// How much of the world is drawn, as it fades in over what is behind.
uniform float Fade;
// On the way up from it, the veil of haze its real ground melts under: how thick, and its colour.
uniform float Veil;
uniform vec3 VeilColour;

in vec3 worldPos;

out vec4 fragColor;

const float PI = 3.14159265358979;
// How far a cloud's shadow and its grey edge are read from it, in turns round the world and from pole to pole.
const vec2 CLOUD_SHADOW = vec2(-0.8 / 360.0, -0.5 / 180.0);
const vec2 CLOUD_EDGE = vec2(0.5 / 360.0, 0.35 / 180.0);
// The steps a turn is cut into for the clouds' paces, and the pace of the weather's fronts.
const float PACE_STEPS = 64.0;
const float FRONT_PACE = 70.0;

// The smaller of a derivative across the map's seam and the same one read from a map turned half round.
float seamless(float plain, float turned) {
    return abs(plain) < abs(turned) ? plain : turned;
}

vec3 shade(vec3 colour, float light, float bumped, vec3 normal, vec3 view) {
    float level = bumped < 0.0 ? Shadow * smoothstep(-0.6, 0.0, bumped)
                               : Shadow + (1.0 - Shadow) * pow(clamp(bumped, 0.0, 1.0), 0.7);
    vec3 lit = colour * level;
    if (HasDusk == 1) {
        float band = exp(-pow(light / 0.09, 2.0)) * 0.38;
        lit = lit * (1.0 - band) + Dusk * band * level;
    }
    if (AirThickness > 0.0) {
        float edge = pow(1.0 - abs(dot(normal, view)), 3.0);
        float haze = edge * 0.45 * smoothstep(-0.05, 0.2, light);
        lit = lit * (1.0 - haze) + Air * haze * level;
    }
    float day = smoothstep(-0.035, 0.045, light);
    return clamp(Night * (1.0 - day) + lit * day, 0.0, 1.0);
}

bool cloudAt(vec2 uv, float lod) {
    float v = clamp(uv.y, 0.0, 1.0);
    float pace = floor(textureLod(Sampler2, vec2(0.0, v), lod).r * 255.0 + 0.5);
    float west = fract(CloudTurn * (pace / PACE_STEPS));
    float east = fract(CloudTurn * (FRONT_PACE / PACE_STEPS));
    return textureLod(Sampler2, vec2(fract(uv.x + west), v), lod).a > 0.5
            && textureLod(Sampler4, vec2(fract(uv.x - east), v), lod).a > 0.5;
}

void main() {
    vec3 ray = normalize(worldPos);
    float along = dot(Centre, ray);
    vec3 closest = ray * along - Centre;
    // How far the ray passes from the middle, and how deep it goes in, worked so a huge world seen from just over its
    // ground keeps a clean horizon.
    float apart = length(cross(Centre, ray));
    float depth = sqrt(max((Radius - apart) * (Radius + apart), 0.0));
    vec3 normal = normalize(ray * (along - depth) - Centre);

    // Where the ray meets the world on its maps: its latitude from its pole, its longitude east from its map's middle.
    vec3 eastAxis = cross(North, Middle);
    float lat = asin(clamp(dot(normal, North), -1.0, 1.0));
    float lon = atan(dot(normal, eastAxis), dot(normal, Middle));
    vec2 uv = vec2(fract(lon / (2.0 * PI) + 0.5), 0.5 - lat / PI);
    vec2 turned = vec2(fract(uv.x + 0.5), uv.y);
    vec2 dx = vec2(seamless(dFdx(uv.x), dFdx(turned.x)), dFdx(uv.y));
    vec2 dy = vec2(seamless(dFdy(uv.x), dFdy(turned.x)), dFdy(uv.y));
    vec2 size = vec2(textureSize(Sampler0, 0));
    float lod = max(0.0, log2(max(length(dx * size), length(dy * size)) * TexelPixels) + 0.5);

    if (apart >= Radius || along <= 0.0) {
        // Past the limb: the air seen edge-on, a glow fading outwards, bright where the sun lights it.
        float reach = apart / Radius;
        if (AirThickness > 0.0 && along > 0.0 && reach < 1.0 + AirThickness * 1.6) {
            float lit = smoothstep(-0.25, 0.25, dot(closest / apart, SunDir));
            float glow = exp(-(reach - 1.0) / AirThickness * 2.2) * lit;
            fragColor = vec4(Air * glow, clamp(glow * 1.2, 0.0, 1.0)) * Fade;
            return;
        }
        discard;
    }

    vec3 colour = textureLod(Sampler0, uv, lod).rgb;
    float light = dot(normal, SunDir);
    float bumped = light;
    if (Relief > 0.0) {
        // The slope of the height map east and north per radian of the sphere, at the copy read, leaning the normal.
        vec2 texel = exp2(floor(lod + 0.5)) / size;
        float north = clamp(uv.y - texel.y, 0.0, 1.0);
        float south = clamp(uv.y + texel.y, 0.0, 1.0);
        float slopeEast = (textureLod(Sampler1, vec2(fract(uv.x + texel.x), uv.y), lod).r
                - textureLod(Sampler1, vec2(fract(uv.x - texel.x), uv.y), lod).r)
                / (2.0 * texel.x * 2.0 * PI * max(cos(lat), 0.05));
        float slopeNorth = (textureLod(Sampler1, vec2(uv.x, north), lod).r
                - textureLod(Sampler1, vec2(uv.x, south), lod).r) / (2.0 * texel.y * PI);
        vec3 eastward = cross(North, normal);
        eastward = length(eastward) > 0.0001 ? normalize(eastward) : eastAxis;
        vec3 northward = cross(normal, eastward);
        bumped = dot(normalize(normal - Relief * (slopeEast * eastward + slopeNorth * northward)), SunDir);
    }

    vec3 view = -ray;
    vec3 shaded;
    if (HasClouds == 1 && cloudAt(uv, lod)) {
        vec3 top = vec3(cloudAt(uv + CLOUD_EDGE, lod) ? 0.97 : 0.82);
        shaded = shade(top, light, light, normal, view);
    } else {
        if (HasClouds == 1 && cloudAt(uv + CLOUD_SHADOW, lod)) {
            colour *= 0.74;
        }
        shaded = shade(colour, light, bumped, normal, view);
    }
    fragColor = vec4(mix(shaded, VeilColour, Veil), 1.0) * Fade;
}
