/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.CopyProgressPayload;
import dev.jstech.computers.os.fs.CopyTiming;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's run of copies as a copy window shows it at one moment: the file going now, how many there are and how
 * many are left, how much of the run has gone, how long is left, and how fast it goes.
 *
 * @param current     the copy going now, or the last one when every copy has gone
 * @param items       how many files the run copies
 * @param itemsLeft   how many it has still to finish
 * @param mbTotal     how much the run weighs, in megabytes
 * @param mbDone      how much of that has gone
 * @param fraction    how far through the run it is, from 0 to 1
 * @param secondsLeft how many seconds are left, rounded up
 * @param running     how long the run has been going, in ticks
 */
record CopyRun(CopyProgressPayload current, int items, int itemsLeft, double mbTotal, double mbDone,
               double fraction, int secondsLeft, double running) {

    /** The run as it stands at the game time {@code now}, or null when it is empty. */
    @Nullable
    static CopyRun of(final List<CopyProgressPayload> run, final double now) {
        if (run.isEmpty()) {
            return null;
        }
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        double total = 0.0;
        double done = 0.0;
        int left = 0;
        CopyProgressPayload current = null;
        for (final CopyProgressPayload copy : run) {
            start = Math.min(start, copy.startTick());
            end = Math.max(end, copy.endTick());
            total += copy.sizeMb();
            final double span = Math.max(1L, copy.endTick() - copy.startTick());
            final double along = copy.done() ? 1.0 : Math.max(0.0, Math.min(1.0, (now - copy.startTick()) / span));
            done += copy.sizeMb() * along;
            if (!copy.done()) {
                left++;
                if (current == null && now < copy.endTick()) {
                    current = copy;
                }
            }
        }
        if (current == null) {
            current = run.get(run.size() - 1);
        }
        final double whole = Math.max(1L, end - start);
        final double fraction = Math.max(0.0, Math.min(1.0, (now - start) / whole));
        final int seconds = (int) Math.max(0L, (long) Math.ceil((end - now) / 20.0));
        return new CopyRun(current, run.size(), left, total, done, fraction, seconds, now - start);
    }

    /** Whether the run has gone on long enough for its window to come up: past the moment the old systems waited. */
    boolean showsWindow() {
        return itemsLeft > 0 && running > CopyTiming.MOMENT_TICKS;
    }

    /** Whether the run is a deletion, the paper flying into the Recycle Bin. */
    boolean deleting() {
        return current.kind() == CopyProgressPayload.DELETE;
    }

    /** How fast it goes, in megabytes a second. */
    double rate() {
        return current.mbPerSecond();
    }
}
