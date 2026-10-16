/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.connect.ConnectedQuadrants;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OptifineLayoutsTest {

    private static final int UP = ConnectedQuadrants.UP;
    private static final int UP_RIGHT = ConnectedQuadrants.UP_RIGHT;
    private static final int RIGHT = ConnectedQuadrants.RIGHT;
    private static final int DOWN_RIGHT = ConnectedQuadrants.DOWN_RIGHT;
    private static final int DOWN = ConnectedQuadrants.DOWN;
    private static final int DOWN_LEFT = ConnectedQuadrants.DOWN_LEFT;
    private static final int LEFT = ConnectedQuadrants.LEFT;
    private static final int UP_LEFT = ConnectedQuadrants.UP_LEFT;
    private static final int EDGES = UP | RIGHT | DOWN | LEFT;
    private static final int EVERY = 255;

    @Test
    void full_givesEveryMaskOneOfTheFortySevenTiles() {
        final Set<Integer> used = new HashSet<>();
        for (int mask = 0; mask <= EVERY; mask++) {
            final int tile = OptifineLayouts.full(mask);
            assertTrue(tile >= 0 && tile < OptifineLayouts.FULL_TILES, "mask " + mask + " gave " + tile);
            used.add(tile);
        }
        assertEquals(OptifineLayouts.FULL_TILES, used.size(), "every tile of the template is reached");
    }

    @Test
    void full_followsTheTemplate() {
        assertEquals(0, OptifineLayouts.full(0), "alone");
        assertEquals(1, OptifineLayouts.full(RIGHT), "joined on the right only");
        assertEquals(2, OptifineLayouts.full(RIGHT | LEFT), "sideways");
        assertEquals(12, OptifineLayouts.full(DOWN), "joined below only");
        assertEquals(24, OptifineLayouts.full(UP | DOWN), "up and down");
        assertEquals(26, OptifineLayouts.full(EVERY), "joined all round with every corner");
        assertEquals(46, OptifineLayouts.full(EDGES), "every edge and no corner");
        assertEquals(36, OptifineLayouts.full(UP), "joined above only");
    }

    @Test
    void full_leavesOutCornersWhoseEdgesAreNotJoined() {
        assertEquals(OptifineLayouts.full(RIGHT), OptifineLayouts.full(RIGHT | UP_RIGHT | DOWN_RIGHT),
                "a corner counts only where both edges beside it are joined");
        assertEquals(13, OptifineLayouts.full(RIGHT | DOWN | DOWN_RIGHT));
        assertEquals(4, OptifineLayouts.full(RIGHT | DOWN), "the same two edges without their corner");
    }

    @Test
    void maskOfFull_isWhatEachTileStandsFor() {
        for (int tile = 0; tile < OptifineLayouts.FULL_TILES; tile++) {
            assertEquals(tile, OptifineLayouts.full(OptifineLayouts.maskOfFull(tile)), "tile " + tile);
        }
    }

    @Test
    void compact_numbersItsTilesAsThePacksDo() {
        assertArrayEquals(new int[] {0, 0, 0, 0}, OptifineLayouts.compact(0), "alone");
        assertArrayEquals(new int[] {1, 1, 1, 1}, OptifineLayouts.compact(EVERY), "joined all round");
        assertArrayEquals(new int[] {4, 4, 4, 4}, OptifineLayouts.compact(EDGES), "every edge, no corner");
        assertArrayEquals(new int[] {3, 3, 3, 3}, OptifineLayouts.compact(LEFT | RIGHT), "sideways");
        assertArrayEquals(new int[] {2, 2, 2, 2}, OptifineLayouts.compact(UP | DOWN), "up and down");
    }

    @Test
    void lines_pickTheirEndsAndMiddles() {
        assertEquals(0, OptifineLayouts.horizontal(RIGHT));
        assertEquals(1, OptifineLayouts.horizontal(LEFT | RIGHT));
        assertEquals(2, OptifineLayouts.horizontal(LEFT));
        assertEquals(3, OptifineLayouts.horizontal(UP | DOWN), "joined only the other way: alone");
        assertEquals(0, OptifineLayouts.vertical(UP));
        assertEquals(1, OptifineLayouts.vertical(UP | DOWN));
        assertEquals(2, OptifineLayouts.vertical(DOWN));
        assertEquals(3, OptifineLayouts.vertical(0));
    }

    @Test
    void twoLines_pickTheFirstWayFirst() {
        assertEquals(1, OptifineLayouts.horizontalVertical(LEFT | RIGHT | UP | DOWN), "sideways wins");
        assertEquals(5, OptifineLayouts.horizontalVertical(UP | DOWN), "else up and down");
        assertEquals(4, OptifineLayouts.horizontalVertical(UP));
        assertEquals(6, OptifineLayouts.horizontalVertical(DOWN));
        assertEquals(1, OptifineLayouts.verticalHorizontal(LEFT | RIGHT | UP | DOWN), "up and down wins");
        assertEquals(5, OptifineLayouts.verticalHorizontal(LEFT | RIGHT));
        assertEquals(4, OptifineLayouts.verticalHorizontal(RIGHT));
        assertEquals(6, OptifineLayouts.verticalHorizontal(LEFT));
        assertEquals(3, OptifineLayouts.verticalHorizontal(0));
    }

    @Test
    void overlay_laysATileForEdgesAndCornersApart() {
        assertArrayEquals(new int[0], OptifineLayouts.overlay(0));
        assertArrayEquals(new int[] {8}, OptifineLayouts.overlay(EDGES));
        assertArrayEquals(new int[] {1}, OptifineLayouts.overlay(DOWN));
        assertArrayEquals(new int[] {3}, OptifineLayouts.overlay(RIGHT | DOWN));
        assertArrayEquals(new int[] {5}, OptifineLayouts.overlay(RIGHT | DOWN | LEFT));
        assertArrayEquals(new int[] {15, 1}, OptifineLayouts.overlay(UP | DOWN), "opposite edges each their own");
        assertArrayEquals(new int[] {0}, OptifineLayouts.overlay(DOWN_RIGHT), "a corner alone");
        assertArrayEquals(new int[] {3, 16}, OptifineLayouts.overlay(RIGHT | DOWN | UP_LEFT),
                "a corner whose edges are free beside an edge tile");
        assertArrayEquals(new int[] {9}, OptifineLayouts.overlay(LEFT | UP_LEFT | DOWN_LEFT),
                "corners beside a met edge are the edge's");
    }

    @Test
    void reduce_keepsOnlyCornersBetweenJoinedEdges() {
        assertEquals(UP | RIGHT | UP_RIGHT, OptifineLayouts.reduce(UP | RIGHT | UP_RIGHT | DOWN_LEFT));
        assertEquals(EVERY, OptifineLayouts.reduce(EVERY));
        assertEquals(0, OptifineLayouts.reduce(UP_LEFT | UP_RIGHT | DOWN_LEFT | DOWN_RIGHT));
    }
}
