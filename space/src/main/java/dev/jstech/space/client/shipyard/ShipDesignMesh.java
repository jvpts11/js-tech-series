/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.space.ship.ShipPartModels;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The faces of a ship's models as the Starship Design draws them, in the ship's frame, worked out once for a ship as
 * it stands: which of its parts the line from the camera through the mouse meets first, by the models' own faces, so
 * the empty corners round a wing are empty; and the outline of a part as the camera sees it, the edges between its
 * faces turned toward the camera and those turned away.
 */
final class ShipDesignMesh {

    private final List<ShipPartModels.Placed> placed;
    private final List<Shape> shapes;

    /* The size of a pixel of a model's file, in blocks. */
    private static final float PIXEL = 1.0F / 16.0F;
    /* How many numbers a triangle has, three corners of three; and how finely corners are matched as the same. */
    private static final int TRIANGLE = 9;
    private static final float WELD = 512.0F;
    private static final double EPSILON = 1.0e-9;

    private ShipDesignMesh(final List<ShipPartModels.Placed> placed, final List<Shape> shapes) {
        this.placed = placed;
        this.shapes = shapes;
    }

    /**
     * The faces of {@code placed}, the models of a ship drawn for {@code owner}, each where its instance stands now;
     * {@code was} again when it is of the same models.
     */
    static ShipDesignMesh of(final BlockEntity owner, final List<ShipPartModels.Placed> placed,
                             @Nullable final ShipDesignMesh was) {
        if (was != null && was.placed.equals(placed)) {
            return was;
        }
        final List<Shape> shapes = new ArrayList<>();
        for (final ShipPartModels.Placed part : placed) {
            final Optional<KitModel> model = KitModels.get(part.model());
            if (model.isPresent()) {
                // The same instance the view draws the model with, so its hidden bones and moved gear are the same.
                final KitInstance instance = KitAnimations.part(owner, part.model().getPath() + "@" + part.z(),
                        model.get());
                shapes.add(Shape.of(part, model.get(), instance));
            }
        }
        return new ShipDesignMesh(List.copyOf(placed), shapes);
    }

    /** The number of the part whose faces the line from {@code from} along {@code way} meets first; -1 for none. */
    int partAt(final Vec3 from, final Vec3 way) {
        double nearest = Double.MAX_VALUE;
        int best = -1;
        for (final Shape shape : this.shapes) {
            if (!shape.boxMet(from, way, nearest)) {
                continue;
            }
            final double at = shape.met(from, way, nearest);
            if (at < nearest) {
                nearest = at;
                best = shape.part();
            }
        }
        return best;
    }

    /**
     * The outline of the part numbered {@code part} seen from {@code eye}: each edge, as its two ends, between one of
     * its faces turned toward the eye and one turned away, or along the rim of a face with none beside it.
     */
    List<float[]> outline(final int part, final Vec3 eye) {
        final List<float[]> out = new ArrayList<>();
        for (final Shape shape : this.shapes) {
            if (shape.part() == part) {
                shape.outline(eye, out);
            }
        }
        return out;
    }

    /*
     * One model's triangles in the ship's frame, the box round them, and, worked out the first time its outline is
     * asked for, each edge with the triangles either side of it.
     */
    private static final class Shape {

        private final int part;
        private final float[] triangles;
        private final float[] low;
        private final float[] high;
        private @Nullable List<int[]> edges;

        private Shape(final int part, final float[] triangles, final float[] low, final float[] high) {
            this.part = part;
            this.triangles = triangles;
            this.low = low;
            this.high = high;
        }

