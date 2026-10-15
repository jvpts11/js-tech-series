/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.matter.MatterStack;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a processing machine offers a recipe: the stacks in its input slots, the fluids in its input tanks, the matter
 * in its input tanks of gas and the other states, and the temperature and the pressure inside it.
 *
 * @param items       the input slots, in order, empty ones included
 * @param fluids      the input tanks, in order, empty ones included
 * @param matter      the input tanks of matter, every state's in order, empty ones as none
 * @param temperature the temperature inside it, in kelvin
 * @param pressure    the pressure inside it, in kilopascals
 */
public record ProcessingInput(List<ItemStack> items, List<FluidStack> fluids, List<Optional<MatterStack>> matter,
                              double temperature, double pressure) implements RecipeInput {

    /** The overworld's air: 15 degrees Celsius, in kelvin, at its pressure. */
    public static final double AMBIENT_KELVIN = DimensionRules.OVERWORLD.temperature() + 273.15;
    public static final double AMBIENT_KPA = DimensionRules.OVERWORLD.pressure();

    public ProcessingInput {
        items = List.copyOf(items);
        fluids = List.copyOf(fluids);
        matter = List.copyOf(matter);
    }

    /** Slots and tanks of liquids only, in the overworld's air. */
    public ProcessingInput(final List<ItemStack> items, final List<FluidStack> fluids) {
        this(items, fluids, List.of(), AMBIENT_KELVIN, AMBIENT_KPA);
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
        for (final Optional<MatterStack> held : matter) {
            if (held.isPresent()) {
                return false;
            }
        }
        return true;
    }
}
