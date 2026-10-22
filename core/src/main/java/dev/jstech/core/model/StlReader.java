/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;

/**
 * Reads STL, the triangles every CAD program and 3D printer knows, written as text or in binary: each solid of a text
 * file a bone of its own; the colour of each triangle where a binary file gives one, in either of the two ways
 * programs write it (VisCAM's and SolidView's, or Materialise's, whose header says {@code COLOR=}), each colour a
 * texture in the game's look ({@link CadLook}). STL says nothing of its unit; as CAD programs write it, it is in
 * millimetres, a thousandth of a block. It is turned from the CAD programs' upright Z to the game's Y and stands on
 * its origin. Pure.
 */
public final class StlReader {

    private final String name;
    private final Map<String, Map<Integer, Part>> solids = new LinkedHashMap<>();
    private final Map<Integer, Integer> colourTextures = new LinkedHashMap<>();
    private final List<ModelTexture> textures = new ArrayList<>();

    private static final float PIXELS = CadLook.PIXELS_A_METRE / 1000.0F;
    private static final int HEADER = 80;
    private static final int TRIANGLE = 50;
    private static final int COUNT = 4;
    private static final int VALID = 0x8000;
    private static final int FIVE_BITS = 0x1F;
    private static final String MAGICS = "COLOR=";
    private static final String DEFAULT_SOLID = "model";

    private StlReader(final String name) {
        this.name = name;
    }

    /**
     * The model an STL file holds.
     *
     * @throws ModelFormatException when the file is not one
     */
    public static ModelData read(final String name, final byte[] bytes) {
        final StlReader reader = new StlReader(name);
        if (binary(bytes)) {
            reader.binaryTriangles(bytes);
        } else {
            reader.textTriangles(new String(bytes, StandardCharsets.US_ASCII));
        }
        return reader.model();
    }

    /*
     * Whether a file is binary: as long as its count of triangles says. A binary file's header may begin with
     * "solid" too, so the start of the file says nothing.
     */
    private static boolean binary(final byte[] bytes) {
        if (bytes.length < HEADER + COUNT) {
            return false;
        }
        final long count = ByteBuffer.wrap(bytes, HEADER, COUNT).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;
        return bytes.length == HEADER + COUNT + count * TRIANGLE;
    }

    private void binaryTriangles(final byte[] bytes) {
        final String header = new String(bytes, 0, HEADER, StandardCharsets.US_ASCII);
        final int magics = header.indexOf(MAGICS);
        // Materialise marks a colour that is the file's own with the bit clear; the others with it set.
        final boolean clearMeansOwn = magics >= 0;
        final ByteBuffer in = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        final int count = in.getInt(HEADER);
        int at = HEADER + COUNT;
        final float[] corners = new float[9];
        for (int t = 0; t < count; t++) {
            for (int k = 0; k < corners.length; k++) {
                corners[k] = in.getFloat(at + 12 + k * 4);
            }
            final int attribute = in.getShort(at + 48) & 0xFFFF;
            final boolean coloured = clearMeansOwn ? (attribute & VALID) == 0 && attribute != 0
                    : (attribute & VALID) != 0;
            final int colour = coloured ? colour(attribute, clearMeansOwn) : CadLook.STEEL;
            add(DEFAULT_SOLID, colour, corners);
            at += TRIANGLE;
        }
    }

    /* A colour of five bits a channel, red first for VisCAM and SolidView, blue first for Materialise. */
    private static int colour(final int attribute, final boolean blueFirst) {
        final int first = attribute & FIVE_BITS;
        final int second = attribute >> 5 & FIVE_BITS;
        final int third = attribute >> 10 & FIVE_BITS;
        final int red = blueFirst ? first : third;
        final int blue = blueFirst ? third : first;
        return CadLook.OPAQUE | widen(red) << 16 | widen(second) << 8 | widen(blue);
    }

    private static int widen(final int five) {
        return five << 3 | five >> 2;
    }

