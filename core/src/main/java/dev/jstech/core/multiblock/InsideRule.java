/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * What may stand inside a structure and what it gives: the blocks allowed there that give nothing (air, for a tank),
 * and the parts that do (cells, moderators, coolers), each with its neighbour rules. When the structure forms, every
 * place inside is read: a block that is neither allowed nor a part keeps it from forming, and each part that works
 * adds its count and its numbers. Pure.
 *
 * <p>A part that needs another part beside it works only when that one works, so a chain of needs settles from the
 * parts that need nothing outwards: two coolers that each need the other never work.
 *
 * @param allows the blocks allowed inside that give nothing
 * @param parts  the parts, the first that fits a block naming it
 */
public record InsideRule(IBlockMatcher allows, List<InsidePart> parts) {

    public InsideRule {
        Objects.requireNonNull(allows, "allows");
        parts = List.copyOf(parts);
        final Set<String> names = new HashSet<>();
        for (final InsidePart part : parts) {
            if (!names.add(part.name())) {
                throw new IllegalArgumentException("two parts inside are called " + part.name());
            }
        }
    }

    /** An inside where only {@code allows} may stand, with no parts yet. */
    public static InsideRule of(final IBlockMatcher allows) {
        return new InsideRule(allows, List.of());
    }

    /** An inside that must be empty: air only, as a slot a data file can hold. */
    public static InsideRule empty() {
        return of(BlockMatch.blocks(Cells.AIR));
    }

    /** The same, with {@code part} too. */
    public InsideRule with(final InsidePart part) {
        final List<InsidePart> more = new ArrayList<>(this.parts);
        more.add(part);
        return new InsideRule(this.allows, more);
    }

    /**
     * The rule, once every part a need or a bonus names is one of its own: a shape takes its inside rule through
     * this, so a rule may be put together in any order and a misspelt part is still caught.
     *
     * @throws IllegalArgumentException when a need or a bonus names a part the rule does not have
     */
    public InsideRule requireKnownParts() {
        final Set<String> names = new HashSet<>();
        for (final InsidePart part : this.parts) {
            names.add(part.name());
        }
        for (final InsidePart part : this.parts) {
            for (final InsidePart.Need need : part.needs()) {
                requireKnown(names, part, need.of());
            }
            for (final InsidePart.Bonus bonus : part.bonuses()) {
                requireKnown(names, part, bonus.of());
            }
        }
        return this;
    }

    /** Whether the block {@code blockId} at {@code pos} may stand inside. */
    public boolean fits(final IBlockProvider provider, final long pos, final String blockId) {
        return partOf(provider, pos, blockId) != null || this.allows.matchesAt(provider, pos, blockId);
    }

    /**
     * Reads the places inside a structure: the first that holds a block that may not stand there, or none, with what
     * the working parts count and give added to {@code tally}.
     */
    @Nullable
    ShapeMatch.Failure read(final IBlockProvider provider, final List<Long> inside, final long controller,
                            final ShapeTally tally) {
        final Map<Long, String> ids = new HashMap<>();
        final Map<Long, InsidePart> partAt = new LinkedHashMap<>();
        for (final long pos : inside) {
            final String id = Cells.blockAt(provider, pos);
            ids.put(pos, id);
            final InsidePart part = partOf(provider, pos, id);
            if (part == null && !this.allows.matchesAt(provider, pos, id)) {
                return Cells.wrongBlock(controller, pos, ShapeMatch.INSIDE, id);
            }
            tally.fits();
            if (part != null) {
                partAt.put(pos, part);
            }
        }
        final Set<Long> working = working(provider, ids, partAt);
        for (final Map.Entry<Long, InsidePart> at : partAt.entrySet()) {
            if (!working.contains(at.getKey())) {
                continue;
            }
            final InsidePart part = at.getValue();
            tally.count(part.name(), 1);
            part.gives().forEach((key, each) -> tally.value(key, Place.Gather.SUM, each));
            for (final InsidePart.Bonus bonus : part.bonuses()) {
                final int beside = beside(provider, ids, partAt, working, at.getKey(), bonus.of());
                tally.value(bonus.key(), Place.Gather.SUM, bonus.each() * beside);
            }
        }
        return null;
    }

    /* The first part whose blocks the block is, or null. */
    @Nullable
    private InsidePart partOf(final IBlockProvider provider, final long pos, final String blockId) {
        for (final InsidePart part : this.parts) {
            if (part.which().matchesAt(provider, pos, blockId)) {
                return part;
            }
        }
        return null;
    }

    /*
     * The places whose part works: those that need nothing first, then each whose needs the working ones meet, until
     * none is added. A place is looked at again only when a neighbour starts working, so it settles in a few passes.
     */
    private Set<Long> working(final IBlockProvider provider, final Map<Long, String> ids,
                              final Map<Long, InsidePart> partAt) {
        final Set<Long> working = new HashSet<>();
        final Deque<Long> queue = new ArrayDeque<>(partAt.keySet());
        while (!queue.isEmpty()) {
            final long pos = queue.poll();
            if (working.contains(pos) || !needsMet(provider, ids, partAt, working, pos)) {
                continue;
            }
            working.add(pos);
            for (final long next : Cells.beside(pos)) {
                if (partAt.containsKey(next) && !working.contains(next)) {
                    queue.add(next);
                }
            }
        }
        return working;
    }

    private static boolean needsMet(final IBlockProvider provider, final Map<Long, String> ids,
                                    final Map<Long, InsidePart> partAt, final Set<Long> working, final long pos) {
        for (final InsidePart.Need need : partAt.get(pos).needs()) {
            if (beside(provider, ids, partAt, working, pos, need.of()) < need.least()) {
                return false;
            }
        }
        return true;
    }

    /* How many of the six places beside pos are the neighbour asked for. */
    private static int beside(final IBlockProvider provider, final Map<Long, String> ids,
                              final Map<Long, InsidePart> partAt, final Set<Long> working, final long pos,
                              final InsidePart.Neighbour of) {
        int count = 0;
        for (final long next : Cells.beside(pos)) {
            final boolean is = switch (of) {
                case InsidePart.Neighbour.Block block -> block.which().matchesAt(provider, next,
                        ids.computeIfAbsent(next, at -> Cells.blockAt(provider, at)));
                case InsidePart.Neighbour.Part part -> working.contains(next)
                        && partAt.get(next).name().equals(part.name());
            };
            if (is) {
                count++;
            }
        }
        return count;
    }

    private static void requireKnown(final Set<String> names, final InsidePart part,
                                     final InsidePart.Neighbour of) {
        if (of instanceof InsidePart.Neighbour.Part needed && !names.contains(needed.name())) {
            throw new IllegalArgumentException("the part " + part.name() + " names the part " + needed.name()
                    + ", which is not inside");
        }
    }
}
