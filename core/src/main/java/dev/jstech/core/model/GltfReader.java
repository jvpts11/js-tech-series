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
import com.google.gson.JsonParser;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Reads glTF 2.0, the format Blender writes natively and Maya, 3ds Max and Cinema 4D write with an exporter, in its
 * text form ({@code .gltf}, its buffers beside it or inside it) and its binary one ({@code .glb}): the nodes as bones,
 * their meshes with normals, pictures, skins and morph targets, the textures of their materials, and the animations,
 * stepped, straight or along cubic splines. glTF is right-handed with Y up, as the game is, so nothing turns round;
 * but it counts in metres and the Core in pixels, sixteen to a metre, a block being a metre. Pure.
 */
public final class GltfReader {

    private final String name;
    private final JsonObject root;
    private final List<byte[]> buffers = new ArrayList<>();
    private final List<String> nodeNames = new ArrayList<>();

    private static final int GLB_MAGIC = 0x46546C67;
    private static final int JSON_CHUNK = 0x4E4F534A;
    private static final int BIN_CHUNK = 0x004E4942;
    private static final float PIXELS = 16.0F;
    private static final String DATA = "data:";
    private static final String BASE64 = ";base64,";
    private static final String CUBIC = "CUBICSPLINE";
    private static final int TRIANGLES = 4;
    private static final int STRIP = 5;
    private static final int FAN = 6;

    private GltfReader(final String name, final JsonObject root, final byte[] binary,
                       final Function<String, byte[]> beside) {
        this.name = name;
        this.root = root;
        for (final JsonElement each : array(root, "buffers")) {
            final JsonObject buffer = each.getAsJsonObject();
            this.buffers.add(buffer.has("uri") ? load(buffer.get("uri").getAsString(), beside) : binary);
        }
    }

    /**
     * The model a glTF file holds, in either form.
     *
     * @param name   what the model is called
     * @param bytes  the file
     * @param beside the bytes of a file the model names beside it, by the name it writes; null for one not there
     * @throws ModelFormatException when the file is not one
     */
    public static ModelData read(final String name, final byte[] bytes, final Function<String, byte[]> beside) {
        final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (bytes.length >= 12 && buffer.getInt(0) == GLB_MAGIC) {
            return binary(name, buffer, beside);
        }
        final JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        return new GltfReader(name, root, new byte[0], beside).model();
    }

    /* The binary form: a header, then the JSON chunk, then the buffer it reads from. */
    private static ModelData binary(final String name, final ByteBuffer file, final Function<String, byte[]> beside) {
        int at = 12;
        JsonObject root = null;
        byte[] binary = new byte[0];
        while (at + 8 <= file.limit()) {
            final int length = file.getInt(at);
            final int type = file.getInt(at + 4);
            if (length < 0 || at + 8 + length > file.limit()) {
                throw new ModelFormatException("the glTF file " + name + " has a chunk longer than the file");
            }
            final byte[] chunk = new byte[length];
            file.get(at + 8, chunk);
            if (type == JSON_CHUNK) {
                root = JsonParser.parseString(new String(chunk, StandardCharsets.UTF_8)).getAsJsonObject();
            } else if (type == BIN_CHUNK) {
                binary = chunk;
            }
            at += 8 + length;
        }
        if (root == null) {
            throw new ModelFormatException("the glTF file " + name + " has no JSON");
        }
        return new GltfReader(name, root, binary, beside).model();
    }

