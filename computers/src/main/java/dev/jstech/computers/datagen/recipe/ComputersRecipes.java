/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.recipe;

import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.core.condition.SettingCondition;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

/**
 * Every recipe of J's Computers (computers/docs/design/recipes.md and survival.md), written by the data generation: the
 * materials and components its chain's machines make, the blocks of its machines, and everything assembled at its
 * benches. Each part of it is written by the rule the design gives its kind, from the part's own figures, so a part
 * added to the catalogue gets its recipe by the same rule.
 */
public final class ComputersRecipes extends RecipeProvider {

    public ComputersRecipes(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(final RecipeOutput output) {
        // Every processing recipe waits until all are written, so each model's chip can be given its class.
        final ChipClassing classing = new ChipClassing(output);
        // The chain's machines' recipes load only while the server's own_chain switch is on.
        MaterialRecipes.write(classing.withConditions(SettingCondition.on(ComputersServerConfig.FILE,
                ComputersServerConfig.OWN_CHAIN)));
        BenchRecipes.write(classing);
        HardwareRecipes.write(classing);
        MachineBlockRecipes.write(classing);
        CardRecipes.write(classing);
        ComputerRecipes.write(classing);
        PeripheralRecipes.write(classing);
        NetworkRecipes.write(classing);
        PowerRecipes.write(classing);
        WirelessRecipes.write(classing);
        VillagerRecipes.write(classing);
        classing.finish();
    }
}
