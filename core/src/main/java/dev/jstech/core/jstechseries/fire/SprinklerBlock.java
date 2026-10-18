/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A sprinkler head hung under a ceiling, fed water by a pipe or a tank beside it: its glass bulb bursts in the heat
 * of a fire below, and it showers the place under it until the heat has long gone. It takes water in on any side and
 * gives none out.
 */
public final class SprinklerBlock extends Block implements EntityBlock {

    /** Whether its bulb has burst and it showers. */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    /* The head under its pipe, against the ceiling. */
    private static final VoxelShape HEAD = Block.box(5, 8, 5, 11, 16, 11);

    public SprinklerBlock(final Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new SprinklerBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> {
            if (entity instanceof SprinklerBlockEntity sprinkler
                    && Math.floorMod(tickLevel.getGameTime() + pos.asLong(), SprinklerBlockEntity.WORKS_EVERY) == 0) {
                sprinkler.work();
            }
        };
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return Ceilings.holds(context.getLevel(), context.getClickedPos()) ? defaultBlockState() : null;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
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
}
