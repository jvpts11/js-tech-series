/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RamLedgerTest {

    @Test
    void usedMb_sumsEveryEntryAndByKind() {
        final RamLedger ledger = new RamLedger(128)
                .add("Frames XP", 64, RamLedger.Kind.SYSTEM)
                .add("IQL Engine", 24, RamLedger.Kind.SERVICE)
                .add("Files", 16, RamLedger.Kind.WINDOW)
                .add("Editor", 16, RamLedger.Kind.WINDOW);

        assertEquals(120, ledger.usedMb());
        assertEquals(32, ledger.usedMb(RamLedger.Kind.WINDOW));
        assertEquals(8, ledger.freeMb());
        assertTrue(ledger.fits(8));
        assertFalse(ledger.fits(9));
    }

    @Test
    void heldBytes_countsWhatIsHeldWhileTheMegabytesStayWhatWasGiven() {
        final RamLedger ledger = new RamLedger(128)
                .add("Frames XP", 64, RamLedger.Kind.SYSTEM)
                .add("farm.sgs", 16, 3L * RamLedger.BYTES_PER_MB, RamLedger.Kind.PROCESS, 1)
                .add("watch.sgs", 16, 512L, RamLedger.Kind.PROCESS, 2);

        // What was promised is what says whether one more thing fits, and two programs were promised 16 each.
        assertEquals(96, ledger.usedMb());
        assertEquals(32, ledger.freeMb());
        // What is held is far less, and it is what moves while those programs run.
        assertEquals(64L * RamLedger.BYTES_PER_MB + 3L * RamLedger.BYTES_PER_MB + 512L, ledger.heldBytes());
    }

    @Test
    void heldLabel_readsInTheUnitThatSaysItPlainly() {
        assertEquals("0 B", RamLedger.heldLabel(0));
        assertEquals("768 B", RamLedger.heldLabel(768));
        assertEquals("1 KB", RamLedger.heldLabel(1024));
        assertEquals("512 KB", RamLedger.heldLabel(512L * 1024L));
        assertEquals("1.0 MB", RamLedger.heldLabel(RamLedger.BYTES_PER_MB));
        assertEquals("3.5 MB", RamLedger.heldLabel(7L * RamLedger.BYTES_PER_MB / 2L));
    }

    @Test
    void add_ignoresAnEmptyEntry() {
        final RamLedger ledger = new RamLedger(64).add("nothing", 0, RamLedger.Kind.WINDOW);

        assertTrue(ledger.entries().isEmpty());
        assertEquals(64, ledger.freeMb());
    }

    @Test
    void freeMb_neverGoesNegative() {
        final RamLedger ledger = new RamLedger(16).add("Frames XP", 64, RamLedger.Kind.SYSTEM);

        assertEquals(0, ledger.freeMb());
        assertFalse(ledger.fits(1));
    }

    @Test
    void withinBudget_keepsTheLeadingItemsThatFit() {
        final List<Integer> kept = RamLedger.withinBudget(List.of(16, 16, 32, 16), w -> w, 64);

        assertEquals(List.of(16, 16, 32), kept);
    }

    @Test
    void withinBudget_dropsEverythingAfterTheFirstMisfit() {
        final List<Integer> kept = RamLedger.withinBudget(List.of(16, 64, 1), w -> w, 32);

        assertEquals(List.of(16), kept);
    }

    @Test
    void withinBudget_keepsAllWhenTheyFit() {
        final List<String> windows = List.of("Files", "Editor");

        assertEquals(windows, RamLedger.withinBudget(windows, w -> 16, 32));
    }

    @Test
    void fits_promisesTheMemoryAndTheSwapTogether() {
        final RamLedger ledger = new RamLedger(1_024, 512, 60).add("OS", 900, RamLedger.Kind.SYSTEM);

        assertEquals(1_536, ledger.promisableMb());
        assertTrue(ledger.fits(600));
        assertFalse(ledger.fits(700));
        assertEquals(0, ledger.swapUsedMb());
        assertTrue(ledger.swapped().isEmpty());
    }

    @Test
    void swapped_takesWhatIsGivenButNotHeldFirstTheNewestFirst() {
        final RamLedger ledger = new RamLedger(4_096, 4_096, 60)
                .add("OS", 2_048, RamLedger.Kind.SYSTEM)
                .add("Old", 1_024, 512L * RamLedger.BYTES_PER_MB, RamLedger.Kind.PROCESS, 1)
                .add("New", 2_048, 1_536L * RamLedger.BYTES_PER_MB, RamLedger.Kind.PROCESS, 2);

        assertEquals(1_024, ledger.swapUsedMb());
        final List<RamLedger.Swapped> swapped = ledger.swapped();
        assertEquals(2, swapped.size());
        assertEquals("Old", swapped.get(0).entry().name());
        assertEquals(512, swapped.get(0).inSwapMb());
        assertEquals("New", swapped.get(1).entry().name());
        assertEquals(512, swapped.get(1).inSwapMb());
    }

    @Test
    void swapped_runsAtItsShareForThePartInSwap() {
        // A quarter of a program in swap on an SSD: three quarters at full speed and one at six tenths, 90 per cent.
        final RamLedger ledger = new RamLedger(3_072, 4_096, 60)
                .add("OS", 3_072, RamLedger.Kind.SYSTEM)
                .add("Soundfoundry", 4_096, 4_096L * RamLedger.BYTES_PER_MB, RamLedger.Kind.WINDOW, 0);

        assertEquals(4_096, ledger.swapUsedMb());
        // All of it went to swap, there being no memory left, so it runs at the swap's six tenths.
        assertEquals(60, ledger.swapped().getFirst().speedPercent());
        final RamLedger quarter = new RamLedger(6_144, 4_096, 60)
                .add("OS", 3_072, RamLedger.Kind.SYSTEM)
                .add("Soundfoundry", 4_096, 4_096L * RamLedger.BYTES_PER_MB, RamLedger.Kind.WINDOW, 0);
        assertEquals(1_024, quarter.swapUsedMb());
        assertEquals(90, quarter.swapped().getFirst().speedPercent());
    }

    @Test
    void swapped_neverTakesTheSystemOrItsDesktop() {
        final RamLedger ledger = new RamLedger(1_024, 1_024, 25)
                .add("OS", 1_024, RamLedger.Kind.SYSTEM)
                .add("Desktop", 512, RamLedger.Kind.DESKTOP);

        assertEquals(512, ledger.swapUsedMb());
        assertTrue(ledger.swapped().isEmpty());
    }

    @Test
    void swapUsedMb_isNeverMoreThanTheSwap() {
        final RamLedger ledger = new RamLedger(1_024, 256, 25)
                .add("OS", 1_024, RamLedger.Kind.SYSTEM)
                .add("Big", 1_024, 0L, RamLedger.Kind.PROCESS, 1);

        assertEquals(256, ledger.swapUsedMb());
        assertEquals(256, ledger.swapped().getFirst().inSwapMb());
    }
}
