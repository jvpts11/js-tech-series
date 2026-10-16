/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.List;
import java.util.Objects;

/**
 * A mesh of a model: corners and the triangles between them, in the model's own space and its pixels, as Blender,
 * Maya and the others export them. Each corner has a place, a normal and a point on a texture; a skinned mesh gives
 * each corner up to four bones of its skin and how much each moves it, so it bends with them; and morph targets move
 * corners toward other shapes by weight.
 *
 * @param positions corners, three numbers each
 * @param normals   the normal at each corner, three numbers each; empty to work them out from the triangles
 * @param uvs       the point on the texture at each corner, two numbers each, from 0 to 1, top left first
 * @param triangles the corners of each triangle, three each, counter-clockwise seen from the front
 * @param joints    for a skinned mesh, four bones of the skin per corner; empty for one that does not bend
 * @param weights   how much each of those four moves the corner, together 1; empty with {@code joints}
 * @param morphs    the shapes it can be moved toward
 * @param texture   which of the model's textures it is painted with, from 0
 */
public record ModelMesh(float[] positions, float[] normals, float[] uvs, int[] triangles, int[] joints,
                        float[] weights, List<MorphTarget> morphs, int texture) {

    public ModelMesh {
        Objects.requireNonNull(positions, "positions");
        Objects.requireNonNull(normals, "normals");
        Objects.requireNonNull(uvs, "uvs");
        Objects.requireNonNull(triangles, "triangles");
        Objects.requireNonNull(joints, "joints");
        Objects.requireNonNull(weights, "weights");
        morphs = List.copyOf(morphs);
        if (positions.length % 3 != 0 || triangles.length % 3 != 0) {
            throw new IllegalArgumentException("a mesh's corners come in threes, and so do its triangles' corners");
        }
    }

    /** How many corners it has. */
    public int corners() {
        return this.positions.length / 3;
    }

    /** Whether it bends with the bones of a skin. */
    public boolean skinned() {
        return this.joints.length > 0;
    }

    /**
     * A shape a mesh can be moved toward, as Blender's shape keys: how far each corner moves at full weight.
     *
     * @param name  its name, by which an animation sets its weight
     * @param moves how far each corner moves, three numbers each
     */
    public record MorphTarget(String name, float[] moves) {

        public MorphTarget {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(moves, "moves");
        }
    }
}