        /* The faces of {@code model} placed as {@code part} is, its bones where {@code instance} has them. */
        static Shape of(final ShipPartModels.Placed part, final KitModel model, final KitInstance instance) {
            final Matrix4f place = new Matrix4f().translate(0, 0, part.z());
            if (part.flipped()) {
                place.scale(-1, 1, 1);
            }
            place.scale(PIXEL * model.scale());
            final Matrix4f[] world = instance.world();
            final List<List<BakedGeometry.Part>> bones = model.geometry().bones();
            final FloatGrower out = new FloatGrower();
            final Matrix4f bone = new Matrix4f();
            final Vector3f corner = new Vector3f();
            final float[] quad = new float[12];
            for (int i = 0; i < bones.size() && i < world.length; i++) {
                if (bones.get(i).isEmpty() || instance.pose().hidden(i)) {
                    continue;
                }
                bone.set(place).mul(world[i]);
                for (final BakedGeometry.Part faces : bones.get(i)) {
                    final float[] v = faces.vertices();
                    for (int at = 0; at < v.length; at += 4 * BakedGeometry.STRIDE) {
                        for (int k = 0; k < 4; k++) {
                            final int c = at + k * BakedGeometry.STRIDE;
                            bone.transformPosition(corner.set(v[c], v[c + 1], v[c + 2]));
                            quad[3 * k] = corner.x;
                            quad[3 * k + 1] = corner.y;
                            quad[3 * k + 2] = corner.z;
                        }
                        out.triangle(quad, 0, 1, 2);
                        out.triangle(quad, 0, 2, 3);
                    }
                }
            }
            final float[] triangles = out.done();
            final float[] low = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
            final float[] high = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (int at = 0; at < triangles.length; at += 3) {
                for (int axis = 0; axis < 3; axis++) {
                    low[axis] = Math.min(low[axis], triangles[at + axis]);
                    high[axis] = Math.max(high[axis], triangles[at + axis]);
                }
            }
            return new Shape(part.part(), triangles, low, high);
        }

        int part() {
            return this.part;
        }

        /* Whether the line meets the box round its faces nearer than {@code nearest}. */
        boolean boxMet(final Vec3 from, final Vec3 way, final double nearest) {
            final double[] o = {from.x, from.y, from.z};
            final double[] d = {way.x, way.y, way.z};
            double enter = 0;
            double leave = nearest;
            for (int axis = 0; axis < 3; axis++) {
                if (Math.abs(d[axis]) < EPSILON) {
                    if (o[axis] < this.low[axis] || o[axis] > this.high[axis]) {
                        return false;
                    }
                    continue;
                }
                final double a = (this.low[axis] - o[axis]) / d[axis];
                final double b = (this.high[axis] - o[axis]) / d[axis];
                enter = Math.max(enter, Math.min(a, b));
                leave = Math.min(leave, Math.max(a, b));
            }
            return enter <= leave;
        }

        /* How far along the line it meets its nearest face, nearer than {@code nearest}; {@code nearest} for none. */
        double met(final Vec3 from, final Vec3 way, final double nearest) {
            double best = nearest;
            final float[] t = this.triangles;
            for (int at = 0; at < t.length; at += TRIANGLE) {
                final double e1x = t[at + 3] - t[at];
                final double e1y = t[at + 4] - t[at + 1];
                final double e1z = t[at + 5] - t[at + 2];
                final double e2x = t[at + 6] - t[at];
                final double e2y = t[at + 7] - t[at + 1];
                final double e2z = t[at + 8] - t[at + 2];
                final double px = way.y * e2z - way.z * e2y;
                final double py = way.z * e2x - way.x * e2z;
                final double pz = way.x * e2y - way.y * e2x;
                final double det = e1x * px + e1y * py + e1z * pz;
                if (Math.abs(det) < EPSILON) {
                    continue;
                }
                final double inv = 1 / det;
                final double sx = from.x - t[at];
                final double sy = from.y - t[at + 1];
                final double sz = from.z - t[at + 2];
                final double u = (sx * px + sy * py + sz * pz) * inv;
                if (u < 0 || u > 1) {
                    continue;
                }
                final double qx = sy * e1z - sz * e1y;
                final double qy = sz * e1x - sx * e1z;
                final double qz = sx * e1y - sy * e1x;
                final double v = (way.x * qx + way.y * qy + way.z * qz) * inv;
                if (v < 0 || u + v > 1) {
                    continue;
                }
                final double along = (e2x * qx + e2y * qy + e2z * qz) * inv;
                if (along > 0 && along < best) {
                    best = along;
                }
            }
            return best;
        }

