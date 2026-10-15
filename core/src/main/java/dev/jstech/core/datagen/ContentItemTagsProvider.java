/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the item tags a mod's creative tabs name: each item declared into a section goes into the section's tags and
 * into its tab's. The tabs already sort every item the way a player browses them, so the tags read the same sorting
 * rather than keeping a list of their own. An item declared in a tag of its own, a common tag other mods know it by,
 * goes into it too.
 */
public final class ContentItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {

    private final ModContent content;

    public ContentItemTagsProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup,
                                   final ModContent content, final ExistingFileHelper existingFiles) {
        super(output, Registries.ITEM, lookup, item -> item.builtInRegistryHolder().key(), content.modid(),
                existingFiles);
        this.content = content;
    }

    /**
     * Whether any tab of the mod, any of their sections, or any item it declared names a tag: only then has this
     * anything to write.
     */
    public static boolean hasTags(final ModContent content) {
        if (content.declaredItems().stream().anyMatch(item -> !item.declaredTags().isEmpty())) {
            return true;
        }
        for (final ContentTab tab : content.declaredTabs()) {
            if (!tab.tags().isEmpty() || tab.sections().stream().anyMatch(section -> !section.tags().isEmpty())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void addTags(final HolderLookup.Provider provider) {
        for (final ItemEntry<?> item : content.declaredItems()) {
            for (final TagKey<Item> tag : item.declaredTags()) {
                tag(tag).add(item.get());
            }
        }
        for (final ContentTab tab : content.declaredTabs()) {
            for (final ContentTab.Section section : tab.sections()) {
                for (final Item item : section.items()) {
                    for (final TagKey<Item> tag : tab.tags()) {
                        tag(tag).add(item);
                    }
                    for (final TagKey<Item> tag : section.tags()) {
                        tag(tag).add(item);
                    }
                }
            }
        }
    }
}
