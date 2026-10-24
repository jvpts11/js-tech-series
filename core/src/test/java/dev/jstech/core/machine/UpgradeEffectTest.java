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
    void addGains_addsWhatEachGainsSoFourMotorsMakeAMachineThreeTimesAsFast() {
        final UpgradeEffect effect = UpgradeEffect.addGains(List.of(
                new UpgradeEffect.Counted(new UpgradeEffect(1.5, 1.75), 4)));
        assertEquals(3.0, effect.speed(), 1e-9);
        assertEquals(4.0, effect.energy(), 1e-9);
        assertEquals(4.0 / 3.0, effect.energy() / effect.speed(), 1e-9, "a third more energy an item");
    }

    @Test
    void addGains_multipliesSavingsSoFourDrivesLeaveFortyOnePercent() {
        final UpgradeEffect effect = UpgradeEffect.addGains(List.of(
                new UpgradeEffect.Counted(new UpgradeEffect(1.0, 0.8), 4)));
        assertEquals(0.4096, effect.energy(), 1e-9);
        assertEquals(1.0, effect.speed(), 1e-9);
    }

    @Test
    void addGains_appliesTheSavingsToWhatTheGainsAddUpTo() {
        final UpgradeEffect effect = UpgradeEffect.addGains(List.of(
                new UpgradeEffect.Counted(new UpgradeEffect(1.5, 1.75), 2),
                new UpgradeEffect.Counted(new UpgradeEffect(1.0, 0.8), 2),
                new UpgradeEffect.Counted(UpgradeEffect.NONE.withParallel(1), 3)));
        assertEquals(2.0, effect.speed(), 1e-9);
        assertEquals(2.5 * 0.64, effect.energy(), 1e-9);
        assertEquals(3, effect.parallel());
        assertEquals(UpgradeEffect.NONE, UpgradeEffect.addGains(List.of()));
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
    void combine_addsTheRecipesAtOnceAndMultipliesTheRest() {
        final UpgradeEffect effect = UpgradeEffect.combine(List.of(
                new UpgradeEffect.Counted(UpgradeEffect.NONE.withParallel(2).withYield(1.5), 2),
                new UpgradeEffect.Counted(UpgradeEffect.NONE.withUse(0.5).withBuffer(4.0), 1)));
        assertEquals(4, effect.parallel());
        assertEquals(5, effect.recipesAtOnce());
        assertEquals(2.25, effect.yield(), 1e-9);
        assertEquals(0.5, effect.use(), 1e-9);
        assertEquals(4.0, effect.buffer(), 1e-9);
    }

    @Test
    void combine_holdsTheRecipesAtOnceToTheMost() {
        final UpgradeEffect effect = UpgradeEffect.combine(List.of(
                new UpgradeEffect.Counted(UpgradeEffect.NONE.withParallel(1_000), 64)));
        assertEquals(UpgradeEffect.MOST_PARALLEL, effect.parallel());
    }

    @Test
    void made_keepsTheWholeAndRollsForTheRest() {
        final UpgradeEffect effect = UpgradeEffect.NONE.withYield(1.5);
        assertEquals(1, effect.made(1, 0.9));
        assertEquals(2, effect.made(1, 0.1));
        assertEquals(3, effect.made(2, 0.99));
        assertEquals(2, effect.mostMade(1));
        assertEquals(0, UpgradeEffect.NONE.withYield(0.0).made(5, 0.0));
    }

    @Test
    void spent_roundsUpAndSpendsNothingOfNothing() {
        final UpgradeEffect effect = UpgradeEffect.NONE.withUse(0.5);
        assertEquals(50L, effect.spent(100L));
        assertEquals(1L, effect.spent(1L));
        assertEquals(0L, effect.spent(0L));
        assertEquals(0L, UpgradeEffect.NONE.withUse(0.0).spent(100L));
    }

    @Test
    void byproductChance_neverPassesACertainty() {
        assertEquals(0.5, UpgradeEffect.NONE.withByproducts(2.0).byproductChance(0.25), 1e-9);
        assertEquals(1.0, UpgradeEffect.NONE.withByproducts(10.0).byproductChance(0.25), 1e-9);
    }

    @Test
    void storage_neverFallsUnderOne() {
        assertEquals(20_000L, UpgradeEffect.NONE.withBuffer(2.0).storage(10_000L));
        assertEquals(1L, UpgradeEffect.NONE.withBuffer(0.0001).storage(10L));
    }

    @Test
    void constructor_refusesNegativeFactorsAndRecipes() {
        assertThrows(IllegalArgumentException.class, () -> UpgradeEffect.NONE.withParallel(-1));
        assertThrows(IllegalArgumentException.class, () -> UpgradeEffect.NONE.withYield(-0.5));
        assertThrows(IllegalArgumentException.class, () -> UpgradeEffect.NONE.withBuffer(0.0));
        assertThrows(IllegalArgumentException.class, () -> UpgradeEffect.NONE.withSound(Double.NaN));
    }

    @Test
    void constructor_refusesASpeedOfZeroOrANegativeEnergy() {
        assertThrows(IllegalArgumentException.class, () -> new UpgradeEffect(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> new UpgradeEffect(1.0, -0.5));
        assertThrows(IllegalArgumentException.class, () -> new UpgradeEffect(Double.NaN, 1.0));
    }
}
