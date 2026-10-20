/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

/**
 * The shade the game gives a face of a block by the way it faces, for a face of a turned structure, which faces between
 * the sides: the game shades a face by the side it faces, lightest up and darkest down, and a face facing between
 * them takes of each side's shade as much as it faces it, so a face facing a side is shaded as the game shades it.
 */
public final class FaceShade {

    /* A face is taken as shaded when built shaded it came out darker than unshaded, by more than rounding. */
    private static final float ROUNDING = 0.99F;
    /* A face facing up the game shades not at all, so it comes out as bright unshaded; it is taken as shaded. */
    private static final float FACING_UP = 0.5F;
    /* A colour's channels: each a byte, red lowest, then green, blue, and its alpha highest. */
    private static final int CHANNEL = 0xFF;
    private static final int GREEN = 8;
    private static final int BLUE = 16;
    private static final int ALPHA = CHANNEL << 24;

    private FaceShade() {
    }

    /**
     * The shade of a face facing {@code (x, y, z)}, of unit length, from the game's shade of a face facing each side.
     *
     * @param east  the shade of a face facing east or west
     * @param north the shade of a face facing north or south
     * @param up    the shade of a face facing up
     * @param down  the shade of a face facing down
     */
    public static float of(final float x, final float y, final float z, final float east, final float north,
                           final float up, final float down) {
        return x * x * east + z * z * north + y * y * (y > 0 ? up : down);
    }

    /**
     * Whether the game shaded a face, from the brightest part of its colour built shaded and built unshaded, and how
     * much it faces up: a face the game does not shade, a plant's say, comes out the same both ways.
     */
    public static boolean shaded(final int brightestShaded, final int brightestUnshaded, final float facingUp) {
        return brightestUnshaded > 0 && (brightestShaded < brightestUnshaded * ROUNDING || facingUp > FACING_UP);
    }

    /** The colour {@code abgr}, as the game packs a vertex's, its red, green and blue times {@code shade}. */
    public static int shade(final int abgr, final float shade) {
        final int red = Math.min(CHANNEL, Math.round((abgr & CHANNEL) * shade));
        final int green = Math.min(CHANNEL, Math.round((abgr >> GREEN & CHANNEL) * shade));
        final int blue = Math.min(CHANNEL, Math.round((abgr >> BLUE & CHANNEL) * shade));
        return abgr & ALPHA | blue << BLUE | green << GREEN | red;
    }

    /** The brightest of the red, green and blue of the colour {@code abgr}. */
    public static int brightest(final int abgr) {
        return Math.max(abgr & CHANNEL, Math.max(abgr >> GREEN & CHANNEL, abgr >> BLUE & CHANNEL));
    }
}
