/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.villager;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A villager's job site (computers/docs/design/survival.md, "Villagers"): the Programmer's Desk or the Repair Bench,
 * which a villager takes as its work, as it takes a lectern or a smithing table. It faces the one who places it.
 */
public final class JobSiteBlock extends HorizontalDirectionalBlock {

    private final Map<Direction, VoxelShape> shapes;

    public JobSiteBlock(final Properties properties, final Map<Direction, VoxelShape> shapes) {
        super(properties);
        this.shapes = shapes;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** A maker of the job site of {@code shapes}, for its registration. */
    public static Function<Properties, JobSiteBlock> with(final Map<Direction, VoxelShape> shapes) {
        return properties -> new JobSiteBlock(properties, shapes);
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(properties -> new JobSiteBlock(properties, shapes));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
