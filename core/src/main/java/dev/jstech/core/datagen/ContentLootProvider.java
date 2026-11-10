/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.ModContent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/**
 * Writes the loot table of every block a mod declared, from what it was declared to drop. Every declared block gets
 * a table, even one that drops nothing, so none is left to the game's default; but one whose table is written by hand,
 * which the data run's check still asks for.
 */
public final class ContentLootProvider extends LootTableProvider {

    public ContentLootProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries,
                               final ModContent content) {
        this(output, registries, new ArrayList<>(List.of(blocks(content))));
    }

    /**
     * Writes the tables of {@code tables}, the declared blocks' among them ({@link #blocks}): a list the mod may still
     * add to until the data run writes, such as the tables of its structures' chests.
     */
    public ContentLootProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries,
                               final List<SubProviderEntry> tables) {
        super(output, Set.of(), tables, registries);
    }

    /** The tables of the blocks {@code content} declared. */
    public static SubProviderEntry blocks(final ModContent content) {
        return new SubProviderEntry(lookup -> new BlockLoot(lookup, content), LootContextParamSets.BLOCK);
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
                    case SELF_WITH_CONTENTS -> add(entry.get(), this::selfWithContents);
                    case NONE -> add(entry.get(), noDrop());
                    case HANDMADE -> {
                        // Its table is the mod's own, in its data.
                    }
                }
            }
        }

        /* The block's item, with every component its block entity writes into it, as a shulker box keeps its items. */
        private LootTable.Builder selfWithContents(final Block block) {
            final LootPool.Builder pool = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(block).apply(
                            CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)));
            return LootTable.lootTable().withPool(applyExplosionCondition(block, pool));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return content.declaredBlocks().stream().filter(entry -> entry.drops() != Drops.HANDMADE)
                    .<Block>map(BlockEntry::get).toList();
        }
    }
}
