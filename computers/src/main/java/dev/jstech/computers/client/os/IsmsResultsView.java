/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.IsmsLayout;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The pane under a query: its Results, Messages and Plan tabs and whichever is in front. The tables a script read
 * stand one under another, as many as it read, each with its own columns; in text they are laid on a grid of
 * characters, the way Results to Text prints them. The messages say what each statement did, an error in red; the
 * plan reads root first, each step indented under what needs it.
 */
final class IsmsResultsView {

    /** The tabs, in the order they stand. */
    static final List<IsmsDocument.ResultsTab> TABS = List.of(IsmsDocument.ResultsTab.RESULTS,
            IsmsDocument.ResultsTab.MESSAGES, IsmsDocument.ResultsTab.PLAN);
    private static final int TAB_PAD = 14;
    private static final int CELL_PAD = 8;
    private static final int MOST_CELL_W = 140;
    /** How wide a character is when a table is laid out as text. */
    private static final int CHAR_W = 6;
    private static final int PLAN_INDENT = 10;
    private static final int NUMBER_LIMIT = 18;

    private IsmsResultsView() {
    }

    /** The words on {@code tab}. */
    static TextKey word(final IsmsDocument.ResultsTab tab) {
        return switch (tab) {
            case RESULTS -> IsmsTexts.RESULTS;
            case MESSAGES -> IsmsTexts.MESSAGES;
            case PLAN -> IsmsTexts.PLAN;
        };
    }

    /** Draws the tabs at ({@code x}, {@code y}) and, under them, the pane in front, down to {@code bottom}. */
    static void render(final GuiGraphics g, final Font font, final IsmsLook.Colours c, final IsmsDocument doc,
                       final int x, final int y, final int w, final int bottom) {
        g.fill(x, y, x + w, y + IsmsLayout.RES_TABS_H, c.head());
        g.fill(x, y + IsmsLayout.RES_TABS_H - 1, x + w, y + IsmsLayout.RES_TABS_H, c.edge());
        int tx = x;
        for (final IsmsDocument.ResultsTab tab : TABS) {
            final String label = GameText.resolve(word(tab));
            final int tw = font.width(label) + TAB_PAD;
            final boolean on = doc.tab == tab;
            if (on) {
                g.fill(tx, y, tx + tw, y + IsmsLayout.RES_TABS_H, c.panel());
                g.fill(tx, y, tx + tw, y + 2, c.accent());
            }
            Draw.text(g, font, label, tx + TAB_PAD / 2, y + 2, on ? c.text() : c.dim(), on ? c.panel() : c.head());
            tx += tw;
        }
        final int top = y + IsmsLayout.RES_TABS_H;
        g.fill(x, top, x + w, bottom, c.panel());
        Draw.pushScissor(g, x, top, x + w, bottom);
        final int fit = Math.max(1, (bottom - top - 2) / IsmsLayout.ROW_H);
        switch (doc.tab) {
            case RESULTS -> {
                if (doc.mode == IsmsSettings.Results.TEXT) {
                    text(g, font, c, doc, x, top, fit);
                } else {
                    grid(g, font, c, doc, x, top, w, fit);
                }
            }
            case MESSAGES -> messages(g, font, c, doc, x, top, w, fit);
            case PLAN -> plan(g, font, c, doc, x, top, w, fit);
        }
        Draw.popScissor(g);
    }

    /** The tab under the point, or null. */
    static IsmsDocument.ResultsTab tabAt(final Font font, final int x, final int y, final double mx, final double my) {
        if (my < y || my >= y + IsmsLayout.RES_TABS_H) {
            return null;
        }
        int tx = x;
        for (final IsmsDocument.ResultsTab tab : TABS) {
            final int tw = font.width(GameText.resolve(word(tab))) + TAB_PAD;
            if (mx >= tx && mx < tx + tw) {
                return tab;
            }
            tx += tw;
        }
        return null;
    }

    /** The middle of {@code tab}, where a test clicks it. */
    static int[] tabCenter(final Font font, final int x, final int y, final IsmsDocument.ResultsTab tab) {
        int tx = x;
        for (final IsmsDocument.ResultsTab each : TABS) {
            final int tw = font.width(GameText.resolve(word(each))) + TAB_PAD;
            if (each == tab) {
                return new int[] {tx + tw / 2, y + IsmsLayout.RES_TABS_H / 2};
            }
            tx += tw;
        }
        return new int[] {x, y};
    }

