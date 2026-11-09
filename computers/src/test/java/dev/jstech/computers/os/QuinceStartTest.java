/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.QuinceBoards;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuinceStartTest {

    @Test
    void of_followsTheEraOfAQuinceBoard() {
        assertEquals(QuinceStart.ROM, QuinceStart.of(QuinceBoards.RUSSET_128K, HardwareEra.VINTAGE));
        assertEquals(QuinceStart.OPEN_FIRMWARE, QuinceStart.of(QuinceBoards.QRUSSET_G3, HardwareEra.LEGACY));
        assertEquals(QuinceStart.EFI, QuinceStart.of(QuinceBoards.QRUSSET_TRANSITION, HardwareEra.TRANSITION));
        assertEquals(QuinceStart.EFI, QuinceStart.of(QuinceBoards.MAINFRAME_STANDARD, HardwareEra.STANDARD));
        assertEquals(QuinceStart.CHIP, QuinceStart.of(QuinceBoards.QRUSSET_ADVANCED, HardwareEra.ADVANCED));
    }

    @Test
    void of_isNoneForAnyOtherBoard() {
        assertEquals(QuinceStart.NONE, QuinceStart.of(FormFactor.ATX, HardwareEra.STANDARD));
        assertEquals(QuinceStart.NONE, QuinceStart.of(FormFactor.EEB, HardwareEra.ADVANCED));
    }

    @Test
    void hasChoices_onEveryStartButTheRom() {
        assertFalse(QuinceStart.ROM.hasChoices());
        assertFalse(QuinceStart.NONE.hasChoices());
        assertTrue(QuinceStart.OPEN_FIRMWARE.hasChoices());
        assertTrue(QuinceStart.EFI.hasChoices());
        assertTrue(QuinceStart.CHIP.hasChoices());
    }

    @Test
    void byId_readsBackEveryStartAndNoneForAnUnknownOne() {
        for (final QuinceStart start : new QuinceStart[] {QuinceStart.NONE, QuinceStart.ROM,
                QuinceStart.OPEN_FIRMWARE, QuinceStart.EFI, QuinceStart.CHIP}) {
            assertEquals(start, QuinceStart.byId(start.id()));
        }
        assertEquals(QuinceStart.NONE, QuinceStart.byId(99));
    }

    @Test
    void isQuince_holdsTheSixteenBoardsOnly() {
        assertEquals(16, QuinceBoards.ALL.size());
        assertTrue(QuinceBoards.isQuince(QuinceBoards.QSERVE_LEGACY));
        assertFalse(QuinceBoards.isQuince(FormFactor.MTX));
    }
}
