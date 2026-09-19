/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.ConsoleIdentity;
import dev.jstech.computers.os.Platform;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConsoleGreetingTest {

    @Test
    void of_greetsFreeBsdInItsOwnShape() {
        final List<String> said = words(new ConsoleIdentity("sh", "desk", "FreeBSD", Platform.FREEBSD, 64));
        assertEquals("FreeBSD/vel64 (desk) (ttyv0)", said.get(0));
        assertEquals("login: player", said.get(2));
        assertEquals("FreeBSD 14.1-RELEASE (GENERIC)", said.get(3));
        assertEquals("Welcome to FreeBSD, player.", said.get(5));
    }

    @Test
    void of_namesTheNarrowerArchitectureOnAThirtyTwoBitMachine() {
        final List<String> said = words(new ConsoleIdentity("sh", "desk", "FreeBSD", Platform.FREEBSD, 32));
        assertEquals("FreeBSD/IA-32 (desk) (ttyv0)", said.get(0));
    }

    @Test
    void of_neverNamesLinuxOnFreeBsd() {
        for (final String line : words(new ConsoleIdentity("sh", "desk", "FreeBSD", Platform.FREEBSD, 64))) {
            assertFalse(line.contains("Linux"), line);
            assertFalse(line.contains("x86_64"), line);
        }
    }

    @Test
    void of_onlyPromisesWhatTheSystemHas() {
        for (final String line : words(new ConsoleIdentity("sh", "desk", "FreeBSD", Platform.FREEBSD, 64))) {
            assertFalse(line.contains("apropos"), line);
            assertFalse(line.contains("man "), line);
        }
    }

    @Test
    void of_greetsALinuxWithItsKernelInBrackets() {
        final List<String> said = words(new ConsoleIdentity("bash", "desk", "Fedora", Platform.LINUX, 64));
        assertEquals("Fedora desk tty1", said.get(0));
        assertEquals("desk login: player", said.get(2));
        assertTrue(said.contains("Welcome to Fedora (Linux 6.8-jsc x86_64)"), said.toString());
    }

    @Test
    void of_greetsSystemVWithTheMachineTheLoginAndWhereItsHelpIs() {
        final List<String> said = words(new ConsoleIdentity("sh", "desk", "UNIX System V", Platform.UNIX, 16));
        assertEquals("desk Console Login: player", said.get(0));
        assertEquals("Type help for the UNIX system on-line help.", said.get(1));
        assertEquals("", said.get(said.size() - 1));
        assertEquals(3, said.size());
    }

    @Test
    void of_logsRootInOnAnInstallerMedium() {
        final List<String> said = words(new ConsoleIdentity(ConsoleIdentity.LIVE, "archiso", "Arch Linux live",
                Platform.LINUX, 64));
        assertEquals("Arch Linux live installation medium (tty1)", said.get(0));
        assertEquals("archiso login: root (automatic login)", said.get(2));
    }

    @Test
    void of_saysNothingWhereThereIsNoUnixPrompt() {
        assertTrue(ConsoleGreeting.of(ConsoleIdentity.NONE).isEmpty());
        assertTrue(ConsoleGreeting.of(new ConsoleIdentity("", "", "MC-DOS", Platform.MC_DOS, 16)).isEmpty());
    }

    @Test
    void of_endsOnABlankLineSoThePromptStandsApart() {
        final List<String> said = words(new ConsoleIdentity("sh", "desk", "FreeBSD", Platform.FREEBSD, 64));
        assertEquals("", said.get(said.size() - 1));
    }

    private static List<String> words(final ConsoleIdentity console) {
        return ConsoleGreeting.of(console).stream().map(CliLine::text).toList();
    }
}
