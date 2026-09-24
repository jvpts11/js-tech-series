/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdeScheme;
import dev.jstech.computers.gui.CdeStyle;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Every wallpaper a desktop can hang, in one table: the id a machine keeps its choice under, and the picture it is.
 *
 * <p>Each is a picture at {@code textures/gui/wallpaper/<id>.png}, made at the size of the monitor's glass so it is
 * drawn 1:1, and the thumbnail a player picks from is the same picture drawn small. A wallpaper that also has a
 * picture at {@code <id>_dark.png} hangs that one on a desktop set to its dark theme. A picture is changed by
 * replacing its file.
 *
 * <p>The id is what is written down with the machine, so it never changes. The order is the order the
 * Personalize page offers them in.
 */
public enum WallpaperStyle {

    /** Frames 95: the classic teal. */
    TEAL("win95", true),

    /** Frames XP: a green hill under a blue sky. */
    BLISS("winxp", true),

    /** Frames 11: the blue bloom, with a dark picture for the dark theme. */
    BLOOM("win11", true),

    /** KDE Plasma's Breeze. */
    BREEZE("breeze", true),

    /** GNOME's Adwaita. */
    ADWAITA("adwaita", true),

    /** Cinnamon's Mint-Y. */
    MINT_Y("minty", true),

    /**
     * CDE's backdrop: no picture to hang, only a pattern in two colours of its palette. It is CDE's own and
     * no other desktop offers it.
     */
    MOTIF("cde", false);

    private final String id;
    private final boolean offered;

    /** The colour a thumbnail of "whatever the desktop comes with" is shown in. */
    private static final int DEFAULT_SWATCH = 0xFF3A6A9A;

    /** The size the pictures are made at, which is the monitor's glass. */
    private static final int PICTURE_W = 384;
    private static final int PICTURE_H = 256;

    WallpaperStyle(final String id, final boolean offered) {
        this.id = id;
        this.offered = offered;
    }

    /** The style a machine keeps under that id, or null for none, which means the desktop's own. */
    @Nullable
    public static WallpaperStyle byId(@Nullable final String id) {
        for (final WallpaperStyle style : values()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        return null;
    }

    /** The styles a player may pick, in the order they are offered. */
    public static List<WallpaperStyle> offered() {
        final List<WallpaperStyle> out = new ArrayList<>();
        for (final WallpaperStyle style : values()) {
            if (style.offered) {
                out.add(style);
            }
        }
        return out;
    }

    /** A thumbnail of the style under that id at {@code (x, y)}; no style is the desktop's own, drawn plain. */
    public static void swatch(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              @Nullable final String id) {
        final WallpaperStyle style = byId(id);
        if (style == null) {
            g.fill(x, y, x + w, y + h, DEFAULT_SWATCH);
        } else {
            style.swatch(g, x, y, w, h);
        }
    }

    /** The id a machine keeps this choice under. */
    public String id() {
        return this.id;
    }

    /**
     * Fills the glass {@code (0,0)-(w,h)} with it.
     *
     * @param dark whether the desktop is set to its dark theme, which hangs the dark picture where there is one
     */
    public void paint(final GuiGraphics g, final int w, final int h, final boolean dark) {
        if (this == MOTIF) {
            MotifChrome.backdrop(g, w, h, CdeScheme.DEFAULT.colours(), CdeStyle.DEFAULT.backdrop(0));
            return;
        }
        g.blit(picture(dark), 0, 0, w, h, 0.0F, 0.0F, PICTURE_W, PICTURE_H, PICTURE_W, PICTURE_H);
    }

    /** A thumbnail of it at {@code (x, y)}: the same picture, drawn small. */
    public void swatch(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        if (this == MOTIF) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0);
            paint(g, w, h, false);
            g.pose().popPose();
            return;
        }
        g.blit(picture(false), x, y, w, h, 0.0F, 0.0F, PICTURE_W, PICTURE_H, PICTURE_W, PICTURE_H);
    }

    /** The picture to hang: the dark one on a dark desktop when this wallpaper has one, its own otherwise. */
    private ResourceLocation picture(final boolean dark) {
        if (dark) {
            final ResourceLocation night = texture(this.id + "_dark");
            if (SkinSprites.exists(night)) {
                return night;
            }
        }
        return texture(this.id);
    }

    private static ResourceLocation texture(final String name) {
        return ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/wallpaper/" + name + ".png");
    }
}
