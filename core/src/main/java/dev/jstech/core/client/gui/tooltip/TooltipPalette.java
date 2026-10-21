/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of what tooltips show besides text: {@code jscore:gui/tooltip_pieces}. */
@PaletteHolder
final class TooltipPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/tooltip_pieces", new Colours(
            0xFF000000, 0xFF281428, 0xFFE64632, 0xFFB4BEC8, 0x30FFFFFF, 0xFFFFFFFF, 0x33000000));

    private TooltipPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * What tooltips show besides text.
     *
     * @param frame  round a bar
     * @param track  the empty part of a bar
     * @param energy the energy an item holds
     * @param gas    the gas an item holds
     * @param shine  laid over the top row of a bar's full part
     * @param more   the number of kinds more in the last slot of what an item holds
     * @param edge   the edges of each block of a 3D preview, so that a structure reads block by block
     */
    record Colours(int frame, int track, int energy, int gas, int shine, int more, int edge) {
    }
}
