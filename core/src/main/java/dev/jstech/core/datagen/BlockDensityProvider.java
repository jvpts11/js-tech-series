/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.BlockDensity;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * The game's own blocks' density, in kilograms a cubic metre, near the real values of what each is made of: wool and
 * leaves hardly anything, wood light, earth and sand middling, stone and concrete heavy, metal blocks heavier, gold the
 * heaviest. A block it says nothing of is as dense as its material, or as rock.
 */
public final class BlockDensityProvider extends DataMapProvider {

    public BlockDensityProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":block_density";
    }

    @Override
    protected void gather() {
        final Builder<Double, Block> map = builder(BlockDensity.DENSITY);
        map.add(BlockTags.WOOL, 100.0, false);
        map.add(BlockTags.LEAVES, 100.0, false);
        map.add(Blocks.HAY_BLOCK.builtInRegistryHolder(), 150.0, false);
        map.add(Blocks.SNOW_BLOCK.builtInRegistryHolder(), 300.0, false);
        map.add(BlockTags.PLANKS, 600.0, false);
        map.add(BlockTags.LOGS, 700.0, false);
        map.add(BlockTags.ICE, 917.0, false);
        map.add(BlockTags.DIRT, 1_500.0, false);
        map.add(BlockTags.SAND, 1_600.0, false);
        map.add(Tags.Blocks.GRAVELS, 1_700.0, false);
        map.add(Blocks.CLAY.builtInRegistryHolder(), 1_800.0, false);
        map.add(Blocks.BRICKS.builtInRegistryHolder(), 1_900.0, false);
        map.add(BlockTags.TERRACOTTA, 1_900.0, false);
        map.add(Tags.Blocks.SANDSTONE_BLOCKS, 2_300.0, false);
        map.add(Tags.Blocks.CONCRETES, 2_400.0, false);
        map.add(Tags.Blocks.GLASS_BLOCKS, 2_500.0, false);
        map.add(Tags.Blocks.OBSIDIANS, 2_600.0, false);
        map.add(Tags.Blocks.STONES, 2_700.0, false);
        map.add(Tags.Blocks.COBBLESTONES, 2_700.0, false);
        map.add(Blocks.BASALT.builtInRegistryHolder(), 3_000.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_DIAMOND, 3_500.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_IRON, 7_874.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_COPPER, 8_960.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_NETHERITE, 10_000.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_GOLD, 19_300.0, false);
    }
}
