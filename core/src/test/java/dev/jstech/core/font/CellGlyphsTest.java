/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class CellGlyphsTest {

    /** The light, double and mixed box lines, which the terminal font draws as the classic fonts do. */
    private static final String CLASSIC = "─│┌┐└┘├┤┬┴┼═║╒╓╔╕╖╗╘╙╚╛╜╝╞╟╠╡╢╣╤╥╦╧╨╩╪╫╬╴╵╶╷";

    @Test
    void isWholeCell_takesTheBoxLinesAndTheBlocksOnly() {
        assertTrue(CellGlyphs.isWholeCell(0x2500));
        assertTrue(CellGlyphs.isWholeCell(0x259F));
        assertFalse(CellGlyphs.isWholeCell(0x24FF));
        assertFalse(CellGlyphs.isWholeCell(0x25A0), "a geometric shape is a glyph like any other");
        assertFalse(CellGlyphs.isWholeCell('A'));
        assertTrue(CellGlyphs.of('A', 6, 10).isEmpty());
    }

    @Test
    void of_fillsTheWholeCellForTheFullBlock() {
        final boolean[][] lit = pixels(0x2588, 6, 10);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 6; x++) {
                assertTrue(lit[y][x], x + "," + y);
            }
        }
    }

    @Test
    void of_cutsTheBlocksInHalvesEighthsAndQuarters() {
        assertEquals(List.of(new CellRect(0, 0, 6, 5, 255)), CellGlyphs.of(0x2580, 6, 10), "the upper half");
        assertEquals(List.of(new CellRect(0, 9, 6, 1, 255)), CellGlyphs.of(0x2581, 6, 10), "one eighth up");
        assertEquals(List.of(new CellRect(0, 5, 6, 5, 255)), CellGlyphs.of(0x2584, 6, 10), "the lower half");
        assertEquals(List.of(new CellRect(0, 0, 3, 10, 255)), CellGlyphs.of(0x258C, 6, 10), "the left half");
        assertEquals(List.of(new CellRect(3, 0, 3, 10, 255)), CellGlyphs.of(0x2590, 6, 10), "the right half");
        assertEquals(List.of(new CellRect(0, 0, 3, 5, 255), new CellRect(3, 5, 3, 5, 255)),
                CellGlyphs.of(0x259A, 6, 10), "upper left and lower right");
    }

    @Test
    void of_shadesTheWholeCellInAQuarterAHalfAndThreeQuarters() {
        assertEquals(64, CellGlyphs.of(0x2591, 6, 10).get(0).alpha());
        assertEquals(128, CellGlyphs.of(0x2592, 6, 10).get(0).alpha());
        assertEquals(191, CellGlyphs.of(0x2593, 6, 10).get(0).alpha());
    }

    @Test
    void of_drawsEveryArmToTheEdgeAtTheHeightTheNextCellTakesItUp() {
        final List<String> wrong = new ArrayList<>();
        for (int codePoint = 0x2500; codePoint <= 0x257F; codePoint++) {
            final int[] arms = arms(codePoint);
            if (arms == null) {
                continue;
            }
            final boolean[][] lit = pixels(codePoint, 6, 10);
            check(wrong, codePoint, "up", arms[0], columnsLit(lit, 0), Set.of(2), Set.of(2, 3), Set.of(1, 3));
            check(wrong, codePoint, "right", arms[1], rowsLit(lit, 5), Set.of(5), Set.of(4, 5), Set.of(4, 6));
            check(wrong, codePoint, "down", arms[2], columnsLit(lit, 9), Set.of(2), Set.of(2, 3), Set.of(1, 3));
            check(wrong, codePoint, "left", arms[3], rowsLit(lit, 0), Set.of(5), Set.of(4, 5), Set.of(4, 6));
        }
        if (!wrong.isEmpty()) {
            fail(String.join("\n", wrong));
        }
    }

    @Test
    void of_drawsTheClassicBoxLinesAsTheTerminalFontDoes() throws IOException {
        final BitmapFont font = fixed();
        final List<String> wrong = new ArrayList<>();
        for (final int codePoint : CLASSIC.codePoints().toArray()) {
            final BitmapGlyph glyph = font.glyph(codePoint);
            final boolean[][] lit = pixels(codePoint, 6, 10);
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 6; x++) {
                    if (glyph.lit(x, y) != lit[y][x]) {
                        wrong.add(new String(Character.toChars(codePoint)) + " at " + x + "," + y);
                    }
                }
            }
        }
        if (!wrong.isEmpty()) {
            fail("drawn differently from the font:\n" + String.join("\n", wrong));
        }
    }

    @Test
    void of_leavesTheMiddleOfADoubleCrossOpen() {
        final boolean[][] lit = pixels('╬', 6, 10);
        assertFalse(lit[5][2], "the centre");
        assertFalse(lit[4][2] || lit[6][2], "between the lines above and below the centre");
        assertTrue(lit[4][1] && lit[4][3] && lit[6][1] && lit[6][3], "the four inner corners");
    }

    @Test
    void of_scalesToAnyCell() {
        final boolean[][] lit = pixels('┼', 8, 16);
        assertTrue(lit[0][3] && lit[15][3], "the vertical runs top to bottom on the column left of centre");
        assertTrue(lit[8][0] && lit[8][7], "the horizontal runs side to side on the row below the middle");
    }

    @Test
    void of_givesNothingForACellTooSmallToHoldALine() {
        assertTrue(CellGlyphs.of('╬', 2, 2).isEmpty());
    }

    private static void check(final List<String> wrong, final int codePoint, final String arm, final int weight,
                              final Set<Integer> lit, final Set<Integer> light, final Set<Integer> heavy,
                              final Set<Integer> doubled) {
        final Set<Integer> expected = switch (weight) {
            case 1 -> light;
            case 2 -> heavy;
            case 3 -> doubled;
            default -> Set.of();
        };
        if (!expected.equals(lit)) {
            wrong.add(new String(Character.toChars(codePoint)) + " " + arm + ": " + lit + " instead of "
                    + new TreeSet<>(expected));
        }
    }

    /**
     * The weight of each arm of a solid box line, up, right, down and left, read from its Unicode name: 0 none,
     * 1 light, 2 heavy, 3 double; null for the dashed lines, the arcs and the diagonals.
     *
     * <p>A name gives a weight either before the arms it covers ("LIGHT DOWN AND RIGHT") or after them ("DOWN LIGHT
     * AND RIGHT HEAVY"), and "HORIZONTAL" and "VERTICAL" stand for both arms of their axis.
     */
    private static int[] arms(final int codePoint) {
        final String name = Character.getName(codePoint);
        if (name.contains("DASH") || name.contains("ARC") || name.contains("DIAGONAL")) {
            return null;
        }
        final int[] arms = new int[4];
        final List<Integer> pending = new ArrayList<>();
        int prefix = 0;
        for (final String word : name.substring("BOX DRAWINGS ".length()).split(" ")) {
            final int weight = switch (word) {
                case "LIGHT", "SINGLE" -> 1;
                case "HEAVY" -> 2;
                case "DOUBLE" -> 3;
                default -> 0;
            };
            final int[] directions = switch (word) {
                case "UP" -> new int[] {0};
                case "RIGHT" -> new int[] {1};
                case "DOWN" -> new int[] {2};
                case "LEFT" -> new int[] {3};
                case "HORIZONTAL" -> new int[] {1, 3};
                case "VERTICAL" -> new int[] {0, 2};
                default -> new int[0];
            };
            if (weight != 0 && !pending.isEmpty()) {
                pending.forEach(direction -> arms[direction] = weight);
                pending.clear();
            } else if (weight != 0) {
                prefix = weight;
            }
            // "AND" is neither, and a weight given before it still covers the arms after it.
            for (final int direction : directions) {
                if (prefix != 0) {
                    arms[direction] = prefix;
                } else {
                    pending.add(direction);
                }
            }
        }
        return arms;
    }

    private static Set<Integer> columnsLit(final boolean[][] lit, final int row) {
        final Set<Integer> out = new TreeSet<>();
        for (int x = 0; x < lit[row].length; x++) {
            if (lit[row][x]) {
                out.add(x);
            }
        }
        return out;
    }

    private static Set<Integer> rowsLit(final boolean[][] lit, final int column) {
        final Set<Integer> out = new TreeSet<>();
        for (int y = 0; y < lit.length; y++) {
            if (lit[y][column]) {
                out.add(y);
            }
        }
        return out;
    }

    private static boolean[][] pixels(final int codePoint, final int width, final int height) {
        final boolean[][] lit = new boolean[height][width];
        for (final CellRect rect : CellGlyphs.of(codePoint, width, height)) {
            for (int y = rect.y(); y < rect.y() + rect.height(); y++) {
                for (int x = rect.x(); x < rect.x() + rect.width(); x++) {
                    lit[y][x] = true;
                }
            }
        }
        return lit;
    }

    private static BitmapFont fixed() throws IOException {
        try (Reader reader = Files.newBufferedReader(Path.of("src", "main", "resources", "assets", "jscore", "font",
                "source", "fixed_6x10.bdf"), StandardCharsets.US_ASCII)) {
            return BdfReader.read(reader);
        }
    }
}
