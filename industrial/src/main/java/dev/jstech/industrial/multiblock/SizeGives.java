/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.multiblock;

import dev.jstech.core.multiblock.ShapeMatch;

/**
 * What the size of a multiblock of J's Industrial gives, which each says of itself
 * (industrial/docs/design/machines.md): nothing for one of a fixed size, more recipes at once for each time its
 * pattern's slice stands, as a longer Coke Oven is a battery of more ovens, or the room inside it, as a tank's.
 */
public sealed interface SizeGives permits SizeGives.Nothing, SizeGives.RecipesPerSlice, SizeGives.BlocksInside {

    /** One of a fixed size, or whose size gives nothing: one recipe at a time. */
    SizeGives NOTHING = new Nothing();

    /** {@code recipes} more at once for each time its pattern's repeated slice stands. */
    static SizeGives recipesPerSlice(final int recipes) {
        return new RecipesPerSlice(recipes);
    }

    /** The room inside it, counted in blocks: what a tank holds, a block a cubic metre. */
    static SizeGives blocksInside() {
        return new BlocksInside();
    }

    /** How many recipes at once a multiblock that formed as {@code formed} runs; never fewer than one. */
    int recipesAtOnce(ShapeMatch.Formed formed);

    /** How many blocks of room a multiblock that formed as {@code formed} holds inside it; none if it holds none. */
    default long blocksInside(final ShapeMatch.Formed formed) {
        return 0L;
    }

    /** Nothing: one recipe at a time, whatever its size. */
    record Nothing() implements SizeGives {

        @Override
        public int recipesAtOnce(final ShapeMatch.Formed formed) {
            return 1;
        }
    }

    /** Room inside: as many blocks as stand inside its shell, as a tank's. */
    record BlocksInside() implements SizeGives {

        @Override
        public int recipesAtOnce(final ShapeMatch.Formed formed) {
            return 1;
        }

        @Override
        public long blocksInside(final ShapeMatch.Formed formed) {
            return formed.inside().size();
        }
    }

    /**
     * More recipes at once for each slice.
     *
     * @param recipes how many recipes at once each time its repeated slice stands gives
     */
    record RecipesPerSlice(int recipes) implements SizeGives {

        public RecipesPerSlice {
            if (recipes < 1) {
                throw new IllegalArgumentException("a slice gives one recipe at once or more; got " + recipes);
            }
        }

        @Override
        public int recipesAtOnce(final ShapeMatch.Formed formed) {
            return (int) Math.min(Integer.MAX_VALUE, (long) this.recipes * Math.max(1, formed.repeats()));
        }
    }
}
