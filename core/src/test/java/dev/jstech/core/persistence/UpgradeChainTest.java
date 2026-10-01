/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpgradeChainTest {

    @Test
    void upgrade_runsEveryStepFromTheFoundVersionInOrder() {
        assertEquals("v0>1>2>3", chain().upgrade("v0", 0));
    }

    @Test
    void upgrade_startsAtTheVersionItWasFoundAt() {
        assertEquals("v2>3", chain().upgrade("v2", 2));
    }

    @Test
    void upgrade_leavesTodaysVersionAsItIs() {
        final String today = "v4";
        assertSame(today, chain().upgrade(today, 4));
    }

    @Test
    void upgrade_leavesANewerVersionAsItIs() {
        final String newer = "v9";
        assertSame(newer, chain().upgrade(newer, 9));
        assertTrue(chain().isNewer(9));
        assertFalse(chain().isNewer(4));
    }

    @Test
    void upgrade_readsAVersionBelowZeroAsZero() {
        assertEquals("v0>1>2>3", chain().upgrade("v0", -3));
    }

    @Test
    void upgrade_passesOverAVersionWithNoStep() {
        final UpgradeChain<String> gap = UpgradeChain.<String>builder("gap").version(4)
                .step(0, value -> value + ">1")
                .step(3, value -> value + ">4")
                .build();
        assertEquals("v0>1>4", gap.upgrade("v0", 0));
        assertEquals("v2>4", gap.upgrade("v2", 2));
    }

    @Test
    void needsSteps_isTrueOnlyWhenAStepLiesAhead() {
        final UpgradeChain<String> late = UpgradeChain.<String>builder("late").version(3)
                .step(2, value -> value + ">3")
                .build();
        assertTrue(late.needsSteps(0));
        assertTrue(late.needsSteps(2));
        assertFalse(late.needsSteps(3));
        assertFalse(late.needsSteps(7));
    }

    @Test
    void upgrade_withNoStepsChangesNothing() {
        final UpgradeChain<String> plain = UpgradeChain.<String>builder("plain").build();
        assertEquals(1, plain.version());
        assertSame("v0", plain.upgrade("v0", 0));
        assertFalse(plain.needsSteps(0));
    }

    @Test
    void upgrade_refusesAStepThatGivesBackNothing() {
        final UpgradeChain<String> broken = UpgradeChain.<String>builder("broken").version(2)
                .step(0, value -> null)
                .build();
        assertThrows(NullPointerException.class, () -> broken.upgrade("v0", 0));
    }

    @Test
    void builder_refusesTwoStepsFromOneVersion() {
        final UpgradeChain.Builder<String> builder = UpgradeChain.<String>builder("twice").version(3)
                .step(1, value -> value);
        assertThrows(IllegalArgumentException.class, () -> builder.step(1, value -> value));
    }

    @Test
    void builder_refusesAStepFromAVersionNotReached() {
        assertThrows(IllegalArgumentException.class, () -> UpgradeChain.<String>builder("ahead").version(2)
                .step(2, value -> value).build());
    }

    @Test
    void builder_refusesAVersionBelowOneOrAStepBelowZero() {
        assertThrows(IllegalArgumentException.class, () -> UpgradeChain.<String>builder("zero").version(0));
        assertThrows(IllegalArgumentException.class, () -> UpgradeChain.<String>builder("below")
                .step(-1, value -> value));
    }

    /** A chain at version 4 with a step from each of 0, 1 and 2, each writing where it took the value. */
    private static UpgradeChain<String> chain() {
        return UpgradeChain.<String>builder("test").version(4)
                .step(2, value -> value + ">3")
                .step(0, value -> value + ">1")
                .step(1, value -> value + ">2")
                .build();
    }
}
