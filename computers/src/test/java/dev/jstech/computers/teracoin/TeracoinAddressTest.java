/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class TeracoinAddressTest {

    @Test
    void create_writesAnAddressThatReadsAsOne() {
        final String address = TeracoinAddress.create(new Random(7));

        assertTrue(address.startsWith("trc1q"));
        assertEquals(TeracoinAddress.LENGTH, address.length());
        assertTrue(TeracoinAddress.valid(address));
    }

    @Test
    void valid_refusesLettersOutsideTheAlphabet() {
        assertTrue(TeracoinAddress.valid("trc1q8f3k2w9xv7m4d0s6l2c8hz5r3n7h4x"));
        assertFalse(TeracoinAddress.valid("trc1q8f3k2w9xv7m4d0s6l2c8hz5r3n7h4b"));
        assertFalse(TeracoinAddress.valid("btc1q8f3k2w9xv7m4d0s6l2c8hz5r3n7h4x"));
        assertFalse(TeracoinAddress.valid("trc1q8f3k"));
    }

    @Test
    void key_isNeverAnAddress() {
        final String key = TeracoinAddress.key(new Random(3));

        assertTrue(TeracoinAddress.validKey(key));
        assertFalse(TeracoinAddress.valid(key));
    }

    @Test
    void shortForm_keepsTheFirstEightAndTheLastFour() {
        assertEquals("trc1q8f3...7h4x", TeracoinAddress.shortForm("trc1q8f3k2w9xv7m4d0s6l2c8hz5r3n7h4x"));
    }
}
