/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramEntryTest {

    /** A program that runs nothing, going by the name it was given. */
    private record Quiet(String name) implements IProgramRuntime {

        @Override
        public int exitCode() {
            return 0;
        }
    }

    @Test
    void name_isTheProgramsOwnOrTheRuntimesWhenItGaveNone() {
        final ProgramEntry<Quiet> named = new ProgramEntry<>(1, "a.asm", "", 1, new Quiet("Sorter"),
                IProgramParent.NONE, List.of(), ProgramPriority.MEDIUM);
        final ProgramEntry<Quiet> nameless = new ProgramEntry<>(2, "b.asm", "", 1, new Quiet(" "),
                IProgramParent.NONE, List.of(), ProgramPriority.MEDIUM);

        assertEquals("Sorter", named.name());
        assertEquals(ProgramEntry.RUNTIME_NAME, nameless.name());
    }

    @Test
    void entry_takesNoParentNoArgumentsAndMediumForWhatWasLeftOut() {
        final ProgramEntry<Quiet> bare = new ProgramEntry<>(1, "a.asm", "", 1, new Quiet(""), null, null, null);

        assertSame(IProgramParent.NONE, bare.parent());
        assertEquals(List.of(), bare.args());
        assertEquals(ProgramPriority.MEDIUM, bare.priority());
    }
}
