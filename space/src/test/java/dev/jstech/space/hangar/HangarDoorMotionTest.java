/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class HangarDoorMotionTest {

    private static final double CLOSE = 1.0e-9;

    @Test
    public void pieces_shutAreEachBlockWhole() {
        assertArrayEquals(new double[] {-12, 0, -11, 1}, HangarDoorMotion.pieces(-12, 0, 0).getFirst(), CLOSE);
        assertArrayEquals(new double[] {5, 50, 6, 51}, HangarDoorMotion.pieces(5, 50, 0).getFirst(), CLOSE);
    }

    @Test
    public void pieces_openAreNone() {
        assertTrue(HangarDoorMotion.pieces(-12, 0, 1).isEmpty());
        assertTrue(HangarDoorMotion.pieces(0, 0, 1).isEmpty());
        assertTrue(HangarDoorMotion.pieces(3, 80, 1).isEmpty());
    }

    @Test
    public void pieces_ofTheLowerDoorSlideTowardItsSidesAsItOpens() {
        // Half open, the left half is half as wide from its outer edge, each block in it with it.
        assertArrayEquals(new double[] {-12, 3, -11.5, 4}, HangarDoorMotion.pieces(-12, 3, 0.5).getFirst(), CLOSE);
        assertArrayEquals(new double[] {12.5, 3, 13, 4}, HangarDoorMotion.pieces(12, 3, 0.5).getFirst(), CLOSE);
    }

    @Test
    public void pieces_ofTheMiddleBlockAreItsTwoHalvesEachWithItsSide() {
        final List<double[]> middle = HangarDoorMotion.pieces(0, 0, 0.5);

        assertEquals(2, middle.size());
        assertArrayEquals(new double[] {-6, 0, -5.75, 1}, middle.get(0), CLOSE);
        assertArrayEquals(new double[] {6.75, 0, 7, 1}, middle.get(1), CLOSE);
    }

    @Test
    public void pieces_ofTheSlotRiseIntoTheirLeavesAsItOpens() {
        // The slot's lowest leaf: from the lower door's top, a seventh of the slot high; half open, half as high.
        final double leaf = (HangarDoorMotion.SLOT_TO - HangarDoorMotion.SLOT_FROM) / HangarDoorMotion.LEAVES;
        final List<double[]> lowest = HangarDoorMotion.pieces(0, (int) HangarDoorMotion.SLOT_FROM, 0.5);

        assertArrayEquals(new double[] {0, 12, 1, 12.5}, lowest.getFirst(), CLOSE);
        assertTrue(leaf > 1, "a leaf is many blocks high");
    }

    @Test
    public void pieces_ofABlockAcrossTwoLeavesAreItsPartInEach() {
        final double leaf = (HangarDoorMotion.SLOT_TO - HangarDoorMotion.SLOT_FROM) / HangarDoorMotion.LEAVES;
        final int across = (int) Math.floor(HangarDoorMotion.SLOT_FROM + leaf);
        final List<double[]> pieces = HangarDoorMotion.pieces(0, across, 0);

        assertEquals(2, pieces.size());
        assertEquals(HangarDoorMotion.SLOT_FROM + leaf, pieces.get(0)[3], CLOSE);
        assertEquals(HangarDoorMotion.SLOT_FROM + leaf, pieces.get(1)[1], CLOSE);
    }

    @Test
    public void share_runsFromShutToOpenOverItsTimeAndBack() {
        assertEquals(0, HangarDoorMotion.share(true, 0), CLOSE);
        assertEquals(0.5, HangarDoorMotion.share(true, HangarDoorMotion.TICKS / 2.0), CLOSE);
        assertEquals(1, HangarDoorMotion.share(true, HangarDoorMotion.TICKS * 2.0), CLOSE);
        assertEquals(1, HangarDoorMotion.share(false, 0), CLOSE);
        assertEquals(0, HangarDoorMotion.share(false, HangarDoorMotion.TICKS), CLOSE);
    }
}
