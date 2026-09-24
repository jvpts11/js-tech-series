/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.audio;

import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/** The colours of an alert's sign at the top of the screen: {@code jscore:audio/alert_sign}. */
@PaletteHolder
final class AlertSignPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "audio/alert_sign",
            new Colours(0xFFF2A93B, 0xD9000000, 0xFF000000, 0xFFFFFFFF, 0xFFF2A93B));

    private AlertSignPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The sign's colours.
     *
     * @param border the line around it, and the square behind its mark
     * @param ground what its text is written on
     * @param mark   the exclamation mark in the square
     * @param text   what it says
     * @param arrow  the arrow pointing where the alert comes from
     */
    record Colours(int border, int ground, int mark, int text, int arrow) {
    }
}
