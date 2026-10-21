/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The last lines each log wrote, kept for the studio: a few hundred a log, the oldest let go as new ones come. Safe to
 * write from any thread.
 */
public final class LogRing {

    private final Map<String, Deque<Line>> byLog = new ConcurrentHashMap<>();
    private final int kept;

    /** A ring that keeps {@code kept} lines of each log. */
    public LogRing(final int kept) {
        if (kept < 1) {
            throw new IllegalArgumentException("a ring keeps a line at least; got " + kept);
        }
        this.kept = kept;
    }

    /** Keeps {@code line}, written by the log {@code log}. */
    public void add(final String log, final Line line) {
        final Deque<Line> lines = this.byLog.computeIfAbsent(log, any -> new ArrayDeque<>());
        synchronized (lines) {
            lines.addLast(line);
            while (lines.size() > this.kept) {
                lines.removeFirst();
            }
        }
    }

    /** The lines {@code log} wrote, the oldest first; none for a log that wrote none. */
    public List<Line> lines(final String log) {
        final Deque<Line> lines = this.byLog.get(log);
        if (lines == null) {
            return List.of();
        }
        synchronized (lines) {
            return List.copyOf(lines);
        }
    }

    /**
     * A line a log wrote.
     *
     * @param millis  when, in milliseconds since the epoch
     * @param level   its level, in lower case
     * @param message what it says
     */
    public record Line(long millis, String level, String message) {
    }
}
