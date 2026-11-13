/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.schedule;

import java.util.ArrayList;
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
 * <p>A task may weigh more than one quantum a round, sit out every other round, or run only once every other task has
 * stopped for the tick; and the tasks may instead run in turn, each keeping the credits until it yields, as on a
 * cooperative system ({@link #runInTurn}).
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
     * Runs one tick in quanta of {@link #QUANTUM}.
     *
     * @param tasks    what is ready to run, in the order the block lists it
     * @param credits  how much work the tick is worth
     * @param clock    the clock the deadline is read against, in the deadline's units
     * @param deadline when the tick must end, on that clock; a deadline already passed runs nothing, and
     *                 {@link Long#MAX_VALUE} means there is none, so the clock is never read
     */
    public Outcome run(final List<? extends ITask> tasks, final int credits, final LongSupplier clock,
                       final long deadline) {
        return this.run(tasks, credits, clock, deadline, QUANTUM);
    }

    /**
     * Runs one tick in quanta of {@code quantum}: the same, with the most work one task does before the next gets its
     * turn chosen by whoever runs the block.
     */
    public Outcome run(final List<? extends ITask> tasks, final int credits, final LongSupplier clock,
                       final long deadline, final int quantum) {
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
        // How many tasks that are not idle are still running: an idle one waits for there to be none.
        int busy = 0;
        for (final ITask task : tasks) {
            busy += task.idle() ? 0 : 1;
        }
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
            final int slice = Math.max(1, Math.min(quantum, left / remaining));
            // A round of none but low ones still runs them, so it is never spent on nothing.
            final boolean lowSitOut = round % 2 == 1 && othersRun(tasks, done, busy);
            for (int k = 0; k < count && left > 0; k++) {
                final int at = (first + k) % count;
                final ITask task = tasks.get(at);
                if (done[at] || (lowSitOut && task.low()) || (task.idle() && busy > 0)) {
                    continue;
                }
                final int offered = Math.min(slice * Math.max(1, task.weight()), left);
                final int used = task.step(offered);
                spent += used;
                left -= used;
                if (used < offered) {
                    done[at] = true;
                    remaining--;
                    busy -= task.idle() ? 0 : 1;
                }
                sinceLook += used;
                // A step that used more than it was offered reached outside itself, which may take real time.
                if (timed && (sinceLook >= CLOCK_BLOCK || used > offered)) {
                    sinceLook = 0;
                    if (clock.getAsLong() >= deadline) {
                        this.start = (at + 1) % count;
                        // A tick whose credits are all spent, or whose tasks all finished, was not cut short.
                        return new Outcome(spent, left > 0 && remaining > 0);
                    }
                }
            }
        }
        this.start = (first + 1) % count;
        return new Outcome(spent, false);
    }

    /**
     * Runs one tick with the tasks in turn, as a cooperative system does: the task whose turn it is gets what is left,
     * a quantum at a time, until it yields, stops, or the credits run out, and then the next one has its turn. A turn
     * the credits ran out on carries over into the next tick, so a task that never yields keeps every tick to itself,
     * while the deadline still ends each one.
     */
    public Outcome runInTurn(final List<? extends ITask> tasks, final int credits, final LongSupplier clock,
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
        int at = Math.floorMod(this.start, count);
        while (left > 0 && remaining > 0) {
            if (done[at]) {
                at = (at + 1) % count;
                continue;
            }
            final ITask task = tasks.get(at);
            final int offered = Math.min(QUANTUM, left);
            final int used = task.step(offered);
            spent += used;
            left -= used;
            sinceLook += used;
            final boolean stopped = used < offered;
            if (stopped) {
                done[at] = true;
                remaining--;
            }
            if (stopped || task.yielded()) {
                at = (at + 1) % count;
            }
            if (timed && (sinceLook >= CLOCK_BLOCK || used > offered)) {
                sinceLook = 0;
                if (clock.getAsLong() >= deadline) {
                    this.start = at;
                    return new Outcome(spent, left > 0 && remaining > 0);
                }
            }
        }
        this.start = at;
        return new Outcome(spent, false);
    }

    /**
     * Runs one tick with the block choosing which task has each slice of {@code quantum}: before each one the chooser
     * is asked, with the tasks still running, and what it spends choosing comes off the tick. A choice of none, or of
     * a task that is not running, gives the slice to the next in turn.
     */
    public Outcome runChosen(final List<? extends ITask> tasks, final int credits, final LongSupplier clock,
                             final long deadline, final int quantum, final IChooser chooser) {
        final int count = tasks.size();
        if (count == 0 || credits <= 0) {
            return Outcome.NOTHING;
        }
        final boolean timed = deadline != Long.MAX_VALUE;
        if (timed && clock.getAsLong() >= deadline) {
            return new Outcome(0, true);
        }
        final boolean[] done = new boolean[count];
        final List<Integer> running = new ArrayList<>(count);
        int remaining = count;
        int left = credits;
        int spent = 0;
        int sinceLook = 0;
        int next = Math.floorMod(this.start, count);
        while (left > 0 && remaining > 0) {
            running.clear();
            for (int i = 0; i < count; i++) {
                if (!done[i]) {
                    running.add(i);
                }
            }
            final Choice choice = chooser.choose(List.copyOf(running), Math.min(quantum, left));
            final int choosing = Math.max(0, choice.spent());
            spent += choosing;
            left -= choosing;
            sinceLook += choosing;
            if (left <= 0) {
                break;
            }
            int at = choice.task();
            if (at < 0 || at >= count || done[at]) {
                while (done[next]) {
                    next = (next + 1) % count;
                }
                at = next;
            }
            next = (at + 1) % count;
            final int offered = Math.min(quantum, left);
            final int used = tasks.get(at).step(offered);
            spent += used;
            left -= used;
            sinceLook += used;
            if (used < offered) {
                done[at] = true;
                remaining--;
            }
            if (timed && (sinceLook >= CLOCK_BLOCK || used > offered)) {
                sinceLook = 0;
                if (clock.getAsLong() >= deadline) {
                    this.start = next;
                    return new Outcome(spent, left > 0 && remaining > 0);
                }
            }
        }
        this.start = next;
        return new Outcome(spent, false);
    }

    /* Whether a task that is not low is still to be offered work this round. */
    private static boolean othersRun(final List<? extends ITask> tasks, final boolean[] done, final int busy) {
        for (int k = 0; k < tasks.size(); k++) {
            final ITask task = tasks.get(k);
            if (!done[k] && !task.low() && !(task.idle() && busy > 0)) {
                return true;
            }
        }
        return false;
    }

    /** What chooses which task has the next slice, when the block chooses for itself. */
    @FunctionalInterface
    public interface IChooser {

        /**
         * Chooses among the tasks still running, by their places in the list, out of at most {@code budget} units of
         * work.
         */
        Choice choose(List<Integer> running, int budget);
    }

    /**
     * A choice of the task to have the next slice.
     *
     * @param task  its place in the list, or -1 for none
     * @param spent what choosing it cost
     */
    public record Choice(int task, int spent) {
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

        /** How many quanta it is offered a round: more than one for a task that is to get on faster. */
        default int weight() {
            return 1;
        }

        /** Whether it runs only once every task that is not idle has stopped for the tick. */
        default boolean idle() {
            return false;
        }

        /** Whether it gave up its turn in the step just run, which ends its turn when the tasks run in turn. */
        default boolean yielded() {
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
