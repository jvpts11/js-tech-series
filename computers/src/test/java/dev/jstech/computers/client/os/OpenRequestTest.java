/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenRequestTest {

    @Test
    void typeAtTerminal_keepsTheLinesItWasGivenWhateverHappensToTheList() {
        final List<String> lines = new ArrayList<>(List.of("cd /", "ls"));
        final OpenRequest.TypeAtTerminal request = new OpenRequest.TypeAtTerminal(lines);
        lines.add("rm -rf /");
        assertEquals(List.of("cd /", "ls"), request.lines());
    }

    @Test
    void typeAtTerminal_linesCannotBeChangedOnceQueued() {
        final OpenRequest.TypeAtTerminal request = new OpenRequest.TypeAtTerminal(List.of("ls"));
        assertThrows(UnsupportedOperationException.class, () -> request.lines().add("rm"));
    }

    @Test
    void openFile_inTheDefaultProgramIsNotTheSameRequestAsInANamedOne() {
        assertNotEquals(new OpenRequest.OpenFile("", "notes.txt"), new OpenRequest.OpenFile("editor", "notes.txt"));
        assertEquals(new OpenRequest.OpenFile("", "notes.txt"), new OpenRequest.OpenFile("", "notes.txt"));
    }

    @Test
    void requests_forTheSamePathButOfDifferentKindsAreDifferent() {
        assertNotEquals(new OpenRequest.ChooseOpener("a.txt"), new OpenRequest.Properties("a.txt"));
        assertNotEquals(new OpenRequest.FilesAt("a"), new OpenRequest.Program("a"));
        assertNotEquals(new OpenRequest.RunAtTerminal("a.sig"), new OpenRequest.OpenFile("", "a.sig"));
    }
}
