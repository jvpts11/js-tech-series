#version 150

// The ground far below the climb to space, out to its horizon: each pixel's ray walks down onto the world's far map,
// its relief raised from the heights the world's generator gives it, on a ground bent away round the eye. The map
// comes in rings, the finest where it reaches, and the blocks once loaded under the climber finest of all. The ground is
// lit as the game lights its blocks, turning to the Sun of space's light near the top; the game's clouds lie over it
// at their own height and drift, and a haze like the game's fog thickens with the distance.

// The near ring's colours and heights, the wide ring's, the loaded blocks', and the game's clouds.
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;
uniform sampler2D Sampler4;
uniform sampler2D Sampler5;
uniform sampler2D Sampler6;

// From the screen to the directions of the world round the eye.
uniform mat4 RayMat;
// The ground's up, east and south under the eye.
uniform vec3 Up;
uniform vec3 East;
uniform vec3 South;
// The eye's height over the sea, the radius the ground bends round, the sea's height, the highest ground over it.
uniform float EyeHeight;
uniform float Bend;
uniform float Sea;
uniform float Top;
// The open sea's colour where a map's texel is land but its block is under the sea, and a shore's the other way.
uniform vec3 Water;
uniform vec3 Shore;
// Each map: the eye's column east and south of its middle, in blocks, its width in blocks, and whether there is one;
// and how many blocks wide a texel of each is.
uniform vec4 Ring0;
uniform vec4 Ring1;
uniform vec4 Loaded;
uniform vec3 Texel;
// How wide a pixel looks, in radians.
uniform float PixelAngle;
// The clouds: the eye's place in their map, in texels, their height over the sea, whether there are any; their colour
// and how solid they are; and whether the game's own clouds are drawn round the eye, which these leave to it.
uniform vec4 Clouds;
uniform vec4 CloudColour;
uniform float CloudSkip;
// The haze: its colour and how far off it is whole, as the game's fog; the veil the ground melts under.
uniform vec3 Haze;
uniform float HazeEnd;
uniform float Veil;
uniform vec3 VeilColour;
// The day's light on the blocks; and the Sun of space, how much it lights the ground, and its night and shadow tones.
uniform float Light;
uniform vec3 SunDir;
uniform float Sunlit;
uniform vec3 Night;
uniform float Shadow;
// How much of the ground covers what is behind.
uniform float Opacity;

in vec2 screen;

out vec4 fragColor;

const int STEPS = 200;
const int REFINE = 6;
const float LOWEST = -64.0;
const float HEIGHT_STEP = 2.0;
const float CLOUD_TEXEL = 12.0;
const float CLOUD_MAP = 256.0;
// The square of the game's clouds round the eye, in their texels from the eye's own.
const float CLOUDS_FROM = -24.0;
const float CLOUDS_TO = 40.0;

bool within(vec2 uv) {
    return uv.x > 0.0 && uv.y > 0.0 && uv.x < 1.0 && uv.y < 1.0;
}

vec2 uvOf(vec4 map, vec2 offset) {
    return (offset + map.xy) / map.z + 0.5;
}

// Which map covers the ground at the offset from the eye's column: 2 the loaded blocks, 0 the near ring, 1 the wide
// ring, -1 none.
int mapAt(vec2 offset) {
    if (Loaded.w > 0.5) {
        vec2 uv = uvOf(Loaded, offset);
        if (within(uv) && textureLod(Sampler4, uv, 0.0).a > 0.5) {
            return 2;
        }
    }
    if (Ring0.w > 0.5 && within(uvOf(Ring0, offset))) {
        return 0;
    }
    if (Ring1.w > 0.5 && within(uvOf(Ring1, offset))) {
        return 1;
    }
    return -1;
}

vec4 ringOf(int map) {
    return map == 2 ? Loaded : map == 0 ? Ring0 : Ring1;
}

// The block the offset falls in, from its map's middle, which stands on a block's corner.
vec2 blockOf(int map, vec2 offset) {
    return floor(offset + ringOf(map).xy);
}

// The ground's height read smoothly between a map's texels at the offset.
float smoothHeight(int map, vec2 offset) {
    if (map == 2) {
        return textureLod(Sampler5, uvOf(Loaded, offset), 0.0).r * 255.0 * HEIGHT_STEP + LOWEST;
    }
    if (map == 0) {
        return textureLod(Sampler1, uvOf(Ring0, offset), 0.0).r * 255.0 * HEIGHT_STEP + LOWEST;
    }
    return textureLod(Sampler3, uvOf(Ring1, offset), 0.0).r * 255.0 * HEIGHT_STEP + LOWEST;
}

