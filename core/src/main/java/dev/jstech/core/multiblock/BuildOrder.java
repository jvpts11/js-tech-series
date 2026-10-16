/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;

/**
 * What building a multiblock's missing places takes from a source of blocks, worked out before a block is placed: the
 * block for each place, the one the player chose for a place that takes several first and then the first the source
 * has; how many of each block it uses; and what the source lacks. Pure, so the tool that builds and its screen work it
 * out alike.
 *
 * @param steps  each place still missing and its block, the bottom layer first, as they are to be placed
 * @param uses   how many of each block it places, by block id, in the order first met
 * @param lacks  how many of each block the source does not have, by block id; empty when it has them all
 */
public record BuildOrder(List<Step> steps, Map<String, Integer> uses, Map<String, Integer> lacks) {

    public BuildOrder {
        steps = List.copyOf(steps);
        uses = Map.copyOf(uses);
        lacks = Map.copyOf(lacks);
    }

    /**
     * Plans {@code wants}, each a place and the blocks that fit it, in order: a place whose blocks have a choice in
     * {@code chosen} (keyed by {@link Want#key()}) takes the chosen block first; otherwise, and when the source has
     * none of the chosen left, the first block that fits which {@code has} says the source still holds; and when it
     * holds none that fits, the chosen or first block, counted as lacking.
     */
    public static BuildOrder plan(final List<Want> wants, final Map<String, String> chosen,
                                  final ToIntFunction<String> has) {
        final List<Step> steps = new ArrayList<>();
        final Map<String, Integer> uses = new LinkedHashMap<>();
        final Map<String, Integer> lacks = new LinkedHashMap<>();
        final Map<String, Integer> taken = new HashMap<>();
        for (final Want want : wants) {
            final List<String> order = new ArrayList<>();
            final String choice = chosen.get(want.key());
            if (choice != null && want.fits().contains(choice)) {
                order.add(choice);
            }
            want.fits().stream().filter(block -> !block.equals(choice)).forEach(order::add);
            String block = null;
            for (final String candidate : order) {
                if (has.applyAsInt(candidate) - taken.getOrDefault(candidate, 0) > 0) {
                    block = candidate;
                    break;
                }
            }
            final boolean lacking = block == null;
            if (lacking) {
                block = order.getFirst();
                lacks.merge(block, 1, Integer::sum);
            }
            taken.merge(block, 1, Integer::sum);
            uses.merge(block, 1, Integer::sum);
            steps.add(new Step(want.x(), want.y(), want.z(), block, lacking));
        }
        return new BuildOrder(steps, uses, lacks);
    }

    /** Whether the source has every block it takes. */
    public boolean ready() {
        return this.lacks.isEmpty();
    }

    /**
     * A place still to be filled, and the blocks that fit it.
     *
     * @param x    where it is, along x
     * @param y    along y
     * @param z    along z
     * @param fits the ids of the blocks that fit it, in the order the shape names them; never empty
     */
    public record Want(int x, int y, int z, List<String> fits) {

        public Want {
            fits = List.copyOf(fits);
            if (fits.isEmpty()) {
                throw new IllegalArgumentException("a place to fill takes a block at least");
            }
        }

        /** What its choice is kept under: the blocks that fit it, so places that take the same share one choice. */
        public String key() {
            return String.join(",", this.fits);
        }
    }

    /**
     * A place and the block to put there.
     *
     * @param x       where it is, along x
     * @param y       along y
     * @param z       along z
     * @param block   the id of the block to put there
     * @param lacking whether the source has none of it to give
     */
    public record Step(int x, int y, int z, String block, boolean lacking) {

        public Step {
            Objects.requireNonNull(block, "block");
        }
    }
}
