/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;

/** The colours of the Item Port's screen, as the approved one has them. */
@PaletteHolder
public final class ItemPortPalette {

    /** The screen's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsSpace.MODID, "item_port", new Colours(
            0xFF7CFF7C, 0xFF469646, 0xFF284A2A, 0xFF3C4148));

    private ItemPortPalette() {
    }

    /**
     * The Item Port screen's colours.
     *
     * @param green    the display's bright green: the ship, what it lacks, a bar filling
     * @param greenDim its dim green: the counts, a block there is enough of, the scroll bar
     * @param track    a bar's and the scroll bar's empty track
     * @param stamp    the dark ink of the hold's label stamped in the steel
     */
    public record Colours(int green, int greenDim, int track, int stamp) {
    }
}
