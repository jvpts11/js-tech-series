/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One way a structure can stand in the world: turned, mirrored or lying on its side, as a signed permutation of the
 * three axes. Each axis of the world takes one axis of the pattern, the same way or reversed. Pure, so it is tested
 * without the game.
 *
 * <p>The sets a multiblock declares it is found in: its four horizontal turns, always; the same mirrored, for a shape
 * that is not symmetric; the twenty-four ways a block can lie, turned and on its side; and those mirrored too.
 *
 * @param xFrom the pattern's axis the world's x takes: 0 for x, 1 for y, 2 for z
 * @param xSign +1, or -1 reversed
 * @param yFrom the pattern's axis the world's y takes
 * @param ySign +1, or -1 reversed
 * @param zFrom the pattern's axis the world's z takes
 * @param zSign +1, or -1 reversed
 */
public record Orientation(int xFrom, int xSign, int yFrom, int ySign, int zFrom, int zSign) {

    /** As drawn, facing north. */
    public static final Orientation NORTH = new Orientation(0, 1, 1, 1, 2, 1);
    /** Turned a quarter clockwise, seen from above. */
    public static final Orientation EAST = new Orientation(2, -1, 1, 1, 0, 1);
    /** Turned a half. */
    public static final Orientation SOUTH = new Orientation(0, -1, 1, 1, 2, -1);
    /** Turned three quarters clockwise. */
    public static final Orientation WEST = new Orientation(2, 1, 1, 1, 0, -1);
    /* A mirror across the pattern's x. */
    private static final Orientation MIRROR = new Orientation(0, -1, 1, 1, 2, 1);
    private static final int AXES = 3;

    public Orientation {
        if (xFrom == yFrom || yFrom == zFrom || xFrom == zFrom || out(xFrom) || out(yFrom) || out(zFrom)
                || Math.abs(xSign) != 1 || Math.abs(ySign) != 1 || Math.abs(zSign) != 1) {
            throw new IllegalArgumentException("an orientation takes each axis once, the same way or reversed");
        }
    }

    /** The four horizontal turns, north first: every structure is found in these. */
    public static List<Orientation> turns() {
        return List.of(NORTH, EAST, SOUTH, WEST);
    }

    /** The four turns, then the same mirrored. */
    public static List<Orientation> turnsAndMirrors() {
        final List<Orientation> out = new ArrayList<>(turns());
        for (final Orientation turn : turns()) {
            out.add(MIRROR.then(turn));
        }
        return out;
    }

    /** Every way a structure can lie, upright and on its side, the four turns first: twenty-four. */
    public static List<Orientation> lying() {
        final List<Orientation> out = new ArrayList<>(turns());
        for (final Orientation each : every()) {
            if (each.proper() && !out.contains(each)) {
                out.add(each);
            }
        }
        return out;
    }

    /** Every way it can lie, and the same mirrored: forty-eight. */
    public static List<Orientation> lyingAndMirrors() {
        final List<Orientation> out = new ArrayList<>(lying());
        for (final Orientation each : every()) {
            if (!out.contains(each)) {
                out.add(each);
            }
        }
        return out;
    }

    /** Where the pattern's offset ({@code x}, {@code y}, {@code z}) stands in the world. */
    public int[] apply(final int x, final int y, final int z) {
        final int[] in = {x, y, z};
        return new int[] {this.xSign * in[this.xFrom], this.ySign * in[this.yFrom], this.zSign * in[this.zFrom]};
    }

    /** This orientation, then {@code next}: what a pattern stood this way and then turned {@code next} looks like. */
    public Orientation then(final Orientation next) {
        final int[] froms = {this.xFrom, this.yFrom, this.zFrom};
        final int[] signs = {this.xSign, this.ySign, this.zSign};
        final int[] nextFroms = {next.xFrom, next.yFrom, next.zFrom};
        final int[] nextSigns = {next.xSign, next.ySign, next.zSign};
        final int[] f = new int[AXES];
        final int[] s = new int[AXES];
        for (int axis = 0; axis < AXES; axis++) {
            f[axis] = froms[nextFroms[axis]];
            s[axis] = nextSigns[axis] * signs[nextFroms[axis]];
        }
        return new Orientation(f[0], s[0], f[1], s[1], f[2], s[2]);
    }

    /** Whether it turns without mirroring. */
    public boolean proper() {
        return determinant() > 0;
    }

    /** Whether it stands upright: the pattern's height is the world's, the same way up. */
    public boolean upright() {
        return this.yFrom == 1 && this.ySign == 1;
    }

    /** The horizontal turn it is, for one of the four turns; none for a mirrored or lying one. */
    public Optional<Rotation> rotation() {
        final List<Orientation> turns = turns();
        final int index = turns.indexOf(this);
        return index < 0 ? Optional.empty() : Optional.of(index == 0 ? Rotation.NORTH : index == 1 ? Rotation.EAST
                : index == 2 ? Rotation.SOUTH : Rotation.WEST);
    }

    private int determinant() {
        final int[][] m = new int[AXES][AXES];
        m[0][this.xFrom] = this.xSign;
        m[1][this.yFrom] = this.ySign;
        m[2][this.zFrom] = this.zSign;
        return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
    }

    /* Every signed permutation of the axes. */
    private static List<Orientation> every() {
        final List<Orientation> out = new ArrayList<>();
        final int[][] permutations = {{0, 1, 2}, {0, 2, 1}, {1, 0, 2}, {1, 2, 0}, {2, 0, 1}, {2, 1, 0}};
        for (final int[] p : permutations) {
            for (int signs = 0; signs < 8; signs++) {
                out.add(new Orientation(p[0], (signs & 1) == 0 ? 1 : -1, p[1], (signs & 2) == 0 ? 1 : -1, p[2],
                        (signs & 4) == 0 ? 1 : -1));
            }
        }
        return out;
    }

    private static boolean out(final int axis) {
        return axis < 0 || axis >= AXES;
    }
}