    private ModelData model() {
        final JsonArray nodes = array(this.root, "nodes");
        for (int i = 0; i < nodes.size(); i++) {
            final String given = ModelJson.string(nodes.get(i).getAsJsonObject(), "name", "node_" + i);
            this.nodeNames.add(this.nodeNames.contains(given) ? given + "_" + i : given);
        }
        final Map<Integer, Integer> parentOf = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            for (final JsonElement child : array(nodes.get(i).getAsJsonObject(), "children")) {
                parentOf.put(child.getAsInt(), i);
            }
        }
        final List<ModelBone> bones = new ArrayList<>();
        final Map<String, ModelSkin> skins = new LinkedHashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            final JsonObject node = nodes.get(i).getAsJsonObject();
            final List<ModelMesh> meshes = node.has("mesh") ? meshes(node.get("mesh").getAsInt()) : List.of();
            if (node.has("skin")) {
                skins.put(this.nodeNames.get(i), skin(node.get("skin").getAsInt()));
            }
            final Integer parent = parentOf.get(i);
            final Transform rest = transform(node);
            bones.add(new ModelBone(this.nodeNames.get(i), parent == null ? null : this.nodeNames.get(parent),
                    new Vector3f(), rest.rotation, new Vector3f(), rest.translation, rest.scale, List.of(), meshes,
                    List.of(), false));
        }
        final List<ModelAnimation> animations = new ArrayList<>();
        int index = 0;
        for (final JsonElement each : array(this.root, "animations")) {
            animations.add(animation(each.getAsJsonObject(), index++, nodes));
        }
        return new ModelData(this.name, 1, 1, bones, skins, textures(), animations);
    }

    private List<ModelMesh> meshes(final int index) {
        final JsonObject mesh = array(this.root, "meshes").get(index).getAsJsonObject();
        final List<String> targetNames = targetNames(mesh);
        final List<ModelMesh> out = new ArrayList<>();
        for (final JsonElement each : array(mesh, "primitives")) {
            final JsonObject primitive = each.getAsJsonObject();
            final JsonObject attributes = primitive.getAsJsonObject("attributes");
            final int mode = (int) ModelJson.number(primitive, "mode", TRIANGLES);
            if (mode < TRIANGLES || !attributes.has("POSITION")) {
                // Points and lines draw nothing of a model.
                continue;
            }
            final float[] positions = scaled(floats(attributes.get("POSITION").getAsInt()));
            final int corners = positions.length / 3;
            final float[] normals = attributes.has("NORMAL") ? floats(attributes.get("NORMAL").getAsInt())
                    : new float[0];
            final float[] uvs = attributes.has("TEXCOORD_0") ? floats(attributes.get("TEXCOORD_0").getAsInt())
                    : new float[corners * 2];
            final int[] order = primitive.has("indices") ? ints(primitive.get("indices").getAsInt())
                    : sequence(corners);
            final int[] joints = attributes.has("JOINTS_0") ? ints(attributes.get("JOINTS_0").getAsInt())
                    : new int[0];
            final float[] weights = attributes.has("WEIGHTS_0") ? floats(attributes.get("WEIGHTS_0").getAsInt())
                    : new float[0];
            final List<ModelMesh.MorphTarget> morphs = new ArrayList<>();
            int target = 0;
            for (final JsonElement morph : array(primitive, "targets")) {
                final JsonObject moves = morph.getAsJsonObject();
                final String targetName = target < targetNames.size() ? targetNames.get(target) : "target_" + target;
                morphs.add(new ModelMesh.MorphTarget(targetName, moves.has("POSITION")
                        ? scaled(floats(moves.get("POSITION").getAsInt())) : new float[corners * 3]));
                target++;
            }
            out.add(new ModelMesh(positions, normals, uvs, triangles(mode, order), joints, weights, morphs,
                    texture(primitive)));
        }
        return out;
    }

    /* The names Blender and most exporters give a mesh's morph targets, in the extras glTF leaves for it. */
    private static List<String> targetNames(final JsonObject mesh) {
        final List<String> out = new ArrayList<>();
        if (mesh.has("extras") && mesh.get("extras").isJsonObject()) {
            for (final JsonElement each : array(mesh.getAsJsonObject("extras"), "targetNames")) {
                out.add(each.getAsString());
            }
        }
        return out;
    }

    /* A primitive's corners as separate triangles, whether it lists them so, as a strip or as a fan. */
    private static int[] triangles(final int mode, final int[] order) {
        if (mode == TRIANGLES) {
            return order.length % 3 == 0 ? order : Arrays.copyOf(order, order.length - order.length % 3);
        }
        final int count = Math.max(0, order.length - 2);
        final int[] out = new int[count * 3];
        for (int i = 0; i < count; i++) {
            if (mode == FAN) {
                out[i * 3] = order[0];
                out[i * 3 + 1] = order[i + 1];
                out[i * 3 + 2] = order[i + 2];
            } else if (mode == STRIP) {
                // Every other triangle of a strip runs the other way round; swapping two corners keeps it facing out.
                final boolean odd = i % 2 == 1;
                out[i * 3] = order[odd ? i + 1 : i];
                out[i * 3 + 1] = order[odd ? i : i + 1];
                out[i * 3 + 2] = order[i + 2];
            }
        }
        return out;
    }

    /* The image of a primitive's material's base colour, by its place among the model's textures. */
    private int texture(final JsonObject primitive) {
        if (!primitive.has("material")) {
            return 0;
        }
        final JsonObject material = array(this.root, "materials").get(primitive.get("material").getAsInt())
                .getAsJsonObject();
        if (!material.has("pbrMetallicRoughness")) {
            return 0;
        }
        final JsonObject pbr = material.getAsJsonObject("pbrMetallicRoughness");
        if (!pbr.has("baseColorTexture")) {
            return 0;
        }
        final int texture = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();
        final JsonObject chosen = array(this.root, "textures").get(texture).getAsJsonObject();
        return (int) ModelJson.number(chosen, "source", 0);
    }

    private ModelSkin skin(final int index) {
        final JsonObject skin = array(this.root, "skins").get(index).getAsJsonObject();
        final List<String> joints = new ArrayList<>();
        for (final JsonElement joint : array(skin, "joints")) {
            joints.add(this.nodeNames.get(joint.getAsInt()));
        }
        final float[] binds = skin.has("inverseBindMatrices") ? floats(skin.get("inverseBindMatrices").getAsInt())
                : new float[0];
        // The space is counted sixteen times larger: each matrix's move, its last column, grows with it.
        for (int i = 0; i + 15 < binds.length; i += 16) {
            binds[i + 12] *= PIXELS;
            binds[i + 13] *= PIXELS;
            binds[i + 14] *= PIXELS;
        }
        return new ModelSkin(joints, binds);
    }

    private List<ModelTexture> textures() {
        final List<ModelTexture> out = new ArrayList<>();
        int index = 0;
        for (final JsonElement each : array(this.root, "images")) {
            final JsonObject image = each.getAsJsonObject();
            final String imageName = ModelJson.string(image, "name", "image_" + index);
            if (image.has("bufferView")) {
                out.add(new ModelTexture(imageName, "", view(image.get("bufferView").getAsInt()), 0, 0));
            } else {
                final String uri = ModelJson.string(image, "uri", "");
                out.add(uri.startsWith(DATA) ? new ModelTexture(imageName, "", dataUri(uri), 0, 0)
                        : ModelTexture.inFile(imageName, uri));
            }
            index++;
        }
        return out;
    }

    private ModelAnimation animation(final JsonObject animation, final int index, final JsonArray nodes) {
        final JsonArray samplers = array(animation, "samplers");
        final Map<String, AnimationChannel[]> byBone = new LinkedHashMap<>();
        final Map<String, AnimationChannel> weights = new LinkedHashMap<>();
        double length = 0;
        for (final JsonElement each : array(animation, "channels")) {
            final JsonObject channel = each.getAsJsonObject();
            final JsonObject target = channel.getAsJsonObject("target");
            if (!target.has("node")) {
                continue;
            }
            final int node = target.get("node").getAsInt();
            final JsonObject sampler = samplers.get(channel.get("sampler").getAsInt()).getAsJsonObject();
            final float[] times = floats(sampler.get("input").getAsInt());
            final float[] values = floats(sampler.get("output").getAsInt());
            final String interpolation = ModelJson.string(sampler, "interpolation", "LINEAR");
            final Transform rest = transform(nodes.get(node).getAsJsonObject());
            for (final float time : times) {
                length = Math.max(length, time);
            }
            final AnimationChannel[] tracks = byBone.computeIfAbsent(this.nodeNames.get(node),
                    key -> new AnimationChannel[3]);
            switch (target.get("path").getAsString()) {
                case "rotation" -> tracks[0] = channel(AnimationChannel.Kind.QUATERNION, times, values,
                        interpolation, new float[] {1, 1, 1, 1}, new float[4]);
                // An animation of glTF puts a node at a place; the Core's moves it from its rest place.
                case "translation" -> tracks[1] = channel(AnimationChannel.Kind.POSITION, times, values,
                        interpolation, new float[] {PIXELS, PIXELS, PIXELS}, new float[] {rest.translation.x,
                                rest.translation.y, rest.translation.z});
                // And sizes it outright, where the Core's multiplies its rest size.
                case "scale" -> tracks[2] = channel(AnimationChannel.Kind.SCALE, times, values, interpolation,
                        new float[] {inverse(rest.scale.x), inverse(rest.scale.y), inverse(rest.scale.z)},
                        new float[3]);
                case "weights" -> weights.putAll(morphWeights(nodes.get(node).getAsJsonObject(), times, values,
                        interpolation));
                default -> {
                    // A path of an extension this reader does not know, as a pointer to a material, is let go.
                }
            }
        }
        final Map<String, ModelAnimation.Tracks> bones = new LinkedHashMap<>();
        byBone.forEach((bone, tracks) -> bones.put(bone, new ModelAnimation.Tracks(tracks[0], tracks[1], tracks[2])));
        return new ModelAnimation(ModelJson.string(animation, "name", "animation_" + index), length,
                ModelAnimation.Loop.LOOP, bones, weights, List.of());
    }

    /* The weights of a node's mesh's morph targets, one channel each, by the targets' names. */
    private Map<String, AnimationChannel> morphWeights(final JsonObject node, final float[] times,
                                                       final float[] values, final String interpolation) {
        final Map<String, AnimationChannel> out = new LinkedHashMap<>();
        if (!node.has("mesh")) {
            return out;
        }
        final JsonObject mesh = array(this.root, "meshes").get(node.get("mesh").getAsInt()).getAsJsonObject();
        final JsonArray primitives = array(mesh, "primitives");
        final int count = primitives.isEmpty() ? 0 : array(primitives.get(0).getAsJsonObject(), "targets").size();
        final List<String> names = targetNames(mesh);
        final boolean cubic = CUBIC.equals(interpolation);
        for (int target = 0; target < count; target++) {
            final List<Keyframe> keyframes = new ArrayList<>();
            for (int k = 0; k < times.length; k++) {
                final int at = cubic ? (k * 3 + 1) * count + target : k * count + target;
                final ModelExpression[] value = {ModelExpression.constant(values[at])};
                final float[] tangents = cubic ? new float[] {values[k * 3 * count + target],
                        values[(k * 3 + 2) * count + target]} : new float[0];
                keyframes.add(new Keyframe(times[k], value, value, easing(interpolation), Double.NaN, new float[0],
                        tangents));
            }
            out.put(target < names.size() ? names.get(target) : "target_" + target,
                    new AnimationChannel(AnimationChannel.Kind.WEIGHT, keyframes));
        }
        return out;
    }

    /*
     * A channel of keyframes from a sampler's times and values, each number made the Core's as number * factor -
     * offset; a cubic spline's tangents, the rates the numbers change at, only by the factor.
     */
    private static AnimationChannel channel(final AnimationChannel.Kind kind, final float[] times,
                                            final float[] values, final String interpolation, final float[] factor,
                                            final float[] offset) {
        final int numbers = kind.numbers();
        final boolean cubic = CUBIC.equals(interpolation);
        final List<Keyframe> keyframes = new ArrayList<>();
        for (int k = 0; k < times.length; k++) {
            final int at = cubic ? (k * 3 + 1) * numbers : k * numbers;
            final ModelExpression[] expressions = new ModelExpression[numbers];
            for (int i = 0; i < numbers; i++) {
                expressions[i] = ModelExpression.constant(values[at + i] * factor[i] - offset[i]);
            }
            float[] tangents = new float[0];
            if (cubic) {
                tangents = new float[numbers * 2];
                for (int i = 0; i < numbers; i++) {
                    tangents[i * 2] = values[k * 3 * numbers + i] * factor[i];
                    tangents[i * 2 + 1] = values[(k * 3 + 2) * numbers + i] * factor[i];
                }
            }
            keyframes.add(new Keyframe(times[k], expressions, expressions, easing(interpolation), Double.NaN,
                    new float[0], tangents));
        }
        return new AnimationChannel(kind, keyframes);
    }

    private static Easing easing(final String interpolation) {
        return "STEP".equals(interpolation) ? Easing.STEP : Easing.LINEAR;
    }

    /* A node's place, turn and size, from its matrix or its parts; its place in pixels. */
    private static Transform transform(final JsonObject node) {
        final Transform out = new Transform(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1));
        if (node.has("matrix")) {
            final Matrix4f matrix = new Matrix4f().set(ModelJson.floats(node.get("matrix"), 16));
            matrix.getTranslation(out.translation);
            matrix.getNormalizedRotation(out.rotation);
            matrix.getScale(out.scale);
        } else {
            if (node.has("translation")) {
                out.translation.set(ModelJson.vector(node.get("translation")));
            }
            if (node.has("rotation")) {
                final float[] q = ModelJson.floats(node.get("rotation"), 4);
                out.rotation.set(q[0], q[1], q[2], q[3]).normalize();
            }
            if (node.has("scale")) {
                out.scale.set(ModelJson.vector(node.get("scale")));
            }
        }
        out.translation.mul(PIXELS);
        return out;
    }

    /* The numbers of an accessor, each as a float, its normalised integers made into fractions. */
    private float[] floats(final int index) {
        final Accessor accessor = accessor(index);
        final int components = accessor.components;
        final float[] out = new float[accessor.count * components];
        for (int i = 0; i < accessor.count; i++) {
            for (int c = 0; c < components; c++) {
                out[i * components + c] = accessor.read(i, c);
            }
        }
        sparse(index, accessor, (element, given, at) -> {
            for (int c = 0; c < components; c++) {
                out[element * components + c] = given.read(at, c);
            }
        });
        return out;
    }

    private int[] ints(final int index) {
        final Accessor accessor = accessor(index);
        final int components = accessor.components;
        final int[] out = new int[accessor.count * components];
        for (int i = 0; i < accessor.count; i++) {
            for (int c = 0; c < components; c++) {
                out[i * components + c] = accessor.readInt(i, c);
            }
        }
        sparse(index, accessor, (element, given, at) -> {
            for (int c = 0; c < components; c++) {
                out[element * components + c] = given.readInt(at, c);
            }
        });
        return out;
    }

    /*
     * A sparse accessor, as exporters write morph targets that move few corners: the elements it names take the
     * values it gives in place of those of its view, or of zeros where it has none.
     */
    private void sparse(final int index, final Accessor dense, final SparseWrite write) {
        final JsonObject accessor = array(this.root, "accessors").get(index).getAsJsonObject();
        if (!accessor.has("sparse")) {
            return;
        }
        final JsonObject sparse = accessor.getAsJsonObject("sparse");
        final int count = sparse.get("count").getAsInt();
        final JsonObject indices = sparse.getAsJsonObject("indices");
        final Accessor at = viewAccessor(indices, count, 1, indices.get("componentType").getAsInt(), false);
        final Accessor given = viewAccessor(sparse.getAsJsonObject("values"), count, dense.components, dense.type,
                dense.normalized);
        for (int i = 0; i < count; i++) {
            final int element = at.readInt(i, 0);
            if (element >= 0 && element < dense.count) {
                write.put(element, given, i);
            }
        }
    }

    private Accessor accessor(final int index) {
        final JsonObject accessor = array(this.root, "accessors").get(index).getAsJsonObject();
        final int count = accessor.get("count").getAsInt();
        final int components = switch (accessor.get("type").getAsString()) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4", "MAT2" -> 4;
            case "MAT3" -> 9;
            case "MAT4" -> 16;
            default -> throw new ModelFormatException("the glTF file " + this.name + " has an accessor of no type");
        };
        final int type = accessor.get("componentType").getAsInt();
        final boolean normalized = ModelJson.flag(accessor, "normalized", false);
        if (!accessor.has("bufferView")) {
            return new Accessor(ByteBuffer.allocate(0), 0, 0, count, components, type, normalized, true);
        }
        return viewAccessor(accessor, count, components, type, normalized);
    }

    /* An accessor reading {@code count} elements from the view an object names, at the offset it gives. */
    private Accessor viewAccessor(final JsonObject object, final int count, final int components, final int type,
                                  final boolean normalized) {
        final JsonObject view = array(this.root, "bufferViews").get(object.get("bufferView").getAsInt())
                .getAsJsonObject();
        final byte[] buffer = this.buffers.get(view.get("buffer").getAsInt());
        final int offset = (int) ModelJson.number(view, "byteOffset", 0) + (int) ModelJson.number(object,
                "byteOffset", 0);
        final int stride = view.has("byteStride") ? view.get("byteStride").getAsInt() : size(type) * components;
        return new Accessor(ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN), offset, stride, count,
                components, type, normalized, false);
    }

    private byte[] view(final int index) {
        final JsonObject view = array(this.root, "bufferViews").get(index).getAsJsonObject();
        final byte[] buffer = this.buffers.get(view.get("buffer").getAsInt());
        final int offset = (int) ModelJson.number(view, "byteOffset", 0);
        final int length = view.get("byteLength").getAsInt();
        return Arrays.copyOfRange(buffer, offset, offset + length);
    }

    private byte[] load(final String uri, final Function<String, byte[]> beside) {
        if (uri.startsWith(DATA)) {
            return dataUri(uri);
        }
        final byte[] found = beside.apply(uri);
        if (found == null) {
            throw new ModelFormatException("the glTF file " + this.name + " names " + uri + ", not beside it");
        }
        return found;
    }

    private static JsonArray array(final JsonObject object, final String key) {
        return object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray();
    }

    private static byte[] dataUri(final String uri) {
        final int at = uri.indexOf(BASE64);
        return at < 0 ? new byte[0] : Base64.getDecoder().decode(uri.substring(at + BASE64.length()));
    }

    private static int size(final int type) {
        return switch (type) {
            case 5120, 5121 -> 1;
            case 5122, 5123 -> 2;
            default -> 4;
        };
    }

    private static float inverse(final float value) {
        return value == 0 ? 1 : 1 / value;
    }

    private static float[] scaled(final float[] positions) {
        for (int i = 0; i < positions.length; i++) {
            positions[i] *= PIXELS;
        }
        return positions;
    }

    private static int[] sequence(final int count) {
        final int[] out = new int[count];
        for (int i = 0; i < count; i++) {
            out[i] = i;
        }
        return out;
    }

    /* Puts the {@code at}th element of a sparse accessor's values in place of a dense one's {@code element}th. */
    @FunctionalInterface
    private interface SparseWrite {
        void put(int element, Accessor given, int at);
    }

    private record Transform(Vector3f translation, Quaternionf rotation, Vector3f scale) {
    }

    /* Where an accessor's numbers are in its buffer, and how each is written. */
    private record Accessor(ByteBuffer buffer, int offset, int stride, int count, int components, int type,
                            boolean normalized, boolean zero) {

        float read(final int element, final int component) {
            if (this.zero) {
                return 0;
            }
            final int at = this.offset + element * this.stride + component * size(this.type);
            return switch (this.type) {
                case 5126 -> this.buffer.getFloat(at);
                case 5120 -> this.normalized ? Math.max(this.buffer.get(at) / 127.0F, -1) : this.buffer.get(at);
                case 5121 -> this.normalized ? (this.buffer.get(at) & 0xFF) / 255.0F : this.buffer.get(at) & 0xFF;
                case 5122 -> this.normalized ? Math.max(this.buffer.getShort(at) / 32767.0F, -1)
                        : this.buffer.getShort(at);
                case 5123 -> this.normalized ? (this.buffer.getShort(at) & 0xFFFF) / 65535.0F
                        : this.buffer.getShort(at) & 0xFFFF;
                default -> Integer.toUnsignedLong(this.buffer.getInt(at));
            };
        }

        int readInt(final int element, final int component) {
            if (this.zero) {
                return 0;
            }
            final int at = this.offset + element * this.stride + component * size(this.type);
            return switch (this.type) {
                case 5120 -> this.buffer.get(at);
                case 5121 -> this.buffer.get(at) & 0xFF;
                case 5122 -> this.buffer.getShort(at);
                case 5123 -> this.buffer.getShort(at) & 0xFFFF;
                case 5126 -> (int) this.buffer.getFloat(at);
                default -> this.buffer.getInt(at);
            };
        }
    }
}
