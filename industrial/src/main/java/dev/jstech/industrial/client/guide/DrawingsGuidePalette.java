/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.guide;

import dev.jstech.core.client.guide.GuidePalettes;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.industrial.JsIndustrial;

/**
 * The colours of the Plant Drawings: cyanotype blue sheets with a faint grid, white line work, pale blue dimensions,
 * the pencil yellow of what warns, in a slate pressboard folder with a cream label and a black elastic band; a listing
 * on a deeper blue, its code in the pale chalk colours a blueprint takes.
 */
@PaletteHolder
public final class DrawingsGuidePalette {

    public static final Palette<GuidePalettes.Binder> DRAWINGS = Palettes.declare(JsIndustrial.MODID,
            "guide/drawings", new GuidePalettes.Binder(
                    0xFF3A4250, 0xFF4E5868, 0xFF1D4F91, 0xFF255B9F, 0xFFEEF5FF, 0xFF9CC0EA, 0xFFEEF5FF, 0xFFEEF5FF,
                    0xFFEEF5FF, 0xFF1D4F91, 0xFF1D4F91, 0xFFC9CCD1, 0xFFF4F1E6, 0xFF23252A, 0xFF255B9F, 0xFFEEF5FF,
                    0xFFFFE27A, 0xFFFFE27A, 0xFFFFE27A, 0xFF1A1D22, 0xFF255B9F, 0xFF232830, 0xFF2C3440,
                    0xFF173F75, 0xFF255B9F, 0xFFFFE27A, 0xFF9CF0E0, 0xFFFFC09A, 0xFFE8C4FF, 0xFF9CC0EA, 0xFF2E7A4F,
                    0xFFE8B0B0, 0xFFC49A3A));

    private DrawingsGuidePalette() {
    }
}
