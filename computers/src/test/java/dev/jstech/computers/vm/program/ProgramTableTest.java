/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramTableTest {

    /** A program that runs nothing, going by the name it was given. */
    private record Quiet(String name) implements IProgramRuntime {

        @Override
        public int exitCode() {
            return 0;
        }
    }

    /** A program under the table's next number, holding that much memory. */
    private static ProgramEntry<Quiet> entry(final ProgramTable<Quiet> table, final String name, final int heapMb) {
        return new ProgramEntry<>(table.takeId(), name + ".asm", "", heapMb, new Quiet(name), IProgramParent.NONE,
                List.of(), ProgramPriority.MEDIUM);
    }

    @Test
    void add_listsProgramsByNumberInTheOrderTheyStarted() {
        final ProgramTable<Quiet> table = new ProgramTable<>();
        final ProgramEntry<Quiet> first = entry(table, "first", 1);
        final ProgramEntry<Quiet> second = entry(table, "second", 1);

        table.add(first);
        table.add(second);

        assertSame(second, table.byId(second.id()));
        assertEquals(List.of(first, second), table.all());
        assertEquals(List.of(1, 2), table.running().stream().map(ProgramEntry::id).toList());
    }

    @Test
    void remove_takesAProgramOutWithoutGivingItsNumberToTheNext() {
        final ProgramTable<Quiet> table = new ProgramTable<>();
        final ProgramEntry<Quiet> first = entry(table, "first", 1);
        final ProgramEntry<Quiet> second = entry(table, "second", 1);
        table.add(first);
        table.add(second);

        assertTrue(table.remove(first.id()));

        assertNull(table.byId(first.id()));
        assertFalse(table.remove(first.id()), "it was already gone");
        assertEquals(List.of(second), table.all());
        assertEquals(3, table.takeId(), "a number once given is not given again");
    }

    @Test
    void heapMb_addsUpWhatEveryProgramWasGiven() {
        final ProgramTable<Quiet> table = new ProgramTable<>();
        table.add(entry(table, "small", 1));
        table.add(entry(table, "large", 4));

        assertEquals(5, table.heapMb());
        assertEquals(2, table.size());
    }

    @Test
    void restart_emptiesTheTableAndNumbersOnFromWhereTheSaveLeftOff() {
        final ProgramTable<Quiet> table = new ProgramTable<>();
        table.add(entry(table, "before", 1));

        table.restart(7);

        assertTrue(table.isEmpty());
        assertEquals(7, table.nextId());
        assertEquals(7, table.takeId());
        table.restart(0);
        assertEquals(1, table.takeId(), "a save with no number starts from one");
    }

    @Test
    void running_followsTheTableAndChangesNothing() {
        final ProgramTable<Quiet> table = new ProgramTable<>();
        final Collection<ProgramEntry<Quiet>> running = table.running();

        table.add(entry(table, "later", 1));

        assertEquals(1, running.size());
        assertThrows(UnsupportedOperationException.class, running::clear);
    }
}
