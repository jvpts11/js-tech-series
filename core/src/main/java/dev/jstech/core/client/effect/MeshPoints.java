/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.ModelPose;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Points on the faces of one of the Core's models at rest, for an emitter shaped as a mesh: its faces cut into
 * triangles, each picked as often as it is big, a point picked evenly within it, in blocks from the model's origin.
 */
final class MeshPoints {

    private static final float PIXEL = 1.0F / 16.0F;
    private static final int CORNERS = 4;
    private static final Map<String, Optional<Triangles>> MESHES = new ConcurrentHashMap<>();

    private MeshPoints() {
    }

    /** Picks a point on the faces of the model {@code model} into {@code at}; the middle for a model not found. */
    static void sample(final String model, final RandomGenerator random, final double[] at) {
        final Optional<Triangles> mesh = MESHES.computeIfAbsent(model, MeshPoints::read);
        mesh.ifPresent(triangles -> triangles.pick(random, at));
    }

    /** Forgets the meshes read, as the models are read again. */
    static void forget() {
        MESHES.clear();
    }

    private static Optional<Triangles> read(final String model) {
        final ResourceLocation id = ResourceLocation.tryParse(model);
        if (id == null) {
            return Optional.empty();
        }
        return KitModels.get(id).map(MeshPoints::triangles);
    }

    private static Triangles triangles(final KitModel model) {
        final ModelPose pose = new ModelPose(model.data());
        final Matrix4f[] world = new Matrix4f[model.data().bones().size()];
        for (int i = 0; i < world.length; i++) {
            world[i] = new Matrix4f();
        }
        pose.world(world);
        float[] corners = new float[0];
        int count = 0;
        final Vector3f corner = new Vector3f();
        for (int bone = 0; bone < world.length; bone++) {
            for (final BakedGeometry.Part part : model.geometry().partsOf(bone)) {
                final float[] vertices = part.vertices();
                final int quads = vertices.length / (BakedGeometry.STRIDE * CORNERS);
                corners = Arrays.copyOf(corners, corners.length + quads * CORNERS * 3);
                for (int at = 0; at < quads * CORNERS; at++) {
                    final int from = at * BakedGeometry.STRIDE;
                    world[bone].transformPosition(corner.set(vertices[from], vertices[from + 1],
                            vertices[from + 2]));
                    corners[count++] = corner.x * PIXEL;
                    corners[count++] = corner.y * PIXEL;
                    corners[count++] = corner.z * PIXEL;
                }
            }
        }
        return new Triangles(corners);
    }

    /* A model's faces as triangles, two to each four corners, and how far through their area each one ends. */
    private static final class Triangles {

        private final float[] corners;
        private final double[] ends;

        Triangles(final float[] corners) {
            this.corners = corners;
            final int quads = corners.length / (CORNERS * 3);
            this.ends = new double[quads * 2];
            double total = 0;
            for (int q = 0; q < quads; q++) {
                total += area(q * CORNERS, 0, 1, 2);
                this.ends[q * 2] = total;
                total += area(q * CORNERS, 0, 2, 3);
                this.ends[q * 2 + 1] = total;
            }
        }

        void pick(final RandomGenerator random, final double[] at) {
            if (this.ends.length == 0 || this.ends[this.ends.length - 1] <= 0) {
                return;
            }
            final double into = random.nextDouble() * this.ends[this.ends.length - 1];
            int t = Arrays.binarySearch(this.ends, into);
            t = t < 0 ? -t - 1 : t;
            t = Math.min(t, this.ends.length - 1);
            final int base = t / 2 * CORNERS;
            final int b = t % 2 == 0 ? 1 : 2;
            final int c = t % 2 == 0 ? 2 : 3;
            double u = random.nextDouble();
            double v = random.nextDouble();
            if (u + v > 1) {
                u = 1 - u;
                v = 1 - v;
            }
            for (int axis = 0; axis < 3; axis++) {
                final double a0 = this.corners[base * 3 + axis];
                at[axis] = a0 + (this.corners[(base + b) * 3 + axis] - a0) * u
                        + (this.corners[(base + c) * 3 + axis] - a0) * v;
            }
        }

        private double area(final int base, final int a, final int b, final int c) {
            final double[] ab = new double[3];
            final double[] ac = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                ab[axis] = this.corners[(base + b) * 3 + axis] - this.corners[(base + a) * 3 + axis];
                ac[axis] = this.corners[(base + c) * 3 + axis] - this.corners[(base + a) * 3 + axis];
            }
            final double x = ab[1] * ac[2] - ab[2] * ac[1];
            final double y = ab[2] * ac[0] - ab[0] * ac[2];
            final double z = ab[0] * ac[1] - ab[1] * ac[0];
            return Math.sqrt(x * x + y * y + z * z) / 2;
        }
    }
}
