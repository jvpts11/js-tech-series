/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LiveProgressTest {

    @Test
    void startOver_forgetsTheChoicesMadeAgainstTheOldBase() {
        final LiveProgress progress = new LiveProgress();
        progress.profile = 3;
        progress.worldUpdated = true;
        progress.kernelChosen = true;
        progress.sources = true;

        progress.startOver();

        assertEquals(0, progress.profile);
        assertFalse(progress.worldUpdated);
        assertFalse(progress.kernelChosen);
        assertFalse(progress.sources);
    }

    @Test
    void startOver_keepsTheHardwareClockSetting() {
        final LiveProgress progress = new LiveProgress();
        progress.clockSet = true;

        progress.startOver();

        assertTrue(progress.clockSet);
    }
}
