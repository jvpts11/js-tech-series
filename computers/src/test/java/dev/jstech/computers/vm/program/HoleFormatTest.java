/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** A value put in a printf hole the way C puts one. */
class HoleFormatTest {

    private static String put(final String spec, final Object value) {
        final HoleFormat hole = HoleFormat.read(spec);
        assertEquals(spec.charAt(spec.length() - 1), hole.letter(), spec);
        return hole.apply(value);
    }

    @Test
    void read_takesFlagsAWidthAPrecisionAndALetter() {
        assertEquals(new HoleFormat("-0", 8, 3, 'x'), HoleFormat.read("%-08.3x"));
        assertEquals(new HoleFormat("", HoleFormat.NONE, 0, 'f'), HoleFormat.read("%.f"));
        assertNull(HoleFormat.read("%q"));
        assertNull(HoleFormat.read("%5"));
        assertNull(HoleFormat.read("5d"));
        assertNull(HoleFormat.read("%1000d"));
        assertNull(HoleFormat.read("%99999999999d"));
    }

    @Test
    void apply_aWholeNumberIsPaddedSignedAndFilledAsInC() {
        assertEquals("   42", put("%5d", 42));
        assertEquals("42   |", put("%-5d", 42) + "|");
        assertEquals("00042", put("%05d", 42));
        assertEquals("-0042", put("%05d", -42));
        assertEquals("+42", put("%+d", 42));
        assertEquals(" 42", put("% d", 42));
        assertEquals("  007", put("%5.3d", 7));
        assertEquals("  007", put("%05.3d", 7), "a precision turns the zeros off");
        assertEquals("", put("%.0d", 0));
        assertEquals("-9223372036854775808", put("%d", Long.MIN_VALUE));
    }

    @Test
    void apply_theBasesReadTheValueAsNeverBelowZeroAtItsOwnWidth() {
        assertEquals("ff", put("%x", 255));
        assertEquals("FF", put("%X", 255));
        assertEquals("0xff", put("%#x", 255));
        assertEquals("0", put("%#x", 0));
        assertEquals("377", put("%o", 255));
        assertEquals("0377", put("%#o", 255));
        assertEquals("0a7", put("%03x", 167));
        assertEquals("ffffffd6", put("%x", -42));
        assertEquals("ffffffffffffffd6", put("%x", -42L));
        assertEquals("4294967254", put("%u", -42));
    }

    @Test
    void apply_aFractionHasThePrecisionsDecimalsOrIsWrittenAsTheLanguageWritesIt() {
        assertEquals("3.14", put("%.2f", 3.14159));
        assertEquals("  3.14", put("%6.2f", 3.14159));
        assertEquals("003.14", put("%06.2f", 3.14159));
        assertEquals("-3.1", put("%.1f", -3.14));
        assertEquals("     2.5", put("%8f", 2.5));
        assertEquals("       4", put("%8f", 4), "a whole number is written as one");
        assertEquals("0.1", put("%3f", 0.1f), "a float as the float it is");
        assertEquals("2", put("%.0f", 2.5), "the exact value rounded half to even, as C does");
        assertEquals("0.12", put("%.2f", 0.125));
        assertEquals("0.30", put("%.2f", 0.3));
    }

    @Test
    void apply_anExponentHasSixDecimalsUnlessTold() {
        assertEquals("1.234560e+02", put("%e", 123.456));
        assertEquals("1.23E+02", put("%.2E", 123.456));
        assertEquals("-1.0e-03", put("%.1e", -0.001));
        assertEquals("1.0e+01", put("%.1e", 9.99), "rounding carried into another digit");
        assertEquals("0.000000e+00", put("%e", 0.0));
        assertEquals("  inf", put("%5e", Double.POSITIVE_INFINITY));
        assertEquals("NAN", put("%E", Double.NaN));
    }

    @Test
    void apply_textIsCutToThePrecisionAndACharacterIsPadded() {
        assertEquals("Iron Ingot  |", put("%-12s", "Iron Ingot") + "|");
        assertEquals("  Iro", put("%5.3s", "Iron"));
        assertEquals("    a", put("%5c", (int) 'a'));
        assertEquals("a", put("%c", 'a'));
    }
}
