/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Brings a model read from its file to what its definition asks, as it is read: its meshes brought down to the
 * triangles it is drawn with, near or far ({@link MeshReducer}), and the meshes of a file that lays no picture on them,
 * as a CAD program's, given pictures from the sides as a block's are, a pixel a sixteenth of a block at the size it is
 * drawn. Its bones, cubes and locators stay as they are, so a model far away moves as it does near. Pure.
 */
public final class ModelShaping {

    private static final float PIXELS = 16.0F;
    private static final int CUBE_TRIANGLES = 12;
    /* The fewest triangles a mesh is brought down to, so a small part keeps a shape. */
    private static final int LEAST = 4;

    private ModelShaping() {
    }

    /**
     * {@code read} as its definition's detail asks.
     *
     * @param far whether for drawing from far away, with the triangles the detail gives there
     */
    public static ModelData shape(final ModelData read, final ModelDefinition.Detail detail, final boolean far) {
        final int most = far ? detail.farTriangles() : detail.triangles();
        final int meshes = meshTriangles(read);
        final int cubes = triangles(read) - meshes;
        final double share = most > 0 && meshes + cubes > most && meshes > 0
                ? Math.max(0.0, most - cubes) / meshes : 1.0;
        final List<ModelBone> bones = new ArrayList<>(read.bones().size());
        boolean changed = false;
        for (final ModelBone bone : read.bones()) {
            final List<ModelMesh> shaped = new ArrayList<>(bone.meshes().size());
            for (final ModelMesh mesh : bone.meshes()) {
                final ModelMesh next = shape(mesh, share, detail.scale());
                changed |= next != mesh;
                shaped.add(next);
            }
            bones.add(shaped.equals(bone.meshes()) ? bone : new ModelBone(bone.name(), bone.parent(), bone.pivot(),
                    bone.rotation(), bone.euler(), bone.translation(), bone.scale(), bone.cubes(), shaped,
                    bone.locators(), bone.hidden()));
        }
        return changed ? new ModelData(read.name(), read.textureWidth(), read.textureHeight(), bones, read.skins(),
                read.textures(), read.animations()) : read;
    }

    /**
     * What keeps {@code data} from being drawn with what {@code detail} says, or nothing: more triangles than a model
     * may have without saying how many ({@link ModelDefinition.Detail#MOST_UNSAID}), or more in its cubes, which are
     * drawn as they are, than it says it is drawn with near or far.
     */
    public static Optional<String> overBudget(final ModelData data, final ModelDefinition.Detail detail) {
        final int all = triangles(data);
        final int cubes = all - meshTriangles(data);
        if (detail.triangles() == 0 && all > ModelDefinition.Detail.MOST_UNSAID) {
            return Optional.of("has " + all + " triangles, more than the " + ModelDefinition.Detail.MOST_UNSAID
                    + " a model may have without saying how many it is drawn with");
        }
        if (cubes > detail.most() || detail.hasFar() && cubes > detail.farTriangles()) {
            return Optional.of("has " + cubes + " triangles in its cubes, which are drawn as they are, more than"
                    + " it says it is drawn with");
        }
        return Optional.empty();
    }

    /** How many triangles a model is drawn with: its meshes' and its cubes', two to a face. */
    public static int triangles(final ModelData data) {
        int count = meshTriangles(data);
        for (final ModelBone bone : data.bones()) {
            count += bone.cubes().size() * CUBE_TRIANGLES;
        }
        return count;
    }

    /** How many triangles a model's meshes have. */
    public static int meshTriangles(final ModelData data) {
        int count = 0;
        for (final ModelBone bone : data.bones()) {
            for (final ModelMesh mesh : bone.meshes()) {
                count += MeshReducer.triangles(mesh);
            }
        }
        return count;
    }

    /** How many triangles the meshes of one bone have. */
    public static int triangles(final ModelBone bone) {
        int count = bone.cubes().size() * CUBE_TRIANGLES;
        for (final ModelMesh mesh : bone.meshes()) {
            count += MeshReducer.triangles(mesh);
        }
        return count;
    }

