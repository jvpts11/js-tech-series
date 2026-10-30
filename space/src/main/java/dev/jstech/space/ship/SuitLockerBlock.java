/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The airlock's suit locker: a row of hooks and shelves where a crew keeps its suits and bottles. */
public final class SuitLockerBlock extends ShipFixtureBlock implements EntityBlock {

    public SuitLockerBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new SuitLockerEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SuitLockerEntity locker) {
            player.openMenu(locker);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState now,
                            final boolean moved) {
        if (!state.is(now.getBlock()) && level.getBlockEntity(pos) instanceof SuitLockerEntity locker) {
            Containers.dropContents(level, pos, locker);
        }
        super.onRemove(state, level, pos, now, moved);
    }
}
