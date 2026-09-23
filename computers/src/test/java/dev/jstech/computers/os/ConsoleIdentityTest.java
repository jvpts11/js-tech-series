/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsoleIdentityTest {

    @Test
    void promptOf_givesEachShellItsOwnShape() {
        assertEquals("player@desk ~ %", ConsoleIdentity.promptOf(Platform.LINUX, ShellKind.ZSH, "desk", "~"));
        assertEquals("player@desk:~$", ConsoleIdentity.promptOf(Platform.LINUX, ShellKind.BASH, "desk", "~"));
        assertEquals("player@desk:/etc $", ConsoleIdentity.promptOf(Platform.UNIX, ShellKind.SH, "desk", "/etc"));
    }

    @Test
    void promptOf_givesAConsoleWithNoShellThePlainShape() {
        assertEquals("player@desk:~$", ConsoleIdentity.promptOf(Platform.LINUX, null, "desk", "~"));
    }

    @Test
    void promptOf_standsAtRootsHashOnFreeBsd() {
        assertEquals("root@desk:/etc #", ConsoleIdentity.promptOf(Platform.FREEBSD, ShellKind.SH, "desk", "/etc"));
    }

    @Test
    void promptLineOf_writesFreeBsdsRootAndHostInRedAndTheRestInThePromptsInk() {
        final CliLine prompt = ConsoleIdentity.promptLineOf(Platform.FREEBSD, ShellKind.SH, "desk", "~");
        assertEquals(List.of(new CliSpan("root@desk", CliStyle.RED), new CliSpan(":~ #", CliStyle.PROMPT)),
                prompt.spans());
        assertEquals(CliStyle.ACCENT,
                ConsoleIdentity.promptLineOf(Platform.LINUX, ShellKind.BASH, "desk", "~").style());
    }

    @Test
    void ofWire_findsTheShellAndTheFamilyByTheirNames() {
        final ConsoleIdentity console = ConsoleIdentity.ofWire("sh", false, "desk", "FreeBSD", "freebsd", 64);
        assertEquals(ShellKind.SH, console.shell());
        assertEquals("sh", console.shellName());
        assertEquals(Platform.FREEBSD, console.platform());
        assertEquals("freebsd", console.platformName());
    }

    @Test
    void ofWire_hasNoShellOrFamilyForANameNobodyDeclares() {
        assertNull(ConsoleIdentity.ofWire("", false, "", "", "", 0).platform());
        assertNull(ConsoleIdentity.ofWire("", false, "", "", "beos", 0).platform());
        assertNull(ConsoleIdentity.ofWire("tcsh", false, "", "", "", 0).shell());
        assertEquals("", ConsoleIdentity.NONE.platformName());
        assertEquals("", ConsoleIdentity.NONE.shellName());
    }

    @Test
    void posix_isTrueWhereAShellIsNamedOrAnInstallerMediumIsUp() {
        assertTrue(new ConsoleIdentity(ShellKind.BASH, false, "desk", "Fedora", Platform.LINUX, 64).posix());
        assertTrue(new ConsoleIdentity(null, true, "archiso", "Arch Linux live", Platform.LINUX, 64).posix());
        assertFalse(new ConsoleIdentity(null, false, "", "MC-DOS", Platform.MC_DOS, 16).posix());
        assertFalse(ConsoleIdentity.NONE.posix());
    }

    @Test
    void live_isTheInstallerMediumsConsoleAndNoOther() {
        assertTrue(new ConsoleIdentity(null, true, "archiso", "Arch Linux live", Platform.LINUX, 64).live());
        assertFalse(new ConsoleIdentity(ShellKind.ZSH, false, "desk", "Arch Linux", Platform.LINUX, 64).live());
    }

    @Test
    void constructor_readsMissingWordsAsEmptyAndNeverKeepsANegativeWidth() {
        final ConsoleIdentity console = new ConsoleIdentity(null, false, null, null, null, -8);
        assertEquals("", console.shellName());
        assertEquals("", console.hostname());
        assertEquals("", console.osLabel());
        assertEquals(0, console.bits());
    }
}
