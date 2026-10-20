/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YardLayoutTest {

    /* The edge of the world the game keeps, in blocks. */
    private static final int WORLD_EDGE = 29_999_984;

    @Test
    void isYardBlock_isEverythingFromTheYardsFirstBlockEast() {
        assertFalse(YardLayout.isYardBlock(YardLayout.FIRST_BLOCK_X - 1));
        assertTrue(YardLayout.isYardBlock(YardLayout.FIRST_BLOCK_X));
        assertFalse(YardLayout.isYardBlock(-YardLayout.FIRST_BLOCK_X));
    }

    @Test
    void plotOfChunk_findsThePlotOfEveryChunkOfIt() {
        for (final int plot : new int[] {0, 1, YardLayout.COLUMNS, 123_456, YardLayout.PLOTS - 1}) {
            final int x = YardLayout.plotChunkX(plot);
            final int z = YardLayout.plotChunkZ(plot);
            assertEquals(plot, YardLayout.plotOfChunk(x, z));
            assertEquals(plot, YardLayout.plotOfChunk(x + YardLayout.PLOT_CHUNKS - 1, z + YardLayout.PLOT_CHUNKS - 1));
            assertEquals(plot, YardLayout.plotOfBlock(YardLayout.centreX(plot), YardLayout.centreZ(plot)));
        }
    }

    @Test
    void plotOfChunk_isNoneOutsideTheYard() {
        assertEquals(-1, YardLayout.plotOfChunk(0, 0));
        assertEquals(-1, YardLayout.plotOfChunk(YardLayout.FIRST_CHUNK_X, Integer.MAX_VALUE / 32));
    }

    @Test
    void plots_allLieInsideTheWorldsEdge() {
        final int last = YardLayout.PLOTS - 1;
        assertTrue((YardLayout.plotChunkX(last) + YardLayout.PLOT_CHUNKS) * 16 <= WORLD_EDGE);
        assertTrue((YardLayout.plotChunkZ(last) + YardLayout.PLOT_CHUNKS) * 16 <= WORLD_EDGE);
        assertTrue(YardLayout.plotChunkZ(0) * 16 >= -WORLD_EDGE);
    }

    @Test
    void plotChunkX_refusesAPlotThatIsNot() {
        assertThrows(IllegalArgumentException.class, () -> YardLayout.plotChunkX(-1));
        assertThrows(IllegalArgumentException.class, () -> YardLayout.plotChunkX(YardLayout.PLOTS));
    }
}
