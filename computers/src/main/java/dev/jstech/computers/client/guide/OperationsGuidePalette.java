/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.guide;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.guide.GuidePalettes;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of the Guide to Operations: the beige vinyl of the computer manuals of the early 1980s with a cyan band
 * at its foot, and inside the same cream pages and dark ink as the series' binder.
 */
@PaletteHolder
public final class OperationsGuidePalette {

    public static final Palette<GuidePalettes.Binder> OPERATIONS = Palettes.declare(JsComputers.MODID,
            "guide/operations", new GuidePalettes.Binder(
                    0xFFD8CEB2, 0xFFE8E0C8, 0xFFEBE5D3, 0xFFD3CCB6, 0xFF23252A, 0xFF6E6A5E, 0xFF1F3F8A, 0xFF1F3F8A,
                    0xFFA39C86, 0xFFE1DAC4, 0xFFF3E6C8, 0xFFC9CCD1, 0xFFF4F1E6, 0xFF23252A, 0xFFB8BCC2, 0xFF23252A,
                    0xFF8A4F12, 0xFF1F3F8A, 0xFF23252A, 0xFF39D6C4, 0xFFD3CCB6, 0xFFA99E82, 0xFF2C3440));

    private OperationsGuidePalette() {
    }
}
