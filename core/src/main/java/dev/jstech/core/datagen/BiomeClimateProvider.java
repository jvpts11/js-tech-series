/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.worldgen.biome.BiomeClimate;
import dev.jstech.core.worldgen.biome.BiomeClimates;
import dev.jstech.core.worldgen.biome.WorldBiomes;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.data.DataMapProvider;

/** Writes the climate of each biome a mod declared, in real values, to the data map of the biomes' climates. */
public final class BiomeClimateProvider extends DataMapProvider {

    private final String modid;

    public BiomeClimateProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup,
                                final String modid) {
        super(output, lookup);
        this.modid = modid;
    }

    @Override
    public String getName() {
        return modid + ":biome_climates";
    }

    @Override
    protected void gather() {
        final Builder<BiomeClimate, Biome> climates = builder(BiomeClimates.CLIMATES);
        WorldBiomes.biomesOf(modid).forEach(biome -> climates.add(biome.key(), biome.climate(), false));
    }
}
