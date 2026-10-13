/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.FluidEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.fluid.CoreFluidTags;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the tags a mod's declared fluids carry: the Core's marks a pipe has to be made for (a gas, a corrosive fluid)
 * and the common tag of gases other mods read.
 */
public final class ContentFluidTagsProvider extends FluidTagsProvider {

    private final ModContent content;

    public ContentFluidTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup,
                                    final ModContent content, final ExistingFileHelper existingFiles) {
        super(output, lookup, content.modid(), existingFiles);
        this.content = content;
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        for (final FluidEntry fluid : this.content.declaredFluids()) {
            if (fluid.gas()) {
                tag(CoreFluidTags.GASES).add(fluid.source(), fluid.flowing());
                tag(Tags.Fluids.GASEOUS).add(fluid.source(), fluid.flowing());
            }
            if (fluid.corrosive()) {
                tag(CoreFluidTags.CORROSIVE).add(fluid.source(), fluid.flowing());
            }
        }
    }
}
