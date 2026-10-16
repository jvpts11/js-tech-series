/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import dev.jstech.core.multiblock.ShapePreview;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The blocks a multiblock's places show on the player's side: the block a ghost is drawn as, its first block or the
 * first block of its first tag; every block a place takes, its tags' blocks with them; and what they are called.
 */
public final class GhostBlocks {

    private GhostBlocks() {
    }

    /** The block {@code ghost} is drawn as, or none for a place that names no block the game knows. */
    @Nullable
    public static BlockState stateOf(final ShapePreview.Ghost ghost) {
        final Block block = blockOf(ghost.first());
        return block == null ? null : block.defaultBlockState();
    }

    /** Every block {@code ghost} takes, its own and those of its tags, in their order, each once. */
    public static List<Block> choicesOf(final ShapePreview.Ghost ghost) {
        final Set<Block> out = new LinkedHashSet<>();
        for (final String id : ghost.blocks()) {
            final Block block = blockOf(id);
            if (block != null) {
                out.add(block);
            }
        }
        for (final String tag : ghost.tags()) {
            final ResourceLocation tagId = ResourceLocation.tryParse(tag);
            if (tagId != null) {
                BuiltInRegistries.BLOCK.getTag(TagKey.create(Registries.BLOCK, tagId))
                        .ifPresent(set -> set.forEach(holder -> out.add(holder.value())));
            }
        }
        return List.copyOf(out);
    }

    /** What {@code first} (a block id, or a tag as {@code #namespace:path}) is called. */
    public static Component nameOf(@Nullable final String first) {
        final Block block = blockOf(first);
        if (block != null && (first == null || !first.startsWith("#"))) {
            return block.getName();
        }
        return Component.literal(first == null ? "" : first);
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
}
