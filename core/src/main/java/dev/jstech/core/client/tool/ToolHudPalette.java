/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of what tools show of themselves: {@code jscore:tools/hud}. */
@PaletteHolder
final class ToolHudPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "tools/hud", new Colours(
            0x46000000, 0xAA000000, 0xFF6E6E6E, 0xD2282828, 0xFFFFFFFF, 0xBE000000, 0xFFE0E0E0, 0xFFFFFF55,
            0xFFFFFFFF, 0xFF55FF55, 0xFFAAAAAA, 0xFF000000, 0xA0000000, 0xFFFFFFFF, 0xFFE64632, 0xFFDC9628,
            0xFF96DCF0, 0xFFFF6E28, 0xFFFF5555, 0x96000000, 0x66000000, 0xAAFFFFFF));

    private ToolHudPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * What tools show of themselves.
     *
     * @param dim         laid over the world while the ring of modes is open
     * @param segment     a segment of the ring
     * @param segmentEdge its edge
     * @param pointed     the segment pointed at
     * @param pointedEdge its edge
     * @param middle      the disc in the ring's middle, behind the item
     * @param label       a mode's picture and name
     * @param pointedText the name of the mode pointed at
     * @param current     the name of the mode the item is in
     * @param currentDot  the dot under it
     * @param hint        the line under the item's name, and the HUD's line of what the item works on
     * @param barGround   behind a bar on an icon
     * @param hudGround   behind a bar under the crosshair
     * @param charge      how charged a tool held in use is
     * @param energy      the energy an item holds
     * @param fuel        the fuel a tool burns
     * @param gas         the gas a tool works on
     * @param heat        how hot a tool is
     * @param overheat    the mark at the end of the heat
     * @param letterBox   behind the mode's letter on an icon
     * @param outline     the boxes round the blocks a tool is about to mine
     * @param ghostEdge   the edges of the blocks a tool is about to place
     */
    record Colours(int dim, int segment, int segmentEdge, int pointed, int pointedEdge, int middle, int label,
                   int pointedText, int current, int currentDot, int hint, int barGround, int hudGround, int charge,
                   int energy, int fuel, int gas, int heat, int overheat, int letterBox, int outline, int ghostEdge) {
    }
}
