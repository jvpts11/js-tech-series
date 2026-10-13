/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConnectedQuadrantsTest {

    @Test
    void tile_takesTheBorderOfTheSidesNotJoined() {
        assertEquals(ConnectedQuadrants.ALONE, ConnectedQuadrants.tile(false, false, false));
        assertEquals(ConnectedQuadrants.ALONE, ConnectedQuadrants.tile(false, false, true),
                "a corner alone joins nothing");
        assertEquals(ConnectedQuadrants.ACROSS, ConnectedQuadrants.tile(true, false, false));
        assertEquals(ConnectedQuadrants.UPRIGHT, ConnectedQuadrants.tile(false, true, false));
        assertEquals(ConnectedQuadrants.INNER, ConnectedQuadrants.tile(true, true, false));
        assertEquals(ConnectedQuadrants.WHOLE, ConnectedQuadrants.tile(true, true, true));
    }

    @Test
    void tiles_ofAFaceAloneHaveABorderAllRound() {
        assertArrayEquals(new int[] {0, 0, 0, 0}, ConnectedQuadrants.tiles(0));
    }

    @Test
    void tiles_ofAFaceJoinedAllRoundHaveNoBorder() {
        final int all = 0xFF;
        final int whole = ConnectedQuadrants.WHOLE;
        assertArrayEquals(new int[] {whole, whole, whole, whole}, ConnectedQuadrants.tiles(all));
    }

    @Test
    void tiles_ofATopCornerOfAWallFollowEachQuarter() {
        // Joined left, down and between: the top corner of a wall that runs left and down.
        final int mask = ConnectedQuadrants.LEFT | ConnectedQuadrants.DOWN | ConnectedQuadrants.DOWN_LEFT;

        final int[] tiles = ConnectedQuadrants.tiles(mask);

        assertEquals(ConnectedQuadrants.ACROSS, tiles[0], "up-left: joined left only");
        assertEquals(ConnectedQuadrants.ALONE, tiles[1], "up-right: joined nowhere");
        assertEquals(ConnectedQuadrants.UPRIGHT, tiles[2], "down-right: joined down only");
        assertEquals(ConnectedQuadrants.WHOLE, tiles[3], "down-left: joined all round");
    }

    @Test
    void tiles_ofAnInnerCornerLeaveANotch() {
        final int mask = 0xFF & ~ConnectedQuadrants.UP_RIGHT;

        assertEquals(ConnectedQuadrants.INNER, ConnectedQuadrants.tiles(mask)[1]);
    }
}
