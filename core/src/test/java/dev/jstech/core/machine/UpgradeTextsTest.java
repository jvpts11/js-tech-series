/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpgradeTextsTest {

    @Test
    void effects_sayAMotorsGainAsAGainAndItsEnergyAsACost() {
        assertEquals(List.of("+50% speed:gain", "+75% energy:cost"),
                said(UpgradeEffect.NONE.withSpeed(1.5).withEnergy(1.75)));
    }

    @Test
    void effects_sayEnergySavedAsAGain() {
        assertEquals(List.of("-20% energy:gain"), said(UpgradeEffect.NONE.withEnergy(0.8)));
    }

    @Test
    void effects_sayTheRecipesAtOnceAndTheirEnergy() {
        assertEquals(List.of("+1 recipe at once:gain", "energy in proportion:cost"),
                said(UpgradeEffect.NONE.withParallel(1)));
        assertEquals(List.of("+2 recipes at once:gain", "energy in proportion:cost"),
                said(UpgradeEffect.NONE.withParallel(2)));
    }

    @Test
    void effects_sayTheGasesAndTheLiquidsEachTheirOwn() {
        assertEquals(List.of("-15% gases spent:gain"), said(UpgradeEffect.NONE.withGases(0.85)));
        assertEquals(List.of("-15% liquids spent:gain"), said(UpgradeEffect.NONE.withLiquids(0.85)));
    }

    @Test
    void effects_sayASilencedMachineIsSilent() {
        assertEquals(List.of("the machine is silent:gain"), said(UpgradeEffect.NONE.withSound(0.0)));
    }

    @Test
    void effects_sayNothingOfWhatDoesNotChange() {
        assertEquals(List.of(), said(UpgradeEffect.NONE));
    }

    @Test
    void most_saysHowManyFitOrThatTheSlotsAreTheLimit() {
        assertEquals("Up to 4 in a machine", UpgradeTexts.most(4).english());
        assertEquals("As many as the slots take", UpgradeTexts.most(IUpgrade.ANY).english());
    }

    /* Each line of {@code effect}'s tooltip in English, with whether it is a gain or a cost. */
    private static List<String> said(final UpgradeEffect effect) {
        return UpgradeTexts.effects(effect).stream()
                .map(line -> line.text().english() + ":" + (line.gain() ? "gain" : "cost")).toList();
    }
}
