/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;

/**
 * Reads the geometry and the animation files of Bedrock, which GeckoLib and Blockbench write ({@code .geo.json},
 * {@code .animation.json}), as they are: every format version, box and per-face pictures, locators, keyframes of
 * numbers or expressions, smooth and stepped ones and the easing curves GeckoLib adds, and sounds, particles and
 * words along an animation. Bedrock's X runs the other way to the game's; it is turned round here, so a model reads
 * in the Core's space. Pure.
 */
public final class BedrockReader {

    private static final String GEOMETRY_LIST = "minecraft:geometry";
    private static final String OLD_PREFIX = "geometry.";

    private BedrockReader() {
    }

    /**
     * The models of a geometry file, the first being the one a model of one geometry is drawn with.
     *
     * @param name what the model is called, unless the file says
     * @throws ModelFormatException when the file is not one
     */
    public static List<ModelData> geometry(final String name, final JsonObject file) {
        final List<ModelData> models = new ArrayList<>();
        if (file.has(GEOMETRY_LIST)) {
            for (final JsonElement each : file.getAsJsonArray(GEOMETRY_LIST)) {
                final JsonObject geometry = each.getAsJsonObject();
                final JsonObject description = geometry.has("description") ? geometry.getAsJsonObject("description")
                        : new JsonObject();
                models.add(model(string(description, "identifier", name), number(description, "texture_width", 16),
                        number(description, "texture_height", 16), geometry));
            }
        }
        for (final Map.Entry<String, JsonElement> entry : file.entrySet()) {
            if (entry.getKey().startsWith(OLD_PREFIX) && entry.getValue().isJsonObject()) {
                final JsonObject geometry = entry.getValue().getAsJsonObject();
                models.add(model(entry.getKey(), number(geometry, "texturewidth", 16),
                        number(geometry, "textureheight", 16), geometry));
            }
        }
        if (models.isEmpty()) {
            throw new ModelFormatException("the geometry file " + name + " holds no geometry");
        }
        return models;
    }

    /**
     * The animations of an animation file.
     *
     * @throws ModelFormatException when the file is not one
     */
    public static List<ModelAnimation> animations(final String name, final JsonObject file) {
        if (!file.has("animations") || !file.get("animations").isJsonObject()) {
            throw new ModelFormatException("the animation file " + name + " holds no animations");
        }
        final List<ModelAnimation> out = new ArrayList<>();
        for (final Map.Entry<String, JsonElement> entry : file.getAsJsonObject("animations").entrySet()) {
            out.add(animation(entry.getKey(), entry.getValue().getAsJsonObject()));
        }
        return out;
    }

    private static ModelData model(final String name, final float width, final float height,
                                   final JsonObject geometry) {
        final List<ModelBone> bones = new ArrayList<>();
        if (geometry.has("bones")) {
            for (final JsonElement each : geometry.getAsJsonArray("bones")) {
                bones.add(bone(each.getAsJsonObject()));
            }
        }
        return new ModelData(name, (int) width, (int) height, bones, Map.of(), List.of(), List.of());
    }

    private static ModelBone bone(final JsonObject bone) {
        final String name = string(bone, "name", "bone");
        final String parent = bone.has("parent") ? bone.get("parent").getAsString() : null;
        final boolean mirror = bone.has("mirror") && bone.get("mirror").getAsBoolean();
        final float inflate = number(bone, "inflate", 0);
        final List<ModelCube> cubes = new ArrayList<>();
        if (bone.has("cubes")) {
            for (final JsonElement each : bone.getAsJsonArray("cubes")) {
                cubes.add(cube(each.getAsJsonObject(), mirror, inflate));
            }
        }
        final List<ModelLocator> locators = new ArrayList<>();
        if (bone.has("locators")) {
            for (final Map.Entry<String, JsonElement> entry : bone.getAsJsonObject("locators").entrySet()) {
                locators.add(locator(entry.getKey(), entry.getValue()));
            }
        }
        return ModelBone.ofEuler(name, parent, flipPoint(vector(bone, "pivot")), flipTurn(vector(bone, "rotation")),
                cubes, List.of(), locators, bone.has("neverRender") && bone.get("neverRender").getAsBoolean());
    }

