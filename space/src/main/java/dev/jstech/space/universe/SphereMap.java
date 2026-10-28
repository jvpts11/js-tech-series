/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

/**
 * How the blocks of a round world lie on its sphere: X is the longitude and Z the latitude, the world's spawn at the
 * middle of its near side (0°, 0°), its west and east ends at the middle of its far side (±180°) and its north and
 * south ends at its poles. A world is as wide from west to east as its whole round, and half as tall from north to
 * south, so a degree is the same number of blocks both ways. The one map drives each place's sky, where a landing
 * comes down, and how the generator lays the world out.
 *
 * <p>Pure: no game types.
 */
public final class SphereMap {

    /** The Earth's width, in kilometres, which every world's is measured against. */
    public static final double EARTH_DIAMETER = 12_742;
    private static final double ROUND = 360;
    private static final double POLE_TO_POLE = 180;

    private SphereMap() {
    }

    /**
     * How wide a world of {@code diameter} kilometres is, in blocks from west to east: the Overworld's border times its
     * width over the Earth's, never more than the Overworld's, since the game's limits hold.
     */
    public static double widthOf(final double overworldWidth, final double diameter) {
        return overworldWidth * Math.min(1.0, diameter / EARTH_DIAMETER);
    }

    /** The longitude of a block's X in a world {@code width} blocks wide, in degrees east, from -180 to 180. */
    public static double longitude(final double x, final double width) {
        return x / width * ROUND;
    }

    /** The latitude of a block's Z in a world {@code width} blocks wide, in degrees north, from -90 to 90. */
    public static double latitude(final double z, final double width) {
        return -z / (width / 2) * POLE_TO_POLE;
    }

    /** The X of a longitude, in degrees east, in a world {@code width} blocks wide. */
    public static double x(final double longitude, final double width) {
        return longitude / ROUND * width;
    }

    /** The Z of a latitude, in degrees north, in a world {@code width} blocks wide. */
    public static double z(final double latitude, final double width) {
        return -latitude / POLE_TO_POLE * (width / 2);
    }

    /** How many blocks a degree is, either way, in a world {@code width} blocks wide. */
    public static double blocksPerDegree(final double width) {
        return width / ROUND;
    }
}
