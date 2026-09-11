/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.machine.Scheduler;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulerTest {

    private static final long NO_DEADLINE = Long.MAX_VALUE;

    /** A program with work for ever: it spends everything it is offered and remembers each offer. */
    private static class Greedy implements Scheduler.ISlot {
        final List<Integer> offers = new ArrayList<>();
        int total;

        @Override
        public int step(final int budget) {
            this.offers.add(budget);
            this.total += budget;
            return budget;
        }
    }

    /** A program that has only so much to do this tick and then stops. */
    private static final class Brief implements Scheduler.ISlot {
        final List<Integer> offers = new ArrayList<>();
        int left;

        Brief(final int work) {
            this.left = work;
        }

        @Override
        public int step(final int budget) {
            this.offers.add(budget);
            final int used = Math.min(budget, this.left);
            this.left -= used;
            return used;
        }
    }

    /** A clock that moves one unit every time it is read. */
    private static final class Ticking {
        long now;

        long read() {
            return this.now++;
        }
    }

    @Test
    void run_splitsASmallTickEvenlyAndSpendsAllOfIt() {
        final Greedy one = new Greedy();
        final Greedy two = new Greedy();
        final Scheduler.Outcome outcome = new Scheduler().run(List.of(one, two), 9, () -> 0L, NO_DEADLINE);
        assertEquals(9, outcome.spent());
        assertFalse(outcome.cutShort());
        assertEquals(9, one.total + two.total);
        assertTrue(Math.abs(one.total - two.total) <= 1, one.total + " against " + two.total);
    }

    @Test
    void run_dealsQuantaInTurn() {
        final Greedy one = new Greedy();
        final Greedy two = new Greedy();
        final Greedy three = new Greedy();
        final Scheduler.Outcome outcome =
                new Scheduler().run(List.of(one, two, three), 1000, () -> 0L, NO_DEADLINE);
        assertEquals(1000, outcome.spent());
        assertEquals(Scheduler.QUANTUM, one.offers.getFirst());
        assertEquals(Scheduler.QUANTUM, two.offers.getFirst());
        assertEquals(Scheduler.QUANTUM, three.offers.getFirst());
        assertTrue(Math.abs(one.total - three.total) <= Scheduler.QUANTUM,
                one.total + " against " + three.total);
    }

    @Test
    void run_rotatesWhoTakesTheRemainderBetweenTicks() {
        final Greedy one = new Greedy();
        final Greedy two = new Greedy();
        final Scheduler scheduler = new Scheduler();
        scheduler.run(List.of(one, two), 9, () -> 0L, NO_DEADLINE);
        final int oneFirst = one.total;
        scheduler.run(List.of(one, two), 9, () -> 0L, NO_DEADLINE);
        assertEquals(9, one.total, "two ticks of nine come to nine each");
        assertEquals(9, two.total);
        assertEquals(5, oneFirst, "the odd credit went to the first program on the first tick");
    }

    @Test
    void run_givesWhatAProgramLeavesToTheOthers() {
        final Brief brief = new Brief(10);
        final Greedy greedy = new Greedy();
        final Scheduler.Outcome outcome =
                new Scheduler().run(List.of(brief, greedy), 500, () -> 0L, NO_DEADLINE);
        assertEquals(500, outcome.spent());
        assertEquals(10, 500 - greedy.total, "the brief one used its ten");
        assertEquals(1, brief.offers.size(), "and was not asked again once it stopped early");
    }

    @Test
    void run_stopsAtTheDeadlineAndStartsWithTheUnservedNextTick() {
        final Greedy one = new Greedy();
        final Greedy two = new Greedy();
        final Greedy three = new Greedy();
        final Scheduler scheduler = new Scheduler();
        final Ticking clock = new Ticking();
        // Read once on the way in (0) and once after each quantum (1, 2...): a deadline of 2 allows two.
        final Scheduler.Outcome first = scheduler.run(List.of(one, two, three), 10_000, clock::read, 2);
        assertTrue(first.cutShort());
        assertEquals(2 * Scheduler.QUANTUM, first.spent());
        assertEquals(0, three.total, "the third program went without");

        clock.now = 0;
        final Scheduler.Outcome second = scheduler.run(List.of(one, two, three), 10_000, clock::read, 2);
        assertTrue(second.cutShort());
        assertEquals(Scheduler.QUANTUM, three.total, "so it goes first next tick");
        assertEquals(2 * Scheduler.QUANTUM, one.total, "and the first takes the second place");
        assertEquals(Scheduler.QUANTUM, two.total);
    }

    @Test
    void run_runsNothingOnceTheDeadlineHasPassed() {
        final Greedy one = new Greedy();
        final Scheduler.Outcome outcome = new Scheduler().run(List.of(one), 500, () -> 10L, 5);
        assertTrue(outcome.cutShort());
        assertEquals(0, outcome.spent());
        assertTrue(one.offers.isEmpty());
    }

    /** A program of low priority: it spends what it gets, but sits out every other round. */
    private static final class Humble extends Greedy {
        @Override
        public boolean low() {
            return true;
        }
    }

    @Test
    void run_passesALowSlotOverEveryOtherRound() {
        final Greedy one = new Greedy();
        final Humble two = new Humble();
        new Scheduler().run(List.of(one, two), 10 * Scheduler.QUANTUM, () -> 0L, NO_DEADLINE);
        assertEquals(10 * Scheduler.QUANTUM, one.total + two.total, "the tick is still spent whole");
        assertTrue(one.total > two.total, one.total + " against " + two.total);
        assertTrue(one.total >= 2 * two.total - Scheduler.QUANTUM,
                "the low one gets about half as many turns; " + one.total + " against " + two.total);
    }

    @Test
    void run_isNothingWithNothingToRunOrNothingToSpend() {
        assertEquals(Scheduler.Outcome.NOTHING, new Scheduler().run(List.of(), 500, () -> 0L, NO_DEADLINE));
        assertEquals(Scheduler.Outcome.NOTHING,
                new Scheduler().run(List.of(new Greedy()), 0, () -> 0L, NO_DEADLINE));
    }
}
