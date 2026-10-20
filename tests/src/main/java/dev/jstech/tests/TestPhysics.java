/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * The blocks the physics of moving structures is proved with, each given its forces by the test mod's own data map
 * ({@code data/jscore/data_maps/block/block_physics.json}): a thruster, a wheel, a burner, a light block for rafts
 * and decks, a lighter one for envelopes, a levitator and a magnet. Each is as strong as stone, so a blow a test
 * means to break glass with leaves them whole. Their block states and models are written by hand.
 */
public final class TestPhysics {

    /* As strong as stone: as hard to mine, and as hard to break by a blow. */
    private static final UnaryOperator<BlockBehaviour.Properties> STONE_STRONG = properties -> properties
            .strength(1.5F, 6.0F);
    /** A rocket of 20 kilonewtons, pushing the way it faces while it has a signal. */
    public static final BlockEntry<FacingBlock> THRUSTER = block("test_thruster", "Test thruster", FacingBlock::new);
    /** A wheel on a spring, rolling the way it faces, driven by a signal behind it. */
    public static final BlockEntry<FacingBlock> WHEEL = block("test_wheel", "Test wheel", FacingBlock::new);
    /** A burner of five megawatts, always lit, heating the room above it. */
    public static final BlockEntry<Block> BURNER = block("test_burner", "Test burner", Block::new);
    /** A block of five kilograms, a raft's or a cart's deck. */
    public static final BlockEntry<Block> LIGHT = block("test_light_block", "Test light block", Block::new);
    /** A block of two hundred grams, a balloon's envelope. */
    public static final BlockEntry<Block> ENVELOPE = block("test_envelope", "Test envelope", Block::new);
    /** A levitator holding up 200 kilonewtons. */
    public static final BlockEntry<Block> HOVER = block("test_hover", "Test hover", Block::new);
    /** A magnet of 50 kilonewtons, pulling magnetic blocks within six. */
    public static final BlockEntry<Block> MAGNET = block("test_magnet", "Test magnet", Block::new);

    private TestPhysics() {
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

    /** A block that faces one of the six ways, the way it was placed looking. */
    public static final class FacingBlock extends DirectionalBlock {

        public static final MapCodec<FacingBlock> CODEC = simpleCodec(FacingBlock::new);

        public FacingBlock(final Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        public BlockState getStateForPlacement(final BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
        }

        @Override
        protected MapCodec<? extends DirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }
    }
}
