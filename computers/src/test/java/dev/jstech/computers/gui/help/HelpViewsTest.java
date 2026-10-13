/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class HelpViewsTest {

    @Test
    void dos_carriesTheSystemAndWhatWasTyped() {
        final String view = HelpViews.dos("mc_net", "copy a b");

        assertTrue(HelpViews.namesDos(view) && HelpViews.names(view));
        assertEquals("mc_net", HelpViews.dosSystem(view));
        assertEquals("copy a b", HelpViews.dosTopic(view));
        assertEquals("", HelpViews.dosTopic(HelpViews.dos("mc_dos", null)));
    }

    @Test
    void infoAndManual_carryWhatWasAsked() {
        assertEquals("graphics cards", HelpViews.infoTopic(HelpViews.info("graphics cards")));
        assertEquals("jsc:graphics_cards", HelpViews.manualEntry(HelpViews.manual("jsc:graphics_cards")));
        assertTrue(HelpViews.names(HelpViews.info("")) && HelpViews.names(HelpViews.manual("x:y")));
        assertFalse(HelpViews.names("C:\\README.TXT"));
        assertFalse(HelpViews.names(null));
    }

    @Test
    void target_isReadBackAsItWasWritten() {
        for (final HelpTarget target : new HelpTarget[] {HelpTarget.node("jscore:technical_reference", "jsc:gpu"),
                HelpTarget.command("dir"), HelpTarget.contents(""), HelpTarget.index("jsc:guide")}) {
            assertEquals(Optional.of(target), HelpTarget.read(target.written()));
        }
        assertEquals(Optional.empty(), HelpTarget.read("nothing|at|all"));
        assertEquals(Optional.empty(), HelpTarget.read("node|only"));
    }
}
