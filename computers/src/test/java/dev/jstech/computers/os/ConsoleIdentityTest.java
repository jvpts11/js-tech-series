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

import org.junit.jupiter.api.Test;

class ConsoleIdentityTest {

    @Test
    void promptOf_givesEachShellItsOwnShape() {
        assertEquals("player@desk ~ %", ConsoleIdentity.promptOf("zsh", "desk", "~"));
        assertEquals("player@desk:~$", ConsoleIdentity.promptOf("bash", "desk", "~"));
        assertEquals("player@desk:/etc $", ConsoleIdentity.promptOf("sh", "desk", "/etc"));
    }

    @Test
    void ofWire_findsTheFamilyByItsName() {
        final ConsoleIdentity console = ConsoleIdentity.ofWire("sh", "desk", "FreeBSD", "freebsd", 64);
        assertEquals(Platform.FREEBSD, console.platform());
        assertEquals("freebsd", console.platformName());
    }

    @Test
    void ofWire_hasNoFamilyForANameNobodyDeclares() {
        assertNull(ConsoleIdentity.ofWire("", "", "", "", 0).platform());
        assertNull(ConsoleIdentity.ofWire("", "", "", "beos", 0).platform());
        assertEquals("", ConsoleIdentity.NONE.platformName());
    }

    @Test
    void posix_isTrueOnlyWhereAShellIsNamed() {
        assertTrue(new ConsoleIdentity("bash", "desk", "Fedora", Platform.LINUX, 64).posix());
        assertFalse(new ConsoleIdentity("", "", "MC-DOS", Platform.MC_DOS, 16).posix());
        assertFalse(ConsoleIdentity.NONE.posix());
    }

    @Test
    void live_isTheInstallerMediumsConsoleAndNoOther() {
        assertTrue(new ConsoleIdentity(ConsoleIdentity.LIVE, "archiso", "Arch Linux live", Platform.LINUX, 64).live());
        assertFalse(new ConsoleIdentity("zsh", "desk", "Arch Linux", Platform.LINUX, 64).live());
    }

    @Test
    void constructor_readsMissingWordsAsEmptyAndNeverKeepsANegativeWidth() {
        final ConsoleIdentity console = new ConsoleIdentity(null, null, null, null, -8);
        assertEquals("", console.shellId());
        assertEquals("", console.hostname());
        assertEquals("", console.osLabel());
        assertEquals(0, console.bits());
    }
}
