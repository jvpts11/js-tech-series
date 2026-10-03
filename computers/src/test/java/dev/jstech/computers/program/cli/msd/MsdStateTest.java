/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.msd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MsdStateTest {

    @Test
    void path_readsBackAsTheSameState() {
        final MsdState state = MsdState.OPENING.on(90, 30).openingPorts(2).asking("disable");

        assertEquals(state, MsdState.of(state.path()));
    }

    @Test
    void path_holdsNoSlash() {
        assertFalse(MsdState.OPENING.openingPorts(1).asking("enable").path().contains("/"),
                "a slash would read as a folder to whatever works out which file is asked for");
    }

    @Test
    void of_opensWhereItOpensOnANameItCannotRead() {
        assertEquals(MsdState.OPENING, MsdState.of("msd:nonsense"));
        assertEquals(MsdState.OPENING, MsdState.of("interac:0:0::0:80:24:"));
        assertEquals(MsdState.OPENING, MsdState.of(null));
    }

    @Test
    void constructor_keepsThePickAndTheGlassSensible() {
        final MsdState state = new MsdState(true, -3, "DISABLE", 0, -1);

        assertEquals(0, state.picked());
        assertEquals("disable", state.action());
        assertEquals(MsdState.DEFAULT_COLUMNS, state.columns());
        assertEquals(MsdState.DEFAULT_ROWS, state.rows());
    }

    @Test
    void closingPorts_putsTheDialogAwayOnTheButtonThatOpenedIt() {
        final MsdState closed = MsdState.OPENING.openingPorts(1).asking("enable").closingPorts(MsdScreen.COM_BUTTON);

        assertFalse(closed.ports());
        assertEquals(MsdScreen.COM_BUTTON, closed.picked());
        assertTrue(closed.action().isEmpty(), "nothing is asked of the main screen");
    }
}
