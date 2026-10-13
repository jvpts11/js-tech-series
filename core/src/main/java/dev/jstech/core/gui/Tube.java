/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * What a monitor's picture tube can show: every colour, one phosphor lit brighter or dimmer, or the sixteen colours of
 * the first colour adapters.
 *
 * <p>A screen is drawn in full colour and the tube decides what reaches the glass: a monochrome tube shows how bright
 * each colour reads in its own phosphor (white, green or amber), and the sixteen-colour tube shows the nearest of its
 * sixteen. So one system looks like itself on a colour monitor, green on a green one and amber on an amber one, with
 * nothing drawn twice.
 *
 * <p>Pure maths, no rendering: the client applies the same rule to whole pictures on the graphics card, and a unit
 * test can check it here.
 */
@PaletteHolder
public enum Tube {

    /** A colour monitor: the picture as it was drawn. */
    COLOUR,
    /** A white-phosphor tube, the paper-white of the first workstation monitors. */
    WHITE,
    /** The green-phosphor tube. */
    GREEN,
    /** The amber-phosphor tube. */
    AMBER,
    /** The sixteen colours of the first colour adapter. */
    SIXTEEN;

    /** The white and the amber phosphors; the green is the Vintage skin's own, kept in {@link Phosphor}. */
    private static final Palette<Glows> GLOWS = Palettes.declare(JsCore.MODID, "gui/tubes",
            new Glows(0xFFECEEF0, 0xFFFFB028));

    /*
     * The sixteen colours, in the adapter's own order: black, the six dark ones, light grey, dark grey, the six
     * bright ones, white. The dark yellow is the brown the real monitor made of it.
     */
    private static final int[] SIXTEEN_COLOURS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    /** Whether the tube shows one colour only. */
    public boolean monochrome() {
        return this == WHITE || this == GREEN || this == AMBER;
    }

    /** Whether the tube changes anything at all. */
    public boolean filters() {
        return this != COLOUR;
    }

    /**
     * The phosphor a monochrome tube glows at full brightness. A tube that shows colours has no one phosphor, and
     * answers the white one, which nothing it shows is lit by.
     */
    public int glow() {
        return switch (this) {
            case GREEN -> Phosphor.glow();
            case AMBER -> GLOWS.get().amber();
            case WHITE, COLOUR, SIXTEEN -> GLOWS.get().white();
        };
    }

    /** {@code argb} as this tube shows it, keeping its alpha. */
    public int apply(final int argb) {
        return switch (this) {
            case COLOUR -> argb;
            case WHITE, GREEN, AMBER -> Phosphor.tint(argb, glow());
            case SIXTEEN -> (argb >>> 24 << 24) | nearestOfSixteen(argb);
        };
    }

    /** The sixteen colours as RGB, in the adapter's order, for the shader that paints a whole picture. */
    public static int[] sixteenColours() {
        return SIXTEEN_COLOURS.clone();
    }

    /** The one of the sixteen closest to {@code argb}, by plain distance in RGB. */
    static int nearestOfSixteen(final int argb) {
        final int r = (argb >> 16) & 0xFF;
        final int g = (argb >> 8) & 0xFF;
        final int b = argb & 0xFF;
        int best = SIXTEEN_COLOURS[0];
        int bestDistance = Integer.MAX_VALUE;
        for (final int colour : SIXTEEN_COLOURS) {
            final int dr = r - ((colour >> 16) & 0xFF);
            final int dg = g - ((colour >> 8) & 0xFF);
            final int db = b - (colour & 0xFF);
            final int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = colour;
            }
        }
        return best;
    }

    private record Glows(int white, int amber) {
    }
}
