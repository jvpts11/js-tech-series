/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hologram;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours a hologram is drawn in: {@code jscore:hologram}. */
@PaletteHolder
final class HologramPalette {

    /* The cyan of a projected sign, on the dim ground the game puts under a name over a head. */
    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "hologram",
            new Colours(0xFF8FF3FF, 0x40000000));

    private HologramPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * A hologram's colours.
     *
     * @param text   its words
     * @param ground the dim band behind each line
     */
    record Colours(int text, int ground) {
    }
}
