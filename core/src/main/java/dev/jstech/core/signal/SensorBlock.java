/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A sensor's block: it gives the redstone its {@link SensorBlockEntity} reads, on every face and to a comparator, and
 * has it read again every {@link SensorBlockEntity#READ_EVERY} ticks. A mod writes only its block entity, with what it
 * reads.
 */
public abstract class SensorBlock extends Block implements EntityBlock {

    protected SensorBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isSignalSource(final BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(final BlockState state, final BlockGetter level, final BlockPos pos,
                            final Direction direction) {
        return level.getBlockEntity(pos) instanceof SensorBlockEntity sensor ? sensor.signal() : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(final BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(final BlockState state, final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SensorBlockEntity sensor ? sensor.signal() : 0;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (tickLevel, pos, tickState, entity) -> {
            if (entity instanceof SensorBlockEntity sensor
                    && Math.floorMod(tickLevel.getGameTime() + pos.asLong(), SensorBlockEntity.READ_EVERY) == 0) {
                sensor.read();
            }
        };
    }
}
