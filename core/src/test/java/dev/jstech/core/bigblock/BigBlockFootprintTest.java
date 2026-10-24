/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.bigblock;

import dev.jstech.core.multiblock.IBlockMatcher;
import dev.jstech.core.multiblock.MultiblockPattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pure-Java unit tests for {@link BigBlockFootprint}.
 *
 * <p>These tests cover block counting and builder invariants without requiring Minecraft types on the classpath. The
 * world positions in all four horizontal facings are checked in the GameTest suite, where {@code BlockPos} and
 * {@code Direction} are available.
 */
class BigBlockFootprintTest {

    // Mainframe: 3 wide x 2 tall x 2 deep = 12 cells, all non-IGNORE
    private static final MultiblockPattern MAINFRAME_PATTERN = MultiblockPattern.builder("mainframe_test")
            .layer("P#P", "PPP")
            .layer("PPP", "PPP")
            .where('P', IBlockMatcher.any())
            .build();

    // Server Rack: 2 wide x 3 tall x 2 deep = 12 cells, all non-IGNORE
    private static final MultiblockPattern SERVER_RACK_PATTERN = MultiblockPattern.builder("server_rack_test")
            .layer("#P", "PP")
            .layer("PP", "PP")
            .layer("PP", "PP")
            .where('P', IBlockMatcher.any())
            .build();

    @Test
    void mainframePattern_blockCount_is12() {
        assertEquals(12, new BigBlockFootprint(MAINFRAME_PATTERN).blockCount());
    }

    @Test
    void serverRackPattern_blockCount_is12() {
        assertEquals(12, new BigBlockFootprint(SERVER_RACK_PATTERN).blockCount());
    }

    @Test
    void singleOriginCell_blockCount_is1() {
        MultiblockPattern single = MultiblockPattern.builder("single")
                .layer("#")
                .build();
        assertEquals(1, new BigBlockFootprint(single).blockCount());
    }

    @Test
    void ignoreChar_isExcludedFromBlockCount() {
        // 3 x 1 x 2 = 6 cells total; one IGNORE at (x=2, y=0, z=0) -> 5 counted
        MultiblockPattern holed = MultiblockPattern.builder("holed")
                .layer("#P ", "PPP")
                .where('P', IBlockMatcher.any())
                .build();
        assertEquals(5, new BigBlockFootprint(holed).blockCount());
    }

    @Test
    void allIgnoreExceptOrigin_blockCount_is1() {
        // A 3x1x1 pattern where only the origin is non-IGNORE
        MultiblockPattern sparse = MultiblockPattern.builder("sparse")
                .layer(" # ")
                .build();
        assertEquals(1, new BigBlockFootprint(sparse).blockCount());
    }

    @Test
    void largerPatternWithMixedIgnore_blockCountMatchesNonSpaceCellCount() {
        // 3x2x3 = 18 cells total; 4 corner spaces per layer x 2 layers = 8 IGNORE -> 10 non-IGNORE
        MultiblockPattern frame = MultiblockPattern.builder("frame")
                .layer(" P ", "P#P", " P ")
                .layer(" P ", "PPP", " P ")
                .where('P', IBlockMatcher.any())
                .build();
        assertEquals(10, new BigBlockFootprint(frame).blockCount());
    }

    @Test
    void nullPattern_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new BigBlockFootprint(null));
    }
}
