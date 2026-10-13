/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen;

import dev.jstech.computers.datagen.advancement.JscAdvancementTabs;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The mod's advancements, one generator per tab: Hardware, Operating Systems, Networks and Operations, and Sigma.
 * The few that need another mod are written by {@code ConditionalAdvancementProvider}.
 */
public final class JscAdvancementProvider extends AdvancementProvider {

    public JscAdvancementProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> registries,
                                  final ExistingFileHelper existingFiles) {
        super(output, registries, existingFiles, List.<AdvancementGenerator>copyOf(JscAdvancementTabs.all()));
    }
}
