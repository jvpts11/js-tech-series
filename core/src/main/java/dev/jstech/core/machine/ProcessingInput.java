/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a processing machine offers a recipe: the stacks in its input slots and the fluids in its input tanks.
 *
 * @param items  the input slots, in order, empty ones included
 * @param fluids the input tanks, in order, empty ones included
 */
public record ProcessingInput(List<ItemStack> items, List<FluidStack> fluids) implements RecipeInput {

    public ProcessingInput {
        items = List.copyOf(items);
        fluids = List.copyOf(fluids);
    }

    /** A machine with one input slot and no tanks. */
    public static ProcessingInput of(final ItemStack item) {
        return new ProcessingInput(List.of(item), List.of());
    }

    @Override
    public ItemStack getItem(final int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (final ItemStack item : items) {
            if (!item.isEmpty()) {
                return false;
            }
        }
        for (final FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
