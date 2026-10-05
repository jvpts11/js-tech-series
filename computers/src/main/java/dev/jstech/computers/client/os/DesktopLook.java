/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.Platform;
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
 * @param skin           the skin its windows are drawn in
 * @param periodSkin     the skin it wore on Legacy-era hardware, or null when it looked the same or never ran there
 * @param transitionSkin the skin it wore on Transition-era hardware, or null when it looked as it does today
 * @param theme          the colours of its panel, launcher and windows
 * @param wallpaper      the wallpaper it ships with
 * @param freeBsdWallpaper the wallpaper it hangs when it comes from FreeBSD instead, or null when this desktop
 *                         hangs the same one everywhere (a Frames edition, which is never on FreeBSD; CDE, which
 *                         hangs no picture at all)
 */
public record DesktopLook(OsSkin skin, @Nullable OsSkin periodSkin, @Nullable OsSkin transitionSkin,
                          DesktopTheme theme, WallpaperStyle wallpaper, @Nullable WallpaperStyle freeBsdWallpaper) {

    /** What a desktop nobody registered a look for is drawn as: the first Frames edition, the plainest. */
    private static final DesktopLook FALLBACK =
            new DesktopLook(OsSkin.FRAMES_95, null, DesktopTheme.WIN95, WallpaperStyle.TEAL);

    private static final Map<ResourceLocation, DesktopLook> BY_DESKTOP = new HashMap<>();

    static {
        register("frames_95", FALLBACK);
        register("frames_xp", new DesktopLook(OsSkin.FRAMES_XP, null, DesktopTheme.XP, WallpaperStyle.BLISS));
        register("frames_7", new DesktopLook(OsSkin.FRAMES_7, null, DesktopTheme.SEVEN, WallpaperStyle.HARMONY));
        register("frames_10", new DesktopLook(OsSkin.FRAMES_10, null, DesktopTheme.TEN, WallpaperStyle.HERO));
        register("frames_11", new DesktopLook(OsSkin.FRAMES_11, null, DesktopTheme.WIN11, WallpaperStyle.BLOOM));
        /*
         * Of the Unix desktops only KDE and GNOME wore other faces on older hardware, because only those two
         * install there at all: Cinnamon is a later desktop that needs a Standard machine. On the Legacy they are
         * KDE 2 and GNOME 1, on the Transition KDE 4 and GNOME 2. Each also carries FreeBSD's own wallpaper for
         * it, which a machine installed on FreeBSD hangs instead of the Linux one; the era never changes it.
         */
        register("kde_plasma", new DesktopLook(OsSkin.KDE_PLASMA, OsSkin.KDE_PLASMA_LEGACY,
                OsSkin.KDE_PLASMA_TRANSITION, DesktopTheme.KDE, WallpaperStyle.BREEZE, WallpaperStyle.FREEBSD_PLASMA));
        register("gnome", new DesktopLook(OsSkin.GNOME, OsSkin.GNOME_LEGACY, OsSkin.GNOME_TRANSITION,
                DesktopTheme.GNOME, WallpaperStyle.ADWAITA, WallpaperStyle.FREEBSD_GNOME));
        register("cinnamon", new DesktopLook(OsSkin.CINNAMON, null, null, DesktopTheme.CINNAMON,
                WallpaperStyle.MINT_Y, WallpaperStyle.FREEBSD_CINNAMON));
        register("cde", new DesktopLook(OsSkin.CDE, null, DesktopTheme.CDE_DEFAULT, WallpaperStyle.MOTIF));
    }

    /** The same look, with no FreeBSD wallpaper of its own and no Transition face (a Frames edition, or CDE). */
    public DesktopLook(final OsSkin skin, @Nullable final OsSkin periodSkin, final DesktopTheme theme,
                       final WallpaperStyle wallpaper) {
        this(skin, periodSkin, null, theme, wallpaper, null);
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
     * The skin for hardware of {@code era}. The Frames editions already are their era (95 is Legacy, 7 is
     * Transition, 11 is Advanced), so only a desktop that outlived its first faces has others to wear: its period
     * one up to the Legacy, its Transition one there, and today's from the Standard on.
     */
    public OsSkin skinOn(@Nullable final HardwareEra era) {
        if (era == null) {
            return this.skin;
        }
        if (this.periodSkin != null && era.isAtMost(HardwareEra.LEGACY)) {
            return this.periodSkin;
        }
        return this.transitionSkin != null && era == HardwareEra.TRANSITION ? this.transitionSkin : this.skin;
    }

    /**
     * The wallpaper this desktop ships with on {@code platform}: its FreeBSD one there when it has one, its
     * usual one everywhere else. The wallpaper comes with the system, not with the desktop alone.
     */
    public WallpaperStyle wallpaperOn(final Platform platform) {
        return platform == Platform.FREEBSD && this.freeBsdWallpaper != null ? this.freeBsdWallpaper : this.wallpaper;
    }

    private static void register(final String path, final DesktopLook look) {
        register(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path), look);
    }
}
