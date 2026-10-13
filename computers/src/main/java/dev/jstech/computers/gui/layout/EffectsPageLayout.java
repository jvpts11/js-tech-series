/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.gui.EffectsPages;
import dev.jstech.core.gui.layout.GuiLayout;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Where a system's effects page lays its content out inside the Settings window's page area: its title along the
 * top, with the way back at its right; its rows one under another in a part that scrolls when they outgrow the
 * window; and the dialog's buttons along the foot, for the systems whose page had them.
 *
 * <p>A box and a long line wrap onto as many lines as they need, which only the game's font can tell, so the rows'
 * heights are worked out from the number of lines each one takes, given by whoever draws them; the test gives each
 * the most it could take.
 */
public final class EffectsPageLayout {

    /** The title row, and the space under it. */
    public static final int TITLE_H = 14;
    /** One line of the small text the rows are written in. */
    public static final int LINE_H = 8;
    /** The side of a box and of a radio button. */
    public static final int BOX = 7;
    /** The height of a button, a switch and a choice. */
    public static final int CONTROL_H = 13;
    /** The width of a switch at a toggle's right, and of a choice's button. */
    public static final int SWITCH_W = 30;
    public static final int CHOICE_W = 64;
    /** The width of each of the dialog's buttons along the foot, and the gap between them. */
    public static final int FOOTER_BUTTON_W = 40;
    public static final int FOOTER_GAP = 4;
    /** The way back to the page this one is reached from, at the title's right. */
    public static final int BACK_W = 34;
    /** How many lines a box's label or a note may take at the most, past which it is cut. */
    public static final int MOST_LINES = 4;
    /** The size the title is written at, against the font's, so a long one keeps most of its words. */
    public static final float TITLE_SCALE = 0.85f;

    private static final int HEADING_H = 12;
    private static final int ROW_GAP = 2;
    private static final int TOGGLE_PAD = 4;
    private static final int SLIDER_H = 27;
    private static final int PRESET_H = 10;
    private static final int FOOTER_H = CONTROL_H + 4;

    private EffectsPageLayout() {
    }

    /**
     * Where everything on a page goes.
     *
     * @param rowY          where each row starts, from the top of the part that scrolls
     * @param rowH          how tall each row is
     * @param contentHeight how tall all the rows are together, which is what the part that scrolls holds
     * @param scrollTop     where the part that scrolls starts, from the top of the page area
     * @param scrollHeight  how tall the part that scrolls is
     * @param footerY       where the dialog's buttons stand, from the top of the page area, or -1 for none
     */
    public record Placed(List<Integer> rowY, List<Integer> rowH, int contentHeight, int scrollTop, int scrollHeight,
                         int footerY) {

        public Placed {
            rowY = List.copyOf(rowY);
            rowH = List.copyOf(rowH);
        }
    }

    /**
     * Lays a page out in a page area {@code width} by {@code height}, each row's lines counted by {@code lines} (a
     * row that is not text takes none).
     */
    public static Placed place(final EffectsPages.Page page, final int width, final int height,
                               final ToIntFunction<EffectsPages.IRow> lines) {
        final Integer[] ys = new Integer[page.rows().size()];
        final Integer[] hs = new Integer[page.rows().size()];
        int y = 0;
        for (int i = 0; i < page.rows().size(); i++) {
            final EffectsPages.IRow row = page.rows().get(i);
            final int h = rowHeight(row, Math.max(1, Math.min(MOST_LINES, lines.applyAsInt(row))));
            ys[i] = y;
            hs[i] = h;
            y += h + ROW_GAP;
        }
        final boolean footer = page.footer() != EffectsPages.Footer.NONE;
        final int scrollHeight = Math.max(0, height - TITLE_H - (footer ? FOOTER_H : 0));
        return new Placed(List.of(ys), List.of(hs), Math.max(0, y - ROW_GAP), TITLE_H, scrollHeight,
                footer ? height - CONTROL_H : -1);
    }

    /** How tall one row is when its text takes that many lines. */
    public static int rowHeight(final EffectsPages.IRow row, final int lines) {
        return switch (row) {
            case EffectsPages.Heading heading -> HEADING_H;
            case EffectsPages.Note note -> lines * LINE_H + ROW_GAP;
            case EffectsPages.Check check -> Math.max(BOX + 3, lines * LINE_H + ROW_GAP);
            case EffectsPages.Toggle toggle -> Math.max(CONTROL_H, LINE_H + 1 + (toggle.description() == null ? 0
                    : lines * LINE_H)) + TOGGLE_PAD;
            case EffectsPages.Slider slider -> SLIDER_H;
            case EffectsPages.Choice choice -> CONTROL_H + ROW_GAP;
            case EffectsPages.SpeedChoice speed -> CONTROL_H + ROW_GAP;
            case EffectsPages.Presets presets -> 4 * PRESET_H + ROW_GAP;
        };
    }

    /** Where Frames XP's preset {@code index} stands within its row. */
    public static int presetY(final int index) {
        return index * PRESET_H;
    }

    /** The left edge of the dialog's button {@code index}, counted from the left along a foot ending at the right. */
    public static int footerX(final int width, final int index, final int count) {
        return width - (count - index) * (FOOTER_BUTTON_W + FOOTER_GAP) + FOOTER_GAP;
    }

    /** How wide the title may run: to the way back where the page has one, the whole width where it does not. */
    public static int titleRoom(final EffectsPages.Page page, final int width) {
        return page.footer() == EffectsPages.Footer.NONE ? width - BACK_W - 4 : width;
    }

    /**
     * The page as a layout to check: the title, cut to its room, and the way back; every row as a solid box where it
     * stands once the part that scrolls is scrolled to the top (the rows past its foot are clipped by it, not drawn
     * over the foot); and the dialog's buttons.
     */
    public static GuiLayout layout(final EffectsPages.Page page, final int width, final int height,
                                   final ToIntFunction<EffectsPages.IRow> lines, final int titleChars) {
        final Placed placed = place(page, width, height, lines);
        final GuiLayout layout = new GuiLayout(width, height);
        final int fits = (int) (titleRoom(page, width) / (GuiLayout.GLYPH_WIDTH * TITLE_SCALE));
        layout.text("title", 0, 2, Math.min(titleChars, fits), TITLE_SCALE);
        if (page.footer() == EffectsPages.Footer.NONE) {
            layout.box("back", width - BACK_W, 0, BACK_W, CONTROL_H);
        }
        for (int i = 0; i < placed.rowY().size(); i++) {
            final int top = placed.scrollTop() + placed.rowY().get(i);
            if (top + placed.rowH().get(i) <= placed.scrollTop() + placed.scrollHeight()) {
                layout.box("row " + i, 0, top, width, placed.rowH().get(i));
            }
        }
        if (placed.footerY() >= 0) {
            for (int i = 0; i < 3; i++) {
                layout.box("footer " + i, footerX(width, i, 3), placed.footerY(), FOOTER_BUTTON_W, CONTROL_H);
            }
        }
        return layout;
    }
}
