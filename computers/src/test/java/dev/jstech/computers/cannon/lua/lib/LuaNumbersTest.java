/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class LuaNumbersTest {

    @Test
    void format_writesWholeNumbersPlainAndRealsWithTheirPoint() {
        assertEquals("3", LuaNumbers.format(3L));
        assertEquals("3.0", LuaNumbers.format(3.0));
        assertEquals("0.1", LuaNumbers.format(0.1));
        assertEquals("3.1415926535898", LuaNumbers.format(Math.PI));
        assertEquals("1e+15", LuaNumbers.format(1e15));
        assertEquals("1e-05", LuaNumbers.format(0.00001));
        assertEquals("-0.0", LuaNumbers.format(-0.0));
        assertEquals("inf", LuaNumbers.format(Double.POSITIVE_INFINITY));
    }

    @Test
    void parse_readsTheLanguagesNumbersAndNothingElse() {
        assertEquals(10L, LuaNumbers.parse(" 10 "));
        assertEquals(-2.5, LuaNumbers.parse("-2.5"));
        assertEquals(255L, LuaNumbers.parse("0xFF"));
        assertEquals(1e10, LuaNumbers.parse("1e10"));
        assertNull(LuaNumbers.parse("10d"));
        assertNull(LuaNumbers.parse("Infinity"));
        assertNull(LuaNumbers.parse(""));
        assertNull(LuaNumbers.parse("1 2"));
    }

    @Test
    void parse_readsAWholeNumberInAnyBase() {
        assertEquals(5L, LuaNumbers.parse("101", 2));
        assertEquals(35L, LuaNumbers.parse("z", 36));
        assertNull(LuaNumbers.parse("9", 8));
    }

    @Test
    void toInteger_acceptsOnlyRealsWithNoFraction() {
        assertEquals(4L, LuaNumbers.toInteger(4.0));
        assertNull(LuaNumbers.toInteger(4.5));
        assertEquals(7L, LuaNumbers.toInteger("7"));
    }

    @Test
    void modulo_andFloorDivisionFollowTheDivisor() {
        assertEquals(1L, LuaNumbers.modulo(-5L, 3L));
        assertEquals(-1L, LuaNumbers.modulo(5L, -3L));
        assertEquals(-2L, LuaNumbers.floorDivide(-3L, 2L));
        assertEquals(1.5, LuaNumbers.modulo(5.5, 2L));
        assertNull(LuaNumbers.modulo(1L, 0L));
    }

    @Test
    void arithmetic_wrapsWholeNumbersAndPromotesToReal() {
        assertEquals(Long.MIN_VALUE, LuaNumbers.add(Long.MAX_VALUE, 1L));
        assertEquals(3.5, LuaNumbers.add(1L, 2.5));
        assertEquals(2.0, LuaNumbers.divide(4L, 2L));
    }
}
