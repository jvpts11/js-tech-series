/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

/**
 * When a turned box moving in a straight line first touches a box standing square to the axes, and from which side:
 * the arithmetic under walking on a structure turned any way. An entity's box is square to the world, a structure's
 * blocks are square to the structure; seen from the structure, the entity's box is turned. Two boxes are apart while
 * some axis keeps their shadows apart, and for two boxes the axes worth trying are fifteen: the three sides of each
 * and the nine crossings of a side of one with a side of the other. Pure: no part of the game.
 *
 * <pre>{@code
 * SweptBoxes.Turned box = new SweptBoxes.Turned();
 * box.set(cx, cy, cz, hx, hy, hz, axes);
 * SweptBoxes.Contact hit = SweptBoxes.sweep(box, mx, my, mz, 0, 0, 0, 1, 1, 1);
 * }</pre>
 */
public final class SweptBoxes {

    /**
     * Two shadows nearer than this on an axis count as apart, so a box sliding over a floor of many blocks does not
     * catch on the seams between them.
     */
    public static final double SKIN = 1.0e-7;
    /* Below this length an axis made by crossing two sides is a side already, and is left out. */
    private static final double PARALLEL = 1.0e-6;
    /* Below this speed along an axis the box is taken for not moving along it. */
    private static final double STILL = 1.0e-12;

    private SweptBoxes() {
    }

    /**
     * Where and when a moving box first touches the other: {@code time} is the share of the motion made before the
     * touch, from 0 to 1; the normal points from the standing box toward the moving one, against the motion. A box
     * already inside the other has {@code overlapping} set, a time of 0, and the normal it is pushed out along the
     * shortest way.
     *
     * @param time        the share of the motion before the touch
     * @param nx          the normal's east part
     * @param ny          the normal's up part
     * @param nz          the normal's south part
     * @param overlapping whether the boxes were already one inside the other
     * @param depth       for an overlapping one, how far it is inside along the normal
     */
    public record Contact(double time, double nx, double ny, double nz, boolean overlapping, double depth) {
    }

    /**
     * Where the moving box first touches the box from {@code (minX, minY, minZ)} to {@code (maxX, maxY, maxZ)},
     * moving by {@code (vx, vy, vz)} over the whole motion; null when it does not touch it.
     */
    public static Contact sweep(final Turned moving, final double vx, final double vy, final double vz,
                                final double minX, final double minY, final double minZ, final double maxX,
                                final double maxY, final double maxZ) {
        final double ax = (minX + maxX) / 2;
        final double ay = (minY + maxY) / 2;
        final double az = (minZ + maxZ) / 2;
        final double hx = (maxX - minX) / 2;
        final double hy = (maxY - minY) / 2;
        final double hz = (maxZ - minZ) / 2;
        double enter = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        double enterX = 0;
        double enterY = 0;
        double enterZ = 0;
        double leastDepth = Double.POSITIVE_INFINITY;
        double pushX = 0;
        double pushY = 0;
        double pushZ = 0;
        final double[] axes = moving.axes;
        for (int k = 0; k < moving.count; k++) {
            final double lx = axes[k * 3];
            final double ly = axes[k * 3 + 1];
            final double lz = axes[k * 3 + 2];
            final double ra = hx * Math.abs(lx) + hy * Math.abs(ly) + hz * Math.abs(lz);
            final double rb = moving.radii[k];
            final double ca = ax * lx + ay * ly + az * lz;
            final double cb = moving.cx * lx + moving.cy * ly + moving.cz * lz;
            final double minA = ca - ra;
            final double maxA = ca + ra;
            final double minB = cb - rb;
            final double maxB = cb + rb;
            final double depth = Math.min(maxA - minB, maxB - minA);
            if (depth < leastDepth) {
                leastDepth = depth;
                final double sign = cb >= ca ? 1 : -1;
                pushX = lx * sign;
                pushY = ly * sign;
                pushZ = lz * sign;
            }
            final double v = vx * lx + vy * ly + vz * lz;
            if (Math.abs(v) < STILL) {
                if (maxB <= minA + SKIN || minB >= maxA - SKIN) {
                    return null;
                }
                continue;
            }
            final double t0;
            final double t1;
            if (v > 0) {
                t0 = (minA + SKIN - maxB) / v;
                t1 = (maxA - SKIN - minB) / v;
            } else {
                t0 = (maxA - SKIN - minB) / v;
                t1 = (minA + SKIN - maxB) / v;
            }
            if (t0 > enter) {
                enter = t0;
                final double sign = v > 0 ? -1 : 1;
                enterX = lx * sign;
                enterY = ly * sign;
                enterZ = lz * sign;
            }
            if (t1 < exit) {
                exit = t1;
            }
            if (enter > exit || exit < 0) {
                return null;
            }
        }
        if (enter > 1) {
            return null;
        }
        if (enter < 0) {
            if (leastDepth <= SKIN) {
                return null;
            }
            return new Contact(0, pushX, pushY, pushZ, true, leastDepth);
        }
        return new Contact(enter, enterX, enterY, enterZ, false, 0);
    }

