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
import org.jetbrains.annotations.Nullable;

/**
 * Every wallpaper a desktop can hang, in one table: the id a machine keeps its choice under, how it is painted
 * across the glass, and how it is shown in a thumbnail small enough to pick from.
 *
 * <p>The id is what is written down with the machine, so it never changes. The order is the order the
 * Personalize page offers them in.
 */
public enum WallpaperStyle {

    /** Frames 95: the classic teal. */
    TEAL("win95", true, WallpaperPainter::paintNineFive, null),

    /** Frames XP: a sky over a rolling green hill, which a thumbnail shows as its blue alone. */
    BLISS("winxp", true, WallpaperPainter::paintXp,
            (g, x, y, w, h) -> g.fillGradient(x, y, x + w, y + h, 0xFF5B95DD, 0xFF2C5FA8)),

    /** Frames 11: a deep blue with a soft bloom, which a thumbnail draws small enough to fit. */
    BLOOM("win11", true, WallpaperPainter::paintEleven, (g, x, y, w, h) -> {
        g.fillGradient(x, y, x + w, y + h, 0xFF2C3C68, 0xFF141C33);
        g.fill(x + w / 2 - 3, y + h / 2 - 3, x + w / 2 + 3, y + h / 2 + 3, 0x55FFFFFF);
    }),

    /** KDE's Breeze. */
    BREEZE("breeze", true, WallpaperPainter::paintBreeze, null),

    /** GNOME's Adwaita. */
    ADWAITA("adwaita", true, WallpaperPainter::paintAdwaita, null),

    /** Cinnamon's Mint-Y. */
    MINT_Y("minty", true, WallpaperPainter::paintMintY, null),

    /**
     * CDE's backdrop: no picture to hang, only a pattern in two colours of its palette. It is CDE's own and
     * no other desktop offers it.
     */
    MOTIF("cde", false,
            (g, w, h) -> MotifChrome.backdrop(g, w, h, CdeScheme.DEFAULT.colours(), CdeStyle.DEFAULT.backdrop(0)),
            null);

    /** The colour a thumbnail of "whatever the desktop comes with" is shown in. */
    private static final int DEFAULT_SWATCH = 0xFF3A6A9A;

    private final String id;
    private final boolean offered;
    private final IPainter painter;
    @Nullable
    private final ISwatch swatch;

    WallpaperStyle(final String id, final boolean offered, final IPainter painter, @Nullable final ISwatch swatch) {
        this.id = id;
        this.offered = offered;
        this.painter = painter;
        this.swatch = swatch;
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

    /** Fills the glass {@code (0,0)-(w,h)} with it. */
    public void paint(final GuiGraphics g, final int w, final int h) {
        this.painter.paint(g, w, h);
    }

    /**
     * A thumbnail of it at {@code (x, y)}. A style whose picture stays inside its rectangle at any size is simply
     * painted small; one that does not, because parts of it are drawn at sizes a thumbnail cannot hold, has one
     * of its own.
     */
    public void swatch(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        if (this.swatch != null) {
            this.swatch.draw(g, x, y, w, h);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        this.painter.paint(g, w, h);
        g.pose().popPose();
    }

    /** How a style fills the glass. */
    @FunctionalInterface
    interface IPainter {
        void paint(GuiGraphics g, int w, int h);
    }

    /** How a style is shown small. */
    @FunctionalInterface
    interface ISwatch {
        void draw(GuiGraphics g, int x, int y, int w, int h);
    }
}
