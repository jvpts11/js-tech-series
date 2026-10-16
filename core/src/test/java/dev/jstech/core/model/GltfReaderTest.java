/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class GltfReaderTest {

    private static final double EPSILON = 1.0e-4;
    private static final int FLOAT = 5126;
    private static final int SHORT = 5123;

    @Test
    void read_countsMetresAsPixels() {
        final Builder gltf = triangle();
        final ModelData model = GltfReader.read("part", gltf.text(), file -> null);
        final ModelBone body = model.bone("body").orElseThrow();
        assertEquals(16, body.translation().x(), EPSILON, "a metre is a block, sixteen pixels");
        assertEquals(8, body.translation().z(), EPSILON);
        final ModelMesh mesh = body.meshes().getFirst();
        assertEquals(16, mesh.positions()[3], EPSILON);
        assertArrayEquals(new int[] {0, 1, 2}, mesh.triangles());
    }

    @Test
    void read_readsTheBinaryFormAsTheText() {
        final Builder gltf = triangle();
        final ModelData text = GltfReader.read("part", gltf.text(), file -> null);
        final ModelData binary = GltfReader.read("part", gltf.binary(), file -> null);
        assertArrayEquals(text.bone("body").orElseThrow().meshes().getFirst().positions(),
                binary.bone("body").orElseThrow().meshes().getFirst().positions());
    }

    @Test
    void read_findsABufferBesideTheFile() {
        final Builder gltf = triangle();
        final byte[] bin = gltf.data();
        gltf.root.getAsJsonArray("buffers").get(0).getAsJsonObject().addProperty("uri", "part.bin");
        final ModelData model = GltfReader.read("part", gltf.json(), file -> file.equals("part.bin") ? bin : null);
        assertEquals(3, model.bone("body").orElseThrow().meshes().getFirst().corners());
        assertThrows(ModelFormatException.class, () -> GltfReader.read("part", gltf.json(), file -> null),
                "a buffer named but not there");
    }

    @Test
    void animation_movesFromTheRestPlaceAndTurnsOutright() {
        final Builder gltf = triangle();
        final int times = gltf.accessor(gltf.view(floats(0, 1)), FLOAT, 2, "SCALAR");
        final int places = gltf.accessor(gltf.view(floats(1, 0, 0.5F, 3, 0, 0.5F)), FLOAT, 2, "VEC3");
        final int turns = gltf.accessor(gltf.view(floats(0, 0, 0, 1, 0, 0.7071068F, 0, 0.7071068F)), FLOAT, 2,
                "VEC4");
        final JsonObject animation = JsonParser.parseString("""
                {"name": "slide", "samplers": [
                   {"input": %d, "output": %d, "interpolation": "LINEAR"},
                   {"input": %d, "output": %d, "interpolation": "STEP"}],
                 "channels": [{"sampler": 0, "target": {"node": 0, "path": "translation"}},
                              {"sampler": 1, "target": {"node": 0, "path": "rotation"}}]}
                """.formatted(times, places, times, turns)).getAsJsonObject();
        gltf.root.add("animations", array(animation));
        final ModelAnimation slide = GltfReader.read("part", gltf.text(), file -> null).animations().getFirst();
        assertEquals(1, slide.length(), EPSILON);
        final float[] out = new float[4];
        slide.bones().get("body").position().evaluate(1, new ExpressionContext(), out);
        assertEquals(48 - 16, out[0], EPSILON, "two metres on from its rest place");
        slide.bones().get("body").rotation().evaluate(0.5, new ExpressionContext(), out);
        assertEquals(1, out[3], EPSILON, "a stepped turn holds until its next keyframe");
    }

    @Test
    void read_breaksStripsAndFansIntoTriangles() {
        final Builder gltf = new Builder();
        final int square = gltf.accessor(gltf.view(floats(0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0)), FLOAT, 4, "VEC3");
        gltf.root.add("meshes", array(JsonParser.parseString("""
                {"primitives": [{"attributes": {"POSITION": %d}, "mode": 5},
                                {"attributes": {"POSITION": %d}, "mode": 6},
                                {"attributes": {"POSITION": %d}, "mode": 1}]}
                """.formatted(square, square, square)).getAsJsonObject()));
        gltf.root.add("nodes", array(JsonParser.parseString("{\"name\": \"plate\", \"mesh\": 0}").getAsJsonObject()));
        final ModelBone plate = GltfReader.read("plate", gltf.text(), file -> null).bone("plate").orElseThrow();
        assertEquals(2, plate.meshes().size(), "lines draw nothing");
        assertArrayEquals(new int[] {0, 1, 2, 2, 1, 3}, plate.meshes().get(0).triangles(),
                "every other triangle of a strip turned to face the same way");
        assertArrayEquals(new int[] {0, 1, 2, 0, 2, 3}, plate.meshes().get(1).triangles());
    }

    @Test
    void read_readsSparseMorphTargetsByTheirNames() {
        final Builder gltf = new Builder();
        final int corners = gltf.accessor(gltf.view(floats(0, 0, 0, 1, 0, 0, 0, 1, 0)), FLOAT, 3, "VEC3");
        final int index = gltf.view(shorts(2));
        final int value = gltf.view(floats(0, 0, 1));
        final JsonObject moves = JsonParser.parseString("""
                {"componentType": %d, "count": 3, "type": "VEC3", "sparse": {"count": 1,
                  "indices": {"bufferView": %d, "componentType": %d},
                  "values": {"bufferView": %d}}}
                """.formatted(FLOAT, index, SHORT, value)).getAsJsonObject();
        gltf.root.getAsJsonArray("accessors").add(moves);
        final int target = gltf.root.getAsJsonArray("accessors").size() - 1;
        gltf.root.add("meshes", array(JsonParser.parseString("""
                {"primitives": [{"attributes": {"POSITION": %d}, "targets": [{"POSITION": %d}]}],
                 "extras": {"targetNames": ["smile"]}}
                """.formatted(corners, target)).getAsJsonObject()));
        gltf.root.add("nodes", array(JsonParser.parseString("{\"name\": \"face\", \"mesh\": 0}").getAsJsonObject()));
        final ModelMesh face = GltfReader.read("face", gltf.text(), file -> null).bone("face").orElseThrow().meshes()
                .getFirst();
        final ModelMesh.MorphTarget smile = face.morphs().getFirst();
        assertEquals("smile", smile.name());
        assertArrayEquals(new float[] {0, 0, 0, 0, 0, 0, 0, 0, 16}, smile.moves(), (float) EPSILON,
                "only the corner the target names moves, a metre a block");
    }

    /* A model of one triangle on a node moved a metre east and half a metre south. */
    private static Builder triangle() {
        final Builder gltf = new Builder();
        final int positions = gltf.accessor(gltf.view(floats(0, 0, 0, 1, 0, 0, 0, 1, 0)), FLOAT, 3, "VEC3");
        final int indices = gltf.accessor(gltf.view(shorts(0, 1, 2)), SHORT, 3, "SCALAR");
        gltf.root.add("meshes", array(JsonParser.parseString("""
                {"primitives": [{"attributes": {"POSITION": %d}, "indices": %d}]}
                """.formatted(positions, indices)).getAsJsonObject()));
        gltf.root.add("nodes", array(JsonParser.parseString("""
                {"name": "body", "mesh": 0, "translation": [1, 0, 0.5]}
                """).getAsJsonObject()));
        return gltf;
    }

    private static byte[] floats(final float... values) {
        final ByteBuffer out = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (final float value : values) {
            out.putFloat(value);
        }
        return out.array();
    }

    private static byte[] shorts(final int... values) {
        final ByteBuffer out = ByteBuffer.allocate(values.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (final int value : values) {
            out.putShort((short) value);
        }
        return out.array();
    }

    private static JsonArray array(final JsonObject only) {
        final JsonArray out = new JsonArray();
        out.add(only);
        return out;
    }

    /* A glTF file being written: its JSON and the one buffer its views are laid in, each on four bytes. */
    private static final class Builder {

        private final JsonObject root = new JsonObject();
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Builder() {
            this.root.add("asset", JsonParser.parseString("{\"version\": \"2.0\"}"));
            this.root.add("buffers", new JsonArray());
            this.root.getAsJsonArray("buffers").add(new JsonObject());
            this.root.add("bufferViews", new JsonArray());
            this.root.add("accessors", new JsonArray());
        }

        int view(final byte[] bytes) {
            final JsonObject view = new JsonObject();
            view.addProperty("buffer", 0);
            view.addProperty("byteOffset", this.buffer.size());
            view.addProperty("byteLength", bytes.length);
            this.buffer.writeBytes(bytes);
            while (this.buffer.size() % 4 != 0) {
                this.buffer.write(0);
            }
            this.root.getAsJsonArray("bufferViews").add(view);
            return this.root.getAsJsonArray("bufferViews").size() - 1;
        }

        int accessor(final int view, final int componentType, final int count, final String type) {
            final JsonObject accessor = new JsonObject();
            accessor.addProperty("bufferView", view);
            accessor.addProperty("componentType", componentType);
            accessor.addProperty("count", count);
            accessor.addProperty("type", type);
            this.root.getAsJsonArray("accessors").add(accessor);
            return this.root.getAsJsonArray("accessors").size() - 1;
        }

        byte[] data() {
            return this.buffer.toByteArray();
        }

        /* The text form, its buffer inside it. */
        byte[] text() {
            final JsonObject buffer = this.root.getAsJsonArray("buffers").get(0).getAsJsonObject();
            buffer.addProperty("byteLength", this.buffer.size());
            buffer.addProperty("uri", "data:application/octet-stream;base64,"
                    + Base64.getEncoder().encodeToString(data()));
            return json();
        }

        byte[] json() {
            return this.root.toString().getBytes(StandardCharsets.UTF_8);
        }

        /* The binary form: header, JSON chunk, binary chunk. */
        byte[] binary() {
            final JsonObject buffer = this.root.getAsJsonArray("buffers").get(0).getAsJsonObject();
            buffer.remove("uri");
            buffer.addProperty("byteLength", this.buffer.size());
            byte[] json = json();
            json = pad(json, (byte) ' ');
            final byte[] bin = pad(data(), (byte) 0);
            final ByteBuffer out = ByteBuffer.allocate(12 + 8 + json.length + 8 + bin.length)
                    .order(ByteOrder.LITTLE_ENDIAN);
            out.putInt(0x46546C67).putInt(2).putInt(out.capacity());
            out.putInt(json.length).putInt(0x4E4F534A).put(json);
            out.putInt(bin.length).putInt(0x004E4942).put(bin);
            return out.array();
        }

        private static byte[] pad(final byte[] bytes, final byte with) {
            final byte[] out = new byte[(bytes.length + 3) / 4 * 4];
            System.arraycopy(bytes, 0, out, 0, bytes.length);
            for (int i = bytes.length; i < out.length; i++) {
                out[i] = with;
            }
            return out;
        }
    }
}