// The top of the block the offset falls in: the loaded blocks' own, and the far rings' read at the block's middle and
// cut to whole blocks, so their ground is made of blocks too.
float rawHeight(int map, vec2 offset) {
    if (map == 2) {
        return smoothHeight(map, offset);
    }
    vec4 ring = ringOf(map);
    return floor(smoothHeight(map, blockOf(map, offset) + 0.5 - ring.xy));
}

// The top of the ground or of the sea over it, over the sea, at the offset; far under it where no map reaches.
float groundAt(vec2 offset) {
    int map = mapAt(offset);
    if (map < 0) {
        return -1.0e6;
    }
    return max(rawHeight(map, offset), Sea) - Sea;
}

// How high the point the ray reaches at t stands over the bent ground's sea, and how far round the ground it lies.
float altitude(float t, float rise, float level, out float arc) {
    float above = EyeHeight + t * rise;
    float side = t * level;
    float middle = Bend + above;
    float reach = sqrt(middle * middle + side * side);
    arc = Bend * atan(side, middle);
    return above + side * side / (reach + middle);
}

// Where the ray first meets the sphere at the height over the sea, or a negative number.
float meet(float height, float rise) {
    float r = Bend + EyeHeight;
    float lift = (EyeHeight - height) * (2.0 * Bend + EyeHeight + height);
    float b = rise * r;
    float d = b * b - lift;
    if (d < 0.0) {
        return -1.0;
    }
    if (lift <= 0.0) {
        return -b + sqrt(d);
    }
    return b < 0.0 ? lift / (-b + sqrt(d)) : -1.0;
}

vec4 colourAt(int map, vec2 offset, float lod) {
    if (map == 2) {
        return textureLod(Sampler4, uvOf(Loaded, offset), lod);
    }
    if (map == 0) {
        return textureLod(Sampler0, uvOf(Ring0, offset), lod);
    }
    return textureLod(Sampler2, uvOf(Ring1, offset), lod);
}

// The colour of the block the offset falls in, its top at the height: water where it is under the sea, whatever the
// map's texel there says, and the shore's sand where the texel is water but the block is not; each block a little
// lighter or darker than the next, as a block's own texture makes it from afar.
vec3 blockColour(int map, vec2 offset, float lod, float height) {
    vec4 texel = colourAt(map, offset, lod);
    bool wet = texel.a < 254.5 / 255.0;
    vec3 colour = height < Sea ? (wet ? texel.rgb : Water) : (wet ? Shore : texel.rgb);
    vec2 block = blockOf(map, offset);
    float grain = fract(sin(dot(block, vec2(12.9898, 78.233))) * 43758.5453);
    return colour * (0.95 + 0.1 * grain);
}

float texelOf(int map) {
    return map == 2 ? Texel.z : map == 0 ? Texel.x : Texel.y;
}

