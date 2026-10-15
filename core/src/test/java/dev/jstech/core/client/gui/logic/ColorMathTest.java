/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ColorMathTest {

    @Test
    void hsv_turnsRoundAndBackToTheSameColour() {
        for (final int colour : new int[] {0xFF39D6C4, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF808080, 0xFF102030}) {
            final double[] hsv = ColorMath.toHsv(colour);
            assertEquals(colour, ColorMath.hsv(hsv[0], hsv[1], hsv[2]), ColorMath.hex(colour));
        }
    }

    @Test
    void hex_isWrittenAndReadAsAPersonWritesIt() {
        assertEquals("#39D6C4", ColorMath.hex(0xFF39D6C4));
        assertEquals(OptionalInt.of(0xFF39D6C4), ColorMath.parse("#39d6c4"));
        assertEquals(OptionalInt.of(0xFF000000), ColorMath.parse("000000"));
        assertTrue(ColorMath.parse("#12345").isEmpty());
        assertTrue(ColorMath.parse("#12345G").isEmpty());
    }
}
