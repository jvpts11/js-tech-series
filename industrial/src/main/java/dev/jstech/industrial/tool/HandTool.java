/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.tool;

import dev.jstech.industrial.metallurgy.FormRoute;
import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A hand tool of the metalworker's start, used at a crafting table: the Forge Hammer beats ingots into plates, the
 * Draw Plate pulls them into wire, the Mortar grinds them into dust. It stays in the grid after each use, a use worn,
 * until it is worn out; and by hand always yields less than a machine does.
 */
public class HandTool extends Item {

    private final FormRoute.ByHand work;

    public HandTool(final Properties properties, final FormRoute.ByHand work) {
        super(properties);
        this.work = Objects.requireNonNull(work, "work");
    }

    /** The work it does. */
    public FormRoute.ByHand work() {
        return this.work;
    }

    @Override
    public boolean hasCraftingRemainingItem(final ItemStack stack) {
        return true;
    }

    /** What stays in the grid: itself, a use worn, or nothing once worn out. */
    @Override
    public ItemStack getCraftingRemainingItem(final ItemStack stack) {
        final ItemStack worn = stack.copyWithCount(1);
        final int damage = worn.getDamageValue() + 1;
        if (damage >= worn.getMaxDamage()) {
            return ItemStack.EMPTY;
        }
        worn.setDamageValue(damage);
        return worn;
    }
}
