/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.joml.Vector3f;

/**
 * Reads Wavefront OBJ, the plainest of model formats, which every tool writes: its points, pictures and normals, its
 * faces of any number of corners, each object or group a bone of its own, and the textures of the materials its
 * library file beside it gives. OBJ says nothing of its unit; as Blender writes it, one is a metre, a block, sixteen
 * pixels. It does not move. Pure.
 */
public final class ObjReader {

    private final String name;
    private final Function<String, byte[]> beside;
    private final List<float[]> points = new ArrayList<>();
    private final List<float[]> pictures = new ArrayList<>();
    private final List<float[]> normals = new ArrayList<>();
    private final Map<String, Map<String, Part>> parts = new LinkedHashMap<>();
    private final Map<String, Integer> materialTextures = new LinkedHashMap<>();
    private final List<ModelTexture> textures = new ArrayList<>();
    private String part = DEFAULT_PART;
    private String material = "";
    private boolean namedByObject;

    private static final float PIXELS = 16.0F;
    private static final String DEFAULT_PART = "model";

    private ObjReader(final String name, final Function<String, byte[]> beside) {
        this.name = name;
        this.beside = beside;
    }

    /**
     * The model an OBJ file holds.
     *
     * @param name   what the model is called
     * @param bytes  the file
     * @param beside the bytes of a file the model names beside it, as its material library; null for one not there
     * @throws ModelFormatException when the file is not one
     */
    public static ModelData read(final String name, final byte[] bytes, final Function<String, byte[]> beside) {
        final ObjReader reader = new ObjReader(name, beside);
        reader.lines(new String(bytes, StandardCharsets.UTF_8));
        return reader.model();
    }

    private void lines(final String text) {
        final String[] lines = text.replace("\\\r\n", " ").replace("\\\n", " ").split("\r?\n");
        int number = 0;
        for (final String line : lines) {
            number++;
            final String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.charAt(0) == '#') {
                continue;
            }
            final String[] words = trimmed.split("\\s+");
            try {
                line(words);
            } catch (final NumberFormatException | IndexOutOfBoundsException broken) {
                throw new ModelFormatException("the OBJ file " + this.name + " has a line it cannot read, line "
                        + number + ": " + trimmed);
            }
        }
    }

    private void line(final String[] words) {
        switch (words[0]) {
            case "v" -> this.points.add(new float[] {Float.parseFloat(words[1]) * PIXELS,
                    Float.parseFloat(words[2]) * PIXELS, Float.parseFloat(words[3]) * PIXELS});
            // OBJ counts a picture's height from its bottom; the Core, as the game, from its top.
            case "vt" -> this.pictures.add(new float[] {Float.parseFloat(words[1]),
                    1 - (words.length > 2 ? Float.parseFloat(words[2]) : 0)});
            case "vn" -> {
                final Vector3f normal = new Vector3f(Float.parseFloat(words[1]), Float.parseFloat(words[2]),
                        Float.parseFloat(words[3])).normalize();
                this.normals.add(new float[] {normal.x, normal.y, normal.z});
            }
            case "o" -> {
                this.part = rest(words);
                this.namedByObject = true;
            }
            // Groups name the parts only of a file that names no objects, as most tools write one or the other.
            case "g" -> {
                if (!this.namedByObject) {
                    this.part = rest(words);
                }
            }
            case "usemtl" -> this.material = rest(words);
            case "mtllib" -> {
                for (int i = 1; i < words.length; i++) {
                    library(words[i]);
                }
            }
            case "f" -> face(words);
            default -> {
                // Smoothing groups, lines, points and curves draw nothing the Core draws.
            }
        }
    }

    private void face(final String[] words) {
        final Part target = this.parts.computeIfAbsent(this.part, key -> new LinkedHashMap<>())
                .computeIfAbsent(this.material, key -> new Part());
        final int first = target.corners();
        final int count = words.length - 1;
        for (int i = 1; i < words.length; i++) {
            final String[] refs = words[i].split("/", -1);
            final float[] point = this.points.get(index(refs[0], this.points.size()));
            target.positions.addAll(List.of(point[0], point[1], point[2]));
            if (refs.length > 1 && !refs[1].isEmpty()) {
                final float[] picture = this.pictures.get(index(refs[1], this.pictures.size()));
                target.uvs.addAll(List.of(picture[0], picture[1]));
            } else {
                target.uvs.addAll(List.of(0.0F, 0.0F));
            }
            if (refs.length > 2 && !refs[2].isEmpty()) {
                final float[] normal = this.normals.get(index(refs[2], this.normals.size()));
                target.normals.addAll(List.of(normal[0], normal[1], normal[2]));
            } else {
                target.withoutNormals = true;
            }
        }
        for (int k = 1; k + 1 < count; k++) {
            target.triangles.addAll(List.of(first, first + k, first + k + 1));
        }
    }

    /* A face's reference to a point, counted from 1, or back from the last one written for a negative one. */
    private static int index(final String written, final int count) {
        final int value = Integer.parseInt(written);
        return value < 0 ? count + value : value - 1;
    }

    /* The materials of a library file, each the texture of its diffuse colour. */
    private void library(final String file) {
        final byte[] bytes = this.beside.apply(file);
        if (bytes == null) {
            return;
        }
        String current = "";
        for (final String line : new String(bytes, StandardCharsets.UTF_8).split("\r?\n")) {
            final String[] words = line.strip().split("\\s+");
            if (words[0].equals("newmtl")) {
                current = rest(words);
            } else if (words[0].equals("map_Kd") && words.length > 1) {
                // Options as -s or -o come before the file, which is last.
                final String texture = words[words.length - 1].replace('\\', '/');
                this.materialTextures.put(current, this.textures.size());
                this.textures.add(ModelTexture.inFile(current, texture));
            }
        }
    }

    private ModelData model() {
        final List<ModelBone> bones = new ArrayList<>();
        this.parts.forEach((partName, byMaterial) -> {
            final List<ModelMesh> meshes = new ArrayList<>();
            byMaterial.forEach((materialName, part) -> meshes.add(part.build(
                    this.materialTextures.getOrDefault(materialName, 0))));
            bones.add(ModelBone.ofEuler(partName, null, new Vector3f(), new Vector3f(), List.of(), meshes,
                    List.of(), false));
        });
        return new ModelData(this.name, 1, 1, bones, Map.of(), this.textures, List.of());
    }

    private static String rest(final String[] words) {
        return words.length < 2 ? DEFAULT_PART : String.join(" ", Arrays.copyOfRange(words, 1, words.length));
    }

    /* The corners and triangles of one part of the file painted with one material. */
    private static final class Part {

        private final List<Float> positions = new ArrayList<>();
        private final List<Float> uvs = new ArrayList<>();
        private final List<Float> normals = new ArrayList<>();
        private final List<Integer> triangles = new ArrayList<>();
        private boolean withoutNormals;

        int corners() {
            return this.positions.size() / 3;
        }

        ModelMesh build(final int texture) {
            final int[] order = new int[this.triangles.size()];
            for (int i = 0; i < order.length; i++) {
                order[i] = this.triangles.get(i);
            }
            return new ModelMesh(floats(this.positions), this.withoutNormals ? new float[0] : floats(this.normals),
                    floats(this.uvs), order, new int[0], new float[0], List.of(), texture);
        }

        private static float[] floats(final List<Float> values) {
            final float[] out = new float[values.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = values.get(i);
            }
            return out;
        }
    }
}
