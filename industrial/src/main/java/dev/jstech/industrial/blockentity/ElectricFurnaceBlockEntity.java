/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.machine.IndustrialProcessingMachine;
import dev.jstech.industrial.machine.MachineRating;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Electric Furnace: smelts its input by the game's own smelting recipes, drawing 3 kW at low voltage over
 * {@value #PROCESS_TIME} ticks (Tier 1).
 */
public class ElectricFurnaceBlockEntity extends IndustrialProcessingMachine {

    /** A T1 machine on low voltage, drawing 3 kW as it works, as a small electric furnace does. */
    public static final MachineRating RATING = MachineRating.of(IndustrialTier.T1, JsEnergy.LOW, 3_000);
    public static final int PROCESS_TIME = 160;

    public ElectricFurnaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.ELECTRIC_FURNACE_BE.get(), pos, state, RATING, Layout.items(1, 1));
    }

    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        return level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input.getItem(0)), level)
                .map(recipe -> Processing.single(0, recipe.value().getResultItem(level.registryAccess()),
                        PROCESS_TIME));
    }
}