    /* A mesh brought down to {@code share} of its triangles and, painted with no picture, given one from the sides. */
    private static ModelMesh shape(final ModelMesh mesh, final double share, final double scale) {
        final boolean bare = mesh.uvs().length == 0 && !mesh.skinned() && mesh.morphs().isEmpty();
        ModelMesh out = bare ? welded(mesh) : mesh;
        if (share < 1.0) {
            out = MeshReducer.reduce(out, Math.max(LEAST, (int) Math.round(MeshReducer.triangles(mesh) * share)));
        }
        return bare ? fromTheSides(out, scale) : out == mesh ? mesh : out;
    }

    /* A mesh whose corners at one place are one corner, so its triangles fold together; its normals are let go. */
    private static ModelMesh welded(final ModelMesh mesh) {
        final float[] positions = mesh.positions();
        final Map<Point, Integer> seen = new HashMap<>();
        final int[] onto = new int[mesh.corners()];
        final List<Float> kept = new ArrayList<>();
        for (int i = 0; i < onto.length; i++) {
            final Point point = new Point(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]);
            final Integer before = seen.putIfAbsent(point, seen.size());
            onto[i] = before == null ? seen.size() - 1 : before;
            if (before == null) {
                kept.add(point.x());
                kept.add(point.y());
                kept.add(point.z());
            }
        }
        final float[] corners = new float[kept.size()];
        for (int i = 0; i < corners.length; i++) {
            corners[i] = kept.get(i);
        }
        final int[] triangles = new int[mesh.triangles().length];
        for (int i = 0; i < triangles.length; i++) {
            triangles[i] = onto[mesh.triangles()[i]];
        }
        return new ModelMesh(corners, new float[0], new float[0], triangles, new int[0], new float[0], List.of(),
                mesh.texture());
    }

    /*
     * Pictures laid from the sides: each triangle takes the picture as seen along the axis it faces most, X, Y or Z,
     * a texture across every sixteen pixels of the size it is drawn, its corners its own so each face is flat.
     */
    private static ModelMesh fromTheSides(final ModelMesh mesh, final double scale) {
        final float[] points = mesh.positions();
        final int[] order = mesh.triangles();
        final float[] positions = new float[order.length * 3];
        final float[] uvs = new float[order.length * 2];
        final int[] triangles = new int[order.length];
        final float across = (float) (scale / PIXELS);
        for (int t = 0; t < order.length; t += 3) {
            final int a = order[t] * 3;
            final int b = order[t + 1] * 3;
            final int c = order[t + 2] * 3;
            final float ux = points[b] - points[a];
            final float uy = points[b + 1] - points[a + 1];
            final float uz = points[b + 2] - points[a + 2];
            final float vx = points[c] - points[a];
            final float vy = points[c + 1] - points[a + 1];
            final float vz = points[c + 2] - points[a + 2];
            final float nx = Math.abs(uy * vz - uz * vy);
            final float ny = Math.abs(uz * vx - ux * vz);
            final float nz = Math.abs(ux * vy - uy * vx);
            for (int k = 0; k < 3; k++) {
                final int from = order[t + k] * 3;
                final float x = points[from];
                final float y = points[from + 1];
                final float z = points[from + 2];
                System.arraycopy(points, from, positions, (t + k) * 3, 3);
                triangles[t + k] = t + k;
                // Seen from the side it faces most; a picture's height runs down, as the world's runs up.
                if (nx >= ny && nx >= nz) {
                    uvs[(t + k) * 2] = z * across;
                    uvs[(t + k) * 2 + 1] = -y * across;
                } else if (ny >= nz) {
                    uvs[(t + k) * 2] = x * across;
                    uvs[(t + k) * 2 + 1] = z * across;
                } else {
                    uvs[(t + k) * 2] = x * across;
                    uvs[(t + k) * 2 + 1] = -y * across;
                }
            }
        }
        return new ModelMesh(positions, new float[0], uvs, triangles, new int[0], new float[0], List.of(),
                mesh.texture());
    }

    /* A corner's place, compared exactly. */
    private record Point(float x, float y, float z) {
    }
}
