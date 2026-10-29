/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.platform;

import dev.jstech.core.multiblock.IBlockMatcher;
import dev.jstech.core.multiblock.IBlockProvider;
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.Limit;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.ShapeMatch;
import dev.jstech.core.multiblock.ShapePreview;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A landing platform's shape: a square slab of landing pads round its terminal, the largest of its patterns that
 * stands whole. Each pattern is read as the data in force says it, so a data pack may change a size. Pure.
 *
 * @param patterns the ids of its patterns, the largest first
 */
public record LandingPlatformShape(List<ResourceLocation> patterns) implements IStructureShape {

    public LandingPlatformShape {
        patterns = List.copyOf(patterns);
        if (patterns.isEmpty()) {
            throw new IllegalArgumentException("a landing platform has at least one pattern");
        }
    }

    /* The largest that forms; failing all, what the smallest found wrong, the one a player starting out builds. */
    @Override
    public ShapeMatch check(final IBlockProvider provider, final long terminal) {
        ShapeMatch last = null;
        for (final ResourceLocation id : this.patterns) {
            final Optional<MultiblockPattern> pattern = MultiblockPatterns.get(id);
            if (pattern.isEmpty()) {
                continue;
            }
            last = pattern.get().check(provider, terminal);
            if (last instanceof ShapeMatch.Formed) {
                return last;
            }
        }
        return Objects.requireNonNull(last, "no pattern of a landing platform is declared");
    }

    @Override
    public int reach() {
        return this.patterns.stream().map(MultiblockPatterns::get).flatMap(Optional::stream)
                .mapToInt(MultiblockPattern::reach).max().orElse(1);
    }

    @Override
    public Map<String, Limit> limits() {
        return Map.of();
    }

    @Override
    public @Nullable IBlockMatcher wanted(final String wanted) {
        return smallest().map(pattern -> pattern.wanted(wanted)).orElse(null);
    }

    @Override
    public Optional<ShapePreview> preview(final int grow) {
        return smallest().flatMap(pattern -> pattern.preview(grow));
    }

    private Optional<MultiblockPattern> smallest() {
        return MultiblockPatterns.get(this.patterns.getLast());
    }
}
