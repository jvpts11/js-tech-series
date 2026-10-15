/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IWrongClassRuleTest {

    @Test
    void standard_burnsOneUpArcsTwoUpAndIsShortBelow() {
        assertEquals(WrongClass.NONE, IWrongClassRule.STANDARD.at(0));
        assertEquals(WrongClass.BURNS, IWrongClassRule.STANDARD.at(1));
        assertEquals(WrongClass.ARC_FLASH, IWrongClassRule.STANDARD.at(2));
        assertEquals(WrongClass.ARC_FLASH, IWrongClassRule.STANDARD.at(4));
        assertEquals(WrongClass.UNDERVOLTAGE, IWrongClassRule.STANDARD.at(-1));
        assertEquals(WrongClass.UNDERVOLTAGE, IWrongClassRule.STANDARD.at(-3));
    }

    @Test
    void refuses_harmsNothing() {
        assertEquals(WrongClass.UNDERVOLTAGE, IWrongClassRule.REFUSES.at(2));
        assertEquals(WrongClass.UNDERVOLTAGE, IWrongClassRule.REFUSES.at(-2));
        assertEquals(WrongClass.NONE, IWrongClassRule.REFUSES.at(0));
    }

    @Test
    void any_takesEveryClass() {
        assertEquals(WrongClass.NONE, IWrongClassRule.ANY.at(3));
        assertEquals(WrongClass.NONE, IWrongClassRule.ANY.at(-3));
    }
}
