/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PartialWorkTest {

    @Test
    void pay_aWholeTicksWorthDoesATickOfWork() {
        final PartialWork work = new PartialWork();

        assertEquals(1, work.pay(40L, 40L));
        assertEquals(PartialWork.PERCENT, work.lastPercent());
        assertEquals(0L, work.paid());
    }

    @Test
    void pay_halfTheEnergyWorksAtHalfTheSpeed() {
        final PartialWork work = new PartialWork();
        int done = 0;
        for (int tick = 0; tick < 10; tick++) {
            done += work.pay(20L, 40L);
            assertEquals(50, work.lastPercent());
        }

        assertEquals(5, done);
    }

    @Test
    void pay_carriesWhatIsPaidOverToTheNextTick() {
        final PartialWork work = new PartialWork();

        assertEquals(0, work.pay(30L, 40L));
        assertEquals(1, work.pay(30L, 40L));
        assertEquals(20L, work.paid());
    }

    @Test
    void pay_workThatCostsNothingIsAlwaysDone() {
        final PartialWork work = new PartialWork();

        assertEquals(1, work.pay(0L, 0L));
        assertEquals(PartialWork.PERCENT, work.lastPercent());
    }

    @Test
    void reset_forgetsWhatWasPaid() {
        final PartialWork work = new PartialWork();
        work.pay(30L, 40L);
        work.reset();

        assertEquals(0L, work.paid());
        assertEquals(PartialWork.PERCENT, work.lastPercent());
    }
}
