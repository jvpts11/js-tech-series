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
import dev.jstech.core.content.IStandingParts;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The blocks the tests place: one drawn with a connected texture, its five tiles in five colours to tell apart; and
 * one with a post standing half a block out of its top, for the aim at standing parts.
 */
public final class TestBlocks {

    /** How hard the test marble is to break; an ore in it is harder by the ores' own margin. */
    public static final float MARBLE_HARDNESS = 2.0f;

    /* Their block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<Block> CONNECTED_PANEL = TestSounds.CONTENT.block("connected_panel", Block::new)
            .named("Connected panel").look(IBlockLook.cubeAll("connected_panel")).register();
    public static final BlockEntry<PostBlock> STANDING_POST = TestSounds.CONTENT.block("test_standing_post",
            PostBlock::new).named("Test standing post").look(IBlockLook.cubeAll("test_standing_post")).register();
    /**
     * A rock of the test mod's own, put in the common tag of stones, which no mod declares as a rock: the Core finds it
     * by the tag and gives it the materials' ores. It is drawn as a column, so its texture is not at its own name, and
     * the ores' art has to find it through its model.
     */
    public static final BlockEntry<Block> MARBLE = TestSounds.CONTENT.block("test_marble", Block::new)
            .named("Test Marble").look(IBlockLook.column("test_marble"))
            .properties(properties -> properties.strength(MARBLE_HARDNESS, 6.0f).requiresCorrectToolForDrops()
                    .sound(SoundType.STONE))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .register();

    private TestBlocks() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** A block with a post standing half a block out of its top, outside its cube. */
    public static final class PostBlock extends Block implements IStandingParts {

        /** The post, in the block's own coordinates. */
        public static final AABB POST = new AABB(0.375, 1.0, 0.375, 0.625, 1.5, 0.625);

        public PostBlock(final Properties properties) {
            super(properties);
        }

        @Override
        public List<AABB> standingParts(final BlockState state, final BlockGetter level, final BlockPos pos) {
            return List.of(POST);
        }
    }
}
