/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio.acoustics;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the sound debugging overlay: {@code jscore:audio/acoustics_debug}. */
@PaletteHolder
final class AcousticsDebugPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "audio/acoustics_debug",
            new Colours(0xFF5FE07A, 0xFF7D8A9C, 0xFFF0B23A, 0xFFEF6A5A));

    private AcousticsDebugPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The overlay's colours.
     *
     * @param wall    a ray of the place's round that met a wall
     * @param open    a ray that reached the open air
     * @param clear   the line to a sound nothing muffles
     * @param muffled the line to a sound walls muffle
     */
    record Colours(int wall, int open, int clear, int muffled) {
    }
}
