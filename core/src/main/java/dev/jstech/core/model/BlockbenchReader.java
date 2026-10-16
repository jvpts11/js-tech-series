/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reads Blockbench's own model files ({@code .bbmodel}) as Blockbench saves them: groups as bones, cubes with box or
 * per-face pictures on any of the model's textures, meshes of any shape, locators, the textures the file carries,
 * and the animations, keyframes of numbers or expressions, stepped, straight, smooth or by bezier handles. A model in
 * Java's block format stands on the block's corner and is moved to stand on its middle, as Bedrock's models do. Pure.
 */
public final class BlockbenchReader {

    private static final String CUBE = "cube";
    private static final String MESH = "mesh";
    private static final String LOCATOR = "locator";
    private static final String JAVA_BLOCK = "java_block";
    private static final String DATA_PNG = "data:image/png;base64,";
    private static final String ROOT = "root";
    private static final float BLOCK_MIDDLE = 8.0F;

    private BlockbenchReader() {
    }

    /**
     * The model a Blockbench file holds, with its animations.
     *
     * @throws ModelFormatException when the file is not one
     */
    public static ModelData read(final String name, final JsonObject file) {
        if (!file.has("elements")) {
            throw new ModelFormatException("the Blockbench file " + name + " holds no elements");
        }
        final JsonObject meta = file.has("meta") ? file.getAsJsonObject("meta") : new JsonObject();
        final Vector3f shift = JAVA_BLOCK.equals(ModelJson.string(meta, "model_format", ""))
                ? new Vector3f(-BLOCK_MIDDLE, 0, -BLOCK_MIDDLE) : new Vector3f();
        final JsonObject resolution = file.has("resolution") ? file.getAsJsonObject("resolution") : new JsonObject();
        final List<ModelTexture> textures = textures(file);
        final Map<String, Integer> textureIndex = new HashMap<>();
        if (file.has("textures")) {
            int index = 0;
            for (final JsonElement each : file.getAsJsonArray("textures")) {
                textureIndex.put(ModelJson.string(each.getAsJsonObject(), "uuid", String.valueOf(index)), index);
                index++;
            }
        }
        final Map<String, JsonObject> elements = new LinkedHashMap<>();
        for (final JsonElement each : file.getAsJsonArray("elements")) {
            final JsonObject element = each.getAsJsonObject();
            elements.put(ModelJson.string(element, "uuid", String.valueOf(elements.size())), element);
        }
        final float width = ModelJson.number(resolution, "width", 16);
        final float height = ModelJson.number(resolution, "height", 16);
        final Reading reading = new Reading(shift, textureIndex, width, height, textures);
        final List<ModelBone> bones = new ArrayList<>();
        final Map<String, String> boneOfUuid = new HashMap<>();
        final List<JsonObject> loose = new ArrayList<>();
        if (file.has("outliner")) {
            for (final JsonElement node : file.getAsJsonArray("outliner")) {
                outline(node, null, elements, reading, bones, boneOfUuid, loose);
            }
        } else {
            loose.addAll(elements.values());
        }
        if (!loose.isEmpty()) {
            bones.add(0, holder(rootName(bones), null, new Vector3f(), new Vector3f(), loose, reading));
        }
        final List<ModelAnimation> animations = new ArrayList<>();
        if (file.has("animations")) {
            for (final JsonElement each : file.getAsJsonArray("animations")) {
                animations.add(animation(each.getAsJsonObject(), boneOfUuid));
            }
        }
        return new ModelData(ModelJson.string(file, "name", name), (int) width, (int) height, bones, Map.of(), textures,
                animations);
    }

