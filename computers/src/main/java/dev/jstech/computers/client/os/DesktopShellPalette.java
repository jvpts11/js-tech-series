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
 * The colours the desktop draws for itself, whatever system it wears: the crash of a system that shares its memory,
 * what is dragged across it, the panel's own menu in its dark and its light form, the tray's balloon, the shade
 * behind the power dialog and the mark on a hovered slot. They are {@code jsc:desktop/shell}.
 */
@PaletteHolder
final class DesktopShellPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "desktop/shell",
            new Colours(0xFF0000AA, 0xFFAAAAAA, 0xFFFFFFFF,
                    0xD0303848, 0xFFFFFFFF, 0x334C84F0, 0xCC4C84F0,
                    0xFF262B36, 0xFFE7E9EF, 0xFF11151E, 0xFF3A4150,
                    0xFFE8E8EC, 0xFF1A2230, 0xFF000000, 0xFFFFFFFF, 0xFFB6BAC4, 0xFFFFFFFF,
                    0xFF000000, 0xFFFFFFE1, 0xFF1C53C9, 0xFFFFFFFF, 0xFF000000, 0xFF303030, 0xFFE8E8CA, 0xFF6A6A55,
                    0x99000000, 0x80FFFFFF));

    private DesktopShellPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The desktop's own colours.
     *
     * @param crashGround      the blue page a crashed system writes on
     * @param crashBand        the grey band its heading sits on
     * @param crashInk         its words
     * @param ghostFill        the label that follows a dragged icon
     * @param ghostInk         the words on it
     * @param bandFill         the inside of the band a drag selects with
     * @param bandEdge         its edges
     * @param darkMenuFill     the panel menu on a light panel's ink, which is a dark menu
     * @param darkMenuInk      its words
     * @param darkMenuBorder   its border
     * @param darkMenuRule     its top light and its separators
     * @param lightMenuFill    the panel menu on a dark panel's ink, which is a light menu
     * @param lightMenuInk     its words
     * @param lightMenuBorder  its border
     * @param lightMenuTop     its top light
     * @param lightMenuRule    its separators
     * @param menuHoverInk     the words of the row under the cursor, on the accent
     * @param balloonBorder    the line around the tray's balloon and its tail
     * @param balloonFill      the balloon's paper
     * @param balloonIcon      the round mark at its corner
     * @param balloonIconMark  the letter in that mark
     * @param balloonTitle     the balloon's heading
     * @param balloonBody      its words and its close cross
     * @param balloonCloseFill the close box
     * @param balloonCloseEdge the close box's edge
     * @param powerShade       what dims the desktop behind the power dialog
     * @param slotHover        the light over a hovered inventory slot
     */
    record Colours(int crashGround, int crashBand, int crashInk,
                   int ghostFill, int ghostInk, int bandFill, int bandEdge,
                   int darkMenuFill, int darkMenuInk, int darkMenuBorder, int darkMenuRule,
                   int lightMenuFill, int lightMenuInk, int lightMenuBorder, int lightMenuTop, int lightMenuRule,
                   int menuHoverInk,
                   int balloonBorder, int balloonFill, int balloonIcon, int balloonIconMark, int balloonTitle,
                   int balloonBody, int balloonCloseFill, int balloonCloseEdge,
                   int powerShade, int slotHover) {
    }
}
