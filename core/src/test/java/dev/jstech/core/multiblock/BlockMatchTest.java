/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockMatchTest {

    @Test
    void matches_anyOfItsBlocks() {
        final BlockMatch match = BlockMatch.blocks("minecraft:iron_block", "minecraft:gold_block");
        assertTrue(match.matches("minecraft:gold_block"));
        assertFalse(match.matches("minecraft:stone"));
    }

    @Test
    void matchesAt_asksTheWorldForATag() {
        final BlockMatch match = BlockMatch.tag("minecraft:logs");
        final IBlockProvider tagged = new IBlockProvider() {
            @Override
            public String blockAt(final long encodedPos) {
                return "minecraft:oak_log";
            }

            @Override
            public boolean hasTag(final long encodedPos, final String tag) {
                return tag.equals("minecraft:logs");
            }
        };
        assertFalse(match.matches("minecraft:oak_log"), "an id alone says nothing of a tag");
        assertTrue(match.matchesAt(tagged, 0L, "minecraft:oak_log"));
    }

    @Test
    void constructor_refusesASlotThatNamesNothing() {
        assertThrows(IllegalArgumentException.class, () -> new BlockMatch(List.of(), List.of()));
    }
}
