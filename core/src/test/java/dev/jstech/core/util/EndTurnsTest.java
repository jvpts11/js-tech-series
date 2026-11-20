/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.EndRouter;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EndTurnsTest {

    private EndTurns turns;

    @BeforeEach
    void setUp() {
        turns = new EndTurns();
    }

    @Test
    void of_aPartThatMovedNothingIsAtTheFirstTurn() {
        assertEquals(0L, turns.of(List.of(new End(3), new End(7))));
    }

    @Test
    void moved_countsEachDeliveryOfThePart() {
        final List<End> part = List.of(new End(3), new End(7));
        turns.moved(part);
        turns.moved(part);
        assertEquals(2L, turns.of(part));
    }

    @Test
    void of_keepsThePartsTurnWhenItsEndsAreWorkedOutAgainInAnotherOrder() {
        turns.moved(List.of(new End(3), new End(7)));
        assertEquals(1L, turns.of(List.of(new End(7), new End(3))));
    }

    @Test
    void of_keepsEachPartsOwnTurn() {
        turns.moved(List.of(new End(3), new End(7)));
        assertEquals(0L, turns.of(List.of(new End(5), new End(9))));
    }

    @Test
    void keep_forgetsThePartsNoLongerStanding() {
        final List<End> gone = List.of(new End(3), new End(7));
        final List<End> standing = List.of(new End(5), new End(9));
        turns.moved(gone);
        turns.moved(standing);
        turns.keep(List.of(standing));
        assertEquals(0L, turns.of(gone));
        assertEquals(1L, turns.of(standing));
    }

    /* An end that is only its block's number: what the turns are kept by. */
    private record End(long key) implements EndRouter.IEnd<String> {

        @Override
        public EndMode mode() {
            return EndMode.NORMAL;
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public long rate() {
            return 0L;
        }

        @Override
        public int channel() {
            return 0;
        }

        @Override
        public boolean allows(final String resource) {
            return true;
        }

        @Override
        public long keep(final String resource) {
            return 0L;
        }

        @Override
        public EndRouter.Offer<String> offer(final long most, final Predicate<String> wanted) {
            return null;
        }

        @Override
        public boolean gives(final String resource) {
            return false;
        }

        @Override
        public boolean takes(final String resource) {
            return false;
        }

        @Override
        public long take(final String resource, final long most) {
            return 0L;
        }

        @Override
        public long holding(final String resource) {
            return 0L;
        }
    }
}
