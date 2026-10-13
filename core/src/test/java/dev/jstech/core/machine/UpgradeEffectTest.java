/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpgradeEffectTest {

    @Test
    void combine_ofNothingIsNoUpgrade() {
        assertEquals(UpgradeEffect.NONE, UpgradeEffect.combine(List.of()));
    }

    @Test
    void combine_multipliesByTheCount() {
        final UpgradeEffect effect = UpgradeEffect.combine(List.of(
                new UpgradeEffect.Counted(new UpgradeEffect(1.5, 2.0), 2)));
        assertEquals(2.25, effect.speed(), 1e-9);
        assertEquals(4.0, effect.energy(), 1e-9);
    }

    @Test
    void combine_multipliesDifferentUpgrades() {
        final UpgradeEffect effect = UpgradeEffect.combine(List.of(
                new UpgradeEffect.Counted(new UpgradeEffect(2.0, 1.5), 1),
                new UpgradeEffect.Counted(new UpgradeEffect(1.0, 0.5), 1)));
        assertEquals(2.0, effect.speed(), 1e-9);
        assertEquals(0.75, effect.energy(), 1e-9);
    }

    @Test
    void combine_staysFiniteWithAGreatManyUpgrades() {
        final UpgradeEffect effect = UpgradeEffect.combine(List.of(
                new UpgradeEffect.Counted(new UpgradeEffect(10.0, 10.0), 400)));
        assertEquals(Double.MAX_VALUE, effect.speed());
        assertEquals(1, effect.ticks(200));
    }

    @Test
    void ticks_roundsUpAndNeverGoesUnderOne() {
        final UpgradeEffect effect = new UpgradeEffect(3.0, 1.0);
        assertEquals(4, effect.ticks(10));
        assertEquals(1, effect.ticks(1));
        assertEquals(1, new UpgradeEffect(1000.0, 1.0).ticks(200));
    }

    @Test
    void ticks_neverPassesAnInt() {
        final UpgradeEffect slow = UpgradeEffect.combine(List.of(
                new UpgradeEffect.Counted(new UpgradeEffect(0.001, 1.0), 400)));
        assertEquals(Integer.MAX_VALUE, slow.ticks(200));
    }

    @Test
    void energyPerTick_roundsUp() {
        assertEquals(6L, new UpgradeEffect(2.0, 1.5).energyPerTick(4));
        assertEquals(5L, new UpgradeEffect(1.0, 1.1).energyPerTick(4));
        assertEquals(0L, new UpgradeEffect(1.0, 0.0).energyPerTick(40));
    }

    @Test
    void constructor_refusesASpeedOfZeroOrANegativeEnergy() {
        assertThrows(IllegalArgumentException.class, () -> new UpgradeEffect(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new UpgradeEffect(1.0, -0.5));
        assertThrows(IllegalArgumentException.class, () -> new UpgradeEffect(Double.NaN, 1.0));
    }
}
