/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProgramPriorityTest {

    @Test
    void named_readsANameInAnyCaseAndTakesMediumForAnythingElse() {
        assertEquals(ProgramPriority.LOW, ProgramPriority.named("low"));
        assertEquals(ProgramPriority.HIGH, ProgramPriority.named(" HIGH "));
        assertEquals(ProgramPriority.MEDIUM, ProgramPriority.named("Medium"));
        assertEquals(ProgramPriority.MEDIUM, ProgramPriority.named(null));
        assertEquals(ProgramPriority.MEDIUM, ProgramPriority.named(""));
        assertEquals(ProgramPriority.MEDIUM, ProgramPriority.named("urgent"));
    }

    @Test
    void serializedName_isWhatASaveHasAlwaysHeld() {
        assertEquals("low", ProgramPriority.LOW.serializedName());
        assertEquals("medium", ProgramPriority.MEDIUM.serializedName());
        assertEquals("high", ProgramPriority.HIGH.serializedName());
    }
}
