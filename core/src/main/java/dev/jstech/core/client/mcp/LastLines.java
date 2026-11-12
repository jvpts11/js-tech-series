/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The last lines of something that keeps coming, as many as are kept, each counted as it comes: what came after a
 * moment is asked by the count at that moment, which the oldest lines dropping off the front never moves. Pure.
 *
 * @param <T> what a line is
 */
final class LastLines<T> {

    private final Deque<T> lines = new ArrayDeque<>();
    private final int kept;
    /** How many lines ever came, the dropped ones included. */
    private long added;

    LastLines(final int kept) {
        this.kept = kept;
    }

    /** Keeps a line that came, dropping the oldest past what is kept. */
    synchronized void add(final T line) {
        lines.addLast(line);
        added++;
        while (lines.size() > kept) {
            lines.removeFirst();
        }
    }

    /** The lines kept, the oldest first. */
    synchronized List<T> all() {
        return List.copyOf(lines);
    }

    /** How many lines have come so far: the moment to ask {@link #since} from. */
    synchronized long added() {
        return added;
    }

    /** The lines that came after the {@code count}th, as many of them as are still kept. */
    synchronized List<T> since(final long count) {
        final long dropped = added - lines.size();
        final long skip = Math.max(0L, count - dropped);
        final List<T> out = new ArrayList<>();
        long at = 0L;
        for (final T line : lines) {
            if (at++ >= skip) {
                out.add(line);
            }
        }
        return out;
    }
}
