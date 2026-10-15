/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LoadFollowerTest {

    @Test
    void instant_givesWhateverIsDrawnUpToItsMost() {
        final LoadFollower generator = LoadFollower.instant(500L);

        assertEquals(500L, generator.draw(800L, false));
        assertEquals(500L, generator.settle());
        assertEquals(120L, generator.draw(120L, false));
        assertEquals(120L, generator.settle());
    }

    @Test
    void draw_neverGivesMoreThanItsOfferInATick() {
        final LoadFollower generator = LoadFollower.instant(500L);

        assertEquals(300L, generator.draw(300L, false));
        assertEquals(200L, generator.draw(300L, true));
        assertEquals(300L, generator.drawn());
    }

    @Test
    void settle_burnsOnlyWhatIsDrawn() {
        final LoadFollower generator = LoadFollower.instant(1_000L);
        generator.draw(250L, false);

        assertEquals(250L, generator.settle());
        assertEquals(0L, generator.drawn());
    }

    @Test
    void settle_makesItsMinimumEvenWhenNothingIsDrawn() {
        final LoadFollower reactor = new LoadFollower(1_000L, 300L, 0L);

        assertEquals(300L, reactor.settle());
    }

    @Test
    void offer_climbsNoFasterThanItsRamp() {
        final LoadFollower generator = new LoadFollower(1_000L, 0L, 200L);
        long previous = 0L;
        for (int tick = 0; tick < 8; tick++) {
            final long drawn = generator.draw(1_000L, false);
            generator.settle();
            assertEquals(Math.min(1_000L, previous + 200L), drawn);
            previous = drawn;
        }
        assertEquals(1_000L, generator.output());
    }

    @Test
    void floor_comesDownNoFasterThanItsRamp() {
        final LoadFollower generator = new LoadFollower(1_000L, 100L, 300L);
        generator.restore(1_000L);

        assertEquals(700L, generator.settle());
        assertEquals(400L, generator.settle());
        assertEquals(100L, generator.settle());
        assertEquals(100L, generator.settle());
    }

    @Test
    void stop_startsItAgainFromNothing() {
        final LoadFollower generator = new LoadFollower(1_000L, 0L, 200L);
        generator.restore(800L);
        generator.stop();

        assertEquals(200L, generator.offer());
    }

    @Test
    void new_refusesAMinimumAboveItsMost() {
        assertThrows(IllegalArgumentException.class, () -> new LoadFollower(100L, 200L, 0L));
        assertThrows(IllegalArgumentException.class, () -> new LoadFollower(100L, 0L, -1L));
    }
}
