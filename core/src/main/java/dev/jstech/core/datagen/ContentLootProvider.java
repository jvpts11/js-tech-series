/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ModContent;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/**
 * Writes the loot table of every block a mod declared, from what it was declared to drop. Every declared block gets
 * a table, even one that drops nothing, so none is left to the game's default.
 */
public final class ContentLootProvider extends LootTableProvider {

    public ContentLootProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries,
                               final ModContent content) {
        super(output, Set.of(),
                List.of(new SubProviderEntry(lookup -> new BlockLoot(lookup, content), LootContextParamSets.BLOCK)),
                registries);
    }

    private static final class BlockLoot extends BlockLootSubProvider {

        private final ModContent content;

        private BlockLoot(final HolderLookup.Provider registries, final ModContent content) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
            this.content = content;
        }

        @Override
        protected void generate() {
            for (final BlockEntry<?> entry : content.declaredBlocks()) {
                switch (entry.drops()) {
                    case SELF -> dropSelf(entry.get());
                    case NONE -> add(entry.get(), noDrop());
                }
            }
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return content.declaredBlocks().stream().<Block>map(BlockEntry::get).toList();
        }
    }
}
