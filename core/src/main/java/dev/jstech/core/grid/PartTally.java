/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What each block a part of a routed line plugs into gave and took in the last tick, kept as the line moves it, so an
 * instrument can read it: a line of items, liquids or gases keeps one for each part. Pure.
 */
public final class PartTally {

    private final Map<Long, long[]> blocks = new LinkedHashMap<>();
    private final int cables;
    private final Optional<LineSurvey.Narrowest> narrowest;
    private long moved;

    private static final int GAVE = 0;
    private static final int TOOK = 1;

    /**
     * A tally for a part of {@code cables} cables whose narrowest is {@code narrowest}, which plugs into the blocks at
     * {@code blocks}.
     */
    public PartTally(final int cables, final Optional<LineSurvey.Narrowest> narrowest, final Iterable<Long> blocks) {
        this.cables = cables;
        this.narrowest = narrowest;
        for (final long block : blocks) {
            this.blocks.putIfAbsent(block, new long[2]);
        }
    }

    /** Forgets the last tick, as a new one starts. */
    public void reset() {
        for (final long[] counts : this.blocks.values()) {
            counts[GAVE] = 0L;
            counts[TOOK] = 0L;
        }
        this.moved = 0L;
    }

    /** {@code amount} went from the block at {@code from} to the block at {@code to}; hands {@code amount} back. */
    public long moved(final long from, final long to, final long amount) {
        if (amount <= 0) {
            return amount;
        }
        this.blocks.computeIfAbsent(from, block -> new long[2])[GAVE] += amount;
        this.blocks.computeIfAbsent(to, block -> new long[2])[TOOK] += amount;
        this.moved += amount;
        return amount;
    }

    /** What the part did in the last tick; a routed line cannot tell what was asked of it, and loses nothing. */
    public LineSurvey survey() {
        final List<LineSurvey.Member> members = new ArrayList<>(this.blocks.size());
        for (final Map.Entry<Long, long[]> block : this.blocks.entrySet()) {
            final long gave = block.getValue()[GAVE];
            final long took = block.getValue()[TOOK];
            members.add(new LineSurvey.Member(block.getKey(), LineSurvey.Role.of(gave > 0, took > 0), gave, took));
        }
        return new LineSurvey(members, this.moved, LineSurvey.UNKNOWN, 0L, this.cables, this.narrowest);
    }
}
