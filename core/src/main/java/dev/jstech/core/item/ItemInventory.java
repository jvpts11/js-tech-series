/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.item;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ComponentItemHandler;

/**
 * The stacks an item holds, kept in its {@link ItemStates#INVENTORY} component so they go wherever the item goes. It
 * takes no item that holds stacks itself, so an item is never kept inside another without end, and none the game
 * keeps out of containers (a shulker box).
 */
public final class ItemInventory extends ComponentItemHandler {

    /**
     * @param stack the item that holds the stacks
     * @param slots how many it holds
     */
    public ItemInventory(final ItemStack stack, final int slots) {
        super(stack, ItemStates.INVENTORY.get(), slots);
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        return super.isItemValid(slot, stack) && !ItemStates.of(stack).holdsItems();
    }
}
