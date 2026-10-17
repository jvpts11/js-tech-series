/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.explosion.BlastStrength;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * The overpressure the game's own blocks stand before a blast breaks them, in kilopascals, near what the real things
 * stand: a window gives at a few, a wooden door at twenty, a wall of planks near a hundred, brick and stone a few
 * hundred, concrete more, obsidian and metal blocks far more. A block it says nothing of stands what its resistance to
 * the game's explosions says.
 */
public final class BlastStrengthProvider extends DataMapProvider {

    public BlastStrengthProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":blast_strength";
    }

    @Override
    protected void gather() {
        final Builder<Double, Block> map = builder(BlastStrength.STRENGTH);
        map.add(Tags.Blocks.GLASS_PANES, 2.0, false);
        map.add(Tags.Blocks.GLASS_BLOCKS, 4.0, false);
        map.add(BlockTags.WOODEN_DOORS, 20.0, false);
        map.add(BlockTags.WOODEN_TRAPDOORS, 20.0, false);
        map.add(BlockTags.WOOL, 20.0, false);
        map.add(BlockTags.ICE, 30.0, false);
        map.add(BlockTags.SAND, 40.0, false);
        map.add(Tags.Blocks.GRAVELS, 50.0, false);
        map.add(BlockTags.DIRT, 60.0, false);
        map.add(BlockTags.PLANKS, 80.0, false);
        map.add(BlockTags.LOGS, 120.0, false);
        map.add(Tags.Blocks.COBBLESTONES, 150.0, false);
        map.add(Blocks.BRICKS.builtInRegistryHolder(), 150.0, false);
        map.add(BlockTags.TERRACOTTA, 150.0, false);
        map.add(Tags.Blocks.SANDSTONE_BLOCKS, 150.0, false);
        map.add(Tags.Blocks.STONES, 200.0, false);
        map.add(Blocks.STONE_BRICKS.builtInRegistryHolder(), 250.0, false);
        map.add(Blocks.IRON_DOOR.builtInRegistryHolder(), 300.0, false);
        map.add(Tags.Blocks.CONCRETES, 700.0, false);
        map.add(Tags.Blocks.STORAGE_BLOCKS_IRON, 5_000.0, false);
        map.add(Tags.Blocks.OBSIDIANS, 20_000.0, false);
    }
}
