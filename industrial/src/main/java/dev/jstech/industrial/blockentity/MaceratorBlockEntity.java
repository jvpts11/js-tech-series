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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * The Macerator: grinds its input into the recipe's result, spending {@value #FE_PER_TICK} FE per tick over the
 * recipe's processing time (Tier 1: 200 ticks, ore to two dusts).
 */
public class MaceratorBlockEntity extends ProcessingMachineBlockEntity {

    public static final int FE_PER_TICK = 40;
    private static final int ENERGY_CAPACITY = 16_000;
    private static final int ENERGY_MAX_RECEIVE = 1_000;

    public MaceratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.MACERATOR_BE.get(), pos, state, ENERGY_CAPACITY, ENERGY_MAX_RECEIVE, FE_PER_TICK);
    }

    @Override
    protected Optional<Processing> process(final Level level, final ItemStack input) {
        return level.getRecipeManager()
                .getRecipeFor(IndustrialModule.MACERATING_TYPE.get(), new SingleRecipeInput(input), level)
                .map(recipe -> new Processing(recipe.value().result(), recipe.value().processingTime()));
    }
}
