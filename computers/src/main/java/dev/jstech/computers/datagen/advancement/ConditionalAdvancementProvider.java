/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.WithConditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Writes the advancements that exist only beside another mod, each carrying the condition that it be loaded.
 *
 * <p>The game reads such a condition off any advancement file, but the stock advancement provider writes files
 * without one, so these are written here instead, the same way and to the same place, with it.
 */
public final class ConditionalAdvancementProvider implements DataProvider {

    private final PackOutput.PathProvider paths;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ConditionalAdvancementProvider(final PackOutput output,
                                          final CompletableFuture<HolderLookup.Provider> registries) {
        this.paths = output.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput output) {
        return this.registries.thenCompose(lookup -> {
            final List<CompletableFuture<?>> written = new ArrayList<>();
            for (final AdvancementTab tab : JscAdvancementTabs.all()) {
                tab.generateConditional((holder, mod) -> written.add(DataProvider.saveStable(output, lookup,
                        Advancement.CONDITIONAL_CODEC,
                        Optional.of(new WithConditions<>(holder.value(), new ModLoadedCondition(mod))),
                        this.paths.json(holder.id()))));
            }
            return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return "Advancements that need another mod";
    }
}
