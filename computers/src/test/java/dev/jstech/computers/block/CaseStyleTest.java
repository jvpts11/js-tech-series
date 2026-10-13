/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.jstech.core.tier.HardwareEra;
import org.junit.jupiter.api.Test;

/** A case's name in the models: the age alone for the one case of its age, the age and the style from Standard on. */
class CaseStyleTest {

    @Test
    void caseName_ofTheSoleCase_isTheAgeAlone() {
        assertEquals("vintage", CaseStyle.SOLE.caseName(HardwareEra.VINTAGE));
        assertEquals("transition", CaseStyle.SOLE.caseName(HardwareEra.TRANSITION));
    }

    @Test
    void caseName_ofAStyle_isTheAgeThenTheStyle() {
        assertEquals("standard_neutral", CaseStyle.NEUTRAL.caseName(HardwareEra.STANDARD));
        assertEquals("standard_high_performance", CaseStyle.HIGH_PERFORMANCE.caseName(HardwareEra.STANDARD));
        assertEquals("advanced_aesthetic", CaseStyle.AESTHETIC.caseName(HardwareEra.ADVANCED));
    }
}
