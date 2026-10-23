/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TraceGradeTest {

    @Test
    void share_countsATraceAtAThousandTimesItsRealGrade() {
        assertEquals(0.01, TraceGrade.share(0.000_01), 1e-12);
        assertEquals(0.001, TraceGrade.share(0.000_001), 1e-12);
    }

    @Test
    void share_neverCountsATraceAtMoreThanATenth() {
        assertEquals(TraceGrade.MOST, TraceGrade.share(0.03));
    }

    @Test
    void share_refusesAGradeOutsideTheMineral() {
        assertThrows(IllegalArgumentException.class, () -> TraceGrade.share(0.0));
        assertThrows(IllegalArgumentException.class, () -> TraceGrade.share(1.0));
    }
}
