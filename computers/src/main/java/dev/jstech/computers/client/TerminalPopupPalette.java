/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours the terminal's popups share, the one that asks for data and the one that drops it:
 * {@code jsc:terminal/popup}.
 */
@PaletteHolder
final class TerminalPopupPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "terminal/popup",
            new Colours(0xE0070A0F, 0xFF0F151C, 0xFF1A222B, 0xFF24323C, 0xFF1F9488, 0xFF2BB3A4, 0xFFFFFFFF,
                    0xFF2A3340, 0xFF11161D, 0xFF0F151C, 0xFF2A3340, 0xFF8A241C, 0xFFB23228, 0xFF3A2420,
                    0xFFFFFFFF));

    private TerminalPopupPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The popups' colours.
     *
     * @param veil          what dims the terminal behind a popup
     * @param panel         the popup itself
     * @param control       a small button at rest
     * @param controlHover  a small button under the cursor
     * @param action        the button that does what the popup is for
     * @param actionHover   that button under the cursor
     * @param actionInk     its words
     * @param checkEdge     the edge of a box that is not ticked
     * @param checkFill     the inside of that box
     * @param onAccent      words written on the accent
     * @param cancelHover   Cancel under the cursor
     * @param danger        the button that throws data away
     * @param dangerHover   that button under the cursor
     * @param dangerOff     that button while it cannot be pressed
     * @param dangerInk     its words
     */
    record Colours(int veil, int panel, int control, int controlHover, int action, int actionHover, int actionInk,
                   int checkEdge, int checkFill, int onAccent, int cancelHover, int danger, int dangerHover,
                   int dangerOff, int dangerInk) {
    }
}
