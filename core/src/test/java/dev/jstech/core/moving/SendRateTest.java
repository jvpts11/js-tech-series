/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SendRateTest {

    @Test
    void every_tellsNearPlayersEveryTickAndFarOnesLessOften() {
        assertEquals(1, SendRate.every(10));
        assertEquals(2, SendRate.every(100));
        assertEquals(4, SendRate.every(200));
        assertEquals(SendRate.LONGEST, SendRate.every(100_000));
    }
}
