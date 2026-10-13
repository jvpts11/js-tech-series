/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class InterfaceScriptTest {

    @Test
    void iql_writesTheModeTheMostJobsAndTheState() {
        assertEquals(List.of("SET INTERFACE 'Kiln A' EXCLUSIVE ON", "SET INTERFACE 'Kiln A' MAX JOBS 4",
                "PAUSE INTERFACE 'Kiln A'"), InterfaceScript.iql("Kiln A", true, 4, true));
    }

    @Test
    void iql_writesNoMostAsAuto() {
        assertEquals("SET INTERFACE 'x' MAX JOBS AUTO", InterfaceScript.iql("x", false, 0, false).get(1));
        assertEquals("RESUME INTERFACE 'x'", InterfaceScript.iql("x", false, 0, false).get(2));
    }

    @Test
    void iql_doublesAQuoteInTheName() {
        assertEquals("SET INTERFACE 'Joe''s kiln' EXCLUSIVE OFF", InterfaceScript.iql("Joe's kiln", false, 0, false)
                .get(0));
    }

    @Test
    void sigma_writesTheSameSettingsAsCalls() {
        assertEquals(List.of("var i = craftInterface(\"Kiln \\\"A\\\"\");", "i.Exclusive(false).MaxJobs(2);",
                "i.Resume();"), InterfaceScript.sigma("Kiln \"A\"", false, 2, false));
        assertEquals("i.Pause();", InterfaceScript.sigma("x", true, -1, true).get(2));
        assertEquals("i.Exclusive(true).MaxJobs(0);", InterfaceScript.sigma("x", true, -1, true).get(1));
    }

    @Test
    void route_namesThePatternTheInputAndTheRouter() {
        assertEquals("SET INTERFACE 'Mixer' ROUTE 'Coarse dirt' INPUT gravel TO ROUTER 'North'",
                InterfaceScript.route("Mixer", "Coarse dirt", "gravel", "North"));
    }
}
