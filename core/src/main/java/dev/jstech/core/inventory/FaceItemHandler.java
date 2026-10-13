/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * An item handler as one face of a block gives it out: the block's own, taking in only through a face that lets things
 * in and only what the filter lets pass, and giving out only through a face that lets things out.
 */
public final class FaceItemHandler implements IItemHandler {

    private final IItemHandler inner;
    private final FaceMode mode;
    private final ItemFilter filter;

    /**
     * @param inner  the block's own handler
     * @param mode   what the face lets through
     * @param filter what may go in
     */
    public FaceItemHandler(final IItemHandler inner, final FaceMode mode, final ItemFilter filter) {
        this.inner = Objects.requireNonNull(inner, "inner");
        this.mode = Objects.requireNonNull(mode, "mode");
        this.filter = Objects.requireNonNull(filter, "filter");
    }

    @Override
    public int getSlots() {
        return this.inner.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return this.inner.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (!this.mode.takesIn() || stack.isEmpty() || !ItemFilters.allows(this.filter, stack)) {
            return stack;
        }
        return this.inner.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return this.mode.givesOut() ? this.inner.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(final int slot) {
        return this.inner.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return this.mode.takesIn() && ItemFilters.allows(this.filter, stack) && this.inner.isItemValid(slot, stack);
    }
}
