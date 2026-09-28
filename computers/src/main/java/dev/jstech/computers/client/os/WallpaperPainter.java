/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.Platform;
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
     * The style that hangs on {@code desktopId} running {@code platform} with the player's {@code choice}: the
     * chosen style when it names one, else whatever that desktop ships with there (FreeBSD's own picture for a
     * desktop installed on FreeBSD rather than the Linux one it hangs everywhere else). The one place this
     * choice is worked out, so what is painted and what a caller reads back as "the current wallpaper" can
     * never drift apart.
     *
     * @param desktopId the desktop drawn, whose look names the wallpaper it ships with
     * @param platform  the platform the desktop runs on, which the wallpaper it ships with follows
     * @param choice    the player's chosen style id, or empty for the desktop's own
     */
    static WallpaperStyle styleFor(final ResourceLocation desktopId, final Platform platform, final String choice) {
        final WallpaperStyle chosen = WallpaperStyle.byId(choice);
        return chosen != null ? chosen : DesktopLook.of(desktopId).wallpaperOn(platform);
    }

    /**
     * Fills the desktop glass {@code (0,0)-(w,h)} with {@link #styleFor}'s wallpaper.
     *
     * @param g    the graphics context (pose already translated to the desktop origin)
     * @param dark whether the desktop is set to its dark theme
     */
    static void paint(final GuiGraphics g, final int w, final int h, final ResourceLocation desktopId,
                      final Platform platform, final String choice, final boolean dark) {
        styleFor(desktopId, platform, choice).paint(g, w, h, dark);
    }
}
