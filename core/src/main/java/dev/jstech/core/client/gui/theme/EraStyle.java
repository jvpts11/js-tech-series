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
 * <p>A style is written as the flat one with its overlays named on top, {@code EraStyle.flat(0.75f).withScanlines()},
 * so a declaration reads as what the skin adds rather than as a row of switches.
 *
 * @param scanlines      draw a faint horizontal scanline overlay over the window background (CRT look)
 * @param doubleBevel    draw a 1px raised/inset bevel on panels, buttons and slots instead of a flat 1px line
 * @param glowAccent     lay a faint additive halo around accent-colored fills drawn through the helpers
 * @param glassBands     paint a header in the selected tab's colour, and split it, a button and a selected tab into
 *                       two bands, the lighter over the darker, the glass surfaces of their day
 * @param accentRule     draw the line under a header in the accent colour instead of the separator colour
 * @param fontScaleSmall the scale factor for the small-text helpers (the former {@code JsTechTheme.SMALL})
 */
public record EraStyle(
        boolean scanlines,
        boolean doubleBevel,
        boolean glowAccent,
        boolean glassBands,
        boolean accentRule,
        float fontScaleSmall) {

    /** A flat style with every overlay disabled, the STANDARD baseline. {@code small} is the only live value. */
    public static EraStyle flat(final float fontScaleSmall) {
        return new EraStyle(false, false, false, false, false, fontScaleSmall);
    }

    /** This style with the CRT scanlines over the window. */
    public EraStyle withScanlines() {
        return new EraStyle(true, doubleBevel, glowAccent, glassBands, accentRule, fontScaleSmall);
    }

    /** This style with raised and sunken bevels on its panels, buttons and slots. */
    public EraStyle withDoubleBevel() {
        return new EraStyle(scanlines, true, glowAccent, glassBands, accentRule, fontScaleSmall);
    }

    /** This style with a halo around its accent-coloured fills. */
    public EraStyle withGlowAccent() {
        return new EraStyle(scanlines, doubleBevel, true, glassBands, accentRule, fontScaleSmall);
    }

    /** This style with the two glass bands on its header, buttons and selected tabs. */
    public EraStyle withGlassBands() {
        return new EraStyle(scanlines, doubleBevel, glowAccent, true, accentRule, fontScaleSmall);
    }

    /** This style with the accent-coloured line under its headers. */
    public EraStyle withAccentRule() {
        return new EraStyle(scanlines, doubleBevel, glowAccent, glassBands, true, fontScaleSmall);
    }
}
