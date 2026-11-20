/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import dev.jstech.core.grid.EndRouter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Whose turn it is among the ends of each part of a line ({@link EndRouter}): how many times the part has moved
 * something. It is kept by the part's own ends, so it carries on when the grid works its parts out again, as it does
 * whenever a line anywhere in the level changes, and a part that keeps its ends keeps its turn. Pure: no game types.
 */
public final class EndTurns {

    private final Map<Long, Long> turns = new HashMap<>();

    /** The turn of the part whose ends are {@code ends}. */
    public long of(final List<? extends EndRouter.IEnd<?>> ends) {
        return this.turns.getOrDefault(partOf(ends), 0L);
    }

    /** Counts a time the part whose ends are {@code ends} moved something: the next of its ends is served first. */
    public void moved(final List<? extends EndRouter.IEnd<?>> ends) {
        this.turns.merge(partOf(ends), 1L, Long::sum);
    }

    /** Forgets every part but those whose ends are among {@code standing}, once the grid's parts are worked out. */
    public void keep(final Collection<? extends List<? extends EndRouter.IEnd<?>>> standing) {
        final Set<Long> kept = new HashSet<>();
        for (final List<? extends EndRouter.IEnd<?>> ends : standing) {
            kept.add(partOf(ends));
        }
        this.turns.keySet().retainAll(kept);
    }

    /* A part, by the least of its ends' blocks: the same part for as long as it keeps that end. */
    private static long partOf(final List<? extends EndRouter.IEnd<?>> ends) {
        long least = Long.MAX_VALUE;
        for (final EndRouter.IEnd<?> end : ends) {
            least = Math.min(least, end.key());
        }
        return least;
    }
}
