/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.function.Predicate;

/**
 * While a menu stays open. A menu opened on a block stays open while that very block entity stands there and the
 * player can reach it, which holds for every variant of the block (every era of a machine) that makes the same
 * block entity, so no menu can close the moment it opens because it was checked against one variant only.
 */
public final class MenuValidity {

    /** How far past the player's reach a menu stays open, as the game's own containers allow. */
    private static final double REACH_MARGIN = 4.0;

    private MenuValidity() {
    }

    /** Valid while {@code blockEntity} still stands where it was and the player can reach it. */
    public static Predicate<Player> blockEntity(final BlockEntity blockEntity) {
        return player -> {
            final Level level = blockEntity.getLevel();
            final BlockPos pos = blockEntity.getBlockPos();
            return level != null && !blockEntity.isRemoved() && level.getBlockEntity(pos) == blockEntity
                    && player.canInteractWithBlock(pos, REACH_MARGIN);
        };
    }
}
