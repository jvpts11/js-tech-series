/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotherboardSpecTest {

    @Test
    void hasOnBoardAudio_fromTheTransitionOn() {
        assertTrue(board(HardwareEra.TRANSITION).hasOnBoardAudio());
        assertTrue(board(HardwareEra.STANDARD).hasOnBoardAudio());
        assertTrue(board(HardwareEra.ADVANCED).hasOnBoardAudio());
    }

    @Test
    void hasOnBoardAudio_notBeforeTheTransition() {
        assertFalse(board(HardwareEra.VINTAGE).hasOnBoardAudio());
        assertFalse(board(HardwareEra.LEGACY).hasOnBoardAudio());
    }

    @Test
    void constructor_refusesNoMemoryGeneration() {
        assertThrows(IllegalArgumentException.class, () -> new MotherboardSpec(FormFactor.ATX,
                HardwareEra.TRANSITION, CpuSocketId.LGA_775, 1, Set.of(), 4, PcieGeneration.PCIE_1_0, 2, 4));
    }

    private static MotherboardSpec board(final HardwareEra era) {
        return new MotherboardSpec(FormFactor.ATX, era, CpuSocketId.LGA_775, 1, Set.of(RamGeneration.DDR2), 4,
                PcieGeneration.PCIE_1_0, 2, 4);
    }
}
