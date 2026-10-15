/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.id.IStableName;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * A place of a structure, with its rule: what fits there; the blocks there that are counted, under a name the
 * multiblock limits ("from one to four energy ports"); and the number a block there gives the formed machine, as the
 * coils of a blast furnace give its highest temperature, gathered over every such place as the multiblock says (the
 * lowest, the highest, the sum, the mean). Pure.
 *
 * @param fits    what fits there
 * @param counted the blocks there that are counted, and under what name; none for a place that counts nothing
 * @param valued  the number a block there gives, and under what key; none for a place that gives nothing
 */
public record Place(IBlockMatcher fits, @Nullable Counted counted, @Nullable Valued valued) {

    public Place {
        Objects.requireNonNull(fits, "fits");
    }

    /** A place where {@code fits} fits, counting and giving nothing. */
    public static Place of(final IBlockMatcher fits) {
        return new Place(fits, null, null);
    }

    /** The same, counting the blocks there that {@code which} names under {@code name}. */
    public Place counting(final String name, final IBlockMatcher which) {
        return new Place(this.fits, new Counted(name, which), this.valued);
    }

    /**
     * The same, each block there giving the number {@code byBlock} names it by (a block id, or a tag as
     * {@code #namespace:path}) under {@code key}, gathered as {@code how} says.
     */
    public Place valued(final String key, final Map<String, Double> byBlock, final Gather how) {
        return new Place(this.fits, this.counted, new Valued(key, byBlock, how));
    }

    /**
     * The blocks of a place that are counted.
     *
     * @param name  what the count is called, which the multiblock's limits name
     * @param which the blocks that are counted
     */
    public record Counted(String name, IBlockMatcher which) {

        public Counted {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(which, "which");
        }
    }

    /**
     * The number a block of a place gives.
     *
     * @param key     what the number is called
     * @param byBlock the number of each block, by its id or by a tag as {@code #namespace:path}
     * @param how     how the numbers of every such place are gathered into one
     */
    public record Valued(String key, Map<String, Double> byBlock, Gather how) {

        public Valued {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(how, "how");
            byBlock = Map.copyOf(byBlock);
            if (byBlock.isEmpty()) {
                throw new IllegalArgumentException("a valued place gives a number for at least one block");
            }
        }

        /** The number the block {@code blockId} at {@code pos} gives, or none for a block it names no number for. */
        public @Nullable Double of(final IBlockProvider provider, final long pos, final String blockId) {
            final Double own = this.byBlock.get(blockId);
            if (own != null) {
                return own;
            }
            for (final Map.Entry<String, Double> entry : this.byBlock.entrySet()) {
                if (entry.getKey().startsWith("#") && provider.hasTag(pos, entry.getKey().substring(1))) {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    /** How the numbers of a key are gathered into one. */
    public enum Gather implements IStableName {
        /** The lowest: a blast furnace is as hot as its coolest coil. */
        LEAST("least"),
        /** The highest. */
        MOST("most"),
        /** Their sum. */
        SUM("sum"),
        /** Their mean. */
        MEAN("mean");

        private final String serializedName;

        Gather(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
