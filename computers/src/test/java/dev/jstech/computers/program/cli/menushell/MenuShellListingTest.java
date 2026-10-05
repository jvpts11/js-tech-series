/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class MenuShellListingTest {

    @Test
    void writeThenRead_givesBackTheSameListing() {
        final MenuShellListing listing = new MenuShellListing("C:\\DOS", true, true,
                List.of(new MenuShellListing.Drive('A', false, 0L, 0L),
                        new MenuShellListing.Drive('C', true, 2_000L, 1_250L)),
                List.of("C:\\DOS", "C:\\SIGMA"),
                List.of(new MenuShellListing.Entry("README", "TXT", 3L, "09-29-87  10:42", false, true),
                        new MenuShellListing.Entry("SUB", "", 0L, "09-29-87  10:42", true, false)),
                List.of(new MenuShellListing.Program("Sigma Runtime", "sigma")),
                List.of("C:\\DOS\\README.TXT"));
        assertEquals(listing, MenuShellListing.read(listing.write()));
    }

    @Test
    void write_turnsATabInsideANameIntoASpace() {
        final MenuShellListing listing = new MenuShellListing("/usr/player", true, false, List.of(), List.of(),
                List.of(new MenuShellListing.Entry("odd\tname", "", 1L, "", false, false)), List.of(), List.of());
        assertEquals("odd name", MenuShellListing.read(listing.write()).entries().get(0).name());
    }

    @Test
    void read_leavesOutLinesItCannotRead() {
        final MenuShellListing read = MenuShellListing.read("D\tC:\\\t1\t0\nX\tsomething newer\nF\ttoo\tfew\n");
        assertEquals("C:\\", read.dir());
        assertTrue(read.found());
        assertFalse(read.printer());
        assertTrue(read.entries().isEmpty());
    }

    @Test
    void names_tellAViewFromASearch() {
        assertEquals("C:\\DOS", MenuShellListing.viewed(MenuShellListing.view("C:\\DOS")));
        assertEquals("/usr/player", MenuShellListing.viewed(MenuShellListing.view("/usr/player")));
        assertEquals("*.TXT", MenuShellListing.searched(MenuShellListing.search("*.TXT")));
        assertNull(MenuShellListing.searched(MenuShellListing.view("C:\\")));
        assertNull(MenuShellListing.viewed("C:\\AUTOEXEC.BAT"));
    }

    @Test
    void fullName_putsTheExtensionAfterADot() {
        assertEquals("AUTOEXEC.BAT", new MenuShellListing.Entry("AUTOEXEC", "BAT", 1L, "", false, false).fullName());
        assertEquals("DOS", new MenuShellListing.Entry("DOS", "", 0L, "", true, false).fullName());
    }
}
