/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.room.Seal;
import dev.jstech.core.room.Seals;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * How the game's own blocks seal a room, where their shape alone would say otherwise: glass leaks a little and stands
 * little, its panes less; leaves and copper grates let air through; doors, trapdoors and gates leak a little while shut
 * and let everything through while open, the iron ones standing more. A whole solid block it says nothing of does not
 * leak and stands as hard as it is to blow up.
 */
public final class SealProvider extends DataMapProvider {

    private static final List<Block> COPPER_GRATES = List.of(Blocks.COPPER_GRATE, Blocks.EXPOSED_COPPER_GRATE,
            Blocks.WEATHERED_COPPER_GRATE, Blocks.OXIDIZED_COPPER_GRATE, Blocks.WAXED_COPPER_GRATE,
            Blocks.WAXED_EXPOSED_COPPER_GRATE, Blocks.WAXED_WEATHERED_COPPER_GRATE,
            Blocks.WAXED_OXIDIZED_COPPER_GRATE);

    public SealProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":seals";
    }

    @Override
    protected void gather() {
        final Builder<Seal, Block> map = builder(Seals.SEALS);
        map.add(Tags.Blocks.GLASS_BLOCKS, Seal.wall(0.0005, 120), false);
        map.add(Tags.Blocks.GLASS_PANES, Seal.wall(0.001, 60), false);
        map.add(BlockTags.LEAVES, Seal.wall(0.5, 5), false);
        for (final Block grate : COPPER_GRATES) {
            map.add(grate.builtInRegistryHolder(), Seal.wall(0.25, 50), false);
        }
        map.add(BlockTags.DOORS, Seal.door(0.002, 60), false);
        map.add(BlockTags.TRAPDOORS, Seal.door(0.002, 60), false);
        map.add(BlockTags.FENCE_GATES, Seal.door(0.05, 30), false);
        map.add(Blocks.IRON_DOOR.builtInRegistryHolder(), Seal.door(0.001, 400), false);
        map.add(Blocks.IRON_TRAPDOOR.builtInRegistryHolder(), Seal.door(0.001, 400), false);
    }
}
