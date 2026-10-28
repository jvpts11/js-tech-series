/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChecklistLinesTest {

    @Test
    void check_splitsWhatToLookAtFromWhatItShouldBeAtTheDots() {
        assertEquals(new ChecklistLines.Line("Rocket", "on pad", false), ChecklistLines.check("[ ] Rocket ... on pad"));
        assertEquals(new ChecklistLines.Line("T-0", "go", false), ChecklistLines.check("T-0 ........ go"));
    }

    @Test
    void check_withNoDotsHasNoState() {
        assertEquals(new ChecklistLines.Line("Climb", "", false), ChecklistLines.check("[ ] Climb"));
    }

    @Test
    void check_aMarkedBoxIsALineTheRevisionChanged() {
        assertEquals(new ChecklistLines.Line("Cryo tanks", "venting", true),
                ChecklistLines.check("[!] Cryo tanks ... venting"));
    }

    @Test
    void check_twoDotsAreNoLeader() {
        assertEquals(new ChecklistLines.Line("Wait.. then go", "", false), ChecklistLines.check("Wait.. then go"));
    }

    @Test
    void step_splitsTheNameFromWhatItIsAtTheFirstColon() {
        assertEquals(new ChecklistLines.Line("Ignition", "engines lit: deluge to steam", false),
                ChecklistLines.step("[ ] Ignition: engines lit: deluge to steam"));
        assertEquals(new ChecklistLines.Line("In orbit", "", true), ChecklistLines.step("[!] In orbit"));
    }

    @Test
    void boxed_isTrueOnlyForAnItemOpeningWithABox() {
        assertTrue(ChecklistLines.boxed("[ ] Rocket ... on pad"));
        assertTrue(ChecklistLines.boxed("[!] Hoses"));
        assertFalse(ChecklistLines.boxed("Rocket [ ] on pad"));
        assertFalse(ChecklistLines.boxed("[x] Done"));
    }
}
