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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CollectionFunctionsTest {

    private static final int LINE = 7;

    /** Calls a method of a list the way a program does, through the call it loaded with. */
    private static Object onList(final Values.ListValue list, final String name, final List<String> parameters,
                                 final Object... arguments) {
        return PureFunctions.REGISTRY.find("List", name, parameters).function()
                .call(new Heap(64L * 1024), list, arguments, LINE);
    }

    private static void assertOutOfRange(final String name, final List<String> parameters, final Object... arguments) {
        final Halt halt = assertThrows(Halt.class, () -> onList(listOf(1, 2), name, parameters, arguments));
        assertEquals(Halt.Reason.OUT_OF_RANGE, halt.reason(), halt.getMessage());
        assertEquals(LINE, halt.line());
    }

    private static Values.ListValue listOf(final Object... items) {
        final Values.ListValue list = new Values.ListValue();
        list.items().addAll(List.of(items));
        return list;
    }

    @Test
    void insert_putsAValueInsideTheListOrAtItsEnd() {
        final Values.ListValue list = listOf(1, 3);
        onList(list, "Insert", List.of("int", "T"), 1, 2);
        onList(list, "Insert", List.of("int", "T"), 3, 4);
        assertEquals(List.of(1, 2, 3, 4), list.items());
    }

    @Test
    void insert_haltsOutsideTheListInsteadOfThrowing() {
        assertOutOfRange("Insert", List.of("int", "T"), 3, 0);
        assertOutOfRange("Insert", List.of("int", "T"), -1, 0);
    }

    @Test
    void removeAt_haltsOutsideTheListInsteadOfThrowing() {
        assertOutOfRange("RemoveAt", List.of("int"), 2);
        assertOutOfRange("RemoveAt", List.of("int"), -1);
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

        onList(list, "Sort", List.of());

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
