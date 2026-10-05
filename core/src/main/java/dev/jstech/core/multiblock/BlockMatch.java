/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.List;

/**
 * A slot of a pattern that is data rather than code: any of these blocks, by id, or any block in one of these tags.
 * A pattern made only of these can be written to a data file and read back, which is what a datapack overriding a
 * structure needs.
 *
 * @param blocks the blocks that fit, by id such as {@code minecraft:iron_block}
 * @param tags   the block tags whose blocks fit, by id such as {@code minecraft:logs}
 */
public record BlockMatch(List<String> blocks, List<String> tags) implements IBlockMatcher {

    public BlockMatch {
        blocks = List.copyOf(blocks);
        tags = List.copyOf(tags);
        if (blocks.isEmpty() && tags.isEmpty()) {
            throw new IllegalArgumentException("a slot names at least one block or tag");
        }
    }

    /** Any of these blocks. */
    public static BlockMatch blocks(final String... ids) {
        return new BlockMatch(List.of(ids), List.of());
    }

    /** Any block of this tag. */
    public static BlockMatch tag(final String tag) {
        return new BlockMatch(List.of(), List.of(tag));
    }

    /** Whether the block is one of those named; a tag needs the world, through {@link #matchesAt}. */
    @Override
    public boolean matches(final String blockId) {
        return blocks.contains(blockId);
    }

    @Override
    public boolean matchesAt(final IBlockProvider provider, final long encodedPos, final String blockId) {
        if (blocks.contains(blockId)) {
            return true;
        }
        for (final String tag : tags) {
            if (provider.hasTag(encodedPos, tag)) {
                return true;
            }
        }
        return false;
    }
}
