/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SplitSearchTest {

    /* Far out, as the yard's places are. */
    private static final int YARD_X = 27_000_000;

    @Test
    void pieces_partsABarBrokenInTheMiddle() {
        final Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 9; x++) {
            blocks.add(at(x, 64, 0));
        }
        blocks.remove(at(4, 64, 0));
        final List<Set<Long>> pieces = SplitSearch.pieces(Set.of(at(3, 64, 0), at(5, 64, 0)), blocks::contains);
        assertEquals(1, pieces.size());
        assertEquals(4, pieces.get(0).size());
    }

    @Test
    void pieces_findsNoneWhereTheBlocksStillJoinRound() {
        final Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                blocks.add(at(x, 64, z));
            }
        }
        blocks.remove(at(1, 64, 0));
        final List<Set<Long>> pieces = SplitSearch.pieces(Set.of(at(0, 64, 0), at(2, 64, 0), at(1, 64, 1)),
                blocks::contains);
        assertTrue(pieces.isEmpty(), "a ring with a gap in its side is still one: " + pieces);
    }

    @Test
    void pieces_partsOnlyTheSmallCornerOfAGreatPlate() {
        final Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 60; x++) {
            for (int z = 0; z < 60; z++) {
                blocks.add(at(x, 64, z));
            }
        }
        // A corner of four blocks cut away from the rest by a wall of gaps.
        blocks.remove(at(2, 64, 0));
        blocks.remove(at(2, 64, 1));
        blocks.remove(at(0, 64, 2));
        blocks.remove(at(1, 64, 2));
        blocks.remove(at(2, 64, 2));
        final List<Set<Long>> pieces = SplitSearch.pieces(Set.of(at(1, 64, 1), at(3, 64, 1), at(1, 64, 3)),
                blocks::contains);
        assertEquals(1, pieces.size());
        assertEquals(4, pieces.get(0).size());
    }

    @Test
    void pieces_partsEveryPieceButTheLargestWhenAllAreFound() {
        final Set<Long> blocks = new HashSet<>();
        for (int x = 0; x < 11; x++) {
            blocks.add(at(x, 64, 0));
        }
        blocks.remove(at(2, 64, 0));
        blocks.remove(at(5, 64, 0));
        final List<Set<Long>> pieces = SplitSearch.pieces(Set.of(at(1, 64, 0), at(3, 64, 0), at(4, 64, 0),
                at(6, 64, 0)), blocks::contains);
        assertEquals(2, pieces.size());
        int sum = 0;
        for (final Set<Long> piece : pieces) {
            sum += piece.size();
            assertTrue(piece.size() < 5, "the largest, of five, goes on: " + piece.size());
        }
        assertEquals(4, sum);
    }

    @Test
    void offset_movesAPackedPlaceAsTheGamePacksIt() {
        final long from = at(-5, -60, 12);
        assertEquals(at(-4, -61, 14), SplitSearch.offset(from, 1, -1, 2));
    }

    private static long at(final int x, final int y, final int z) {
        return SplitSearch.pack(YARD_X + x, y, -3_000_000 + z);
    }
}
