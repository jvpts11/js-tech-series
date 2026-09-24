/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Hangs a desktop's wallpaper: the one the player chose, or the one the desktop ships with. Which pictures there
 * are, and which desktop ships which, is {@link WallpaperStyle}'s table and {@link DesktopLook}'s.
 */
final class WallpaperPainter {

    private WallpaperPainter() {
    }

    /**
     * Fills the desktop glass {@code (0,0)-(w,h)} with a wallpaper. The player's {@code choice} overrides the
     * desktop's own when set; an empty choice falls back to what the desktop ships with.
     *
     * @param g         the graphics context (pose already translated to the desktop origin)
     * @param desktopId the desktop drawn, whose look names the wallpaper it ships with
     * @param choice    the player's chosen style id, or empty for the desktop's own
     * @param dark      whether the desktop is set to its dark theme
     */
    static void paint(final GuiGraphics g, final int w, final int h, final ResourceLocation desktopId,
                      final String choice, final boolean dark) {
        final WallpaperStyle chosen = WallpaperStyle.byId(choice);
        (chosen != null ? chosen : DesktopLook.of(desktopId).wallpaper()).paint(g, w, h, dark);
    }
}
