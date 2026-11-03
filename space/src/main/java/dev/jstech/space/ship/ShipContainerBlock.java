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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A starship's fixture that holds things ({@link ShipContainerEntity}), a suit locker, a barrel: a click opens what it
 * holds, and broken it gives it all back.
 */
public abstract class ShipContainerBlock extends ShipFixtureBlock {

    protected ShipContainerBlock(final Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ShipContainerEntity container) {
            player.openMenu(container);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState now,
                            final boolean moved) {
        if (!state.is(now.getBlock()) && level.getBlockEntity(pos) instanceof ShipContainerEntity container) {
            Containers.dropContents(level, pos, container);
        }
        super.onRemove(state, level, pos, now, moved);
    }
}
