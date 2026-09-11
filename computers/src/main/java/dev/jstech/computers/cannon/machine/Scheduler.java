/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.machine;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * Hands one machine's tick out to the programs that want it.
 *
 * <p>The tick is worth so many instructions, the credits, and may last so long by the clock, the
 * deadline. The credits are dealt in quanta, going round the programs in turn so that no program
 * finishes its share before another has begun; a program that stops early (finished, parked, halted)
 * gives up its place for the rest of the tick and the others take what it left. The deadline is looked
 * at between quanta: when it passes, the tick ends where it stands and the next one starts with the
 * first program that went without, so a machine that is always short of time is short for everyone
 * on it in turn rather than always for the same one.
 *
 * <p>Nothing here knows what a program is. It is given things that can be stepped and told how far each
 * got, which is what lets the whole of it be run against nothing at all.
 */
public final class Scheduler {

    /** The most instructions one program runs before the next gets its turn. */
    public static final int QUANTUM = 64;

    /** Something that can be given instructions and says how many it used. */
    @FunctionalInterface
    public interface ISlot {

        /**
         * Runs up to {@code budget} instructions and says how many were used.
         *
         * <p>Fewer than offered means it has nothing more to do this tick; more is allowed, when the
         * last instruction reached into the machine and cost more than one.
         */
        int step(int budget);

        /** Whether this one may be passed over every other round, so the others get on faster. */
        default boolean low() {
            return false;
        }
    }

    /** What one tick came to: the instructions spent, and whether the clock cut it short. */
    public record Outcome(int spent, boolean cutShort) {

        /** A tick on which nothing ran. */
        public static final Outcome NOTHING = new Outcome(0, false);
    }

    private int start;

    /**
     * Runs one tick.
     *
     * @param slots    what is ready to run, in the order the machine lists it
     * @param credits  how many instructions the tick is worth
     * @param clock    the clock the deadline is read against, in the deadline's units
     * @param deadline when the tick must end, on that clock; a deadline already passed runs nothing
     */
    public Outcome run(final List<? extends ISlot> slots, final int credits, final LongSupplier clock,
                       final long deadline) {
        final int count = slots.size();
        if (count == 0 || credits <= 0) {
            return Outcome.NOTHING;
        }
        if (clock.getAsLong() >= deadline) {
            return new Outcome(0, true);
        }
        final boolean[] done = new boolean[count];
        int remaining = count;
        int left = credits;
        int spent = 0;
        final int first = Math.floorMod(this.start, count);
        for (int round = 0; left > 0 && remaining > 0; round++) {
            /*
             * A round offers everyone still running an equal cut of what is left, up to a quantum, so a
             * tick too small for a quantum each is still shared and never spent on the first alone. A
             * low one sits out every other round; a tick of one round leaves it the odd credits.
             */
            final int slice = Math.max(1, Math.min(QUANTUM, left / remaining));
            boolean any = false;
            for (int k = 0; k < count && left > 0; k++) {
                final int at = (first + k) % count;
                if (done[at] || (round % 2 == 1 && slots.get(at).low() && any)) {
                    continue;
                }
                any = true;
                final int offered = Math.min(slice, left);
                final int used = slots.get(at).step(offered);
                spent += used;
                left -= used;
                if (used < offered) {
                    done[at] = true;
                    remaining--;
                }
                if (clock.getAsLong() >= deadline) {
                    this.start = (at + 1) % count;
                    return new Outcome(spent, true);
                }
            }
        }
        this.start = (first + 1) % count;
        return new Outcome(spent, false);
    }
}
