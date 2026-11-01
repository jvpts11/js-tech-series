/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.universe;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;
import dev.jstech.space.universe.SolarBody;

/**
 * The colours the worlds of the Solar System are seen in as points, from outside their region of its space: the
 * Overworld a pale blue, the Moon a light grey, Mars an orange red, as a planet shines in another's sky.
 */
@PaletteHolder
public final class WorldPoints {

    /** The worlds' colours as points. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "world_points",
            new Colours(0xFFB4D2FF, 0xFFE4E2DA, 0xFFFF9A5A));

    private WorldPoints() {
    }

    /** The colour {@code body} is seen in as a point, as ARGB. */
    public static int of(final SolarBody body) {
        final Colours colours = PALETTE.get();
        return switch (body) {
            case OVERWORLD -> colours.overworld();
            case MOON -> colours.moon();
            case MARS -> colours.mars();
        };
    }

    /**
     * The worlds' colours as points, as ARGB.
     *
     * @param overworld the Overworld's
     * @param moon      the Moon's
     * @param mars      Mars'
     */
    public record Colours(int overworld, int moon, int mars) {
    }
}
