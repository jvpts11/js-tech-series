/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;

/** The colours of a ship's HUD, as the approved flight paints it: green on the canopy, amber where it calls out. */
@PaletteHolder
public final class ShipHudArt {

    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "ship_hud", new Colours(
            0xFF78FFA0, 0x9678FFA0, 0xFFFFD65A, 0xFF0A1E10, 0xA0285A3C));

    private ShipHudArt() {
    }

    /**
     * A ship HUD's colours.
     *
     * @param symbol   its symbols and the figures they read
     * @param dim      what labels them, fainter
     * @param hot      what calls out: the boost in use, the gear down
     * @param lit      the word of an autopilot's mode lit, dark on its green
     * @param unfilled the boost's bar where it is empty
     */
    public record Colours(int symbol, int dim, int hot, int lit, int unfilled) {
    }
}
