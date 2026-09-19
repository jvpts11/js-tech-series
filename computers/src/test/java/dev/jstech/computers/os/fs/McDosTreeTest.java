/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class McDosTreeTest {

    @Test
    void entries_putTheSystemAtTheRootAndItsToolsUnderDos() {
        final List<String> paths = paths(facts());
        assertTrue(paths.containsAll(List.of("DOS", "AUTOEXEC.BAT", "COMMAND.COM", "CONFIG.SYS", "MCDOS.SYS",
                "DOS/HIMEM.SYS", "DOS/README.TXT")), paths.toString());
        assertEquals(7, paths.size());
    }

    @Test
    void entries_giveEachProgramADirectoryWithItsExecutableAndItsNotes() {
        final List<String> paths = paths(facts("SCC", "VIM"));
        assertTrue(paths.containsAll(List.of("SCC", "SCC/SCC.EXE", "SCC/README.TXT", "VIM", "VIM/VIM.EXE",
                "VIM/README.TXT")), paths.toString());
    }

    @Test
    void entries_listEveryFolderBeforeAnyFile() {
        boolean fileSeen = false;
        for (final InstallerLayout.Entry entry : McDosTree.entries(facts("SCC"))) {
            assertFalse(entry.directory() && fileSeen, entry.path() + " is a folder listed after a file");
            fileSeen |= !entry.directory();
        }
    }

    @Test
    void nameOf_keepsEightCapitalsAndDropsWhatThatFilesystemCouldNotHold() {
        assertEquals("SCC", McDosTree.nameOf("scc"));
        assertEquals("MCDOS", McDosTree.nameOf("mc_dos"));
        assertEquals("IQLENGIN", McDosTree.nameOf("iqlengine"));
    }

    @Test
    void text_namesOnlyTheSystemsToolsInThePathOfAFreshSystem() {
        assertEquals("@ECHO OFF\nPROMPT $P$G\nPATH C:\\DOS\n", McDosTree.text(facts(), McDosTree.AUTOEXEC).get());
    }

    @Test
    void text_addsEveryProgramsDirectoryToThePathInTheOrderTheyWereInstalled() {
        assertTrue(McDosTree.text(facts("VIM", "SCC"), McDosTree.AUTOEXEC).get()
                .contains("PATH C:\\DOS;C:\\VIM;C:\\SCC\n"));
    }

    @Test
    void text_loadsTheMemoryDriverTheTreeReallyHolds() {
        final String config = McDosTree.text(facts(), McDosTree.CONFIG).get();
        assertTrue(config.startsWith("DEVICE=C:\\DOS\\HIMEM.SYS\n"), config);
        assertTrue(paths(facts()).contains(McDosTree.MEMORY_DRIVER));
    }

    @Test
    void text_signsTheReadmeAndNamesTheSystemFile() {
        final String readme = McDosTree.text(facts(), McDosTree.README).get();
        assertTrue(readme.startsWith("MC-DOS\n(c) 1988 Midsoft\n"), readme);
        assertTrue(readme.contains("MCDOS.SYS and COMMAND.COM"), readme);
    }

    @Test
    void text_keepsEveryLineOfTheReadmeInsideAnEightyColumnScreen() {
        for (final String line : McDosTree.text(facts(), McDosTree.README).get().split("\n")) {
            assertTrue(line.length() <= 78, line);
        }
    }

    @Test
    void text_hasNothingToSayForAFileThatIsNotText() {
        assertTrue(McDosTree.text(facts(), McDosTree.COMMAND).isEmpty());
        assertTrue(McDosTree.text(facts("SCC"), "SCC/SCC.EXE").isEmpty());
        assertTrue(McDosTree.text(facts(), "NOTHING.TXT").isEmpty());
    }

    private static McDosTree.Facts facts(final String... programs) {
        return new McDosTree.Facts("MC-DOS", "(c) 1988 Midsoft", "MCDOS.SYS", List.of(programs));
    }

    private static List<String> paths(final McDosTree.Facts facts) {
        return McDosTree.entries(facts).stream().map(InstallerLayout.Entry::path).toList();
    }
}
