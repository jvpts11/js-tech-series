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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.Deflater;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class FbxReaderTest {

    private static final double EPSILON = 1.0e-3;
    private static final long SECOND = 46_186_158_000L;
    private static final String SPLIT = "\u0000\u0001";

    @Test
    void read_hangsTheFileFromARootThatTurnsItsAxesAndCountsItsUnit() {
        final ModelData model = FbxReader.read("plate", binary(document(7400)));
        final ModelBone axes = model.bone(FbxReader.AXES).orElseThrow();
        final Vector3f up = new Quaternionf(axes.rotation()).transform(new Vector3f(0, 0, 1));
        assertEquals(1, up.y, EPSILON, "a file whose Z is up stands with it up the game's Y");
        final Vector3f front = new Quaternionf(axes.rotation()).transform(new Vector3f(0, -1, 0));
        assertEquals(1, front.z, EPSILON, "and its front, toward -Y, toward the game's +Z");
        assertEquals(16, axes.scale().y(), EPSILON, "a unit of a metre is sixteen pixels");
        assertEquals(FbxReader.AXES, model.bone("Plate").orElseThrow().parent());
    }

    @Test
    void read_readsTheTextFormAsTheBinary() {
        final ModelData binary = FbxReader.read("plate", binary(document(7400)));
        final ModelData text = FbxReader.read("plate", text(document(7400)).getBytes(StandardCharsets.UTF_8));
        assertEquals(binary.bones().size(), text.bones().size());
        assertArrayEquals(binary.bone("Plate").orElseThrow().meshes().getFirst().positions(),
                text.bone("Plate").orElseThrow().meshes().getFirst().positions());
        assertEquals(binary.animations().getFirst().length(), text.animations().getFirst().length(), EPSILON);
    }

    @Test
    void read_readsEveryPolygonAsTrianglesWithItsPictures() {
        final ModelMesh plate = FbxReader.read("plate", binary(document(7500))).bone("Plate").orElseThrow()
                .meshes().getFirst();
        assertArrayEquals(new int[] {0, 1, 2, 0, 2, 3}, plate.triangles(), "a quad as two triangles");
        assertEquals(1, plate.positions()[3], EPSILON, "in the file's own unit, the root sizes it");
        assertEquals(1, plate.uvs()[1], EPSILON, "a picture's height counted from its top");
        assertEquals(0, plate.uvs()[5], EPSILON);
    }

    @Test
    void read_turnsAModelByItsTurnBeforeAndItsOwn() {
        final ModelBone arm = FbxReader.read("plate", binary(document(7400))).bone("Arm").orElseThrow();
        final Vector3f x = new Quaternionf(arm.rotation()).transform(new Vector3f(1, 0, 0));
        // Its own turn of 90 about Z takes X to Y; the turn before, 90 about X, then takes Y to Z.
        assertEquals(1, x.z, EPSILON);
        assertEquals("Plate", arm.parent());
    }

    @Test
    void read_readsAnimationStacksAsAnimations() {
        final ModelAnimation wave = FbxReader.read("plate", binary(document(7400))).animations().getFirst();
        assertEquals("Wave", wave.name());
        assertEquals(1, wave.length(), EPSILON);
        final float[] out = new float[3];
        wave.bones().get("Plate").position().evaluate(0.5, new ExpressionContext(), out);
        assertEquals(5, out[0], EPSILON, "halfway along a straight curve");
        assertEquals(0, out[2], EPSILON, "a number of no curve stays at its rest");
    }

    @Test
    void read_refusesFilesBeforeVersionSeven() {
        final ModelFormatException refused = assertThrows(ModelFormatException.class,
                () -> FbxReader.read("old", binary(document(6100))));
        assertTrue(refused.getMessage().contains("7.4"), "and says what to export it as");
    }

    /* A plate of one quad on a model moved up, an arm on it turned, and an animation sliding the plate east. */
    private static List<Rec> document(final int version) {
        final List<Rec> axes = List.of(p("UpAxis", "int", 2), p("UpAxisSign", "int", 1), p("FrontAxis", "int", 1),
                p("FrontAxisSign", "int", -1), p("CoordAxis", "int", 0), p("CoordAxisSign", "int", 1),
                p("UnitScaleFactor", "double", 100.0));
        final Rec uv = new Rec("LayerElementUV", List.of(0), List.of(
                new Rec("MappingInformationType", List.of("ByPolygonVertex"), List.of()),
                new Rec("ReferenceInformationType", List.of("IndexToDirect"), List.of()),
                new Rec("UV", List.of(new double[] {0, 0, 1, 0, 1, 1, 0, 1}), List.of()),
                new Rec("UVIndex", List.of(new int[] {0, 1, 2, 3}), List.of())));
        final Rec objects = new Rec("Objects", List.of(), List.of(
                new Rec("Geometry", List.of(10L, "Plate" + SPLIT + "Geometry", "Mesh"), List.of(
                        new Rec("Vertices", List.of(new double[] {0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0}), List.of()),
                        new Rec("PolygonVertexIndex", List.of(new int[] {0, 1, 2, -4}), List.of()), uv)),
                new Rec("Model", List.of(20L, "Plate" + SPLIT + "Model", "Mesh"), List.of(properties(
                        p("Lcl Translation", "Lcl Translation", 0.0, 0.0, 5.0)))),
                new Rec("Model", List.of(21L, "Arm" + SPLIT + "Model", "LimbNode"), List.of(properties(
                        p("Lcl Rotation", "Lcl Rotation", 0.0, 0.0, 90.0),
                        p("PreRotation", "Vector3D", 90.0, 0.0, 0.0)))),
                new Rec("AnimationStack", List.of(30L, "Wave" + SPLIT + "AnimStack", ""), List.of(properties(
                        p("LocalStop", "KTime", SECOND)))),
                new Rec("AnimationLayer", List.of(31L, "Base" + SPLIT + "AnimLayer", ""), List.of()),
                new Rec("AnimationCurveNode", List.of(32L, "T" + SPLIT + "AnimCurveNode", ""), List.of(properties(
                        p("d|X", "Number", 0.0), p("d|Y", "Number", 0.0), p("d|Z", "Number", 5.0)))),
                new Rec("AnimationCurve", List.of(33L, SPLIT + "AnimCurve", ""), List.of(
                        new Rec("KeyTime", List.of(new long[] {0, SECOND}), List.of()),
                        new Rec("KeyValueFloat", List.of(new double[] {0, 10}), List.of()),
                        new Rec("KeyAttrFlags", List.of(new int[] {4}), List.of()),
                        new Rec("KeyAttrDataFloat", List.of(new double[] {0, 0, 0, 0}), List.of()),
                        new Rec("KeyAttrRefCount", List.of(new int[] {2}), List.of())))));
        final Rec connections = new Rec("Connections", List.of(), List.of(c("OO", 20, 0), c("OO", 10, 20),
                c("OO", 21, 20), c("OO", 31, 30), c("OO", 32, 31), new Rec("C", List.of("OP", 32L, 20L,
                        "Lcl Translation"), List.of()), new Rec("C", List.of("OP", 33L, 32L, "d|X"), List.of())));
        return List.of(new Rec("FBXHeaderExtension", List.of(), List.of(new Rec("FBXVersion", List.of(version),
                List.of()))), new Rec("GlobalSettings", List.of(), List.of(properties(axes.toArray(Rec[]::new)))),
                objects, connections);
    }

    private static Rec properties(final Rec... entries) {
        return new Rec("Properties70", List.of(), List.of(entries));
    }

    private static Rec p(final String name, final String type, final Object... values) {
        final List<Object> out = new ArrayList<>(List.of(name, type, "", "A"));
        out.addAll(List.of(values));
        return new Rec("P", out, List.of());
    }

    private static Rec c(final String type, final long child, final long parent) {
        return new Rec("C", List.of(type, child, parent), List.of());
    }

    /* The binary form, of the version the document's header says, its arrays of doubles packed with zlib. */
    private static byte[] binary(final List<Rec> top) {
        final int version = (int) top.getFirst().children().getFirst().properties().getFirst();
        final boolean wide = version >= 7500;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("Kaydara FBX Binary  ".getBytes(StandardCharsets.US_ASCII));
        out.write(0);
        out.write(0x1A);
        out.write(0);
        out.writeBytes(le(4).putInt(version).array());
        for (final Rec record : top) {
            write(record, out, wide, 0);
        }
        out.writeBytes(new byte[wide ? 25 : 13]);
        return out.toByteArray();
    }

    /*
     * Writes a record to {@code out}, which starts {@code base} bytes into the file: a record's end is counted from
     * the file's start.
     */
    private static void write(final Rec record, final ByteArrayOutputStream out, final boolean wide, final long base) {
        final ByteArrayOutputStream properties = new ByteArrayOutputStream();
        for (final Object property : record.properties()) {
            property(property, properties);
        }
        final byte[] name = record.name().getBytes(StandardCharsets.UTF_8);
        final int header = wide ? 25 : 13;
        final long childrenStart = base + out.size() + header + name.length + properties.size();
        final ByteArrayOutputStream children = new ByteArrayOutputStream();
        for (final Rec child : record.children()) {
            write(child, children, wide, childrenStart);
        }
        if (!record.children().isEmpty()) {
            children.writeBytes(new byte[wide ? 25 : 13]);
        }
        final long end = childrenStart + children.size();
        if (wide) {
            out.writeBytes(le(24).putLong(end).putLong(record.properties().size()).putLong(properties.size())
                    .array());
        } else {
            out.writeBytes(le(12).putInt((int) end).putInt(record.properties().size()).putInt(properties.size())
                    .array());
        }
        out.write(name.length);
        out.writeBytes(name);
        out.writeBytes(properties.toByteArray());
        out.writeBytes(children.toByteArray());
    }

    private static void property(final Object property, final ByteArrayOutputStream out) {
        switch (property) {
            case Long value -> {
                out.write('L');
                out.writeBytes(le(8).putLong(value).array());
            }
            case Integer value -> {
                out.write('I');
                out.writeBytes(le(4).putInt(value).array());
            }
            case Double value -> {
                out.write('D');
                out.writeBytes(le(8).putDouble(value).array());
            }
            case String value -> {
                final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                out.write('S');
                out.writeBytes(le(4).putInt(bytes.length).array());
                out.writeBytes(bytes);
            }
            case double[] values -> {
                final ByteBuffer raw = le(values.length * 8);
                for (final double value : values) {
                    raw.putDouble(value);
                }
                final byte[] packed = deflate(raw.array());
                out.write('d');
                out.writeBytes(le(12).putInt(values.length).putInt(1).putInt(packed.length).array());
                out.writeBytes(packed);
            }
            case long[] values -> {
                out.write('l');
                out.writeBytes(le(12).putInt(values.length).putInt(0).putInt(values.length * 8).array());
                final ByteBuffer raw = le(values.length * 8);
                for (final long value : values) {
                    raw.putLong(value);
                }
                out.writeBytes(raw.array());
            }
            case int[] values -> {
                out.write('i');
                out.writeBytes(le(12).putInt(values.length).putInt(0).putInt(values.length * 4).array());
                final ByteBuffer raw = le(values.length * 4);
                for (final int value : values) {
                    raw.putInt(value);
                }
                out.writeBytes(raw.array());
            }
            default -> throw new IllegalArgumentException("no FBX property of " + property);
        }
    }

    /* The text form of the same records. */
    private static String text(final List<Rec> top) {
        final StringBuilder out = new StringBuilder("; FBX 7.4.0 project file\n");
        for (final Rec record : top) {
            text(record, "", out);
        }
        return out.toString();
    }

    private static void text(final Rec record, final String indent, final StringBuilder out) {
        out.append(indent).append(record.name()).append(": ");
        for (int i = 0; i < record.properties().size(); i++) {
            final Object property = record.properties().get(i);
            if (i > 0) {
                out.append(", ");
            }
            switch (property) {
                case String value -> {
                    final int split = value.indexOf(SPLIT);
                    out.append('"').append(split < 0 ? value : value.substring(split + 2) + "::"
                            + value.substring(0, split)).append('"');
                }
                case double[] values -> array(values.length, Arrays.toString(values), indent, out);
                case long[] values -> array(values.length, Arrays.toString(values), indent, out);
                case int[] values -> array(values.length, Arrays.toString(values), indent, out);
                default -> out.append(property);
            }
        }
        if (!record.children().isEmpty()) {
            out.append(" {\n");
            for (final Rec child : record.children()) {
                text(child, indent + "\t", out);
            }
            out.append(indent).append('}');
        }
        out.append('\n');
    }

    private static void array(final int count, final String listed, final String indent, final StringBuilder out) {
        out.append('*').append(count).append(" {\n").append(indent).append("\ta: ")
                .append(listed, 1, listed.length() - 1).append('\n').append(indent).append('}');
    }

    private static ByteBuffer le(final int size) {
        return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static byte[] deflate(final byte[] raw) {
        final Deflater deflater = new Deflater();
        deflater.setInput(raw);
        deflater.finish();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] chunk = new byte[256];
        while (!deflater.finished()) {
            out.write(chunk, 0, deflater.deflate(chunk));
        }
        deflater.end();
        return out.toByteArray();
    }

    /* A record of an FBX file being written. */
    private record Rec(String name, List<Object> properties, List<Rec> children) {
    }
}
