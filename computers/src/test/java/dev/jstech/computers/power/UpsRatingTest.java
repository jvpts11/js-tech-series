/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpsRatingTest {

    @Test
    void powerGuards_lastTheirMinutesAtWhatTheyCarry() {
        assertEquals(5L, UpsRating.minutesLeft(UpsRating.POWERGUARD_300.batteryJoules(), 300));
        assertEquals(8L, UpsRating.minutesLeft(UpsRating.POWERGUARD_500.batteryJoules(), 500));
        assertEquals(10L, UpsRating.minutesLeft(UpsRating.POWERGUARD_800.batteryJoules(), 800));
        assertEquals(15L, UpsRating.minutesLeft(UpsRating.POWERGUARD_1500.batteryJoules(), 1_500));
        assertEquals(15L, UpsRating.minutesLeft(UpsRating.RACK.batteryJoules(), 3_000));
    }

    @Test
    void minutesLeft_isNoneWithNoLoadOrNoCharge() {
        assertEquals(0L, UpsRating.minutesLeft(480_000L, 0.0));
        assertEquals(0L, UpsRating.minutesLeft(0L, 800.0));
        assertEquals(0L, UpsRating.minutesLeft(-5L, 800.0));
    }

    @Test
    void chargePerTick_fillsAnEmptyBatteryInTenGameMinutes() {
        assertEquals(40L, UpsRating.POWERGUARD_800.chargePerTick());
        assertEquals(UpsRating.POWERGUARD_800.batteryJoules(), UpsRating.POWERGUARD_800.chargePerTick() * 12_000L);
        assertEquals(1L, new UpsRating(5L, 10).chargePerTick(), "a tiny battery still charges");
        assertEquals(0L, new UpsRating(0L, 10).chargePerTick(), "no battery charges nothing");
    }

    @Test
    void bypassedBy_onlyAboveWhatItCarries() {
        assertFalse(UpsRating.POWERGUARD_800.bypassedBy(800.0));
        assertTrue(UpsRating.POWERGUARD_800.bypassedBy(800.5));
        assertFalse(UpsRating.POWERGUARD_800.bypassedBy(0.0));
    }

    @Test
    void permille_isClampedBetweenEmptyAndFull() {
        final UpsRating rating = UpsRating.POWERGUARD_800;
        assertEquals(0, rating.permille(0L));
        assertEquals(0, rating.permille(-10L));
        assertEquals(500, rating.permille(240_000L));
        assertEquals(1_000, rating.permille(480_000L));
        assertEquals(1_000, rating.permille(999_999L));
        assertEquals(0, new UpsRating(0L, 100).permille(5L));
    }

    @Test
    void shutsDownAt_aTenthOrLess() {
        final UpsRating rating = UpsRating.POWERGUARD_800;
        assertTrue(rating.shutsDownAt(48_000L));
        assertTrue(rating.shutsDownAt(0L));
        assertFalse(rating.shutsDownAt(48_480L));
    }

    @Test
    void tickOf_roundsUpToAWholeJoule() {
        assertEquals(40L, UpsRating.tickOf(800.0));
        assertEquals(41L, UpsRating.tickOf(801.0));
        assertEquals(0L, UpsRating.tickOf(-3.0));
    }

    @Test
    void withBattery_addsToTheBatteryAlone() {
        final UpsRating more = UpsRating.RACK.withBattery(UpsRating.EXPANSION_JOULES);
        assertEquals(5_400_000L, more.batteryJoules());
        assertEquals(3_000, more.carryWatts());
        assertEquals(UpsRating.RACK, UpsRating.RACK.withBattery(-1L), "nothing is taken away");
    }

    @Test
    void constructor_refusesLessThanNothing() {
        assertThrows(IllegalArgumentException.class, () -> new UpsRating(-1L, 300));
        assertThrows(IllegalArgumentException.class, () -> new UpsRating(1L, -1));
    }
}
