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
import org.jetbrains.annotations.Nullable;

/**
 * A model's shape worked out once, as it is drawn: every face of every cube and mesh of each bone laid out as the
 * corners of four-cornered faces in the bone's own space, with the point of the texture and the way it faces at each,
 * grouped by the texture they are painted with and whether they glow. A mesh that bends with a skin, or moves toward
 * a morph target, is kept as it is, to be worked out each frame. Built by {@link GeometryBaker}; pure.
 *
 * @param bones    the parts of each bone, in the model's order of bones
 * @param deformed the meshes worked out each frame
 */
public record BakedGeometry(List<List<Part>> bones, List<Deformed> deformed) {

    /** How many numbers each corner has: its place, its point on the texture, the way it faces. */
    public static final int STRIDE = 8;

    public BakedGeometry {
        bones = bones.stream().map(List::copyOf).toList();
        deformed = List.copyOf(deformed);
    }

    /** The parts of bone {@code bone}. */
    public List<Part> partsOf(final int bone) {
        return this.bones.get(bone);
    }

    /** How many four-cornered faces it has in all, those worked out each frame aside. */
    public int faces() {
        int out = 0;
        for (final List<Part> parts : this.bones) {
            for (final Part part : parts) {
                out += part.faces();
            }
        }
        return out;
    }

    /**
     * The faces of a bone painted with one texture, in its own space: {@link #STRIDE} numbers for each corner (x, y,
     * z in pixels, u and v from 0 to 1, and the normal), four corners for each face, a triangle's last corner twice.
     *
     * @param texture  which of the model's textures they are painted with
     * @param glow     whether they glow in the dark
     * @param vertices their corners
     */
    public record Part(int texture, boolean glow, float[] vertices) {

        public Part {
            Objects.requireNonNull(vertices, "vertices");
            if (vertices.length % (STRIDE * 4) != 0) {
                throw new IllegalArgumentException("a part's corners come four to a face");
            }
        }

        /** How many faces it has. */
        public int faces() {
            return this.vertices.length / (STRIDE * 4);
        }
    }

    /**
     * A mesh worked out each frame: one that bends with the bones of a skin, whose corners are then in the model's own
     * space, or one that moves toward morph targets, in its bone's.
     *
     * @param bone    the bone it hangs from, by its place
     * @param glow    whether it glows
     * @param mesh    the mesh, at rest
     * @param skin    the skin it bends with, or none
     */
    public record Deformed(int bone, boolean glow, ModelMesh mesh, @Nullable ModelSkin skin) {

        public Deformed {
            Objects.requireNonNull(mesh, "mesh");
        }
    }
}
