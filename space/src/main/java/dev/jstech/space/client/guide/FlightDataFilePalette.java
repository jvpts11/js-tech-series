/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.guide;

import dev.jstech.core.client.guide.GuidePalettes;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.space.JsSpace;

/**
 * The colours of the Mission Handbook, a crew's flight document: an off-white vinyl binder with steel rings, cream
 * pages typed in near black, rules in a warm grey, the agencies' blue for links and notes and their red for what
 * warns; the tabs lettered in white on each section's colour, and a listing on a deeper cream.
 */
@PaletteHolder
public final class FlightDataFilePalette {

    public static final Palette<GuidePalettes.Binder> FLIGHT_DATA_FILE = Palettes.declare(JsSpace.MODID,
            "guide/flight_data_file", new GuidePalettes.Binder(
                    0xFFE8E5DB, 0xFFF5F3EC, 0xFFF3EFE2, 0xFFDCD6C4, 0xFF1D2026, 0xFF6A6759, 0xFF1D2026, 0xFF133A8E,
                    0xFFAAA38C, 0xFFE6E0CE, 0xFFF7E8C4, 0xFFC9CCD1, 0xFFFBF9F2, 0xFF1D2026, 0xFF4B4E57, 0xFFFFFFFF,
                    0xFFB3261E, 0xFF133A8E, 0xFF1D2026, 0xFF133A8E, 0xFFE6E0CE, 0xFFBCB6A6, 0xFF1D2026,
                    0xFFEDE8D8, 0xFFDCD6C4, 0xFF1F3F8A, 0xFF0F6E66, 0xFF9A3B16, 0xFF7A2D8A, 0xFF6A6759, 0xFFCFE8CF,
                    0xFFE8B0B0, 0xFFC49A3A));

    private FlightDataFilePalette() {
    }
}