    private static ModelCube cube(final JsonObject cube, final boolean boneMirror, final float boneInflate) {
        final Vector3f origin = vector(cube, "origin");
        final Vector3f size = vector(cube, "size");
        final boolean mirror = cube.has("mirror") ? cube.get("mirror").getAsBoolean() : boneMirror;
        final Vector3f from = new Vector3f(-(origin.x + size.x), origin.y, origin.z);
        final Map<CubeFace, FaceUv> faces;
        final JsonElement uv = cube.get("uv");
        if (uv != null && uv.isJsonObject()) {
            faces = new EnumMap<>(CubeFace.class);
            for (final Map.Entry<String, JsonElement> entry : uv.getAsJsonObject().entrySet()) {
                final CubeFace face = CubeFace.byName(entry.getKey());
                final JsonObject picture = entry.getValue().getAsJsonObject();
                if (face != null && picture.has("uv")) {
                    final float[] corner = floats(picture.get("uv"), 2);
                    final float[] extent = picture.has("uv_size") ? floats(picture.get("uv_size"), 2)
                            : new float[] {0, 0};
                    faces.put(face, new FaceUv(corner[0], corner[1], extent[0], extent[1],
                            (int) number(picture, "uv_rotation", 0), 0));
                }
            }
        } else {
            final float[] corner = uv == null ? new float[] {0, 0} : floats(uv, 2);
            faces = ModelCube.boxUv(corner[0], corner[1], size);
        }
        return new ModelCube(from, size, number(cube, "inflate", boneInflate), flipPoint(vector(cube, "pivot")),
                flipTurn(vector(cube, "rotation")), faces, mirror);
    }

    private static ModelLocator locator(final String name, final JsonElement value) {
        if (value.isJsonArray()) {
            return new ModelLocator(name, flipPoint(toVector(value)), new Vector3f());
        }
        final JsonObject locator = value.getAsJsonObject();
        return new ModelLocator(name, flipPoint(vector(locator, "offset")), flipTurn(vector(locator, "rotation")));
    }

    private static ModelAnimation animation(final String name, final JsonObject animation) {
        final ModelAnimation.Loop loop = loop(animation.get("loop"));
        final Map<String, ModelAnimation.Tracks> bones = new LinkedHashMap<>();
        double longest = 0;
        if (animation.has("bones")) {
            for (final Map.Entry<String, JsonElement> entry : animation.getAsJsonObject("bones").entrySet()) {
                final JsonObject bone = entry.getValue().getAsJsonObject();
                final AnimationChannel rotation = channel(bone.get("rotation"), AnimationChannel.Kind.EULER,
                        new boolean[] {true, true, false});
                final AnimationChannel position = channel(bone.get("position"), AnimationChannel.Kind.POSITION,
                        new boolean[] {true, false, false});
                final AnimationChannel scale = channel(bone.get("scale"), AnimationChannel.Kind.SCALE,
                        new boolean[] {false, false, false});
                for (final AnimationChannel channel : new AnimationChannel[] {rotation, position, scale}) {
                    if (channel != null) {
                        longest = Math.max(longest, channel.length());
                    }
                }
                bones.put(entry.getKey(), new ModelAnimation.Tracks(rotation, position, scale));
            }
        }
        final List<ModelAnimation.Event> events = new ArrayList<>();
        events(animation.get("sound_effects"), ModelAnimation.EventKind.SOUND, events);
        events(animation.get("particle_effects"), ModelAnimation.EventKind.PARTICLE, events);
        events(animation.get("timeline"), ModelAnimation.EventKind.INSTRUCTION, events);
        for (final ModelAnimation.Event event : events) {
            longest = Math.max(longest, event.time());
        }
        final double length = animation.has("animation_length") ? animation.get("animation_length").getAsDouble()
                : longest;
        return new ModelAnimation(name, length, loop, bones, Map.of(), events);
    }

    private static ModelAnimation.Loop loop(final JsonElement loop) {
        if (loop == null) {
            return ModelAnimation.Loop.ONCE;
        }
        final JsonPrimitive value = loop.getAsJsonPrimitive();
        if (value.isBoolean()) {
            return value.getAsBoolean() ? ModelAnimation.Loop.LOOP : ModelAnimation.Loop.ONCE;
        }
        return ModelAnimation.Loop.HOLD.serializedName().equals(value.getAsString()) ? ModelAnimation.Loop.HOLD
                : ModelAnimation.Loop.ONCE;
    }

    /*
     * A channel as Bedrock writes it: one value for all time, or keyframes by time, each a value or a pre and post
     * value with a lerp mode, or GeckoLib's vector with an easing; the numbers marked turned round as X is.
     */
    private static AnimationChannel channel(final JsonElement value, final AnimationChannel.Kind kind,
                                            final boolean[] flip) {
        if (value == null) {
            return null;
        }
        final List<Keyframe> keyframes = new ArrayList<>();
        if (value.isJsonObject() && !isKeyframeObject(value.getAsJsonObject())) {
            for (final Map.Entry<String, JsonElement> entry : value.getAsJsonObject().entrySet()) {
                keyframes.add(keyframe(Double.parseDouble(entry.getKey()), entry.getValue(), flip));
            }
        } else {
            keyframes.add(keyframe(0, value, flip));
        }
        return keyframes.isEmpty() ? null : new AnimationChannel(kind, keyframes);
    }

