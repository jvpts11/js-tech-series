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
 * The wallpaper's own colours, shared by the built-in swatch shown for "whatever the desktop comes with" and the
 * wall a drawn picture is hung against: {@code jsc:desktop/wallpaper}.
 */
@PaletteHolder
final class WallpaperPalette {

    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "desktop/wallpaper",
            new Colours(0xFF3A6A9A, 0xFF1E1E1E));

    private WallpaperPalette() {
    }

    /** The colours as the loaded palette has them now. */
    static Colours get() {
        return PALETTE.get();
    }

    /**
     * The wallpaper's colours.
     *
     * @param defaultSwatch   the thumbnail shown for "whatever the desktop comes with"
     * @param pictureBackdrop the wall around a drawn picture whose own corner gives no colour to fall back on
     */
    record Colours(int defaultSwatch, int pictureBackdrop) {
    }
}
