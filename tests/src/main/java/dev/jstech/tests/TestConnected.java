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

/**
 * Blocks the test mod's own resources draw with connected textures, as a resource pack would, in both formats packs
 * write them: by block, the full 47 tiles of OptiFine's and Continuity's {@code ctm} method, its tiles the game's
 * coloured wool, concrete and terracotta so a test can tell each tile; by texture, their compact method of five; by
 * CTM's metadata of a texture, its {@code ctm} type with a companion; and by CTM's override in a block model, a
 * pattern two by two. Their block states, models and rules are written by hand.
 */
public final class TestConnected {

    /** The block of the full method's rule, by block. */
    public static final BlockEntry<Block> FULL = TestSounds.CONTENT.block("test_ctm_full", Block::new)
            .named("Test connected full").look(IBlockLook.cubeAll("test_ctm_full")).register();
    /** A panel whose texture has a glowing one beside it, {@code _e}, as a pack in OptiFine's form writes it. */
    public static final BlockEntry<Block> GLOW_PANEL = TestSounds.CONTENT.block("test_glow_panel", Block::new)
            .named("Test glowing panel").look(IBlockLook.cubeAll("test_glow_panel")).register();
    /** The texture drawn glowing over the panel's. */
    public static final String GLOW_PANEL_GLOWING = "jstests:block/test_glow_panel_e";
    /** The block of the compact method's rule, by its texture. */
    public static final BlockEntry<Block> COMPACT = TestSounds.CONTENT.block("test_ctm_compact", Block::new)
            .named("Test connected compact").look(IBlockLook.cubeAll("test_ctm_compact")).register();
    /** The block whose texture's metadata is CTM's. */
    public static final BlockEntry<Block> FRAME = TestSounds.CONTENT.block("test_ctm_frame", Block::new)
            .named("Test connected frame").look(IBlockLook.cubeAll("test_ctm_frame")).register();
    /** The block whose model overrides its faces of tint index 0 with CTM's pattern. */
    public static final BlockEntry<Block> OVERRIDE = TestSounds.CONTENT.block("test_ctm_override", Block::new)
            .named("Test connected override").look(IBlockLook.cubeAll("test_ctm_override")).register();

    /** The full method's tiles, in their order: the 16 wools, the 16 concretes, the first 15 terracottas. */
    public static final String[] COLOURS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
        "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    /** The compact method's five tiles, in their order. */
    public static final String[] COMPACT_TILES = {"minecraft:block/white_wool", "minecraft:block/red_wool",
        "minecraft:block/blue_wool", "minecraft:block/lime_wool", "minecraft:block/yellow_wool"};

    private TestConnected() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The texture of the full method's tile {@code tile}. */
    public static String fullTile(final int tile) {
        final int colours = COLOURS.length;
        final String kind = tile < colours ? "wool" : tile < colours * 2 ? "concrete" : "terracotta";
        return "minecraft:block/" + COLOURS[tile % colours] + "_" + kind;
    }
}
