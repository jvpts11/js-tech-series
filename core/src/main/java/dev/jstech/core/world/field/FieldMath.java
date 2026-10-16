/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.field;

/**
 * How a field over chunks moves on by one step, without the game: each chunk keeps a share of its value and hands the
 * rest evenly to its four neighbours, all of it fading by the decay. A chunk is packed into one number, its x in the
 * low half and its z in the high, as the game packs them.
 */
public final class FieldMath {

    private static final int SIDES = 4;

    private FieldMath() {
    }

    /** The chunk {@code x, z} as one number. */
    public static long pack(final int x, final int z) {
        return Integer.toUnsignedLong(x) | (long) z << Integer.SIZE;
    }

    /** The x of a packed chunk. */
    public static int x(final long packed) {
        return (int) packed;
    }

    /** The z of a packed chunk. */
    public static int z(final long packed) {
        return (int) (packed >> Integer.SIZE);
    }

    /**
     * Moves the values on a step, adding what each chunk holds after it to {@code into}.
     *
     * @param chunks the chunks that hold some, packed
     * @param values what each holds, in the same order
     * @param spread the share of each chunk's value handed to its neighbours, 0 to 1
     * @param decay  the share of everything that fades, 0 to 1
     */
    public static void step(final long[] chunks, final float[] values, final double spread, final double decay,
                            final ISink into) {
        final float keep = (float) ((1 - spread) * (1 - decay));
        final float share = (float) (spread / SIDES * (1 - decay));
        for (int i = 0; i < chunks.length; i++) {
            final long at = chunks[i];
            final float value = values[i];
            into.add(at, value * keep);
            if (share > 0) {
                final int x = x(at);
                final int z = z(at);
                into.add(pack(x + 1, z), value * share);
                into.add(pack(x - 1, z), value * share);
                into.add(pack(x, z + 1), value * share);
                into.add(pack(x, z - 1), value * share);
            }
        }
    }

    /** Where a step's values are added up. */
    @FunctionalInterface
    public interface ISink {

        /** Adds {@code amount} to the chunk {@code chunk}, packed. */
        void add(long chunk, float amount);
    }
}
