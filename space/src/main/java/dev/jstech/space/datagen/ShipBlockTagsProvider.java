/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.datagen;

import dev.jstech.core.multiblock.plan.IPlanMember;
import dev.jstech.space.JsSpace;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * The vanilla blocks a starship's kits want where they work aboard, the workshop's crafting table, furnace and smithing
 * table and the holds' barrels, keeping themselves in a ship's plan rather than merging into its parts' models.
 */
public final class ShipBlockTagsProvider extends BlockTagsProvider {

    public ShipBlockTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup,
                                 final ExistingFileHelper files) {
        super(output, lookup, JsSpace.MODID, files);
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        tag(IPlanMember.KEEPS_ITSELF).add(Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.SMITHING_TABLE,
                Blocks.BARREL);
    }

    @Override
    public String getName() {
        return JsSpace.MODID + ":ship_block_tags";
    }
}
