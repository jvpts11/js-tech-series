/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Spills a block entity's stored items into the world when its block is removed, so a machine's input and output, a
 * computer's installed hardware or a rack's servers are never destroyed with it. A spilled slot is emptied, so a block
 * that spills on a player's break in {@code playerWillDestroy} can spill again in {@code onRemove}, for any other
 * removal, without giving anything twice.
 */
public final class BlockDrops {

    private BlockDrops() {
    }

    public static void spill(final Level level, final BlockPos pos, final ItemStackHandler handler) {
        if (level.isClientSide()) {
            return;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                handler.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }
}
