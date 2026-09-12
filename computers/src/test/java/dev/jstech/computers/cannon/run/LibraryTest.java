/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.asm.IOperand;
import java.util.List;
import org.junit.jupiter.api.Test;

class LibraryTest {

    private static final int LINE = 7;

    private static Object call(final String owner, final String name, final Object self,
                               final List<Object> arguments) {
        final Library library = new Library(new Heap(64L * 1024), IHost.still());
        return library.call(new IOperand.Method(owner, name, List.of(), "void"), self, arguments, LINE).value();
    }

    private static void assertOutOfRange(final String owner, final String name, final Object self,
                                         final List<Object> arguments) {
        final Halt halt = assertThrows(Halt.class, () -> call(owner, name, self, arguments));
        assertEquals(Halt.Reason.OUT_OF_RANGE, halt.reason(), halt.getMessage());
        assertEquals(LINE, halt.line());
    }

    private static Values.ListValue listOf(final Object... items) {
        final Values.ListValue list = new Values.ListValue();
        list.items().addAll(List.of(items));
        return list;
    }

    @Test
    void substring_takesTheCharactersItIsAskedFor() {
        assertEquals("cde", call("string", "Substring", "abcdef", List.of(2, 3)));
        assertEquals("cdef", call("string", "Substring", "abcdef", List.of(2)));
        assertEquals("", call("string", "Substring", "abcdef", List.of(6)));
        assertEquals("", call("string", "Substring", "abcdef", List.of(6, 0)));
    }

    @Test
    void substring_haltsOutsideTheStringInsteadOfThrowing() {
        assertOutOfRange("string", "Substring", "abc", List.of(4));
        assertOutOfRange("string", "Substring", "abc", List.of(-1));
        assertOutOfRange("string", "Substring", "abc", List.of(1, 3));
        assertOutOfRange("string", "Substring", "abc", List.of(1, -1));
        assertOutOfRange("string", "Substring", "abc", List.of(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }

    @Test
    void insert_putsAValueInsideTheListOrAtItsEnd() {
        final Values.ListValue list = listOf(1, 3);
        call("List", "Insert", list, List.of(1, 2));
        call("List", "Insert", list, List.of(3, 4));
        assertEquals(List.of(1, 2, 3, 4), list.items());
    }

    @Test
    void insert_haltsOutsideTheListInsteadOfThrowing() {
        assertOutOfRange("List", "Insert", listOf(1, 2), List.of(3, 0));
        assertOutOfRange("List", "Insert", listOf(1, 2), List.of(-1, 0));
    }

    @Test
    void removeAt_haltsOutsideTheListInsteadOfThrowing() {
        assertOutOfRange("List", "RemoveAt", listOf(1, 2), List.of(2));
        assertOutOfRange("List", "RemoveAt", listOf(1, 2), List.of(-1));
    }

    @Test
    void sort_ordersAMixedListNumbersFirstWithoutGivingUp() {
        /*
         * Long enough for the sort to check its comparisons against each other, which is where an order
         * that was not one order over every value used to make it throw.
         */
        final Values.ListValue list = new Values.ListValue();
        for (int i = 0; i < 100; i++) {
            list.items().add(100 - i);
            list.items().add("t" + (char) ('z' - i % 26) + i);
        }

        call("List", "Sort", list, List.of());

        final List<Object> sorted = list.items();
        for (int i = 0; i < 100; i++) {
            assertEquals(i + 1, sorted.get(i));
        }
        for (int i = 101; i < sorted.size(); i++) {
            final String before = (String) sorted.get(i - 1);
            final String after = (String) sorted.get(i);
            assertTrue(before.compareTo(after) <= 0, before + " then " + after);
        }
    }
}