    /**
     * A box turned any way: its centre, its three sides as unit directions, and half its size along each. It is
     * changed in place as it moves, so a sweep over many blocks makes no garbage; the axes worth trying against a box
     * square to the axes are worked out once, when its sides are set.
     */
    public static final class Turned {

        private final double[] sides = new double[9];
        private final double[] halves = new double[3];
        private final double[] axes = new double[15 * 3];
        private final double[] radii = new double[15];
        private int count;
        private double cx;
        private double cy;
        private double cz;

        /**
         * Sets the box: centre {@code (cx, cy, cz)}, half sizes {@code (h0, h1, h2)} along its sides, and those sides
         * as the rows of {@code sides}, three unit directions at right angles, nine numbers in all.
         */
        public void set(final double centreX, final double centreY, final double centreZ, final double h0,
                        final double h1, final double h2, final double[] sideRows) {
            System.arraycopy(sideRows, 0, sides, 0, 9);
            halves[0] = h0;
            halves[1] = h1;
            halves[2] = h2;
            moveTo(centreX, centreY, centreZ);
            count = 0;
            addAxis(1, 0, 0);
            addAxis(0, 1, 0);
            addAxis(0, 0, 1);
            for (int i = 0; i < 3; i++) {
                addAxis(sides[i * 3], sides[i * 3 + 1], sides[i * 3 + 2]);
            }
            for (int a = 0; a < 3; a++) {
                final double ux = a == 0 ? 1 : 0;
                final double uy = a == 1 ? 1 : 0;
                final double uz = a == 2 ? 1 : 0;
                for (int b = 0; b < 3; b++) {
                    final double sx = sides[b * 3];
                    final double sy = sides[b * 3 + 1];
                    final double sz = sides[b * 3 + 2];
                    addAxis(uy * sz - uz * sy, uz * sx - ux * sz, ux * sy - uy * sx);
                }
            }
        }

        /** Moves the box, its sides and size kept. */
        public void moveTo(final double centreX, final double centreY, final double centreZ) {
            this.cx = centreX;
            this.cy = centreY;
            this.cz = centreZ;
        }

        public double centreX() {
            return cx;
        }

        public double centreY() {
            return cy;
        }

        public double centreZ() {
            return cz;
        }

        /** How far the box reaches from its centre along the axis {@code (x, y, z)}, of unit length. */
        public double reach(final double x, final double y, final double z) {
            double r = 0;
            for (int i = 0; i < 3; i++) {
                r += halves[i] * Math.abs(sides[i * 3] * x + sides[i * 3 + 1] * y + sides[i * 3 + 2] * z);
            }
            return r;
        }

        private void addAxis(final double x, final double y, final double z) {
            final double length = Math.sqrt(x * x + y * y + z * z);
            if (length < PARALLEL) {
                return;
            }
            final double nx = x / length;
            final double ny = y / length;
            final double nz = z / length;
            for (int k = 0; k < count; k++) {
                final double dot = axes[k * 3] * nx + axes[k * 3 + 1] * ny + axes[k * 3 + 2] * nz;
                if (Math.abs(dot) > 1 - PARALLEL) {
                    return;
                }
            }
            axes[count * 3] = nx;
            axes[count * 3 + 1] = ny;
            axes[count * 3 + 2] = nz;
            radii[count] = reach(nx, ny, nz);
            count++;
        }
    }
}
