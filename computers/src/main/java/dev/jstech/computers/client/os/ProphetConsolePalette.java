/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of the Prophet Reactive Console beside the system's own: the house's orange on the status bar and the
 * prompt, the chips of the states, the band a level is held in and the line of the level. The graph is a light card
 * on every desktop, so what is drawn on it reads the same on all of them. Pure, so a test reads every pairing.
 */
@PaletteHolder
public final class ProphetConsolePalette {

    /** The console's colours: app/prophet_console. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/prophet_console",
            new Colours(0xFFB85A12, 0xFFFFFFFF, 0xFFB85A12, 0xFF1E7F34, 0xFF0067C0, 0xFFB3263A, 0xFF5E5E5E,
                    0xFF8A5A00, 0xFFFAFAFA, 0xFF1B1B1B, 0xFF5A6472, 0x3A2EA043, 0xFF2EA043, 0xFF0067C0, 0xFFD9DEE5,
                    0xFF1B1B1B));

    private ProphetConsolePalette() {
    }

    /** The console's colours as the palette file sets them. */
    public static Colours colours() {
        return PALETTE.get();
    }

    /**
     * The console's colours.
     *
     * @param status     the status bar's ground
     * @param statusText the status bar's text
     * @param prompt     the prompt before the statement
     * @param holding    a state held in its band
     * @param working    a state with work on its way
     * @param trouble    a state that cannot hold, or is over its band
     * @param waiting    a watch armed, a state not looked at yet
     * @param over       a level above its band
     * @param graph      the graph's ground
     * @param graphText  the numbers on the graph
     * @param graphDim   the graph's grid and its quieter words
     * @param band       the band a level is held in
     * @param bandEdge   the edges of that band
     * @param level      the line of the level, and its projection
     * @param track      the ground of a level's bar in the list
     * @param marker     the mark of the level on that bar
     */
    public record Colours(int status, int statusText, int prompt, int holding, int working, int trouble, int waiting,
                          int over, int graph, int graphText, int graphDim, int band, int bandEdge, int level,
                          int track, int marker) {
    }
}
