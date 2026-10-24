/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

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
 * A block of a Computational Research Centre that stays a block of its own once the structure forms, the terminal or
 * an entry: it faces whoever placed it, and while the structure stands formed its own model is not drawn, since the
 * formed model draws it in its place, the console at the front and each entry in its cell at the back.
 */
public class CrcBlock extends DeviceBlock {

    /** Whether the structure it is part of stands formed, its formed model drawing it. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public CrcBlock(final Properties properties, final Device<?> device) {
        super(properties, device);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    /*
     * Formed, it draws nothing and the model, set half a pixel in from the block's edges, draws it: so it hides no face
     * of its neighbours, or the ground under it would leave a slit of sky along the model's foot.
     */
    @Override
    protected VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }
}
