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

import dev.jstech.computers.os.fs.StoredFile;
import java.util.ArrayList;
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
    void write_staysWithinWhatAFileHolds() {
        final MenuShellListing listing = new MenuShellListing("C:\\", true, false, List.of(), manyFolders(2_000),
                manyEntries(2_000), List.of(), List.of());
        assertTrue(listing.write().length() <= StoredFile.MOST_CHARS, "a listing past a file's cap is refused whole");
    }

    @Test
    void write_keepsTheFilesBeforeTheTree() {
        final MenuShellListing listing = new MenuShellListing("C:\\", true, false, List.of(), manyFolders(2_000),
                manyEntries(40), List.of(), List.of());
        final MenuShellListing read = MenuShellListing.read(listing.write());
        assertEquals(40, read.entries().size(), "every file of the folder is listed");
        assertTrue(read.tree().size() < 2_000, "and it is the far end of the tree that does not fit");
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

    private static List<String> manyFolders(final int count) {
        final List<String> folders = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            folders.add("C:\\PROJECTS\\ARCHIVE" + i);
        }
        return folders;
    }

    private static List<MenuShellListing.Entry> manyEntries(final int count) {
        final List<MenuShellListing.Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new MenuShellListing.Entry("REPORT" + i, "TXT", 1L, "09-29-87  10:42", false, false));
        }
        return entries;
    }
}
