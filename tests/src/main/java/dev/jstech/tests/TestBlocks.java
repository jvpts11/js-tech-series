/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import net.minecraft.world.level.block.Block;

/** The blocks the tests place: one drawn with a connected texture, its five tiles in five colours to tell apart. */
public final class TestBlocks {

    /* Its block state and model are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<Block> CONNECTED_PANEL = TestSounds.CONTENT.block("connected_panel", Block::new)
            .named("Connected panel").look(IBlockLook.cubeAll("connected_panel")).register();

    private TestBlocks() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }
}
