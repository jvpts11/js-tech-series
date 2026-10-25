/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.core.jstechseries.operation.OperationPriority;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IqlRunStatementTest {

    @Test
    void parse_readsTheKindAlone() {
        final IqlRunStatement run = IqlRunStatement.parse("RUN jstests:accelerate");

        assertEquals("jstests:accelerate", run.kind());
        assertEquals("", run.machine());
        assertTrue(run.fields().isEmpty());
        assertEquals(OperationPriority.DEFAULT, run.priority());
    }

    @Test
    void parse_readsTheMachineTheFieldsAndThePriority() {
        final IqlRunStatement run = IqlRunStatement.parse(
                "RUN jsindustrial:accel_run ON 'Cyclotron-3fa2c1' WITH beam = 'proton', energy = 20 PRIORITY HIGH");

        assertEquals("jsindustrial:accel_run", run.kind());
        assertEquals("Cyclotron-3fa2c1", run.machine());
        assertEquals(List.of(new IqlRunStatement.Field("beam", "proton"), new IqlRunStatement.Field("energy", 20L)),
                run.fields());
        assertEquals(OperationPriority.HIGH, run.priority());
    }

    /* The clauses come in any order, in any case, and the words of a value may be glued to the comma after it. */
    @Test
    void parse_takesTheClausesInAnyOrder() {
        final IqlRunStatement run = IqlRunStatement.parse("run jstests:accelerate priority low with seconds=3,"
                + "open = true, rate = 1.5 on Beamline");

        assertEquals("Beamline", run.machine());
        assertEquals(OperationPriority.LOW, run.priority());
        assertEquals(List.of(new IqlRunStatement.Field("seconds", 3L), new IqlRunStatement.Field("open", true),
                new IqlRunStatement.Field("rate", 1.5)), run.fields());
    }

    @Test
    void parse_readsAPositionAsTheMachine() {
        assertEquals("12, 64, -3", IqlRunStatement.parse("RUN jstests:accelerate ON '12, 64, -3'").machine());
    }

    @Test
    void asked_listsTheFieldsAsTheLogShowsThem() {
        assertEquals("beam proton, energy 20", IqlRunStatement.parse(
                "RUN jsindustrial:accel_run WITH beam = proton, energy = 20").asked());
        assertEquals("", IqlRunStatement.parse("RUN jstests:accelerate").asked());
    }

    @Test
    void parse_refusesAStatementWithNoKind() {
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN"));
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN accelerate"));
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN JSTESTS:Accelerate"));
    }

    @Test
    void parse_refusesAFieldWithNoValue() {
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN jstests:accelerate WITH seconds"));
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN jstests:accelerate WITH seconds ="));
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN jstests:accelerate WITH seconds = 3,"));
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN jstests:accelerate WITH seconds 3"));
    }

    @Test
    void parse_refusesAClauseOrAFieldSaidTwice() {
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN jstests:accelerate ON a ON b"));
        assertThrows(IqlError.class,
                () -> IqlRunStatement.parse("RUN jstests:accelerate WITH seconds = 1, seconds = 2"));
    }

    @Test
    void parse_refusesWhatComesAfterTheClauses() {
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN jstests:accelerate NOW"));
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN jstests:accelerate ON"));
        assertThrows(IqlError.class, () -> IqlRunStatement.parse("RUN jstests:accelerate PRIORITY SOON"));
    }

    @Test
    void isRun_knowsItsStatementFromTheOthers() {
        assertTrue(IqlRunStatement.isRun("RUN jstests:accelerate"));
        assertTrue(IqlRunStatement.isRun("run x"));
        assertFalse(IqlRunStatement.isRun("SELECT 5 iron_ingot"));
        assertFalse(IqlRunStatement.isRun(""));
    }

    /* The language's own parser hands a RUN to this one, so every surface that speaks it takes one. */
    @Test
    void tryParse_givesARunStatement() {
        final IqlParseResult parsed = IqlParser.tryParse("RUN jstests:accelerate WITH seconds = 3");
        assertTrue(parsed.ok() && parsed.isRun());
        assertEquals("jstests:accelerate", parsed.run().kind());
        assertFalse(IqlParser.tryParse("RUN jstests:accelerate WITH").ok());
    }
}
