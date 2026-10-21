/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of the MCP server's question and its mark, the game's own by default: {@code jscore:gui/mcp}. */
@PaletteHolder
public final class McpPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gui/mcp", new Colours(
            0xFFFFFFFF, 0xFFE0E0E0, 0xFFA0A0A0, 0x96000000, 0xFF55FF55, 0xFFFFFF55, 0xFF55FFFF, 0xFFFF5555));

    private McpPalette() {
    }

    /** The colours as the loaded palette has them now. */
    public static Colours get() {
        return PALETTE.get();
    }

    /**
     * The MCP server's colours.
     *
     * @param title     the question's heading, and the mark's words
     * @param text      the question's words
     * @param dim       its notes, and what reading and acting mean
     * @param ground    behind the mark
     * @param connected the mark's light while agents are let in, and a client that reads
     * @param acting    the light while one acts, and what it does
     * @param acts      a client that reads and acts
     * @param refused   a call refused
     */
    public record Colours(int title, int text, int dim, int ground, int connected, int acting, int acts,
                          int refused) {
    }
}
