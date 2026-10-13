/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GridLayoutTest {

    /** An e with an acute accent: a letter the test's cell font lacks and the game's font draws a cell wide. */
    private static final int E_ACUTE = 0x00E9;

    /** A grid of six-pixel cells whose own font has the ASCII letters, every one six wide but i, l and the W. */
    private static final IGridMetrics METRICS = new IGridMetrics() {
        @Override
        public int cellWidth() {
            return 6;
        }

        @Override
        public boolean inCellFont(final int codePoint) {
            return codePoint < 0x80;
        }

        @Override
        public int advance(final int codePoint, final boolean cellFont) {
            if (!cellFont) {
                return codePoint == E_ACUTE ? 6 : 4;
            }
            return switch (codePoint) {
                case 'i', 'l' -> 4;
                case 'W' -> 7;
                default -> 6;
            };
        }
    };

    @Test
    void layout_drawsARunOfFullCellsAsOneString() {
        assertEquals(List.of(text("abc", 0, true)), GridLayout.layout(row(span("abc", "x")), METRICS));
    }

    @Test
    void layout_drawsANarrowCellFontCharacterAtItsCellsLeftEdge() {
        assertEquals(List.of(text("a", 0, true), text("i", 6, true), text("b", 12, true)),
                GridLayout.layout(row(span("aib", "x")), METRICS));
    }

    @Test
    void layout_drawsAWideCellFontCharacterAtItsCellsLeftEdgeToo() {
        assertEquals(List.of(text("W", 0, true), text("a", 6, true)),
                GridLayout.layout(row(span("Wa", "x")), METRICS));
    }

    @Test
    void layout_centresACharacterOnlyTheGameHasInItsCell() {
        assertEquals(List.of(text("a", 0, true), text("ж", 7, false)),
                GridLayout.layout(row(span("aж", "x")), METRICS));
    }

    @Test
    void layout_runsFullCellsOfTheGamesFontTogetherButApartFromTheCellFonts() {
        final String e = Character.toString(E_ACUTE);
        assertEquals(List.of(text("ab", 0, true), text(e + e, 12, false), text("c", 24, true)),
                GridLayout.layout(row(span("ab" + e + e + "c", "x")), METRICS));
    }

    @Test
    void layout_letsTheGridDrawTheBoxLinesAndBlocks() {
        assertEquals(List.of(new IGridPiece.Whole<>('┌', 0, "x"), new IGridPiece.Whole<>('─', 6, "x"),
                text("a", 12, true), new IGridPiece.Whole<>('█', 18, "x")),
                GridLayout.layout(row(span("┌─a█", "x")), METRICS));
    }

    @Test
    void layout_leavesASpaceEmptyAndStartsAgainAfterIt() {
        assertEquals(List.of(text("a", 0, true), text("b", 12, true)),
                GridLayout.layout(row(span("a b", "x")), METRICS));
    }

    @Test
    void layout_startsAPieceWhereTheStyleChanges() {
        assertEquals(List.of(text("ab", 0, true), new IGridPiece.Text<>("cd", 12, "y", true)),
                GridLayout.layout(row(span("ab", "x"), span("cd", "y")), METRICS));
    }

    @Test
    void layout_givesACharacterOutsideTheBasicPlaneOneCell() {
        // The G clef, beyond the basic plane: a string holds it as two chars, and it takes one cell.
        final String clef = Character.toString(0x1D11E);
        final List<IGridPiece<String>> pieces = GridLayout.layout(row(span(clef + "a", "x")), METRICS);
        assertEquals(text("a", 6, true), pieces.get(1));
    }

    @Test
    void layout_drawsNothingForAnEmptyRow() {
        assertTrue(GridLayout.layout(row(), METRICS).isEmpty());
        assertTrue(GridLayout.layout(row(span("   ", "x")), METRICS).isEmpty());
    }

    private static IGridPiece<String> text(final String text, final int x, final boolean cellFont) {
        return new IGridPiece.Text<>(text, x, "x", cellFont);
    }

    @SafeVarargs
    private static List<GridSpan<String>> row(final GridSpan<String>... spans) {
        return List.of(spans);
    }

    private static GridSpan<String> span(final String text, final String style) {
        return new GridSpan<>(text, style);
    }
}