    private static void outline(final JsonElement node, final String parent, final Map<String, JsonObject> elements,
                                final Reading reading, final List<ModelBone> bones,
                                final Map<String, String> boneOfUuid, final List<JsonObject> loose) {
        if (node.isJsonPrimitive()) {
            final JsonObject element = elements.get(node.getAsString());
            if (element != null && parent == null) {
                loose.add(element);
            }
            return;
        }
        final JsonObject group = node.getAsJsonObject();
        final String name = ModelJson.string(group, "name", "group");
        final List<JsonObject> own = new ArrayList<>();
        final List<JsonElement> children = new ArrayList<>();
        if (group.has("children")) {
            for (final JsonElement child : group.getAsJsonArray("children")) {
                if (child.isJsonPrimitive()) {
                    final JsonObject element = elements.get(child.getAsString());
                    if (element != null) {
                        own.add(element);
                    }
                } else {
                    children.add(child);
                }
            }
        }
        final Vector3f pivot = ModelJson.vector(group, "origin").add(reading.shift);
        bones.add(holder(name, parent, pivot, ModelJson.vector(group, "rotation"), own, reading));
        boneOfUuid.put(ModelJson.string(group, "uuid", name), name);
        for (final JsonElement child : children) {
            outline(child, name, elements, reading, bones, boneOfUuid, loose);
        }
    }

    /* A bone holding {@code elements}: its cubes, its meshes and its locators. */
    private static ModelBone holder(final String name, final String parent, final Vector3f pivot,
                                    final Vector3f rotation, final List<JsonObject> elements, final Reading reading) {
        final List<ModelCube> cubes = new ArrayList<>();
        final List<ModelMesh> meshes = new ArrayList<>();
        final List<ModelLocator> locators = new ArrayList<>();
        for (final JsonObject element : elements) {
            final String type = ModelJson.string(element, "type", CUBE);
            if (!ModelJson.flag(element, "export", true)) {
                continue;
            }
            switch (type) {
                case CUBE -> cubes.add(cube(element, reading));
                case MESH -> meshes.addAll(meshes(element, reading));
                case LOCATOR -> locators.add(new ModelLocator(ModelJson.string(element, "name", LOCATOR),
                        ModelJson.vector(element, "position").add(reading.shift),
                        ModelJson.vector(element, "rotation")));
                default -> {
                    // Nulls and other helpers of the editor draw nothing.
                }
            }
        }
        return ModelBone.ofEuler(name, parent, pivot, rotation, cubes, meshes, locators, false);
    }

    private static ModelCube cube(final JsonObject element, final Reading reading) {
        final Vector3f from = ModelJson.vector(element, "from").add(reading.shift);
        final Vector3f to = ModelJson.vector(element, "to").add(reading.shift);
        final Vector3f size = new Vector3f(to).sub(from);
        final Map<CubeFace, FaceUv> faces;
        if (ModelJson.flag(element, "box_uv", false)) {
            final float[] offset = element.has("uv_offset") ? ModelJson.floats(element.get("uv_offset"), 2)
                    : new float[] {0, 0};
            faces = ModelCube.boxUv(offset[0], offset[1], size);
        } else {
            faces = new EnumMap<>(CubeFace.class);
            if (element.has("faces")) {
                for (final Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
                    final CubeFace face = CubeFace.byName(entry.getKey());
                    final JsonObject picture = entry.getValue().getAsJsonObject();
                    final int texture = reading.texture(picture.get("texture"));
                    if (face == null || texture < 0 || !picture.has("uv")) {
                        continue;
                    }
                    final float[] uv = ModelJson.floats(picture.get("uv"), 4);
                    final int turn = (int) ModelJson.number(picture, "rotation", 0);
                    if (face == CubeFace.UP || face == CubeFace.DOWN) {
                        // Blockbench counts a top or bottom face's picture from its other corner than Bedrock does,
                        // and turns it round as it writes a Bedrock model: the same here, so both draw alike.
                        faces.put(face, new FaceUv(uv[2], uv[3], uv[0] - uv[2], uv[1] - uv[3], turn, texture));
                    } else {
                        faces.put(face, new FaceUv(uv[0], uv[1], uv[2] - uv[0], uv[3] - uv[1], turn, texture));
                    }
                }
            }
        }
        return new ModelCube(from, size, ModelJson.number(element, "inflate", 0),
                ModelJson.vector(element, "origin").add(reading.shift), ModelJson.vector(element, "rotation"),
                faces, ModelJson.flag(element, "mirror_uv", false));
    }

