/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ProcessValuesTest {

    @Test
    void bindings_readEveryValueOfTheLanguagesCore() {
        for (final String core : ProgramImage.CORE_VALUES) {
            final int dot = core.indexOf('.');
            assertNotNull(ProcessValues.find(core.substring(0, dot), core.substring(dot + 1), false), core);
        }
    }

    @Test
    void find_keepsTheTypeApartFromItsObjectsAndLeavesTheMachinesValuesAlone() {
        assertNull(ProcessValues.find("Program", "Name", false));
        assertNull(ProcessValues.find("List", "Count", true));
        assertNull(ProcessValues.find("Computer", "Name", true));
    }
}