    private static boolean isKeyframeObject(final JsonObject value) {
        return value.has("vector") || value.has("pre") || value.has("post");
    }

    private static Keyframe keyframe(final double time, final JsonElement value, final boolean[] flip) {
        if (value.isJsonObject()) {
            final JsonObject frame = value.getAsJsonObject();
            if (frame.has("vector")) {
                final double argument = frame.has("easingArgs") && frame.getAsJsonArray("easingArgs").size() > 0
                        ? frame.getAsJsonArray("easingArgs").get(0).getAsDouble() : Double.NaN;
                final ModelExpression[] vector = expressions(frame.get("vector"), flip);
                return new Keyframe(time, vector, vector, Easing.byName(string(frame, "easing", "linear")), argument,
                        new float[0], new float[0]);
            }
            final ModelExpression[] pre = expressions(frame.has("pre") ? frame.get("pre") : frame.get("post"), flip);
            final ModelExpression[] post = expressions(frame.has("post") ? frame.get("post") : frame.get("pre"),
                    flip);
            final Easing easing = Easing.byName(string(frame, "lerp_mode", "linear"));
            return new Keyframe(time, pre, post, easing, Double.NaN, new float[0], new float[0]);
        }
        return Keyframe.of(time, expressions(value, flip), Easing.LINEAR);
    }

    /* Three expressions from a number, a string or an array of either; those marked turned round. */
    private static ModelExpression[] expressions(final JsonElement value, final boolean[] flip) {
        final ModelExpression[] out = new ModelExpression[3];
        for (int i = 0; i < 3; i++) {
            final JsonElement part = value.isJsonArray() ? value.getAsJsonArray().get(Math.min(i,
                    value.getAsJsonArray().size() - 1)) : value;
            final ModelExpression read = expression(part);
            out[i] = flip[i] ? negate(read) : read;
        }
        return out;
    }

    private static ModelExpression expression(final JsonElement part) {
        final JsonPrimitive primitive = part.getAsJsonPrimitive();
        return primitive.isNumber() ? ModelExpression.constant(primitive.getAsDouble())
                : ExpressionParser.parse(primitive.getAsString());
    }

    private static ModelExpression negate(final ModelExpression value) {
        return value.constant() ? ModelExpression.constant(-value.evaluate(null))
                : context -> -value.evaluate(context);
    }

    private static void events(final JsonElement events, final ModelAnimation.EventKind kind,
                               final List<ModelAnimation.Event> out) {
        if (events == null || !events.isJsonObject()) {
            return;
        }
        for (final Map.Entry<String, JsonElement> entry : events.getAsJsonObject().entrySet()) {
            final double time = Double.parseDouble(entry.getKey());
            final JsonElement value = entry.getValue();
            final List<JsonElement> each = new ArrayList<>();
            if (value.isJsonArray()) {
                value.getAsJsonArray().forEach(each::add);
            } else {
                each.add(value);
            }
            for (final JsonElement one : each) {
                if (one.isJsonObject()) {
                    final JsonObject event = one.getAsJsonObject();
                    out.add(new ModelAnimation.Event(time, kind, string(event, "effect", ""),
                            string(event, "locator", "")));
                } else {
                    out.add(new ModelAnimation.Event(time, kind, one.getAsString(), ""));
                }
            }
        }
    }

    /* A point of Bedrock's space in the Core's: X the other way. */
    private static Vector3f flipPoint(final Vector3f point) {
        return point.mul(-1, 1, 1);
    }

    /* A turn of Bedrock's space in the Core's: X the other way turns the angles about X and Y round. */
    private static Vector3f flipTurn(final Vector3f turn) {
        return turn.mul(-1, -1, 1);
    }

    private static Vector3f vector(final JsonObject object, final String key) {
        return ModelJson.vector(object, key);
    }

    private static Vector3f toVector(final JsonElement value) {
        return ModelJson.vector(value);
    }

    private static float[] floats(final JsonElement value, final int count) {
        return ModelJson.floats(value, count);
    }

    private static float number(final JsonObject object, final String key, final float otherwise) {
        return ModelJson.number(object, key, otherwise);
    }

    private static String string(final JsonObject object, final String key, final String otherwise) {
        return ModelJson.string(object, key, otherwise);
    }
}