    /* A mesh's corners are kept about its origin, turned with it; they are laid where they stand in the model. */
    private static List<ModelMesh> meshes(final JsonObject element, final Reading reading) {
        final Vector3f origin = ModelJson.vector(element, "origin").add(reading.shift);
        final Quaternionf turn = Rotations.ofEuler(ModelJson.vector(element, "rotation"));
        final Map<String, Vector3f> corners = new LinkedHashMap<>();
        if (element.has("vertices")) {
            for (final Map.Entry<String, JsonElement> entry : element.getAsJsonObject("vertices").entrySet()) {
                corners.put(entry.getKey(), turn.transform(ModelJson.vector(entry.getValue())).add(origin));
            }
        }
        // A mesh's faces can each be painted with a texture of their own: a mesh of the Core is one for each.
        final Map<Integer, MeshParts> byTexture = new LinkedHashMap<>();
        if (element.has("faces")) {
            for (final JsonElement each : element.getAsJsonObject("faces").asMap().values()) {
                final JsonObject face = each.getAsJsonObject();
                final int texture = Math.max(0, reading.texture(face.get("texture")));
                final MeshParts parts = byTexture.computeIfAbsent(texture, key -> new MeshParts());
                final List<String> keys = sorted(face.getAsJsonArray("vertices"), corners);
                final JsonObject faceUv = face.has("uv") ? face.getAsJsonObject("uv") : new JsonObject();
                final int first = parts.positions.size() / 3;
                for (final String key : keys) {
                    final Vector3f corner = corners.get(key);
                    final float[] uv = faceUv.has(key) ? ModelJson.floats(faceUv.get(key), 2) : new float[] {0, 0};
                    parts.positions.add(corner.x);
                    parts.positions.add(corner.y);
                    parts.positions.add(corner.z);
                    parts.uvs.add(uv[0] / reading.width(texture));
                    parts.uvs.add(uv[1] / reading.height(texture));
                }
                // A face of three corners is one triangle, of four two; Blockbench keeps no more.
                for (int i = 1; i + 1 < keys.size(); i++) {
                    parts.triangles.add(first);
                    parts.triangles.add(first + i);
                    parts.triangles.add(first + i + 1);
                }
            }
        }
        final List<ModelMesh> out = new ArrayList<>();
        byTexture.forEach((texture, parts) -> out.add(new ModelMesh(floats(parts.positions), new float[0],
                floats(parts.uvs), ints(parts.triangles), new int[0], new float[0], List.of(), texture)));
        return out;
    }

    /*
     * A face's corners in their order round it. Blockbench keeps a face of four corners in any order and walks it
     * round when it draws it, so a face whose fourth corner lies across a diagonal from another is walked the same
     * way here: past the line between two corners, seen from a third, is the corner opposite that third.
     */
    private static List<String> sorted(final JsonArray written, final Map<String, Vector3f> corners) {
        final List<String> keys = new ArrayList<>();
        for (final JsonElement key : written) {
            keys.add(key.getAsString());
        }
        if (keys.size() != 4 || !corners.keySet().containsAll(keys)) {
            return keys;
        }
        final Vector3f a = corners.get(keys.get(0));
        final Vector3f b = corners.get(keys.get(1));
        final Vector3f c = corners.get(keys.get(2));
        final Vector3f d = corners.get(keys.get(3));
        if (across(b, c, a, d)) {
            return List.of(keys.get(2), keys.get(0), keys.get(1), keys.get(3));
        }
        if (across(a, b, c, d)) {
            return List.of(keys.get(0), keys.get(2), keys.get(1), keys.get(3));
        }
        return keys;
    }

    /* Whether {@code check} lies on the other side of the line through {@code from} and {@code to} than {@code top}. */
    private static boolean across(final Vector3f from, final Vector3f to, final Vector3f top, final Vector3f check) {
        final Vector3f line = new Vector3f(to).sub(from);
        final float length = line.lengthSquared();
        final float along = length == 0 ? 0 : new Vector3f(top).sub(from).dot(line) / length;
        final Vector3f foot = new Vector3f(line).mul(along).add(from);
        final Vector3f away = foot.sub(top);
        return away.dot(new Vector3f(check).sub(to)) > 0;
    }

