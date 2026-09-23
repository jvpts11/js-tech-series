/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.theme;

/**
 * The full ARGB color palette of one GUI skin. Every color {@code JsTechTheme} historically exposed as a
 * {@code public static final int} constant lives here as a field, so a palette is a complete, self-contained
 * description of how a computing screen is painted. Pure data (no Minecraft types), so it can be selected and
 * compared without a running client.
 *
 * <p>The last four are the colours of the overlays an {@link EraStyle} switches on; a skin whose style leaves an
 * overlay off gives it {@code 0}, and a transparent colour draws nothing even where the style asks for it.
 *
 * @param outer      the 1px border drawn just outside the window
 * @param screen     the window background fill
 * @param rail       a slightly raised sidebar/rail fill
 * @param panel      a recessed information panel fill
 * @param line       1px separator and panel top-edge color
 * @param track      the empty portion of a progress/value track
 * @param slotBg     the inner fill of an item slot cell
 * @param slotEdge   the 1px lighter frame around an item slot cell
 * @param accent     the primary accent (active tabs, emphasis fills)
 * @param accent2    the secondary accent
 * @param green      a positive/ok status color
 * @param amber      a caution/warning status color
 * @param red        a negative/error status color
 * @param text       the primary foreground text color
 * @param dim        the muted/label text color
 * @param tabOn      the background of a selected tab
 * @param tabLabelOn the text and icon color drawn on top of a selected tab (must contrast with tabOn)
 * @param hover      the background of a hovered button
 * @param bevelLight the highlight edge of a bevel
 * @param bevelDark  the shadow edge of a bevel
 * @param scanline   the low-alpha lines of the scanline finish
 * @param glow       the low-alpha halo around accent-coloured fills
 */
public record EraPalette(
        int outer, int screen, int rail, int panel, int line, int track,
        int slotBg, int slotEdge,
        int accent, int accent2,
        int green, int amber, int red,
        int text, int dim,
        int tabOn, int tabLabelOn, int hover,
        int bevelLight, int bevelDark, int scanline, int glow) {
}
