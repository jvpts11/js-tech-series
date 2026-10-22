/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

/**
 * Daylight come down through blocks that tint it, as through a roof of stained glass: from the highest of them, each
 * place below down to the first wall, or {@link #DEPTH} places, is lit by day in the colour the blocks above gave the
 * light, every one it passed tinting it again. Pure; colours are {@code 0xRRGGBB}.
 */
public final class SkyTints {

    /** How far below a tinting block daylight keeps its colour: as far as the light of a lamp goes. */
    public static final int DEPTH = 16;

    private SkyTints() {
    }

    /**
     * The places below the tinting block at {@code x, y, z} and the colour daylight lights them in; nothing for a
     * block that tints nothing, or one within {@link #DEPTH} places below another that tints, whose places the higher
     * gives.
     */
    public static void below(final int x, final int y, final int z, final LightFlood.IPassage passage,
                             final ISink sink) {
        int colour = passage.tint(x, y, z);
        if (colour == LightMath.WHITE) {
            return;
        }
        for (int up = 1; up <= DEPTH; up++) {
            if (passage.tint(x, y + up, z) != LightMath.WHITE) {
                // A tinting block above lights this one's places, its colour taken through this one.
                return;
            }
            if (passage.dims(x, y + up, z) >= LightFlood.OPAQUE) {
                break;
            }
        }
        for (int down = 1; down <= DEPTH; down++) {
            final int at = y - down;
            if (passage.dims(x, at, z) >= LightFlood.OPAQUE) {
                return;
            }
            final int tint = passage.tint(x, at, z);
            if (tint != LightMath.WHITE) {
                colour = LightMath.multiply(colour, tint);
            }
            sink.lit(x, at, z, colour);
        }
    }

    /** Where the places below go. */
    public interface ISink {

        /** The place {@code x, y, z} is lit by day in {@code colour}. */
        void lit(int x, int y, int z, int colour);
    }
}
