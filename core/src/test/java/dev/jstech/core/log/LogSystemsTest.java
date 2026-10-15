/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.core.util.DeclarationError;
import org.junit.jupiter.api.Test;

class LogSystemsTest {

    @Test
    void declare_namesTheSystemForCommandsAndForTheLog() {
        final LogSystem system = LogSystems.declare("logtest", "naming");

        assertEquals("logtest:naming", system.id());
        assertEquals("logtest/naming", system.loggerName());
    }

    @Test
    void byId_findsADeclaredSystemAndNothingElse() {
        final LogSystem system = LogSystems.declare("logtest", "finding");

        assertSame(system, LogSystems.byId("logtest:finding").orElseThrow());
        assertTrue(LogSystems.byId("logtest:never_declared").isEmpty());
        assertTrue(LogSystems.all().contains(system));
    }

    @Test
    void declare_refusesTheSameSystemTwice() {
        LogSystems.declare("logtest", "twice");

        final DeclarationError error = assertThrows(DeclarationError.class,
                () -> LogSystems.declare("logtest", "twice"));
        assertEquals("logtest", error.mod());
    }

    @Test
    void declare_refusesANameTheCommandCouldNotTake() {
        assertThrows(DeclarationError.class, () -> LogSystems.declare("logtest", "Has Spaces"));
        assertThrows(DeclarationError.class, () -> LogSystems.declare("Log Test", "fine"));
    }

    @Test
    void byName_readsALevelInAnyCase() {
        assertEquals(LogLevel.DEBUG, LogLevel.byName("Debug").orElseThrow());
        assertEquals("trace", LogLevel.TRACE.serializedName());
        assertTrue(LogLevel.byName("loud").isEmpty());
    }
}
