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
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the block tags a mod's declared blocks belong to, such as the one telling the game which tool mines them.
 */
public final class ContentBlockTagsProvider extends BlockTagsProvider {

    private final ModContent content;

    public ContentBlockTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup,
                                    final ModContent content, final ExistingFileHelper existingFiles) {
        super(output, lookup, content.modid(), existingFiles);
        this.content = content;
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        for (final BlockEntry<?> entry : content.declaredBlocks()) {
            for (final TagKey<Block> tag : entry.declaredTags()) {
                tag(tag).add(entry.get());
            }
        }
    }
}
