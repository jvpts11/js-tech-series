/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InteracStateTest {

    @Test
    void path_andBack_saysTheSameThing() {
        final InteracState state = new InteracState(2, 5, "oak log", "get", 42L);

        assertEquals(state, InteracState.of(state.path()));
    }

    @Test
    void path_keepsASearchWithSpacesAndColonsWhole() {
        final InteracState state = InteracState.OPENING.searchingFor("minecraft:oak log");

        assertEquals("minecraft:oak log", InteracState.of(state.path()).search());
    }

    @Test
    void of_anythingElseOpensTheViewWhereItOpens() {
        assertEquals(InteracState.OPENING, InteracState.of("notes.txt"));
        assertEquals(InteracState.OPENING, InteracState.of(null));
        assertEquals(InteracState.OPENING, InteracState.of("interac:nonsense"));
    }

    @Test
    void names_knowsOneOfTheseFromAFile() {
        assertTrue(InteracState.names(InteracState.OPENING.path()));
        assertFalse(InteracState.names("notes.txt"));
    }

    @Test
    void onTab_walksRoundAndStartsTheRowsAgain() {
        final InteracState last = InteracState.OPENING.picking(7).onTab(InteracState.TAB_STARRED);

        assertEquals("Starred", last.tabName());
        assertEquals(0, last.selected(), "another tab is other rows, so nothing is picked yet");
        assertEquals("Network", last.onTab(InteracState.TABS.length).tabName(), "and it walks round");
        assertEquals("Starred", last.onTab(-1).tabName(), "the other way too");
    }

    @Test
    void asking_andDone_holdWhatWasAskedAndThenLetItGo() {
        final InteracState asked = InteracState.OPENING.picking(3).asking("GET", 64L);

        assertEquals("get", asked.action(), "what was asked is held without regard to case");
        assertEquals(64L, asked.amount());
        assertEquals(3, asked.selected(), "and the row stays picked");
        assertEquals("", asked.done().action());
    }

    @Test
    void searchingFor_startsTheRowsAgainFromTheTop() {
        assertEquals(0, InteracState.OPENING.picking(9).searchingFor("stone").selected());
    }
}
