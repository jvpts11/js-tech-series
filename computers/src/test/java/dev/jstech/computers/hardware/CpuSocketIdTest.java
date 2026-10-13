/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CpuSocketIdTest {

    @Test
    void display_isThePathAsABoardWritesIt() {
        assertEquals("LGA_1155", CpuSocketId.LGA_1155.display());
        assertEquals("G34", CpuSocketId.G34.display());
    }

    @Test
    void display_spellsThePlusOfASocketNamedForTheOneBeforeIt() {
        assertEquals("AM3+", CpuSocketId.AM3_PLUS.display());
        assertEquals("FM2+", CpuSocketId.FM2_PLUS.display());
    }

    @Test
    void equals_tellsASocketFromTheOneItFollows() {
        assertNotEquals(CpuSocketId.AM3, CpuSocketId.AM3_PLUS);
    }

    @Test
    void constructor_refusesAnIdWithoutANamespace() {
        assertThrows(IllegalArgumentException.class, () -> new CpuSocketId("am3"));
    }
}
