/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.industrial.IndustrialModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * The Electric Furnace: smelts its input by the game's own smelting recipes, spending {@value #FE_PER_TICK} FE per
 * tick over {@value #PROCESS_TIME} ticks (Tier 1).
 */
public class ElectricFurnaceBlockEntity extends ProcessingMachineBlockEntity {

    public static final int FE_PER_TICK = 30;
    public static final int PROCESS_TIME = 160;
    private static final int ENERGY_CAPACITY = 12_000;
    private static final int ENERGY_MAX_RECEIVE = 600;

    public ElectricFurnaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.ELECTRIC_FURNACE_BE.get(), pos, state, ENERGY_CAPACITY, ENERGY_MAX_RECEIVE,
                FE_PER_TICK);
    }

    @Override
    protected Optional<Processing> process(final Level level, final ItemStack input) {
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                .map(recipe -> new Processing(recipe.value().getResultItem(level.registryAccess()), PROCESS_TIME));
    }
}