    private static List<ModelTexture> textures(final JsonObject file) {
        final List<ModelTexture> out = new ArrayList<>();
        if (!file.has("textures")) {
            return out;
        }
        for (final JsonElement each : file.getAsJsonArray("textures")) {
            final JsonObject texture = each.getAsJsonObject();
            final String source = ModelJson.string(texture, "source", "");
            final byte[] bytes = source.startsWith(DATA_PNG) ? Base64.getDecoder().decode(source.substring(
                    DATA_PNG.length())) : new byte[0];
            out.add(new ModelTexture(ModelJson.string(texture, "name", "texture"),
                    ModelJson.string(texture, "relative_path", ModelJson.string(texture, "path", "")), bytes,
                    (int) ModelJson.number(texture, "uv_width", 0), (int) ModelJson.number(texture, "uv_height", 0)));
        }
        return out;
    }

    private static ModelAnimation animation(final JsonObject animation, final Map<String, String> boneOfUuid) {
        final String loopName = ModelJson.string(animation, "loop", "once");
        final ModelAnimation.Loop loop = switch (loopName) {
            case "loop" -> ModelAnimation.Loop.LOOP;
            case "hold" -> ModelAnimation.Loop.HOLD;
            default -> ModelAnimation.Loop.ONCE;
        };
        final Map<String, ModelAnimation.Tracks> bones = new LinkedHashMap<>();
        final List<ModelAnimation.Event> events = new ArrayList<>();
        if (animation.has("animators")) {
            for (final Map.Entry<String, JsonElement> entry : animation.getAsJsonObject("animators").entrySet()) {
                final JsonObject animator = entry.getValue().getAsJsonObject();
                final Map<String, List<Keyframe>> byChannel = new HashMap<>();
                for (final JsonElement each : animator.getAsJsonArray("keyframes")) {
                    final JsonObject keyframe = each.getAsJsonObject();
                    final String channel = ModelJson.string(keyframe, "channel", "rotation");
                    final double time = ModelJson.number(keyframe, "time", 0);
                    switch (channel) {
                        case "sound" -> event(keyframe, time, ModelAnimation.EventKind.SOUND, events);
                        case "particle" -> event(keyframe, time, ModelAnimation.EventKind.PARTICLE, events);
                        case "timeline" -> event(keyframe, time, ModelAnimation.EventKind.INSTRUCTION, events);
                        default -> byChannel.computeIfAbsent(channel, key -> new ArrayList<>()).add(keyframe(keyframe,
                                time));
                    }
                }
                final String bone = boneOfUuid.getOrDefault(entry.getKey(), ModelJson.string(animator, "name",
                        entry.getKey()));
                if (!byChannel.isEmpty()) {
                    bones.put(bone, new ModelAnimation.Tracks(channel(byChannel.get("rotation"),
                            AnimationChannel.Kind.EULER), channel(byChannel.get("position"),
                            AnimationChannel.Kind.POSITION), channel(byChannel.get("scale"),
                            AnimationChannel.Kind.SCALE)));
                }
            }
        }
        return new ModelAnimation(ModelJson.string(animation, "name", "animation"),
                ModelJson.number(animation, "length", 0), loop, bones, Map.of(), events);
    }

    private static AnimationChannel channel(final List<Keyframe> keyframes, final AnimationChannel.Kind kind) {
        return keyframes == null || keyframes.isEmpty() ? null : new AnimationChannel(kind, keyframes);
    }