        /* Adds to {@code out} the edges of its outline seen from {@code eye}. */
        void outline(final Vec3 eye, final List<float[]> out) {
            List<int[]> all = this.edges;
            if (all == null) {
                all = edges();
                this.edges = all;
            }
            final int count = this.triangles.length / TRIANGLE;
            final boolean[] toward = new boolean[count];
            final boolean[] flat = new boolean[count];
            final float[] t = this.triangles;
            for (int i = 0; i < count; i++) {
                final int at = i * TRIANGLE;
                final double e1x = t[at + 3] - t[at];
                final double e1y = t[at + 4] - t[at + 1];
                final double e1z = t[at + 5] - t[at + 2];
                final double e2x = t[at + 6] - t[at];
                final double e2y = t[at + 7] - t[at + 1];
                final double e2z = t[at + 8] - t[at + 2];
                final double nx = e1y * e2z - e1z * e2y;
                final double ny = e1z * e2x - e1x * e2z;
                final double nz = e1x * e2y - e1y * e2x;
                flat[i] = nx * nx + ny * ny + nz * nz < EPSILON;
                toward[i] = nx * (eye.x - t[at]) + ny * (eye.y - t[at + 1]) + nz * (eye.z - t[at + 2]) > 0;
            }
            for (final int[] edge : all) {
                // An edge holds its two corners' places, then the triangles beside it, -1 for none.
                final int a = edge[2];
                final int b = edge[3];
                final boolean rim = b < 0 && !flat[a] && toward[a];
                final boolean fold = b >= 0 && !flat[a] && !flat[b] && toward[a] != toward[b];
                if (rim || fold) {
                    out.add(new float[] {t[edge[0]], t[edge[0] + 1], t[edge[0] + 2], t[edge[1]], t[edge[1] + 1],
                        t[edge[1] + 2]});
                }
            }
        }

        /* Each edge, by its corners' places among the triangles' numbers, with the one or two triangles beside it. */
        private List<int[]> edges() {
            final Map<Long, Integer> corners = new HashMap<>();
            final Map<Long, int[]> byPair = new HashMap<>();
            final float[] t = this.triangles;
            final int count = t.length / TRIANGLE;
            final int[] id = new int[3];
            for (int i = 0; i < count; i++) {
                for (int k = 0; k < 3; k++) {
                    final int at = i * TRIANGLE + 3 * k;
                    final long key = key(t[at], t[at + 1], t[at + 2]);
                    id[k] = corners.computeIfAbsent(key, any -> corners.size());
                }
                for (int k = 0; k < 3; k++) {
                    final int p = id[k];
                    final int q = id[(k + 1) % 3];
                    if (p == q) {
                        continue;
                    }
                    final long pair = (long) Math.min(p, q) << 32 | Math.max(p, q);
                    final int[] edge = byPair.get(pair);
                    if (edge == null) {
                        byPair.put(pair, new int[] {i * TRIANGLE + 3 * k, i * TRIANGLE + 3 * ((k + 1) % 3), i, -1});
                    } else if (edge[3] < 0) {
                        edge[3] = i;
                    }
                }
            }
            return List.copyOf(byPair.values());
        }

        private static long key(final float x, final float y, final float z) {
            final long a = Math.round(x * WELD) & 0x1FFFFF;
            final long b = Math.round(y * WELD) & 0x1FFFFF;
            final long c = Math.round(z * WELD) & 0x1FFFFF;
            return a << 42 | b << 21 | c;
        }
    }

    /* Triangles' numbers gathered as they come. */
    private static final class FloatGrower {

        private float[] values = new float[TRIANGLE * 256];
        private int size;

        void triangle(final float[] quad, final int a, final int b, final int c) {
            if (this.size + TRIANGLE > this.values.length) {
                final float[] more = new float[this.values.length * 2];
                System.arraycopy(this.values, 0, more, 0, this.size);
                this.values = more;
            }
            for (final int corner : new int[] {a, b, c}) {
                this.values[this.size++] = quad[3 * corner];
                this.values[this.size++] = quad[3 * corner + 1];
                this.values[this.size++] = quad[3 * corner + 2];
            }
        }

        float[] done() {
            final float[] out = new float[this.size];
            System.arraycopy(this.values, 0, out, 0, this.size);
            return out;
        }
    }
}
