/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class TextFunctionsTest {

    private static final int LINE = 7;

    /** Calls Substring on {@code value} the way a program does, through the call it loaded with. */
    private static Object substring(final String value, final Object... arguments) {
        final List<String> parameters = arguments.length == 1 ? List.of("int") : List.of("int", "int");
        return PureFunctions.REGISTRY.find("string", "Substring", parameters).function()
                .call(new Heap(64L * 1024), value, arguments, LINE);
    }

    private static void assertOutOfRange(final String value, final Object... arguments) {
        final Halt halt = assertThrows(Halt.class, () -> substring(value, arguments));
        assertEquals(Halt.Reason.OUT_OF_RANGE, halt.reason(), halt.getMessage());
        assertEquals(LINE, halt.line());
    }

    private static Object format(final Object... arguments) {
        final List<String> parameters = arguments.length == 2 ? List.of("string", "object")
                : List.of("string", "object", "object");
        return PureFunctions.REGISTRY.find("string", "Format", parameters).function()
                .call(new Heap(64L * 1024), null, arguments, LINE);
    }

    @Test
    void format_doesNotReadTextAnArgumentBroughtIn() {
        assertEquals("{1}-x", format("{0}-{1}", "{1}", "x"));
    }

    @Test
    void format_leavesPlaceholdersItHasNoArgumentForAsWritten() {
        assertEquals("a-{1}-{x}-{-1}", format("{0}-{1}-{x}-{-1}", "a"));
    }

    @Test
    void substring_takesTheCharactersItIsAskedFor() {
        assertEquals("cde", substring("abcdef", 2, 3));
        assertEquals("cdef", substring("abcdef", 2));
        assertEquals("", substring("abcdef", 6));
        assertEquals("", substring("abcdef", 6, 0));
    }

    @Test
    void substring_haltsOutsideTheStringInsteadOfThrowing() {
        assertOutOfRange("abc", 4);
        assertOutOfRange("abc", -1);
        assertOutOfRange("abc", 1, 3);
        assertOutOfRange("abc", 1, -1);
        assertOutOfRange("abc", Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
}
