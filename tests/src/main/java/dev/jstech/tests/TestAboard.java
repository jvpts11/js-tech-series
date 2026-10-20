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
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The blocks what moving structures do aboard is proved with, each given its part by the test mod's own data map
 * ({@code data/jscore/data_maps/block/block_aboard.json}): a decoupler, a docking clamp, a drill, a harvester, a
 * plough, a placer, a seat and a helm. Their block states and models are written by hand.
 */
public final class TestAboard {

    /* As strong as stone. */
    private static final UnaryOperator<BlockBehaviour.Properties> STONE_STRONG = properties -> properties
            .strength(1.5F, 6.0F);
    /** A decoupler, firing at a signal, pushing its two sides apart. */
    public static final BlockEntry<TestPhysics.FacingBlock> DECOUPLER = block("test_decoupler", "Test decoupler",
            TestPhysics.FacingBlock::new);
    /** A docking clamp, closing at a signal behind it. */
    public static final BlockEntry<TestPhysics.FacingBlock> CLAMP = block("test_clamp", "Test clamp",
            TestPhysics.FacingBlock::new);
    /** A drill, mining as an iron pickaxe does. */
    public static final BlockEntry<TestPhysics.FacingBlock> DRILL = block("test_drill", "Test drill",
            TestPhysics.FacingBlock::new);
    /** A harvester, taking ripe crops. */
    public static final BlockEntry<TestPhysics.FacingBlock> HARVESTER = block("test_harvester", "Test harvester",
            TestPhysics.FacingBlock::new);
    /** A plough, tilling the ground. */
    public static final BlockEntry<TestPhysics.FacingBlock> PLOUGH = block("test_plough", "Test plough",
            TestPhysics.FacingBlock::new);
    /** A placer, setting the blocks the structure carries. */
    public static final BlockEntry<TestPhysics.FacingBlock> PLACER = block("test_placer", "Test placer",
            TestPhysics.FacingBlock::new);
    /** A seat. */
    public static final BlockEntry<Block> SEAT = block("test_seat", "Test seat", Block::new);
    /** A helm, a seat whose sitter steers. */
    public static final BlockEntry<Block> HELM = block("test_helm", "Test helm", Block::new);

    private TestAboard() {
    }

    /** Declares the blocks, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static <B extends Block> BlockEntry<B> block(final String id, final String name,
                                                         final Function<BlockBehaviour.Properties, B> factory) {
        return TestSounds.CONTENT.block(id, factory).properties(STONE_STRONG).named(name)
                .look(IBlockLook.cubeAll(id)).register();
    }
}
