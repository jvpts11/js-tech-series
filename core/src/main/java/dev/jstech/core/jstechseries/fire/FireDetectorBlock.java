/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.signal.SensorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A detector hung under a ceiling: of smoke, which rises to it from a fire below, or of heat. It is a sensor, giving
 * its alarm and what it reads as redstone on every side and to a comparator; its lamp lights while it calls.
 */
public final class FireDetectorBlock extends SensorBlock {

    private final Sense sense;

    /** Whether it is calling its alarm. */
    public static final BooleanProperty ALARM = BooleanProperty.create("alarm");
    /* A round head eight texels across, two deep, against the ceiling. */
    private static final VoxelShape HEAD = Block.box(4, 14, 4, 12, 16, 12);

    public FireDetectorBlock(final Sense sense, final Properties properties) {
        super(properties);
        this.sense = sense;
        registerDefaultState(this.stateDefinition.any().setValue(ALARM, false));
    }

    /** What it senses. */
    public Sense sense() {
        return this.sense;
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new FireDetectorBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return Ceilings.holds(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ALARM);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return HEAD;
    }

    @Override
    protected boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos) {
        return Ceilings.holds(level, pos);
    }

    @Override
    protected BlockState updateShape(final BlockState state, final Direction direction, final BlockState neighbour,
                                     final LevelAccessor level, final BlockPos pos, final BlockPos neighbourPos) {
        return direction == Direction.UP && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    /** What a fire detector senses. */
    public enum Sense {
        /** The smoke that rose to it. */
        SMOKE,
        /** How hot the air is where it hangs. */
        HEAT
    }
}
