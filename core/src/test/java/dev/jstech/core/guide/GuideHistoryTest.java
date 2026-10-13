/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class GuideHistoryTest {

    @Test
    void back_returnsToWhereTheReaderJumpedFromAndForwardComesBack() {
        final GuideHistory history = new GuideHistory();
        history.left(3);
        history.left(8);

        assertEquals(OptionalInt.of(8), history.behind());
        assertEquals(OptionalInt.of(8), history.back(12));
        assertEquals(OptionalInt.of(3), history.back(8));
        assertEquals(OptionalInt.of(8), history.ahead());
        assertEquals(OptionalInt.of(8), history.forward(3));
        assertEquals(OptionalInt.of(12), history.forward(8));
        assertTrue(history.ahead().isEmpty());
    }

    @Test
    void left_forgetsTheWayForwardAfterGoingSomewhereNew() {
        final GuideHistory history = new GuideHistory();
        history.left(3);
        history.back(8);

        history.left(3);

        assertTrue(history.ahead().isEmpty(), "a new jump forgets where Forward led");
        assertEquals(OptionalInt.of(3), history.behind());
    }

    @Test
    void left_leavesNoMarkForTheCoverNorTwiceForOnePlace() {
        final GuideHistory history = new GuideHistory();
        history.left(-1);
        history.left(4);
        history.left(4);

        assertEquals(OptionalInt.of(4), history.back(9));
        assertTrue(history.behind().isEmpty());
    }

    @Test
    void left_remembersAtMostItsKeptPlaces() {
        final GuideHistory history = new GuideHistory();
        for (int spread = 0; spread < GuideHistory.KEPT + 10; spread++) {
            history.left(spread);
        }

        int steps = 0;
        int at = 1000;
        for (OptionalInt back = history.back(at); back.isPresent(); back = history.back(at)) {
            at = back.getAsInt();
            steps++;
        }
        assertEquals(GuideHistory.KEPT, steps);
        assertEquals(10, at, "the oldest places went first");
    }
}