    private void textTriangles(final String text) {
        String solid = DEFAULT_SOLID;
        final float[] corners = new float[9];
        int corner = 0;
        int number = 0;
        for (final String line : text.split("\r?\n")) {
            number++;
            final String[] words = line.strip().split("\\s+");
            try {
                switch (words[0]) {
                    case "solid" -> {
                        solid = words.length > 1 ? line.strip().substring("solid".length()).strip() : DEFAULT_SOLID;
                        if (solid.isEmpty()) {
                            solid = DEFAULT_SOLID;
                        }
                    }
                    case "facet" -> corner = 0;
                    case "vertex" -> {
                        if (corner < 3) {
                            corners[corner * 3] = Float.parseFloat(words[1]);
                            corners[corner * 3 + 1] = Float.parseFloat(words[2]);
                            corners[corner * 3 + 2] = Float.parseFloat(words[3]);
                        }
                        corner++;
                    }
                    case "endfacet" -> {
                        if (corner == 3) {
                            add(solid, CadLook.STEEL, corners);
                        }
                    }
                    default -> {
                        // "outer loop", "endloop", "endsolid" and the blank lines carry nothing more.
                    }
                }
            } catch (final NumberFormatException | IndexOutOfBoundsException broken) {
                throw new ModelFormatException("the STL file " + this.name + " has a line it cannot read, line "
                        + number + ": " + line.strip());
            }
        }
        if (this.solids.isEmpty()) {
            throw new ModelFormatException("the STL file " + this.name + " has no triangles");
        }
    }

    /* A triangle of the solid, in the part of its colour, its corners turned upright and in the game's pixels. */
    private void add(final String solid, final int colour, final float[] corners) {
        final Part part = this.solids.computeIfAbsent(solid, key -> new LinkedHashMap<>())
                .computeIfAbsent(colour, key -> new Part());
        final Vector3f point = new Vector3f();
        for (int k = 0; k < 3; k++) {
            CadLook.upright(point.set(corners[k * 3], corners[k * 3 + 1], corners[k * 3 + 2]).mul(PIXELS));
            part.corner(point);
        }
    }

    private ModelData model() {
        final List<ModelBone> bones = new ArrayList<>();
        final Map<String, Integer> named = new HashMap<>();
        this.solids.forEach((solid, byColour) -> {
            final List<ModelMesh> meshes = new ArrayList<>();
            byColour.forEach((colour, part) -> meshes.add(part.mesh(texture(colour))));
            final int seen = named.merge(solid, 1, Integer::sum);
            bones.add(ModelBone.ofEuler(seen == 1 ? solid : solid + "_" + seen, null, new Vector3f(), new Vector3f(),
                    List.of(), meshes, List.of(), false));
        });
        return CadLook.standing(new ModelData(this.name, CadLook.SIZE, CadLook.SIZE, bones, Map.of(), this.textures,
                List.of()));
    }

    /* The texture of a colour, made the first time it is asked for. */
    private int texture(final int colour) {
        return this.colourTextures.computeIfAbsent(colour, key -> {
            this.textures.add(CadLook.colour(key));
            return this.textures.size() - 1;
        });
    }

    /* The corners of one solid's triangles of one colour, each place one corner. */
    private static final class Part {

        private final Map<Long, Integer> seen = new HashMap<>();
        private final FloatList positions = new FloatList();
        private final IntList triangles = new IntList();

        void corner(final Vector3f point) {
            final long key = (long) Float.floatToIntBits(point.x) * 31 * 31 ^ (long) Float.floatToIntBits(point.y) * 31
                    ^ Float.floatToIntBits(point.z);
            Integer index = this.seen.get(key);
            if (index == null || !same(index, point)) {
                index = this.positions.size() / 3;
                this.positions.add(point.x);
                this.positions.add(point.y);
                this.positions.add(point.z);
                this.seen.put(key, index);
            }
            this.triangles.add(index);
        }

        ModelMesh mesh(final int texture) {
            return new ModelMesh(this.positions.toArray(), new float[0], new float[0], this.triangles.toArray(),
                    new int[0], new float[0], List.of(), texture);
        }

        private boolean same(final int index, final Vector3f point) {
            return this.positions.get(index * 3) == point.x && this.positions.get(index * 3 + 1) == point.y
                    && this.positions.get(index * 3 + 2) == point.z;
        }
    }
}
