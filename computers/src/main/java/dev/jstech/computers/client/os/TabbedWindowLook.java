/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The look a program window of tabbed pages shares, as the Crypto Wallet, the Mining Panel and the Backup Console are
 * drawn: a row of tabs under a strip of the panel colour, tables of rows, headings a third larger than the body text,
 * paragraphs, and a status bar at the foot, all in the desktop's own text.
 */
final class TabbedWindowLook {

    /** The row the tabs sit in, under a strip of the panel colour. */
    static final int TAB_H = 17;
    /** The status bar at the foot. */
    static final int STATUS_H = 13;
    /** A table's header, its rows right under it. */
    static final int HEADER_H = 12;
    /** A table's row. */
    static final int ROW_H = 13;
    /** A text field, a drop-down or a button. */
    static final int FIELD_H = 15;
    /** The margin of a page's words from the window's sides. */
    static final int PAD = 8;
    /** The pitch of a paragraph's lines. */
    static final int LINE_H = 10;
    /** A heading's scale over the body text. */
    static final float HEADING = 4f / 3f;
    /* Where a cell's words start in from its column's edge. */
    private static final int CELL_INSET = 5;
    private static final int TABS_INSET = 7;

    private TabbedWindowLook() {
    }

    /** Lays the tabs in their row at the top of the window's content. */
    static void placeTabs(final TabStrip tabs, final int x, final int y, final int width) {
        tabs.setUnderline(false);
        tabs.setBounds(x + TABS_INSET, y + 3, width - 2 * TABS_INSET, TAB_H - 3);
    }

    /** The strip under the tabs and its line, drawn before them. */
    static void tabRow(final GuiGraphics g, final OsSkin skin, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + TAB_H, skin.panelBg());
        g.fill(x, y + TAB_H - 1, x + width, y + TAB_H, skin.edge());
    }

    /** A table's header with the words of {@code labels}; these tables are not sorted by a click. */
    static ColumnHeader header(final List<String> labels) {
        return new ColumnHeader(labels).setSortable(false);
    }

    /**
     * The left edges of a table's columns over {@code width} from {@code x}: the first at the table's edge, each
     * other at its {@code percents} of the width.
     */
    static int[] columns(final int x, final int width, final int... percents) {
        final int[] out = new int[percents.length + 1];
        out[0] = x + CELL_INSET;
        for (int i = 0; i < percents.length; i++) {
            out[i + 1] = x + width * percents[i] / 100;
        }
        return out;
    }

    /** One row of a table, each text in its column and cut to the room up to the next. */
    static void cells(final GuiGraphics g, final UiContext ctx, final int x, final int y, final int w,
                      final int[] columns, final String... texts) {
        for (int i = 0; i < texts.length && i < columns.length; i++) {
            final int end = i + 1 < columns.length ? columns[i + 1] : x + w - CELL_INSET;
            Draw.text(g, ctx.font(), Texts.clip(ctx.font(), texts[i], end - columns[i] - 4), columns[i], y + 3,
                    ctx.skin().text());
        }
    }

    /** A heading, a third larger than the body text. */
    static void heading(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                        final int colour) {
        Texts.scaled(g, font, text, x, y, HEADING, colour);
    }

    /** A paragraph wrapped to {@code width}, its lines {@code pitch} apart; the height it took. */
    static int paragraph(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                         final int width, final int pitch, final int colour) {
        final List<FormattedCharSequence> lines = lines(font, text, width);
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(g, font, lines.get(i), x, y + i * pitch, colour);
        }
        return lines.size() * pitch;
    }

    /** The lines {@code text} takes wrapped to {@code width}. */
    static List<FormattedCharSequence> lines(final Font font, final String text, final int width) {
        return font.split(Component.literal(text), Math.max(1, width));
    }

    /** The status bar at the foot of the window: {@code right} at its right, {@code left} cut to the room left. */
    static void statusBar(final GuiGraphics g, final Font font, final OsSkin skin, final int x, final int y,
                          final int width, final String left, final String right) {
        skin.statusBar(g, x, y, width, STATUS_H);
        final int rightX = x + width - PAD - font.width(right);
        Draw.text(g, font, right, rightX, y + 3, skin.dim());
        Draw.text(g, font, Texts.clip(font, left, rightX - 8 - (x + PAD)), x + PAD, y + 3, skin.dim());
    }

    /** A number with its thousands grouped. */
    static String grouped(final long number) {
        return String.format(Locale.ROOT, "%,d", number);
    }
}
