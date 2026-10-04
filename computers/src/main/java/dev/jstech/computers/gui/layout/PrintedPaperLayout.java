/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import dev.jstech.computers.printer.PrintLayout;
import dev.jstech.core.gui.layout.GuiLayout;

/**
 * Geometry of a printed sheet as it is read: the sheet sized to hold a page of print at the small letters, upright or
 * on its side, with the fanfold's tractor margins when the dot matrix printed it, and the pager under it with its two
 * arrows and the page count. The reading screen scales the whole down when the game's window is smaller than it.
 */
public final class PrintedPaperLayout {

    /** The letters a page is printed in, and the room one character and one line take at them. */
    public static final float TEXT_SCALE = 0.75F;
    public static final float CHAR_W = 6 * TEXT_SCALE;
    public static final int LINE_H = 7;
    /** The paper's margin round the print, and the fanfold's strip of tractor holes on each side. */
    public static final int MARGIN = 9;
    public static final int TRACTOR = 12;
    /** The pager under the sheet. */
    public static final int PAGER_GAP = 4;
    public static final int PAGER_H = 12;
    public static final int ARROW_W = 12;

    private PrintedPaperLayout() {
    }

    /**
     * The sheet for pages of {@code columns} by {@code lines}, and where its print starts.
     *
     * @param fanfold whether the dot matrix printed it, which adds the tractor strips
     */
    public static Sheet sheet(final int columns, final int lines, final boolean fanfold) {
        final int side = MARGIN + (fanfold ? TRACTOR : 0);
        final int width = (int) Math.ceil(columns * CHAR_W) + side * 2;
        final int height = lines * LINE_H + MARGIN * 2;
        return new Sheet(width, height, side, MARGIN, width, height + PAGER_GAP + PAGER_H);
    }

    /** The sheet a page upright takes. */
    public static Sheet portrait(final boolean fanfold) {
        return sheet(PrintLayout.PORTRAIT_COLUMNS, PrintLayout.PORTRAIT_LINES, fanfold);
    }

    /** The sheet a page on its side takes. */
    public static Sheet landscape(final boolean fanfold) {
        return sheet(PrintLayout.LANDSCAPE_COLUMNS, PrintLayout.LANDSCAPE_LINES, fanfold);
    }

    /** The sheet, its print and the pager as layout elements, for the layout test. */
    public static GuiLayout layout(final Sheet sheet, final int columns, final int lines) {
        final GuiLayout layout = new GuiLayout(sheet.totalWidth(), sheet.totalHeight());
        layout.box("sheet", 0, 0, sheet.width(), sheet.height());
        for (int i = 0; i < lines; i++) {
            layout.text("line" + i, sheet.textX(), sheet.textY() + i * LINE_H, columns, TEXT_SCALE);
        }
        final int pagerY = sheet.height() + PAGER_GAP;
        layout.box("previous", 0, pagerY, ARROW_W, PAGER_H);
        layout.box("next", sheet.width() - ARROW_W, pagerY, ARROW_W, PAGER_H);
        layout.text("count", sheet.width() / 2 - 30, pagerY + 2, 14, 1.0F);
        return layout;
    }

    /**
     * A sheet's size and where its print starts, and the size of the sheet with its pager.
     *
     * @param width       the sheet's width
     * @param height      the sheet's height
     * @param textX       where the print starts across
     * @param textY       where the print starts down
     * @param totalWidth  the width of sheet and pager together
     * @param totalHeight the height of sheet and pager together
     */
    public record Sheet(int width, int height, int textX, int textY, int totalWidth, int totalHeight) {
    }
}
