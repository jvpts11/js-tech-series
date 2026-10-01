/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.schedule;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * Hands one tick's worth of work out to the tasks that want it, so a block that runs many things (a computer running
 * its programs, a machine working its queue) shares its tick fairly and never holds the server past a deadline.
 *
 * <p>The tick is worth so many units of work, the credits, and may last so long by the clock, the deadline. The
 * credits are dealt in quanta, going round the tasks in turn so that no task finishes its share before another has
 * begun; a task that stops early (it finished, or waits on something) gives up its place for the rest of the tick and
 * the others take what it left. The deadline is looked at between quanta, once a block of work has run since the last
 * look, or at once after a task reached outside itself: when it has passed, the tick ends where it stands and the
 * next one starts with the first task that went without, so a block always short of time is short for each task in
 * turn rather than always for the same one.
 *
 * <p>Nothing here knows what a task is. It is given things that can be stepped and told how far each got, which is
 * what lets the whole of it be run against nothing at all:
 *
 * <pre>{@code
 * private final TickScheduler scheduler = new TickScheduler();
 *
 * void tick() {
 *     long deadline = System.nanoTime() + 200_000;   // a fifth of a millisecond of this tick
 *     scheduler.run(tasks, 2_000, System::nanoTime, deadline);
 * }
 * }</pre>
 *
 * <p>A scheduler keeps which task goes first next time, so each block keeps one of its own.
 */
public final class TickScheduler {

    private int start;

    /** The most work one task does before the next gets its turn. */
    public static final int QUANTUM = 64;

    /**
     * How much work runs between two looks at the clock when no task reached outside itself. Reading the clock after
     * every quantum costs more than a quantum of light work; a block short of time runs at most this much past its
     * deadline.
     */
    public static final int CLOCK_BLOCK = 512;

    /**
     * Runs one tick.
     *
     * @param tasks    what is ready to run, in the order the block lists it
     * @param credits  how much work the tick is worth
     * @param clock    the clock the deadline is read against, in the deadline's units
     * @param deadline when the tick must end, on that clock; a deadline already passed runs nothing, and
     *                 {@link Long#MAX_VALUE} means there is none, so the clock is never read
     */
    public Outcome run(final List<? extends ITask> tasks, final int credits, final LongSupplier clock,
                       final long deadline) {
        final int count = tasks.size();
        if (count == 0 || credits <= 0) {
            return Outcome.NOTHING;
        }
        final boolean timed = deadline != Long.MAX_VALUE;
        if (timed && clock.getAsLong() >= deadline) {
            return new Outcome(0, true);
        }
        final boolean[] done = new boolean[count];
        int remaining = count;
        int left = credits;
        int spent = 0;
        int sinceLook = 0;
        final int first = Math.floorMod(this.start, count);
        for (int round = 0; left > 0 && remaining > 0; round++) {
            /*
             * A round offers everyone still running an equal cut of what is left, up to a quantum, so a tick too
             * small for a quantum each is still shared and never spent on the first alone. A low one sits out every
             * other round; a tick of one round leaves it the odd credits.
             */
            final int slice = Math.max(1, Math.min(QUANTUM, left / remaining));
            boolean any = false;
            for (int k = 0; k < count && left > 0; k++) {
                final int at = (first + k) % count;
                if (done[at] || (round % 2 == 1 && tasks.get(at).low() && any)) {
                    continue;
                }
                any = true;
                final int offered = Math.min(slice, left);
                final int used = tasks.get(at).step(offered);
                spent += used;
                left -= used;
                if (used < offered) {
                    done[at] = true;
                    remaining--;
                }
                sinceLook += used;
                // A step that used more than it was offered reached outside itself, which may take real time.
                if (timed && (sinceLook >= CLOCK_BLOCK || used > offered)) {
                    sinceLook = 0;
                    if (clock.getAsLong() >= deadline) {
                        this.start = (at + 1) % count;
                        return new Outcome(spent, true);
                    }
                }
            }
        }
        this.start = (first + 1) % count;
        return new Outcome(spent, false);
    }

    /** Something that can be given work and says how much it did. */
    @FunctionalInterface
    public interface ITask {

        /**
         * Does up to {@code budget} units of work and says how many it did.
         *
         * <p>Fewer than offered means it has nothing more to do this tick; more is allowed, when the last thing it
         * did reached outside itself and cost more than one.
         */
        int step(int budget);

        /** Whether this one may be passed over every other round, so the others get on faster. */
        default boolean low() {
            return false;
        }
    }

    /**
     * What one tick came to.
     *
     * @param spent    the work done
     * @param cutShort whether the clock ended the tick before its credits were spent
     */
    public record Outcome(int spent, boolean cutShort) {

        /** A tick on which nothing ran. */
        public static final Outcome NOTHING = new Outcome(0, false);
    }
}