    /** How many lines the pane in front holds, for its scrolling. */
    static int lines(final IsmsDocument doc) {
        return switch (doc.tab) {
            case RESULTS -> {
                int lines = 0;
                for (final IsmsDocument.ResultSet set : doc.results) {
                    lines += set.rows().size() + (doc.mode == IsmsSettings.Results.TEXT ? 3 : 2);
                }
                yield lines;
            }
            case MESSAGES -> doc.messages.size();
            case PLAN -> doc.plan.size();
        };
    }

    /** Whether a cell reads as a number, which a grid sets to the right and in the accent. */
    static boolean numeric(final String cell) {
        return !cell.isEmpty() && cell.length() <= NUMBER_LIMIT && cell.matches("-?[\\d,]+(\\.\\d+)?%?");
    }

    private static void grid(final GuiGraphics g, final Font font, final IsmsLook.Colours c, final IsmsDocument doc,
                             final int x, final int top, final int w, final int fit) {
        if (doc.results.isEmpty()) {
            Draw.text(g, font, GameText.resolve(IsmsTexts.NO_RESULTS), x + 4, top + 3, c.dim(), c.panel());
            return;
        }
        int line = 0;
        final int start = doc.resultsScroll;
        for (final IsmsDocument.ResultSet set : doc.results) {
            final int[] xs = columns(font, set, x, w);
            if (line >= start && line - start < fit) {
                header(g, font, c, set, xs, x, top + 1 + (line - start) * IsmsLayout.ROW_H, w);
            }
            line++;
            for (final List<String> row : set.rows()) {
                if (line >= start && line - start < fit) {
                    row(g, font, c, set, row, xs, x, top + 1 + (line - start) * IsmsLayout.ROW_H, w, c.panel());
                }
                line++;
            }
            line++;
        }
    }

    /**
     * Draws one table at {@code top}: its header, then its rows from row {@code scroll}, as many as {@code fit}
     * lines hold, the row {@code selected} lit.
     */
    static void table(final GuiGraphics g, final Font font, final IsmsLook.Colours c,
                      final IsmsDocument.ResultSet set, final int x, final int top, final int w, final int fit,
                      final int scroll, final int selected) {
        final int[] xs = columns(font, set, x, w);
        header(g, font, c, set, xs, x, top, w);
        for (int i = scroll; i < set.rows().size() && i - scroll < fit - 1; i++) {
            final int ry = top + (i - scroll + 1) * IsmsLayout.ROW_H;
            final int ground = i == selected ? c.select() : c.panel();
            if (i == selected) {
                g.fill(x, ry, x + w, ry + IsmsLayout.ROW_H, ground);
            }
            row(g, font, c, set, set.rows().get(i), xs, x, ry, w, ground);
        }
    }

    private static void header(final GuiGraphics g, final Font font, final IsmsLook.Colours c,
                               final IsmsDocument.ResultSet set, final int[] xs, final int x, final int ry,
                               final int w) {
        g.fill(x, ry, x + w, ry + IsmsLayout.ROW_H, c.head());
        for (int col = 0; col < set.columns().size(); col++) {
            final int right = col + 1 < xs.length ? xs[col + 1] : x + w;
            g.fill(right - 1, ry, right, ry + IsmsLayout.ROW_H, c.edge());
            Draw.text(g, font, Texts.clip(font, set.columns().get(col), right - xs[col] - CELL_PAD), xs[col] + 4,
                    ry + 1, c.headText(), c.head());
        }
    }

    private static void row(final GuiGraphics g, final Font font, final IsmsLook.Colours c,
                            final IsmsDocument.ResultSet set, final List<String> row, final int[] xs, final int x,
                            final int ry, final int w, final int ground) {
        g.fill(x, ry + IsmsLayout.ROW_H - 1, x + w, ry + IsmsLayout.ROW_H, c.split());
        for (int col = 0; col < set.columns().size() && col < row.size(); col++) {
            final int right = col + 1 < xs.length ? xs[col + 1] : x + w;
            final String cell = Texts.clip(font, row.get(col), right - xs[col] - CELL_PAD);
            if (numeric(row.get(col))) {
                Draw.text(g, font, cell, right - 4 - font.width(cell), ry + 1, c.accent(), ground);
            } else {
                Draw.text(g, font, cell, xs[col] + 4, ry + 1, c.text(), ground);
            }
        }
    }

    /* Where each column of {@code set} starts: as wide as its widest cell, within reason, the last taking the rest. */
    private static int[] columns(final Font font, final IsmsDocument.ResultSet set, final int x, final int w) {
        final int[] xs = new int[set.columns().size()];
        int at = x;
        for (int col = 0; col < xs.length; col++) {
            xs[col] = at;
            int widest = font.width(set.columns().get(col));
            for (final List<String> row : set.rows()) {
                if (col < row.size()) {
                    widest = Math.max(widest, font.width(row.get(col)));
                }
            }
            at += Math.min(MOST_CELL_W, widest) + CELL_PAD;
        }
        return xs;
    }

