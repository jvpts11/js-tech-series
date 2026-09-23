/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.tier.HardwareEra;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * How a desktop environment looks, found by its id: the skin its windows are drawn in, the one it wore on the
 * machines of an earlier age if it wore another, the colours of its chrome, and the wallpaper it ships with.
 *
 * <p>What a desktop does, where its panel sits and how its launcher opens, is its panel style, which the desktop
 * declares with everything else it is. This is the part that is only seen, so it lives on the client, and a
 * desktop an add-on brings says how it looks by registering one of these under its id.
 *
 * @param skin       the skin its windows are drawn in
 * @param periodSkin the skin it wore on Legacy-era hardware, or null when it looked the same or never ran there
 * @param theme      the colours of its panel, launcher and windows
 * @param wallpaper  the wallpaper it ships with
 */
public record DesktopLook(OsSkin skin, @Nullable OsSkin periodSkin, DesktopTheme theme, WallpaperStyle wallpaper) {

    /** What a desktop nobody registered a look for is drawn as: the first Frames edition, the plainest. */
    private static final DesktopLook FALLBACK =
            new DesktopLook(OsSkin.FRAMES_95, null, DesktopTheme.WIN95, WallpaperStyle.TEAL);

    private static final Map<ResourceLocation, DesktopLook> BY_DESKTOP = new HashMap<>();

    static {
        register("frames_95", FALLBACK);
        register("frames_xp", new DesktopLook(OsSkin.FRAMES_XP, null, DesktopTheme.XP, WallpaperStyle.BLISS));
        register("frames_11", new DesktopLook(OsSkin.FRAMES_11, null, DesktopTheme.WIN11, WallpaperStyle.BLOOM));
        /*
         * Of the Unix desktops only KDE and GNOME wore another face on Legacy hardware, because only those two
         * install there at all: Cinnamon is a later desktop that needs a Standard machine.
         */
        register("kde_plasma", new DesktopLook(OsSkin.KDE_PLASMA, OsSkin.KDE_PLASMA_LEGACY, DesktopTheme.KDE,
                WallpaperStyle.BREEZE));
        register("gnome", new DesktopLook(OsSkin.GNOME, OsSkin.GNOME_LEGACY, DesktopTheme.GNOME,
                WallpaperStyle.ADWAITA));
        register("cinnamon", new DesktopLook(OsSkin.CINNAMON, null, DesktopTheme.CINNAMON, WallpaperStyle.MINT_Y));
        register("cde", new DesktopLook(OsSkin.CDE, null, DesktopTheme.CDE_DEFAULT, WallpaperStyle.MOTIF));
    }

    /** The look of the desktop under that id, or the plainest one when none was registered for it. */
    public static DesktopLook of(@Nullable final ResourceLocation desktopId) {
        return desktopId == null ? FALLBACK : BY_DESKTOP.getOrDefault(desktopId, FALLBACK);
    }

    /** Says how the desktop under that id looks, replacing whatever was said for it before. */
    public static void register(final ResourceLocation desktopId, final DesktopLook look) {
        BY_DESKTOP.put(desktopId, look);
    }

    /**
     * The skin for hardware of {@code era}. The Frames editions already are their era (95 is Legacy, 11 is
     * Standard), so only a desktop that outlived its first face has a second to wear.
     */
    public OsSkin skinOn(@Nullable final HardwareEra era) {
        return this.periodSkin != null && era != null && era.isAtMost(HardwareEra.LEGACY) ? this.periodSkin : this.skin;
    }

    private static void register(final String path, final DesktopLook look) {
        register(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path), look);
    }
}
