/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire;

import dev.jstech.core.JsCore;
import dev.jstech.core.fire.Fires;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * What the game's own blocks leave when they burn away: logs char, planks char, and cloth, leaves, thin wood, hay and
 * books burn to ash. A block it says nothing of leaves nothing, as the game's fire always did.
 */
public final class FireRemainsProvider extends DataMapProvider {

    /* The thin wood and the soft things that burn to ash. */
    private static final List<TagKey<Block>> TO_ASH = List.of(BlockTags.WOODEN_STAIRS, BlockTags.WOODEN_SLABS,
            BlockTags.WOODEN_FENCES, BlockTags.FENCE_GATES, BlockTags.WOODEN_DOORS, BlockTags.WOODEN_TRAPDOORS,
            BlockTags.WOOL, BlockTags.WOOL_CARPETS, BlockTags.LEAVES);
    private static final List<Block> ALSO_TO_ASH = List.of(Blocks.HAY_BLOCK, Blocks.BOOKSHELF, Blocks.SCAFFOLDING,
            Blocks.DRIED_KELP_BLOCK);

    public FireRemainsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":burns_into";
    }

    @Override
    protected void gather() {
        final Builder<Block, Block> map = builder(Fires.BURNS_INTO);
        map.add(BlockTags.LOGS_THAT_BURN, CharredBlocks.CHARRED_LOG.get(), false);
        map.add(BlockTags.PLANKS, CharredBlocks.CHARRED_PLANKS.get(), false);
        for (final TagKey<Block> tag : TO_ASH) {
            map.add(tag, CharredBlocks.ASH.get(), false);
        }
        for (final Block block : ALSO_TO_ASH) {
            map.add(block.builtInRegistryHolder(), CharredBlocks.ASH.get(), false);
        }
    }
}
