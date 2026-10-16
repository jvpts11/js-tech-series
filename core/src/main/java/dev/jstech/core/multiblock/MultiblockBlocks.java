/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The game's blocks a multiblock's places name, on either side: the block a place shows, its first block or its first
 * tag's first block; and every block a place takes, its tags' blocks with them.
 */
public final class MultiblockBlocks {

    private MultiblockBlocks() {
    }

    /** Every block {@code ghost} takes, its own and those of its tags, in their order, each once. */
    public static List<Block> choicesOf(final ShapePreview.Ghost ghost) {
        return choicesOf(ghost.blocks(), ghost.tags());
    }

    /** Every block of {@code blocks} the game knows and of {@code tags}, in their order, each once. */
    public static List<Block> choicesOf(final List<String> blocks, final List<String> tags) {
        final Set<Block> out = new LinkedHashSet<>();
        for (final String id : blocks) {
            final Block block = blockOf(id);
            if (block != null) {
                out.add(block);
            }
        }
        for (final String tag : tags) {
            final ResourceLocation tagId = ResourceLocation.tryParse(tag);
            if (tagId != null) {
                BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, tagId))
                        .ifPresent(set -> set.forEach(holder -> out.add(holder.value())));
            }
        }
        return List.copyOf(out);
    }

    /** The blocks {@code first} (a block id, or a tag as {@code #namespace:path}) stands for: it, or the tag's. */
    public static List<Block> blocksOf(@Nullable final String first) {
        if (first == null) {
            return List.of();
        }
        return first.startsWith("#") ? choicesOf(List.of(), List.of(first.substring(1)))
                : choicesOf(List.of(first), List.of());
    }

    /** The block {@code first} names, a tag's first block for a tag, or none. */
    @Nullable
    public static Block blockOf(@Nullable final String first) {
        if (first == null) {
            return null;
        }
        if (first.startsWith("#")) {
            final ResourceLocation tag = ResourceLocation.tryParse(first.substring(1));
            if (tag == null) {
                return null;
            }
            final Optional<Block> member = BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, tag))
                    .flatMap(set -> set.stream().findFirst()).map(Holder::value);
            return member.orElse(null);
        }
        final ResourceLocation id = ResourceLocation.tryParse(first);
        return id == null || !BuiltInRegistries.BLOCK.containsKey(id) ? null : BuiltInRegistries.BLOCK.get(id);
    }

    /** The id of {@code block}, as the patterns name it. */
    public static String idOf(final Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }
}
