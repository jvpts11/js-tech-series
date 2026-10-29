/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.wreck;

import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * A piece of wreckage and what can be salvaged of it: the blocks of its stage it gives back as it is broken.
 */
public final class RocketWreckageBlockEntity extends SyncedBlockEntity {

    private final FieldItemHandler salvage;

    /** The most stacks a piece of wreckage holds. */
    public static final int SLOTS = 9;

    public RocketWreckageBlockEntity(final BlockPos pos, final BlockState state) {
        super(RocketWreckage.ENTITY.get(), pos, state);
        this.salvage = fields().items("Salvage", SLOTS).save().dropsWhenBroken();
    }

    /** Puts {@code stacks} into what it gives back, as far as it has room; what did not fit. */
    public List<ItemStack> hold(final List<ItemStack> stacks) {
        final List<ItemStack> left = new ArrayList<>();
        for (final ItemStack stack : stacks) {
            final ItemStack rest = ItemHandlerHelper.insertItemStacked(this.salvage, stack.copy(), false);
            if (!rest.isEmpty()) {
                left.add(rest);
            }
        }
        setChanged();
        return List.copyOf(left);
    }

    /** What it gives back, each stack as it is. */
    public List<ItemStack> salvage() {
        final List<ItemStack> held = new ArrayList<>();
        for (int slot = 0; slot < this.salvage.getSlots(); slot++) {
            if (!this.salvage.getStackInSlot(slot).isEmpty()) {
                held.add(this.salvage.getStackInSlot(slot).copy());
            }
        }
        return List.copyOf(held);
    }
}
