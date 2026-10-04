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

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

class VramLedgerTest {

    @Test
    void monitorKbPerBlock_growsFourfoldEachEra() {
        assertEquals(64L, VramLedger.monitorKbPerBlock(HardwareEra.VINTAGE, false));
        assertEquals(256L, VramLedger.monitorKbPerBlock(HardwareEra.VINTAGE, true));
        assertEquals(4L * 1024L, VramLedger.monitorKbPerBlock(HardwareEra.LEGACY, true));
        assertEquals(16L * 1024L, VramLedger.monitorKbPerBlock(HardwareEra.TRANSITION, true));
        assertEquals(64L * 1024L, VramLedger.monitorKbPerBlock(HardwareEra.STANDARD, true));
        assertEquals(256L * 1024L, VramLedger.monitorKbPerBlock(HardwareEra.ADVANCED, true));
    }

    @Test
    void windowKb_isAQuarterBlockAndAWholeOneFullScreen() {
        assertEquals(16L * 1024L, VramLedger.windowKb(HardwareEra.STANDARD, false));
        assertEquals(64L * 1024L, VramLedger.windowKb(HardwareEra.STANDARD, true));
    }

    @Test
    void fits_countsWhatIsAlreadyHeld() {
        // A big Standard screen of forty-eight monitors needs three gigabytes: a six-gigabyte card holds it.
        final VramLedger six = new VramLedger(6L * 1024L * 1024L);
        six.add("Wall", 48L * VramLedger.monitorKbPerBlock(HardwareEra.STANDARD, true), VramLedger.Kind.MONITOR);
        assertEquals(3L * 1024L * 1024L, six.usedKb());
        assertTrue(six.fits(1024L));
        // A two-gigabyte card does not.
        final VramLedger two = new VramLedger(2L * 1024L * 1024L);
        assertFalse(two.fits(48L * VramLedger.monitorKbPerBlock(HardwareEra.STANDARD, true)));
    }

    @Test
    void usedKb_splitsMonitorsFromWindows() {
        final VramLedger ledger = new VramLedger(512L * 1024L);
        ledger.add("Desk", 64L * 1024L, VramLedger.Kind.MONITOR);
        ledger.add("Paint", 16L * 1024L, VramLedger.Kind.WINDOW);
        ledger.add("nothing", 0L, VramLedger.Kind.WINDOW);
        assertEquals(64L * 1024L, ledger.usedKb(VramLedger.Kind.MONITOR));
        assertEquals(16L * 1024L, ledger.usedKb(VramLedger.Kind.WINDOW));
        assertEquals(2, ledger.entries().size(), "nothing is recorded for nothing held");
        assertEquals((512L - 80L) * 1024L, ledger.freeKb());
    }

    @Test
    void label_readsInTheUnitThatFits() {
        assertEquals("64 KB", VramLedger.label(64L));
        assertEquals("16 MB", VramLedger.label(16L * 1024L));
        assertEquals("3.0 GB", VramLedger.label(3L * 1024L * 1024L));
    }
}
