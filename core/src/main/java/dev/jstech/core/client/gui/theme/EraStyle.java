/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.theme;

/**
 * How a skin is drawn, beside the {@link EraPalette} it is coloured with. These switch the optional, era-specific
 * surface overlays on, all of them sharp and square (no rounded corners ever); their colours are the palette's, so a
 * resource pack can recolour an era's scanlines without giving another era any. With every overlay off, which is the
 * STANDARD style, the skin draws exactly what the original flat theme did.
 *
 * @param scanlines      draw a faint horizontal scanline overlay over the window background (CRT look)
 * @param doubleBevel    draw a 1px raised/inset bevel on panels, buttons and slots instead of a flat 1px line
 * @param glowAccent     lay a faint additive halo around accent-colored fills drawn through the helpers
 * @param fontScaleSmall the scale factor for the small-text helpers (the former {@code JsTechTheme.SMALL})
 */
public record EraStyle(
        boolean scanlines,
        boolean doubleBevel,
        boolean glowAccent,
        float fontScaleSmall) {

    /** A flat style with every overlay disabled, the STANDARD baseline. {@code small} is the only live value. */
    public static EraStyle flat(final float fontScaleSmall) {
        return new EraStyle(false, false, false, fontScaleSmall);
    }
}