    private static Keyframe keyframe(final JsonObject keyframe, final double time) {
        final JsonArray points = keyframe.getAsJsonArray("data_points");
        final ModelExpression[] pre = point(points.get(0).getAsJsonObject());
        final ModelExpression[] post = points.size() > 1 ? point(points.get(1).getAsJsonObject()) : pre;
        final String interpolation = ModelJson.string(keyframe, "interpolation", "linear");
        final Easing easing = keyframe.has("easing") ? Easing.byName(ModelJson.string(keyframe, "easing", "linear"))
                : Easing.byName(interpolation);
        final double argument = keyframe.has("easingArgs") && keyframe.getAsJsonArray("easingArgs").size() > 0
                ? keyframe.getAsJsonArray("easingArgs").get(0).getAsDouble() : Double.NaN;
        float[] handles = new float[0];
        if (easing == Easing.BEZIER && keyframe.has("bezier_left_time")) {
            final float[] leftTime = ModelJson.floats(keyframe.get("bezier_left_time"), 3);
            final float[] leftValue = ModelJson.floats(keyframe.get("bezier_left_value"), 3);
            final float[] rightTime = ModelJson.floats(keyframe.get("bezier_right_time"), 3);
            final float[] rightValue = ModelJson.floats(keyframe.get("bezier_right_value"), 3);
            handles = new float[12];
            for (int i = 0; i < 3; i++) {
                handles[i * 4] = leftTime[i];
                handles[i * 4 + 1] = leftValue[i];
                handles[i * 4 + 2] = rightTime[i];
                handles[i * 4 + 3] = rightValue[i];
            }
        }
        return new Keyframe(time, pre, post, easing, argument, handles, new float[0]);
    }

    private static ModelExpression[] point(final JsonObject point) {
        final ModelExpression[] out = new ModelExpression[3];
        final String[] axes = {"x", "y", "z"};
        for (int i = 0; i < 3; i++) {
            final JsonElement value = point.get(axes[i]);
            out[i] = value == null ? ModelExpression.ZERO : value.getAsJsonPrimitive().isNumber()
                    ? ModelExpression.constant(value.getAsDouble()) : ExpressionParser.parse(value.getAsString());
        }
        return out;
    }

    private static void event(final JsonObject keyframe, final double time, final ModelAnimation.EventKind kind,
                              final List<ModelAnimation.Event> events) {
        for (final JsonElement each : keyframe.getAsJsonArray("data_points")) {
            final JsonObject point = each.getAsJsonObject();
            final String value = kind == ModelAnimation.EventKind.INSTRUCTION ? ModelJson.string(point, "script", "")
                    : ModelJson.string(point, "effect", "");
            events.add(new ModelAnimation.Event(time, kind, value, ModelJson.string(point, "locator", "")));
        }
    }

    private static String rootName(final List<ModelBone> bones) {
        for (final ModelBone bone : bones) {
            if (bone.name().equals(ROOT)) {
                return ROOT + "_elements";
            }
        }
        return ROOT;
    }

    private static float[] floats(final List<Float> values) {
        final float[] out = new float[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    private static int[] ints(final List<Integer> values) {
        final int[] out = new int[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    /*
     * What reading one file keeps: where the model is moved to, which texture each uuid is, and the size each
     * texture's pictures are counted on, Blockbench counting a mesh's in pixels where the Core counts from 0 to 1.
     */
    private record Reading(Vector3f shift, Map<String, Integer> textures, float defaultWidth, float defaultHeight,
                           List<ModelTexture> sizes) {

        float width(final int texture) {
            return texture < this.sizes.size() && this.sizes.get(texture).width() > 0
                    ? this.sizes.get(texture).width() : this.defaultWidth;
        }

        float height(final int texture) {
            return texture < this.sizes.size() && this.sizes.get(texture).height() > 0
                    ? this.sizes.get(texture).height() : this.defaultHeight;
        }

        /* The texture a face names, by its place or its uuid; -1 for a face of none. */
        int texture(final JsonElement named) {
            if (named == null || named.isJsonNull()) {
                return -1;
            }
            if (named.getAsJsonPrimitive().isNumber()) {
                return named.getAsInt();
            }
            if (named.getAsJsonPrimitive().isBoolean()) {
                return named.getAsBoolean() ? 0 : -1;
            }
            return this.textures.getOrDefault(named.getAsString(), 0);
        }
    }

    /* The corners, pictures and triangles of the faces of a mesh painted with one texture. */
    private record MeshParts(List<Float> positions, List<Float> uvs, List<Integer> triangles) {

        MeshParts() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
    }
}
