/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What a manual marks for its reader beside what it lists: what is new since they last read it, an entry they have not
 * opened since it came into the book, and what they cannot do yet, an entry whose knowledge their team does not know;
 * and which colour a new bookmark's ribbon takes. Nothing is hidden by a mark: it only says. Pure, so a unit test
 * checks the rules.
 */
public final class ReaderMarks {

    private ReaderMarks() {
    }

    /**
     * The mark beside a section or a chapter holding {@code entries}: NEW when the reader has not seen one of them,
     * else NOT_YET when each of them needs knowledge {@code known} says the reader's team does not know, else none.
     */
    public static Mark of(final Collection<GuideEntry> entries, final Set<String> seen,
                          final Predicate<String> known) {
        if (entries.isEmpty()) {
            return Mark.NONE;
        }
        if (anyNew(entries, seen)) {
            return Mark.NEW;
        }
        return entries.stream().allMatch(entry -> !entry.needs().isEmpty() && !known.test(entry.needs()))
                ? Mark.NOT_YET : Mark.NONE;
    }

    /** Whether the reader has not seen one of {@code entries}: what puts a dot on a chapter's tab. */
    public static boolean anyNew(final Collection<GuideEntry> entries, final Set<String> seen) {
        return entries.stream().anyMatch(entry -> !seen.contains(entry.id()));
    }

    /**
     * The colour a new bookmark's ribbon takes, of {@code colours}: the one the fewest bookmarks already wear, the
     * first of those when several are as few, so the ribbons on a book differ for as long as there are colours.
     */
    public static int nextColour(final List<Integer> worn, final int colours) {
        int best = 0;
        long fewest = Long.MAX_VALUE;
        for (int colour = 0; colour < colours; colour++) {
            final int each = colour;
            final long wearing = worn.stream().filter(one -> one == each).count();
            if (wearing < fewest) {
                fewest = wearing;
                best = colour;
            }
        }
        return best;
    }

    /** A mark beside what a manual lists. */
    public enum Mark {
        /** Nothing to say. */
        NONE,
        /** Something the reader has not opened since it came into the book. */
        NEW,
        /** Something the reader's team does not know enough to do yet. */
        NOT_YET
    }
}
