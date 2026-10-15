/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.peripheral;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PeripheralCableTypeTest {

    @Test
    void computing_hasMaxLength16() {
        // Computing (peripheral) cable has a short range.
        assertEquals(16, PeripheralCableType.COMPUTING.maxLength());
        assertEquals("peripheral_cable",
                PeripheralCableType.COMPUTING.translationKey());
    }

    @Test
    void industrialControl_hasMaxLength8() {
        // Industrial control cable has a very short range.
        assertEquals(8, PeripheralCableType.INDUSTRIAL_CONTROL.maxLength());
        assertEquals("industrial_peripheral_cable",
                PeripheralCableType.INDUSTRIAL_CONTROL.translationKey());
    }

    @Test
    void exactlyTwoTypes_existInTheMod() {
        // Telemetry left the point-to-point systems for a line of its own.
        assertEquals(2, PeripheralCableType.values().length);
    }
}
