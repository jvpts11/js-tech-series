/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.computers.storage.IDataSink;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Gateway's buffer as the destination of a network SELECT: items pulled for the ComputerCraft side
 * land in its nine slots, as many as fit. Only items have a place there; fluids and chemicals stay data.
 */
public final class BufferSink implements IDataSink {

    private final IItemHandler buffer;

    public BufferSink(final IItemHandler buffer) {
        this.buffer = buffer;
    }

    @Override
    public long insert(final StorageKey key, final long amount, final boolean simulate) {
        if (!key.isItem() || amount <= 0L) {
            return 0L;
        }
        if (simulate) {
            return Math.min(amount, room(key));
        }
        long left = amount;
        for (int slot = 0; slot < buffer.getSlots() && left > 0L; slot++) {
            final int batch = (int) Math.min(left, key.prototype().getMaxStackSize());
            final ItemStack rest = buffer.insertItem(slot, key.stack(batch), false);
            left -= batch - rest.getCount();
        }
        return amount - left;
    }

    /**
     * How many of {@code key} the buffer can still take: a full stack per empty slot, the rest of the
     * stack in a slot that already holds the same item. Counted by hand because asking each slot to
     * simulate an insert would see the same empty slot every time.
     */
    private long room(final StorageKey key) {
        final ItemStack prototype = key.prototype();
        final int max = prototype.getMaxStackSize();
        long room = 0L;
        for (int slot = 0; slot < buffer.getSlots(); slot++) {
            final ItemStack held = buffer.getStackInSlot(slot);
            if (held.isEmpty()) {
                room += Math.min(max, buffer.getSlotLimit(slot));
            } else if (ItemStack.isSameItemSameComponents(held, prototype)) {
                room += Math.max(0, Math.min(max, buffer.getSlotLimit(slot)) - held.getCount());
            }
        }
        return room;
    }
}
