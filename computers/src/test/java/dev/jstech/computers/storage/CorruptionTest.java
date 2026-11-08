/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.storage;

import dev.jstech.computers.hardware.StorageTier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorruptionTest {

    @Test
    void share_takesTheWholePartAndOneMoreByChance() {
        assertEquals(10L, Corruption.share(100L, 0.10, () -> 0.99));
        assertEquals(1L, Corruption.share(5L, 0.10, () -> 0.4));
        assertEquals(0L, Corruption.share(5L, 0.10, () -> 0.6));
    }

    @Test
    void share_neverTakesMoreThanThereIs() {
        assertEquals(3L, Corruption.share(3L, 1.5, () -> 0.0));
        assertEquals(0L, Corruption.share(0L, 0.5, () -> 0.0));
        assertEquals(0L, Corruption.share(10L, 0.0, () -> 0.0));
    }

    @Test
    void damage_isTheCorruptedShareOfWhatIsStored() {
        assertEquals(700, Corruption.damage(700L, 1000L));
        assertEquals(0, Corruption.damage(0L, 1000L));
        assertEquals(0, Corruption.damage(5L, 0L));
    }

    @Test
    void badSectors_takeWhatTheDamageGoesOverHalfBy() {
        assertEquals(200, Corruption.badSectors(700));
        assertEquals(0, Corruption.badSectors(500));
        assertEquals(0, Corruption.badSectors(120));
    }

    @Test
    void dies_onlyPastNineTenths() {
        assertFalse(Corruption.dies(900));
        assertTrue(Corruption.dies(901));
    }

    @Test
    void afterPass_mendsAQuarterRoundedUp() {
        assertEquals(9, Corruption.afterPass(12));
        assertEquals(0, Corruption.afterPass(1));
        assertEquals(0, Corruption.afterPass(0));
    }

    @Test
    void usable_losesTheBadShareOfTheCapacity() {
        assertEquals(800L, Corruption.usable(1000L, 200));
        assertEquals(1000L, Corruption.usable(1000L, 0));
        assertEquals(0L, Corruption.usable(1000L, 1200));
    }

    @Test
    void recoveryChance_hardDiskGivesBack95WhateverTheDamage() {
        assertEquals(95, Corruption.recoveryChance(StorageTier.HDD, 0));
        assertEquals(95, Corruption.recoveryChance(StorageTier.HDD, 880));
    }

    @Test
    void recoveryChance_ssdLosesTenPointsATenthAndGivesUpAt70() {
        assertEquals(70, Corruption.recoveryChance(StorageTier.SSD, 50));
        assertEquals(60, Corruption.recoveryChance(StorageTier.SSD, 150));
        assertEquals(10, Corruption.recoveryChance(StorageTier.SSD, 690));
        assertEquals(0, Corruption.recoveryChance(StorageTier.SSD, 700));
    }

    @Test
    void recoveryChance_nvmeLosesFifteenPointsATenthAndGivesUpAt50() {
        assertEquals(60, Corruption.recoveryChance(StorageTier.NVME, 90));
        assertEquals(45, Corruption.recoveryChance(StorageTier.NVME, 100));
        assertEquals(15, Corruption.recoveryChance(StorageTier.NVME, 399));
        assertEquals(0, Corruption.recoveryChance(StorageTier.NVME, 499));
        assertEquals(0, Corruption.recoveryChance(StorageTier.NVME, 500));
    }

    @Test
    void passTicks_followTheKindOfDisk() {
        assertEquals(3600, Corruption.passTicks(StorageTier.HDD));
        assertEquals(1200, Corruption.passTicks(StorageTier.SSD));
        assertEquals(600, Corruption.passTicks(StorageTier.NVME));
    }
}
