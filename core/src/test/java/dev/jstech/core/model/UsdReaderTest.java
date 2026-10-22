/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.font.PngWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class UsdReaderTest {

    private static final double EPSILON = 1.0e-4;
    /* A centimetre, USD's unit unless a stage says another, in the game's pixels. */
    private static final float CENTIMETRE = 16.0F / 100.0F;
    private static final String PUMP = """
            #usda 1.0
            (
                defaultPrim = "Pump"
                metersPerUnit = 0.01
                upAxis = "Z"
                startTimeCode = 0
                endTimeCode = 24
                timeCodesPerSecond = 24
            )

            def Xform "Pump" (
                kind = "component"
            )
            {
                def Mesh "Housing"
                {
                    point3f[] points = [(0, 0, 0), (20, 0, 0), (20, 10, 0), (0, 10, 0)]
                    int[] faceVertexCounts = [4]
                    int[] faceVertexIndices = [0, 1, 2, 3]
                    color3f[] primvars:displayColor = [(1, 0, 0)] (
                        interpolation = "constant"
                    )
                }

                def Xform "Rotor"
                {
                    double3 xformOp:translate = (10, 5, 10)
                    float xformOp:rotateZ.timeSamples = {
                        0: 0,
                        24: 360,
                    }
                    uniform token[] xformOpOrder = ["xformOp:translate", "xformOp:rotateZ"]

                    def Mesh "Blade"
                    {
                        point3f[] points = [(-4, -1, 0), (4, -1, 0), (4, 1, 0), (-4, 1, 0)]
                        int[] faceVertexCounts = [4]
                        int[] faceVertexIndices = [0, 1, 2, 3]
                        texCoord2f[] primvars:st = [(0, 0), (1, 0), (1, 1), (0, 1)] (
                            interpolation = "faceVarying"
                        )
                        rel material:binding = </Pump/Looks/Brass>
                    }
                }

                def Scope "Looks"
                {
                    def Material "Brass"
                    {
                        token outputs:surface.connect = </Pump/Looks/Brass/Surface.outputs:surface>

                        def Shader "Surface"
                        {
                            uniform token info:id = "UsdPreviewSurface"
                            color3f inputs:diffuseColor.connect = </Pump/Looks/Brass/Picture.outputs:rgb>
                            token outputs:surface
                        }

                        def Shader "Picture"
                        {
                            uniform token info:id = "UsdUVTexture"
                            asset inputs:file = @textures/brass.png@
                            float3 outputs:rgb
                        }
                    }
                }
            }
            """;

    @Test
    void text_placesEachPrimAsABoneInTheStagesUnit() {
        final ModelData pump = read("pump.usda", PUMP.getBytes(StandardCharsets.UTF_8));
        assertEquals("Pump", pump.bones().getFirst().name());
        final ModelBone rotor = pump.bone("Rotor").orElseThrow();
        assertEquals("Pump", rotor.parent());
        assertEquals(10 * CENTIMETRE, rotor.translation().x(), EPSILON, "10 cm along X");
        assertEquals(10 * CENTIMETRE, rotor.translation().y(), EPSILON, "the stage's Z, up, is the game's Y");
        assertEquals(-5 * CENTIMETRE, rotor.translation().z(), EPSILON, "and its Y the game's -Z");
    }

    @Test
    void text_laysAMeshsPolygonsAsTriangles() {
        final ModelMesh housing = read("pump.usda", PUMP.getBytes(StandardCharsets.UTF_8)).bone("Housing")
                .orElseThrow().meshes().getFirst();
        assertEquals(6, housing.triangles().length, "a face of four corners as two triangles");
    }

    @Test
    void text_paintsAMeshWithItsMaterialsPictureOrItsDisplayColour() {
        final ModelData pump = read("pump.usda", PUMP.getBytes(StandardCharsets.UTF_8));
        final ModelMesh blade = pump.bone("Blade").orElseThrow().meshes().getFirst();
        assertEquals("textures/brass.png", pump.textures().get(blade.texture()).name());
        assertTrue(pump.textures().get(blade.texture()).embedded(), "the picture beside the file is read");
        assertEquals(1, blade.uvs()[1], EPSILON, "a picture's height counted from its top");
        final ModelMesh housing = pump.bone("Housing").orElseThrow().meshes().getFirst();
        assertTrue(pump.textures().get(housing.texture()).name().contains("ffff0000"), "the display colour, red");
    }

    @Test
    void text_animatesWhatItsSamplesMove() {
        final ModelData pump = read("pump.usda", PUMP.getBytes(StandardCharsets.UTF_8));
        final ModelAnimation animation = pump.animations().getFirst();
        assertEquals(1.0, animation.length(), EPSILON, "24 frames at 24 a second");
        assertTrue(animation.bones().containsKey("Rotor"), "the rotor turns");
        assertEquals(2, animation.bones().get("Rotor").rotation().keyframes().size());
    }

    @Test
    void package_readsItsFirstLayerAndThePicturesInIt() {
        final byte[] usdz = zip(Map.of("pump.usda", PUMP.getBytes(StandardCharsets.UTF_8),
                "textures/brass.png", picture()));
        final ModelData pump = UsdReader.read("pump.usdz", usdz, path -> null);
        final ModelMesh blade = pump.bone("Blade").orElseThrow().meshes().getFirst();
        assertTrue(pump.textures().get(blade.texture()).embedded(), "the picture in the package is read");
    }

    @Test
    void crate_readsTheSameStageWrittenInBinary() {
        final ModelData pump = read("pump.usdc", crate());
        final ModelBone rotor = pump.bone("Rotor").orElseThrow();
        assertEquals("Pump", rotor.parent());
        assertEquals(10 * CENTIMETRE, rotor.translation().x(), EPSILON);
        final ModelMesh blade = pump.bone("Blade").orElseThrow().meshes().getFirst();
        assertEquals(6, blade.triangles().length);
        final Vector3f corner = new Vector3f(blade.positions()[0], blade.positions()[1], blade.positions()[2]);
        assertEquals(-4 * CENTIMETRE, corner.x, EPSILON, "its points read from the crate");
        assertEquals(1, pump.animations().size(), "and its sampled turn");
    }

    @Test
    void text_saysTheLineThatDoesNotRead() {
        final String broken = "#usda 1.0\ndef Xform \"A\"\n{\n    float3 x = (1, 2\n";
        final ModelFormatException thrown = assertThrows(ModelFormatException.class,
                () -> read("broken.usda", broken.getBytes(StandardCharsets.UTF_8)));
        assertTrue(thrown.getMessage().contains("line"), thrown.getMessage());
    }

    private static ModelData read(final String name, final byte[] bytes) {
        return UsdReader.read(name, bytes, path -> path.equals("textures/brass.png") ? picture() : null);
    }

    private static byte[] picture() {
        return PngWriter.rgba(2, 2, (x, y) -> 0xFFC8A03C);
    }

    private static byte[] zip(final Map<String, byte[]> files) {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            // The stage's layer first, as a package lays it.
            for (final String path : List.of("pump.usda", "textures/brass.png")) {
                out.putNextEntry(new ZipEntry(path));
                out.write(files.get(path));
                out.closeEntry();
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return bytes.toByteArray();
    }

    /* ---------------------------------------------------------------- a crate, written as Pixar's library writes */

    /*
     * The pump's rotor and blade as a crate of version 0.8.0: its tables packed as the format packs them (each LZ4
     * block of literals alone, each integer's step written whole), its values inlined or written out.
     */
    private static byte[] crate() {
        final Crate out = new Crate();
        final int root = out.path(-1, "", false);
        final int pump = out.path(root, "Pump", false);
        final int rotor = out.path(pump, "Rotor", false);
        final int blade = out.path(rotor, "Blade", false);
        out.spec(root, 7, Map.of("upAxis", out.tokenValue("Z"), "metersPerUnit", out.inlinedFloat(0.01F),
                "startTimeCode", out.inlinedFloat(0), "endTimeCode", out.inlinedFloat(24)));
        out.spec(pump, 6, Map.of("specifier", out.inlined(42, 0), "typeName", out.tokenValue("Xform")));
        out.spec(rotor, 6, Map.of("specifier", out.inlined(42, 0), "typeName", out.tokenValue("Xform")));
        out.spec(blade, 6, Map.of("specifier", out.inlined(42, 0), "typeName", out.tokenValue("Mesh")));
        out.spec(out.path(rotor, "xformOp:translate", true), 1, Map.of("typeName", out.tokenValue("double3"),
                "default", out.vec3d(10, 5, 10)));
        out.spec(out.path(rotor, "xformOp:rotateZ", true), 1, Map.of("typeName", out.tokenValue("float"),
                "timeSamples", out.samples(new double[] {0, 24}, new float[] {0, 360})));
        out.spec(out.path(rotor, "xformOpOrder", true), 1, Map.of("typeName", out.tokenValue("token[]"),
                "default", out.tokens("xformOp:translate", "xformOp:rotateZ")));
        out.spec(out.path(blade, "points", true), 1, Map.of("typeName", out.tokenValue("point3f[]"),
                "default", out.points(-4, -1, 0, 4, -1, 0, 4, 1, 0, -4, 1, 0)));
        out.spec(out.path(blade, "faceVertexCounts", true), 1, Map.of("typeName", out.tokenValue("int[]"),
                "default", out.ints(4)));
        out.spec(out.path(blade, "faceVertexIndices", true), 1, Map.of("typeName", out.tokenValue("int[]"),
                "default", out.ints(0, 1, 2, 3)));
        return out.bytes();
    }

    /* A crate being written: its tables, and the values written out before them. */
    private static final class Crate {

        private final List<String> tokens = new ArrayList<>();
        private final List<int[]> paths = new ArrayList<>();
        private final List<String> fieldNames = new ArrayList<>();
        private final List<Long> fieldReps = new ArrayList<>();
        private final List<Integer> fieldSets = new ArrayList<>();
        private final List<int[]> specs = new ArrayList<>();
        private final ByteArrayOutputStream values = new ByteArrayOutputStream();

        private static final int HEADER = 88;

        int token(final String text) {
            int at = this.tokens.indexOf(text);
            if (at < 0) {
                this.tokens.add(text);
                at = this.tokens.size() - 1;
            }
            return at;
        }

        /* A path, a child or a property of {@code parent}; its index. Written parents before children. */
        int path(final int parent, final String element, final boolean property) {
            this.paths.add(new int[] {parent, element.isEmpty() ? 0 : token(element), property ? 1 : 0});
            return this.paths.size() - 1;
        }

        void spec(final int path, final int type, final Map<String, Long> fields) {
            final int start = this.fieldSets.size();
            new LinkedHashMap<>(fields).forEach((field, rep) -> {
                token(field);
                this.fieldNames.add(field);
                this.fieldReps.add(rep);
                this.fieldSets.add(this.fieldNames.size() - 1);
            });
            this.fieldSets.add(-1);
            this.specs.add(new int[] {path, start, type});
        }

        /* A token's value, inlined as its index. */
        long tokenValue(final String text) {
            return inlined(11, token(text));
        }

        long inlined(final int type, final long payload) {
            return 1L << 62 | (long) type << 48 | payload & 0xFFFFFFFFL;
        }

        long inlinedFloat(final float value) {
            return inlined(9, Float.floatToIntBits(value));
        }

        long vec3d(final double x, final double y, final double z) {
            final long at = offset();
            write(ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).putDouble(x).putDouble(y).putDouble(z));
            return 23L << 48 | at;
        }

        long tokens(final String... texts) {
            final long at = offset();
            final ByteBuffer out = ByteBuffer.allocate(8 + texts.length * 4).order(ByteOrder.LITTLE_ENDIAN);
            out.putLong(texts.length);
            for (final String text : texts) {
                out.putInt(token(text));
            }
            write(out);
            return 1L << 63 | 11L << 48 | at;
        }

        long points(final float... xyz) {
            final long at = offset();
            final ByteBuffer out = ByteBuffer.allocate(8 + xyz.length * 4).order(ByteOrder.LITTLE_ENDIAN);
            out.putLong(xyz.length / 3);
            for (final float each : xyz) {
                out.putFloat(each);
            }
            write(out);
            return 1L << 63 | 24L << 48 | at;
        }

        long ints(final int... values) {
            final long at = offset();
            final ByteBuffer out = ByteBuffer.allocate(8 + values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
            out.putLong(values.length);
            for (final int each : values) {
                out.putInt(each);
            }
            write(out);
            return 1L << 63 | 3L << 48 | at;
        }

        /* Samples: a jump to the times' reference, the times before it, then a jump to the values. */
        long samples(final double[] times, final float[] floats) {
            final long timesAt = offset();
            final ByteBuffer timeData = ByteBuffer.allocate(8 + times.length * 8).order(ByteOrder.LITTLE_ENDIAN);
            timeData.putLong(times.length);
            for (final double time : times) {
                timeData.putDouble(time);
            }
            write(timeData);
            final long at = offset();
            final ByteBuffer out = ByteBuffer.allocate(8 + 8 + 8 + 8 + floats.length * 8)
                    .order(ByteOrder.LITTLE_ENDIAN);
            out.putLong(8);
            out.putLong(48L << 48 | timesAt);
            out.putLong(8);
            out.putLong(floats.length);
            for (final float each : floats) {
                out.putLong(inlined(8, Float.floatToIntBits(each)));
            }
            write(out);
            return 46L << 48 | at;
        }

        byte[] bytes() {
            final ByteArrayOutputStream file = new ByteArrayOutputStream();
            final byte[] head = new byte[HEADER];
            final byte[] magic = "PXR-USDC".getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(magic, 0, head, 0, magic.length);
            head[9] = 8;
            file.writeBytes(head);
            file.writeBytes(this.values.toByteArray());
            final Map<String, Long> starts = new LinkedHashMap<>();
            starts.put("TOKENS", (long) file.size());
            final ByteArrayOutputStream names = new ByteArrayOutputStream();
            for (final String token : this.tokens) {
                names.writeBytes(token.getBytes(StandardCharsets.UTF_8));
                names.write(0);
            }
            final byte[] packedNames = lz4(names.toByteArray());
            file.writeBytes(longs(this.tokens.size(), names.size(), packedNames.length));
            file.writeBytes(packedNames);
            starts.put("STRINGS", (long) file.size());
            file.writeBytes(longs(0));
            starts.put("FIELDS", (long) file.size());
            file.writeBytes(longs(this.fieldNames.size()));
            final int[] nameTokens = new int[this.fieldNames.size()];
            for (int i = 0; i < nameTokens.length; i++) {
                nameTokens[i] = token(this.fieldNames.get(i));
            }
            file.writeBytes(packed(nameTokens));
            final ByteBuffer reps = ByteBuffer.allocate(this.fieldReps.size() * 8).order(ByteOrder.LITTLE_ENDIAN);
            this.fieldReps.forEach(reps::putLong);
            final byte[] packedReps = lz4(reps.array());
            file.writeBytes(longs(packedReps.length));
            file.writeBytes(packedReps);
            starts.put("FIELDSETS", (long) file.size());
            file.writeBytes(longs(this.fieldSets.size()));
            file.writeBytes(packed(this.fieldSets.stream().mapToInt(Integer::intValue).toArray()));
            starts.put("PATHS", (long) file.size());
            file.writeBytes(paths());
            starts.put("SPECS", (long) file.size());
            file.writeBytes(longs(this.specs.size()));
            for (int k = 0; k < 3; k++) {
                final int column = k;
                file.writeBytes(packed(this.specs.stream().mapToInt(spec -> spec[column]).toArray()));
            }
            final long toc = file.size();
            file.writeBytes(longs(starts.size()));
            for (final Map.Entry<String, Long> section : starts.entrySet()) {
                final byte[] sectionName = new byte[16];
                final byte[] written = section.getKey().getBytes(StandardCharsets.US_ASCII);
                System.arraycopy(written, 0, sectionName, 0, written.length);
                file.writeBytes(sectionName);
                file.writeBytes(longs(section.getValue(), 0));
            }
            final byte[] all = file.toByteArray();
            ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN).putLong(16, toc);
            return all;
        }

        /* The paths as a tree: each the next's parent, or a sibling a jump ahead, as the format walks them. */
        private byte[] paths() {
            final int count = this.paths.size();
            final int[] indexes = new int[count];
            final int[] elements = new int[count];
            final int[] jumps = new int[count];
            final List<Integer> order = new ArrayList<>();
            walk(-1, order);
            for (int i = 0; i < order.size(); i++) {
                final int path = order.get(i);
                indexes[i] = path;
                elements[i] = this.paths.get(path)[2] == 1 ? -this.paths.get(path)[1] : this.paths.get(path)[1];
            }
            for (int i = 0; i < order.size(); i++) {
                final int path = order.get(i);
                final boolean hasChild = i + 1 < order.size() && this.paths.get(order.get(i + 1))[0] == path;
                int sibling = -1;
                for (int k = i + 1; k < order.size(); k++) {
                    if (this.paths.get(order.get(k))[0] == this.paths.get(path)[0]) {
                        sibling = k;
                        break;
                    }
                    if (!under(order.get(k), path)) {
                        break;
                    }
                }
                jumps[i] = hasChild && sibling >= 0 ? sibling - i : hasChild ? -1 : sibling >= 0 ? 0 : -2;
            }
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.writeBytes(longs(count, count));
            out.writeBytes(packed(indexes));
            out.writeBytes(packed(elements));
            out.writeBytes(packed(jumps));
            return out.toByteArray();
        }

        private void walk(final int parent, final List<Integer> order) {
            for (int i = 0; i < this.paths.size(); i++) {
                if (this.paths.get(i)[0] == parent) {
                    order.add(i);
                    walk(i, order);
                }
            }
        }

        private boolean under(final int path, final int ancestor) {
            for (int at = this.paths.get(path)[0]; at >= 0; at = this.paths.get(at)[0]) {
                if (at == ancestor) {
                    return true;
                }
            }
            return false;
        }

        private long offset() {
            return HEADER + this.values.size();
        }

        private void write(final ByteBuffer buffer) {
            this.values.writeBytes(buffer.array());
        }

        /* Integers packed: their size, then an LZ4 block of the common step, the codes, and each step whole. */
        private static byte[] packed(final int[] values) {
            final int codes = (values.length * 2 + 7) / 8;
            final ByteBuffer encoded = ByteBuffer.allocate(4 + codes + values.length * 4)
                    .order(ByteOrder.LITTLE_ENDIAN);
            encoded.putInt(0);
            for (int i = 0; i < codes; i++) {
                encoded.put((byte) 0xFF);
            }
            int previous = 0;
            for (final int value : values) {
                encoded.putInt(value - previous);
                previous = value;
            }
            final byte[] packed = lz4(encoded.array());
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.writeBytes(longs(packed.length));
            out.writeBytes(packed);
            return out.toByteArray();
        }

        /* One LZ4 block of literals alone, a first byte of 0 for one block. */
        private static byte[] lz4(final byte[] raw) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0);
            final int length = raw.length;
            out.write(Math.min(length, 15) << 4);
            if (length >= 15) {
                int rest = length - 15;
                while (rest >= 255) {
                    out.write(255);
                    rest -= 255;
                }
                out.write(rest);
            }
            out.writeBytes(raw);
            return out.toByteArray();
        }

        private static byte[] longs(final long... values) {
            final ByteBuffer out = ByteBuffer.allocate(values.length * 8).order(ByteOrder.LITTLE_ENDIAN);
            for (final long value : values) {
                out.putLong(value);
            }
            return out.array();
        }
    }
}
