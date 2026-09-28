/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Where the Settings app's Personalize page lays its content out, top to bottom: the wallpaper swatches
 * in a grid that wraps to fit the window's width, then the accent and theme rows on every skin but the
 * plain bevel, the clock row on every skin, and the taskbar and appearance rows on the flat skin alone.
 *
 * <p>The swatch grid grows with the number of wallpapers offered, so the page's total height is not fixed:
 * the app places this content in a scrolling panel rather than a plain one, and this class only says where
 * everything sits along that content, never how tall the window itself is.
 */
public final class SettingsLayout {

    public static final int SWATCH_W = 34;
    public static final int SWATCH_H = 21;
    public static final int SWATCH_GAP = 4;
    /**
     * The room ScrollPanel's thumb draws inside the panel's own right edge (its width plus the 1px gap before
     * it): reserved so the last swatch column never lands under it.
     */
    public static final int SCROLL_THUMB_SPACE = 4;

    private static final int HEADING_H = 13;
    private static final int CAPTION_H = 10;
    private static final int GRID_GAP_AFTER = 4;
    private static final int ACCENT_ROW_H = 22;
    private static final int THEME_ROW_H = 19;
    private static final int TOGGLE_ROW_H = 19;

    /** Not a real row: a section left out of a case sits here, off the content this page ever draws. */
    private static final int ABSENT = -1;

    /**
     * Where every row of the Personalize page starts, from the page's own top (its heading is always at 0).
     *
     * @param wallpaperCaptionY the "Wallpaper" caption above the swatch grid
     * @param gridY             the swatch grid's own top
     * @param gridColumns       how many swatches fit across the window at this width
     * @param gridRows          how many rows the swatch grid takes at this width and count
     * @param accentCaptionY    the "Accent" caption, or {@link #ABSENT} on the plain bevel skin
     * @param accentY           the accent swatches' row, or {@link #ABSENT}
     * @param themeCaptionY     the "Theme" caption, or {@link #ABSENT}
     * @param themeY            the theme buttons' row, or {@link #ABSENT}
     * @param clockCaptionY     the "Clock" caption, every skin has one
     * @param clockY            the clock toggle row
     * @param taskbarCaptionY   the "Taskbar" caption, or {@link #ABSENT} off the flat skin
     * @param taskbarY          the taskbar toggle row, or {@link #ABSENT}
     * @param appearanceCaptionY the "Appearance" caption, or {@link #ABSENT}
     * @param appearanceY       the appearance toggle row, or {@link #ABSENT}
     * @param contentHeight     the whole page's height, from the heading to the last row drawn
     */
    public record Offsets(int wallpaperCaptionY, int gridY, int gridColumns, int gridRows,
                          int accentCaptionY, int accentY, int themeCaptionY, int themeY,
                          int clockCaptionY, int clockY, int taskbarCaptionY, int taskbarY,
                          int appearanceCaptionY, int appearanceY, int contentHeight) {

        public boolean richSkin() {
            return accentY != ABSENT;
        }

        public boolean flatSkin() {
            return taskbarY != ABSENT;
        }
    }

    private SettingsLayout() {
    }

    /** How many swatch columns fit across a content area {@code w} pixels wide, clear of the scroll thumb. */
    public static int swatchColumns(final int w) {
        final int usable = w - SCROLL_THUMB_SPACE;
        return Math.max(1, (usable + SWATCH_GAP) / (SWATCH_W + SWATCH_GAP));
    }

    /** The x of swatch {@code index} in a grid of {@code columns} columns, relative to the page's left edge. */
    public static int swatchX(final int index, final int columns) {
        return (index % columns) * (SWATCH_W + SWATCH_GAP);
    }

    /** The y of swatch {@code index} in a grid of {@code columns} columns, under the grid's own top. */
    public static int swatchY(final int gridY, final int index, final int columns) {
        return gridY + (index / columns) * (SWATCH_H + SWATCH_GAP);
    }

    /**
     * Every row of the page, for a content area {@code w} wide, {@code swatchCount} wallpapers offered,
     * {@code richSkin} showing the accent and theme rows (every skin but the plain bevel) and
     * {@code flatSkin} showing the taskbar and appearance rows on top of that (the flat skin alone).
     */
    public static Offsets of(final int w, final int swatchCount, final boolean richSkin, final boolean flatSkin) {
        int y = HEADING_H;
        final int wallpaperCaptionY = y;
        y += CAPTION_H;
        final int gridY = y;
        final int columns = swatchColumns(w);
        final int rows = (swatchCount + columns - 1) / columns;
        y += rows * (SWATCH_H + SWATCH_GAP) + GRID_GAP_AFTER;

        int accentCaptionY = ABSENT;
        int accentY = ABSENT;
        int themeCaptionY = ABSENT;
        int themeY = ABSENT;
        if (richSkin) {
            accentCaptionY = y;
            y += CAPTION_H;
            accentY = y;
            y += ACCENT_ROW_H;
            themeCaptionY = y;
            y += CAPTION_H;
            themeY = y;
            y += THEME_ROW_H;
        }

        final int clockCaptionY = y;
        y += CAPTION_H;
        final int clockY = y;
        y += TOGGLE_ROW_H;

        int taskbarCaptionY = ABSENT;
        int taskbarY = ABSENT;
        int appearanceCaptionY = ABSENT;
        int appearanceY = ABSENT;
        if (flatSkin) {
            taskbarCaptionY = y;
            y += CAPTION_H;
            taskbarY = y;
            y += TOGGLE_ROW_H;
            appearanceCaptionY = y;
            y += CAPTION_H;
            appearanceY = y;
            y += TOGGLE_ROW_H;
        }

        return new Offsets(wallpaperCaptionY, gridY, columns, rows, accentCaptionY, accentY, themeCaptionY, themeY,
                clockCaptionY, clockY, taskbarCaptionY, taskbarY, appearanceCaptionY, appearanceY, y);
    }

    /**
     * The page as solids, for the layout audit: the swatch grid's cells and the band each row (or pair of
     * rows) takes, laid out sequentially so a wrong height calculation that makes two bands trade places
     * shows up as an overlap.
     */
    public static GuiLayout layout(final int w, final int swatchCount, final boolean richSkin,
                                   final boolean flatSkin) {
        final Offsets o = of(w, swatchCount, richSkin, flatSkin);
        final GuiLayout l = new GuiLayout(w, o.contentHeight());
        for (int i = 0; i < swatchCount; i++) {
            l.box("swatch" + i, swatchX(i, o.gridColumns()), swatchY(o.gridY(), i, o.gridColumns()), SWATCH_W,
                    SWATCH_H);
        }
        if (o.richSkin()) {
            l.box("accent", 0, o.accentY(), w, ACCENT_ROW_H);
            l.box("theme", 0, o.themeY(), w, THEME_ROW_H);
        }
        l.box("clock", 0, o.clockY(), w, TOGGLE_ROW_H);
        if (o.flatSkin()) {
            l.box("taskbar", 0, o.taskbarY(), w, TOGGLE_ROW_H);
            l.box("appearance", 0, o.appearanceY(), w, TOGGLE_ROW_H);
        }
        return l;
    }
}
