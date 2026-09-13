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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProgramIdentityTest {

    @Test
    void rename_keepsTheNameWithoutTheSpaceAroundItAndABlankOneIsNone() {
        final ProgramIdentity identity = new ProgramIdentity();
        identity.rename("  miner  ");
        assertEquals("miner", identity.name());
        identity.rename(null);
        assertEquals("", identity.name());
    }

    @Test
    void startWith_keepsItsOwnCopyOfTheArguments() {
        final ProgramIdentity identity = new ProgramIdentity();
        final List<String> given = new ArrayList<>(List.of("north", "64"));
        identity.startWith(given);
        given.add("later");
        assertEquals(List.of("north", "64"), identity.args());
        identity.startWith(null);
        assertEquals(List.of(), identity.args());
    }

    @Test
    void exitCode_isWhatTheProgramSaidUnlessItWasHalted() {
        final ProgramIdentity identity = new ProgramIdentity();
        assertFalse(identity.over());
        identity.exit(3);
        assertTrue(identity.exited());
        assertTrue(identity.over());
        assertEquals(3, identity.exitCode());
        identity.halt("out of memory");
        assertEquals(1, identity.exitCode());
        assertEquals(3, identity.givenExitCode());
        assertEquals("out of memory", identity.message());
    }

    @Test
    void spend_countsEveryInstructionRun() {
        final ProgramIdentity identity = new ProgramIdentity();
        identity.spend(64);
        identity.spend(1);
        assertEquals(65, identity.spent());
        assertNull(identity.message());
    }
}
