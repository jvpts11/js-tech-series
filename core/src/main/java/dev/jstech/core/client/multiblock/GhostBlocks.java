/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import dev.jstech.core.multiblock.MultiblockBlocks;
import dev.jstech.core.multiblock.ShapePreview;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The blocks a multiblock's places show on the player's side: the block a ghost is drawn as, its first block or the
 * first block of its first tag; every block a place takes, its tags' blocks with them; and what they are called. What
 * blocks a place names is {@link MultiblockBlocks}', read alike on either side.
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
        return MultiblockBlocks.choicesOf(ghost);
    }

    /** Every block of {@code blocks} the game knows and of {@code tags}, in their order, each once. */
    public static List<Block> choicesOf(final List<String> blocks, final List<String> tags) {
        return MultiblockBlocks.choicesOf(blocks, tags);
    }

    /** The blocks {@code first} (a block id, or a tag as {@code #namespace:path}) stands for: it, or the tag's. */
    public static List<Block> blocksOf(@Nullable final String first) {
        return MultiblockBlocks.blocksOf(first);
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
        return MultiblockBlocks.blockOf(first);
    }
}
