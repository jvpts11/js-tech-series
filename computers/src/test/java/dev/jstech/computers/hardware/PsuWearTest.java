/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PsuWearTest {

    private static final PsuSpec GOLD = new PsuSpec(1000, 90, 100);
    private static final PsuSpec REDUNDANT = new PsuSpec(3000, 94, false, 250, 2);
    private static final long A_DAY = PsuWear.TICKS_A_DAY;

    @Test
    void lifeTimes_isEightThreeAndOneByTheLoad() {
        assertEquals(8, PsuWear.lifeTimes(0.2));
        assertEquals(8, PsuWear.lifeTimes(0.5));
        assertEquals(3, PsuWear.lifeTimes(0.51));
        assertEquals(3, PsuWear.lifeTimes(0.8));
        assertEquals(1, PsuWear.lifeTimes(0.81));
        assertEquals(1, PsuWear.lifeTimes(1.4));
    }

    @Test
    void worn_atFullLoadWearsOutInItsLife() {
        final PsuCondition worn = PsuWear.worn(PsuCondition.NEW, GOLD, 1000, 100 * A_DAY, 1.0);
        assertEquals(1f, worn.wearOf(0), 1e-6);
        assertTrue(worn.failed(GOLD));
    }

    @Test
    void worn_atALightLoadLivesEightTimesLonger() {
        final PsuCondition worn = PsuWear.worn(PsuCondition.NEW, GOLD, 400, 100 * A_DAY, 1.0);
        assertEquals(0.125f, worn.wearOf(0), 1e-6);
        assertFalse(worn.degraded(GOLD));
    }

    @Test
    void worn_followsItsOwnLifeAndTheMultiplier() {
        final PsuCondition shortLived = new PsuCondition(0.8f, List.of());
        assertEquals(0.5f, PsuWear.worn(shortLived, GOLD, 1000, 40 * A_DAY, 1.0).wearOf(0), 1e-6);
        assertEquals(0.5f, PsuWear.worn(PsuCondition.NEW, GOLD, 1000, 25 * A_DAY, 0.5).wearOf(0), 1e-6);
    }

    @Test
    void worn_degradesFromFourFifths() {
        final PsuCondition worn = PsuWear.worn(PsuCondition.NEW, GOLD, 1000, 80 * A_DAY, 1.0);
        assertTrue(worn.degraded(GOLD));
        assertFalse(worn.failed(GOLD));
        assertEquals(0.8f, worn.worst(GOLD), 1e-6);
    }

    @Test
    void worn_aFailedSupplyStaysAsItIs() {
        final PsuCondition failed = new PsuCondition(1f, List.of(1f));
        assertEquals(failed, PsuWear.worn(failed, GOLD, 1000, A_DAY, 1.0));
    }

    @Test
    void worn_redundantUnitsShareTheLoadUntilOneFails() {
        // Two units under the full rating each carry half of it: a light load, eight times the life.
        final PsuCondition shared = PsuWear.worn(PsuCondition.NEW, REDUNDANT, 3000, 250 * A_DAY, 1.0);
        assertEquals(0.125f, shared.wearOf(0), 1e-6);
        assertEquals(0.125f, shared.wearOf(1), 1e-6);
        final PsuCondition oneFailed = new PsuCondition(1f, List.of(1f, 0f));
        assertTrue(oneFailed.degraded(REDUNDANT));
        assertFalse(oneFailed.failed(REDUNDANT));
        assertEquals(1, oneFailed.failedUnits(REDUNDANT));
        final PsuCondition alone = PsuWear.worn(oneFailed, REDUNDANT, 3000, 250 * A_DAY, 1.0);
        assertEquals(1f, alone.wearOf(0), 1e-6);
        assertTrue(alone.failed(REDUNDANT));
    }

    @Test
    void drawLife_staysBetweenTheShortestAndTheLongest() {
        assertEquals(0.8f, PsuWear.drawLife(0.0), 1e-6);
        assertEquals(1.2f, PsuWear.drawLife(1.0), 1e-6);
        assertEquals(1.2f, PsuWear.drawLife(7.0), 1e-6);
    }

    @Test
    void condition_keepsWearBetweenNothingAndFailed() {
        final PsuCondition condition = new PsuCondition(1f, List.of(-0.5f, 3f));
        assertEquals(0f, condition.wearOf(0));
        assertEquals(1f, condition.wearOf(1));
        assertEquals(0f, condition.wearOf(5));
    }

    @Test
    void condition_withLifeDrawsItOnce() {
        final PsuCondition drawn = PsuCondition.NEW.withLife(1.1f);
        assertEquals(1.1f, drawn.life(), 1e-6);
        assertEquals(1.1f, drawn.withLife(0.9f).life(), 1e-6);
    }
}
