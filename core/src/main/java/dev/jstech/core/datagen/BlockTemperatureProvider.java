/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.BlockTemperatures;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * How hot or cold the game's own blocks are to a body near them, in kelvin, near the real values: a fire's flames at
 * about 1,100, a campfire's 900 while lit, the glow of magma 700, lava in a cauldron as lava; ice a few degrees below
 * freezing, packed and blue ice colder, snow just below it. Lava itself is as hot as its fluid says.
 */
public final class BlockTemperatureProvider extends DataMapProvider {

    public BlockTemperatureProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":block_temperature";
    }

    @Override
    protected void gather() {
        final Builder<Double, Block> map = builder(BlockTemperatures.TEMPERATURE);
        map.add(Blocks.FIRE.builtInRegistryHolder(), 1_100.0, false);
        map.add(Blocks.SOUL_FIRE.builtInRegistryHolder(), 900.0, false);
        map.add(Blocks.CAMPFIRE.builtInRegistryHolder(), 900.0, false);
        map.add(Blocks.SOUL_CAMPFIRE.builtInRegistryHolder(), 700.0, false);
        map.add(Blocks.MAGMA_BLOCK.builtInRegistryHolder(), 700.0, false);
        map.add(Blocks.LAVA_CAULDRON.builtInRegistryHolder(), 1_300.0, false);
        map.add(Blocks.ICE.builtInRegistryHolder(), 268.0, false);
        map.add(Blocks.FROSTED_ICE.builtInRegistryHolder(), 271.0, false);
        map.add(Blocks.PACKED_ICE.builtInRegistryHolder(), 263.0, false);
        map.add(Blocks.BLUE_ICE.builtInRegistryHolder(), 253.0, false);
        map.add(Blocks.SNOW_BLOCK.builtInRegistryHolder(), 270.0, false);
        map.add(Blocks.POWDER_SNOW.builtInRegistryHolder(), 268.0, false);
        map.add(Blocks.SNOW.builtInRegistryHolder(), 271.0, false);
    }
}
