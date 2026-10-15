/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.heat.BlockConductivity;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * The game's own blocks' thermal conductivity, in watts a metre a kelvin, near the real values of what each is made
 * of: wool and straw insulate, snow and wood hardly conduct, ice and stone do a little, metals carry heat away. A
 * block it says nothing of conducts as stone.
 */
public final class BlockConductivityProvider extends DataMapProvider {

    public BlockConductivityProvider(final PackOutput output,
                                     final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void gather() {
        final Builder<Double, Block> map = builder(BlockConductivity.CONDUCTIVITY);
        map.add(BlockTags.WOOL, 0.04, false);
        map.add(Blocks.HAY_BLOCK.builtInRegistryHolder(), 0.07, false);
        map.add(Blocks.SNOW_BLOCK.builtInRegistryHolder(), 0.16, false);
        map.add(BlockTags.PLANKS, 0.15, false);
        map.add(BlockTags.LOGS, 0.15, false);
        map.add(BlockTags.SAND, 0.3, false);
        map.add(BlockTags.DIRT, 0.5, false);
        map.add(Tags.Blocks.GRAVELS, 0.7, false);
        map.add(Blocks.BRICKS.builtInRegistryHolder(), 0.7, false);
        map.add(BlockTags.TERRACOTTA, 0.8, false);
        map.add(Tags.Blocks.GLASS_BLOCKS, 1.0, false);
        map.add(Blocks.CLAY.builtInRegistryHolder(), 1.3, false);
        map.add(Tags.Blocks.OBSIDIANS, 1.4, false);
        map.add(Tags.Blocks.CONCRETES, 1.4, false);
        map.add(Tags.Blocks.SANDSTONE_BLOCKS, 1.7, false);
        map.add(Blocks.BASALT.builtInRegistryHolder(), 2.0, false);
        map.add(BlockTags.ICE, 2.2, false);
        map.add(Blocks.GRANITE.builtInRegistryHolder(), 2.8, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_IRON, 80.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_GOLD, 318.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_COPPER, 401.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_DIAMOND, 2_200.0, false);
    }
}