    private static void text(final GuiGraphics g, final Font font, final IsmsLook.Colours c, final IsmsDocument doc,
                             final int x, final int top, final int fit) {
        if (doc.results.isEmpty()) {
            Draw.text(g, font, GameText.resolve(IsmsTexts.NO_RESULTS), x + 4, top + 3, c.dim(), c.panel());
            return;
        }
        final List<TextLine> lines = new ArrayList<>();
        for (final IsmsDocument.ResultSet set : doc.results) {
            final int[] widths = new int[set.columns().size()];
            for (int col = 0; col < widths.length; col++) {
                widths[col] = set.columns().get(col).length();
                for (final List<String> row : set.rows()) {
                    if (col < row.size()) {
                        widths[col] = Math.max(widths[col], row.get(col).length());
                    }
                }
            }
            lines.add(new TextLine(set.columns(), widths));
            final List<String> rules = new ArrayList<>();
            for (final int width : widths) {
                rules.add("-".repeat(width));
            }
            lines.add(new TextLine(rules, widths));
            set.rows().forEach(row -> lines.add(new TextLine(row, widths)));
            lines.add(new TextLine(List.of(GameText.resolve(rowsSaid(set.rows().size()))), new int[] {0}));
        }
        final int start = Math.min(doc.resultsScroll, Math.max(0, lines.size() - 1));
        for (int i = start; i < lines.size() && i - start < fit; i++) {
            final TextLine line = lines.get(i);
            final int ry = top + 1 + (i - start) * IsmsLayout.ROW_H;
            int chars = 0;
            for (int col = 0; col < line.cells().size(); col++) {
                Draw.text(g, font, line.cells().get(col), x + 4 + chars * CHAR_W, ry + 1, c.text(), c.panel());
                chars += (col < line.widths().length ? line.widths()[col] : 0) + 2;
            }
        }
    }

    /** What a pane says of how many rows a statement read. */
    static Text rowsSaid(final int rows) {
        return rows == 1 ? IsmsTexts.ONE_ROW.text() : IsmsTexts.ROW_COUNT.with(rows);
    }

    private static void messages(final GuiGraphics g, final Font font, final IsmsLook.Colours c,
                                 final IsmsDocument doc, final int x, final int top, final int w, final int fit) {
        final int start = Math.min(doc.resultsScroll, Math.max(0, doc.messages.size() - 1));
        for (int i = start; i < doc.messages.size() && i - start < fit; i++) {
            final IsmsDocument.Message message = doc.messages.get(i);
            final int colour = switch (message.tone()) {
                case PLAIN -> c.text();
                case GOOD -> c.good();
                case BAD -> c.bad();
            };
            Draw.text(g, font, Texts.clip(font, message.text(), w - 8), x + 4, top + 2 + (i - start)
                    * IsmsLayout.ROW_H, colour, c.panel());
        }
    }

    private static void plan(final GuiGraphics g, final Font font, final IsmsLook.Colours c, final IsmsDocument doc,
                             final int x, final int top, final int w, final int fit) {
        if (doc.plan.isEmpty()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(IsmsTexts.NO_PLAN), w - 8), x + 4, top + 3, c.dim(),
                    c.panel());
            return;
        }
        final int start = Math.min(doc.resultsScroll, Math.max(0, doc.plan.size() - 1));
        for (int i = start; i < doc.plan.size() && i - start < fit; i++) {
            final int depth = i < doc.planDepth.size() ? doc.planDepth.get(i) : 0;
            final int ry = top + 2 + (i - start) * IsmsLayout.ROW_H;
            final int lx = x + 4 + depth * PLAN_INDENT;
            if (depth > 0) {
                // The line from what needs this step down to it, as a plan's tree draws it.
                g.fill(lx - PLAN_INDENT + 3, ry - 2, lx - PLAN_INDENT + 4, ry + 4, c.dim());
                g.fill(lx - PLAN_INDENT + 3, ry + 4, lx - 2, ry + 5, c.dim());
            }
            Draw.text(g, font, Texts.clip(font, GameText.resolve(doc.plan.get(i)), x + w - lx - 4), lx, ry,
                    depth == 0 ? c.accent() : c.text(), c.panel());
        }
    }

    /** A line of a table laid out as text: its cells and how many characters each column takes. */
    private record TextLine(List<String> cells, int[] widths) {
    }
}
