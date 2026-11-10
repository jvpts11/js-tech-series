/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block of a chain machine that keeps something of its own, so it never merges into the formed model: a machine's
 * terminal and its entries. While its machine stands formed it draws nothing, the formed model drawing it as the
 * machine's own part.
 */
public class ChainFormedBlock extends DeviceBlock {

    /** Whether the machine it is part of stands formed, its formed model drawing it. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ChainFormedBlock(final Properties properties, final Device<?> device) {
        super(properties, device);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    /* Formed, it hides no face of its neighbours: the formed model stands in its place. */
    @Override
    protected VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }
}
