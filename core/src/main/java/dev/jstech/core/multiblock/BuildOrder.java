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
        return plan(wants, chosen, has, List.of(), Map.of());
    }

    /**
     * The same for a shape that limits counts ({@code quotas}), {@code standing} saying how many of each count already
     * stand in it, by the count's name: the least of each that is still short goes in first, each block in the first
     * places that take it, so the structure forms once built (its one power entry, though the player chose casing for
     * those places); and no place gets a block whose count would pass its most, the next block that fits it going
     * there instead.
     */
    public static BuildOrder plan(final List<Want> wants, final Map<String, String> chosen,
                                  final ToIntFunction<String> has, final List<Quota> quotas,
                                  final Map<String, Integer> standing) {
        final Planner planner = new Planner(has, quotas, standing);
        final Step[] steps = new Step[wants.size()];
        for (final Quota quota : quotas) {
            int missing = quota.limit().least() - planner.counted(quota);
            for (int i = 0; i < wants.size() && missing > 0; i++) {
                if (steps[i] != null) {
                    continue;
                }
                final List<String> counted = order(wants.get(i), chosen).stream()
                        .filter(block -> quota.counts().matches(block)).toList();
                if (!counted.isEmpty()) {
                    steps[i] = planner.place(wants.get(i), counted);
                    missing--;
                }
            }
        }
        for (int i = 0; i < wants.size(); i++) {
            if (steps[i] == null) {
                final List<String> order = order(wants.get(i), chosen);
                final List<String> room = order.stream().filter(planner::hasRoom).toList();
                steps[i] = planner.place(wants.get(i), room.isEmpty() ? order : room);
            }
        }
        return new BuildOrder(List.of(steps), planner.uses, planner.lacks);
    }

    /** Whether the source has every block it takes. */
    public boolean ready() {
        return this.lacks.isEmpty();
    }

    /* The blocks that fit {@code want}, the one chosen for its places first, then in the shape's order. */
    private static List<String> order(final Want want, final Map<String, String> chosen) {
        final List<String> order = new ArrayList<>();
        final String choice = chosen.get(want.key());
        if (choice != null && want.fits().contains(choice)) {
            order.add(choice);
        }
        want.fits().stream().filter(block -> !block.equals(choice)).forEach(order::add);
        return order;
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

    /* What a plan has given out so far: of each block, of each count, and what the source lacks. */
    private static final class Planner {

        private final ToIntFunction<String> has;
        private final List<Quota> quotas;
        private final Map<String, Integer> counted;
        private final Map<String, Integer> taken = new HashMap<>();
        private final Map<String, Integer> uses = new LinkedHashMap<>();
        private final Map<String, Integer> lacks = new LinkedHashMap<>();

        Planner(final ToIntFunction<String> has, final List<Quota> quotas, final Map<String, Integer> standing) {
            this.has = has;
            this.quotas = quotas;
            this.counted = new HashMap<>(standing);
        }

        /* How many of {@code quota}'s count stand or are placed so far. */
        int counted(final Quota quota) {
            return this.counted.getOrDefault(quota.name(), 0);
        }

        /* Whether one more {@code block} keeps every count it is counted under within its most. */
        boolean hasRoom(final String block) {
            for (final Quota quota : this.quotas) {
                if (quota.counts().matches(block) && counted(quota) >= quota.limit().most()) {
                    return false;
                }
            }
            return true;
        }

        /* Places the first of {@code order} the source still has at {@code want}, or the first, lacking. */
        Step place(final Want want, final List<String> order) {
            String block = null;
            for (final String candidate : order) {
                if (this.has.applyAsInt(candidate) - this.taken.getOrDefault(candidate, 0) > 0) {
                    block = candidate;
                    break;
                }
            }
            final boolean lacking = block == null;
            if (lacking) {
                block = order.getFirst();
                this.lacks.merge(block, 1, Integer::sum);
            }
            this.taken.merge(block, 1, Integer::sum);
            this.uses.merge(block, 1, Integer::sum);
            for (final Quota quota : this.quotas) {
                if (quota.counts().matches(block)) {
                    this.counted.merge(quota.name(), 1, Integer::sum);
                }
            }
            return new Step(want.x(), want.y(), want.z(), block, lacking);
        }
    }
}