void main() {
    vec4 near = RayMat * vec4(screen, -1.0, 1.0);
    vec4 far = RayMat * vec4(screen, 1.0, 1.0);
    vec3 ray = normalize(far.xyz / far.w - near.xyz / near.w);
    float rise = dot(ray, Up);
    vec3 across = ray - Up * rise;
    float level = length(across);
    vec2 heading = level > 1.0e-6 ? vec2(dot(across, East), dot(across, South)) / level : vec2(0.0);

    // Walk down the ray onto the ground, from where it comes down to the highest ground.
    float horizon = sqrt(max(2.0 * Bend * EyeHeight + EyeHeight * EyeHeight, 0.0)) + sqrt(2.0 * Bend * max(Top, 1.0));
    float t = 0.0;
    if (EyeHeight > Top) {
        t = meet(Top, rise);
    }
    float arc = 0.0;
    float last = t;
    bool hit = false;
    if (t >= 0.0) {
        for (int i = 0; i < STEPS; i++) {
            float clear = altitude(t, rise, level, arc) - groundAt(heading * arc);
            if (clear <= 0.0) {
                hit = true;
                break;
            }
            if (t > horizon) {
                break;
            }
            last = t;
            t += max(clear * 0.7, 1.0 + t * 0.01);
        }
    }
    if (hit) {
        float low = last;
        float high = t;
        for (int i = 0; i < REFINE; i++) {
            float middle = (low + high) * 0.5;
            if (altitude(middle, rise, level, arc) - groundAt(heading * arc) <= 0.0) {
                high = middle;
            } else {
                low = middle;
            }
        }
        t = high;
        altitude(t, rise, level, arc);
    }

    // The clouds' top, if the ray passes it before the ground.
    float cloudAt = Clouds.w > 0.5 ? meet(Clouds.z, rise) : -1.0;
    if (cloudAt >= 0.0 && hit && cloudAt > t) {
        cloudAt = -1.0;
    }
    if (!hit && cloudAt < 0.0) {
        discard;
    }

    vec3 colour = vec3(0.0);
    if (hit) {
        vec2 offset = heading * arc;
        int map = mapAt(offset);
        float texel = texelOf(map);
        // The slope of the ground, by its smooth heights a texel either side.
        float east = smoothHeight(map, offset + vec2(texel, 0.0)) - smoothHeight(map, offset - vec2(texel, 0.0));
        float south = smoothHeight(map, offset + vec2(0.0, texel)) - smoothHeight(map, offset - vec2(0.0, texel));
        vec3 bumped = normalize(vec3(-east, 2.0 * texel, -south));
        vec3 point = ray * t;
        vec3 outward = normalize(point + Up * (Bend + EyeHeight));
        // How wide a pixel falls on the ground there, read from the copy whose texels are no smaller.
        float facing = max(abs(dot(ray, outward)), 0.15);
        float lod = max(0.0, log2(PixelAngle * t / facing / texel));
        vec3 ground = blockColour(map, offset, lod, rawHeight(map, offset));
        // As the game lights its blocks: the tops whole, the sides facing east and west darkest.
        float face = (bumped.y + 0.6 * abs(bumped.x) + 0.8 * abs(bumped.z))
                / (bumped.y + abs(bumped.x) + abs(bumped.z));
        colour = ground * face * Light;
        if (Sunlit > 0.0) {
            vec3 normal = normalize(East * bumped.x + outward * bumped.y + South * bumped.z);
            float light = dot(outward, SunDir);
            float lean = dot(normal, SunDir);
            float tone = lean < 0.0 ? Shadow * smoothstep(-0.6, 0.0, lean)
                                    : Shadow + (1.0 - Shadow) * pow(clamp(lean, 0.0, 1.0), 0.7);
            float day = smoothstep(-0.035, 0.045, light);
            colour = mix(colour, mix(Night, ground * tone, day), Sunlit);
        }
        colour = mix(colour, Haze, smoothstep(0.0, HazeEnd, t));
    }
    float alpha = hit ? 1.0 : 0.0;
    if (cloudAt >= 0.0) {
        float cloudArc;
        altitude(cloudAt, rise, level, cloudArc);
        vec2 place = Clouds.xy + heading * cloudArc / CLOUD_TEXEL;
        vec2 fromEye = floor(fract(Clouds.xy) + heading * cloudArc / CLOUD_TEXEL);
        bool theGames = CloudSkip > 0.5 && fromEye.x >= CLOUDS_FROM && fromEye.y >= CLOUDS_FROM
                && fromEye.x < CLOUDS_TO && fromEye.y < CLOUDS_TO;
        if (!theGames) {
            float facing = max(abs(rise), 0.05);
            float lod = max(0.0, log2(PixelAngle * cloudAt / facing / CLOUD_TEXEL));
            float cover = textureLod(Sampler6, fract(place / CLOUD_MAP), lod).a * CloudColour.a;
            vec3 cloud = CloudColour.rgb;
            if (Sunlit > 0.0) {
                // Lit by the Sun of space where it stands, as the ground under them is.
                vec3 outward = normalize(ray * cloudAt + Up * (Bend + EyeHeight));
                float day = smoothstep(-0.035, 0.045, dot(outward, SunDir));
                cloud = mix(cloud, mix(Night, vec3(0.95), day), Sunlit);
            }
            cloud = mix(cloud, Haze, smoothstep(0.0, HazeEnd, cloudAt));
            colour = hit ? mix(colour, cloud, cover) : cloud;
            alpha = hit ? 1.0 : cover;
        }
    }
    if (alpha <= 0.0) {
        discard;
    }
    colour = mix(colour, VeilColour, Veil);
    fragColor = vec4(colour, 1.0) * alpha * Opacity;
}
