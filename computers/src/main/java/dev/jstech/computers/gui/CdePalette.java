/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import java.util.List;

/**
 * One of CDE's colour palettes: the handful of colours its whole desktop is read from.
 *
 * <p>CDE never had a wallpaper picture and a separate window theme. It had a palette, and the frames, the
 * active title, the Front Panel and the backdrop were all drawn out of it, which is why changing one changed
 * the whole room at once. Everything CDE draws here asks a palette, so the same holds.
 *
 * <p>Pure, with no Minecraft types, so how readable each palette is can be unit-tested.
 *
 * @param name      what the Style Manager lists it as
 * @param window    the grey every frame, panel and control is made of
 * @param light     the lit edge of a raised thing, top and left
 * @param shade     its shaded edge, bottom and right
 * @param active    the title of the window in front, and the workspace that is up
 * @param inset     the ground of a sunken well: a list, a file view, a text field
 * @param backdropA the backdrop's first colour
 * @param backdropB the backdrop's second colour, which its pattern is drawn in over the first
 * @param ink       the colour text is written in on the window grey
 */
public record CdePalette(String name, int window, int light, int shade, int active, int inset, int backdropA,
                         int backdropB, int ink) {

    public static final CdePalette DEFAULT = new CdePalette("Default",
            0xFFAEB2C3, 0xFFE3E5EC, 0xFF5E6272, 0xFF7C86B0, 0xFFC7CAD6, 0xFF4C6F82, 0xFF45677A, 0xFF1A1A1A);

    /** The eight the Style Manager offers, under the names the real ones had, in the order it lists them. */
    public static final List<CdePalette> ALL = List.of(
            DEFAULT,
            new CdePalette("Alpine",
                    0xFFA9BDB3, 0xFFDDE8E2, 0xFF56675F, 0xFF3F7F6A, 0xFFC4D4CC, 0xFF2F5D50, 0xFF2A5448, 0xFF1A1A1A),
            new CdePalette("Broica",
                    0xFFB9A9B4, 0xFFE6DCE3, 0xFF675A63, 0xFF8C4F78, 0xFFD2C5CE, 0xFF5B3F56, 0xFF52384D, 0xFF1A1A1A),
            new CdePalette("Charcoal",
                    0xFF8C8F96, 0xFFC4C7CE, 0xFF3F4148, 0xFF4A5E7C, 0xFFA6A9B0, 0xFF2E3138, 0xFF282B31, 0xFF101010),
            new CdePalette("Desert",
                    0xFFC9B99A, 0xFFEFE6D2, 0xFF6F6350, 0xFFA9603C, 0xFFDDD0B6, 0xFF8C6F4E, 0xFF836746, 0xFF1A1A1A),
            new CdePalette("Neptune",
                    0xFF9FB2CC, 0xFFD8E2F0, 0xFF4F6078, 0xFF2F5FA8, 0xFFBCCBE0, 0xFF1F3F6E, 0xFF1B3862, 0xFF1A1A1A),
            new CdePalette("SeaFoam",
                    0xFFA8C8C0, 0xFFDCEDE8, 0xFF53706A, 0xFF2E7C70, 0xFFC3DCD6, 0xFF2C6A66, 0xFF27605C, 0xFF1A1A1A),
            new CdePalette("Urchin",
                    0xFFB0A8C8, 0xFFE0DCEE, 0xFF5C5674, 0xFF6A4FA8, 0xFFCAC4DC, 0xFF3E3470, 0xFF372E64, 0xFF1A1A1A));

    /** The palette listed under that name, or the default one for a name nobody has. */
    public static CdePalette named(final String name) {
        for (final CdePalette each : ALL) {
            if (each.name().equalsIgnoreCase(name)) {
                return each;
            }
        }
        return DEFAULT;
    }

    /** The colour an active title is written in, which is white on every one of them. */
    public int activeInk() {
        return 0xFFFFFFFF;
    }
}
