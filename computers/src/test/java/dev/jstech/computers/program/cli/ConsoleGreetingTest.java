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
import dev.jstech.computers.os.ShellKind;
import dev.jstech.core.text.ITextLanguage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ConsoleGreetingTest {

    /** pt_br as the mod ships it, so the alignment is held to the words a player really reads. */
    private static final ITextLanguage PT_BR = shipped("pt_br.json");

    @Test
    void of_greetsFreeBsdInItsOwnShape() {
        final List<String> said = words(installed(ShellKind.SH, "FreeBSD", Platform.FREEBSD, 64));
        assertEquals("FreeBSD/vel64 (desk) (ttyv0)", said.get(0));
        assertEquals("login: player", said.get(2));
        assertEquals("FreeBSD 14.1-RELEASE (GENERIC)", said.get(3));
        assertEquals("Welcome to FreeBSD, player.", said.get(5));
    }

    @Test
    void of_namesTheNarrowerArchitectureOnAThirtyTwoBitMachine() {
        final List<String> said = words(installed(ShellKind.SH, "FreeBSD", Platform.FREEBSD, 32));
        assertEquals("FreeBSD/IA-32 (desk) (ttyv0)", said.get(0));
    }

    @Test
    void of_neverNamesLinuxOnFreeBsd() {
        for (final String line : words(installed(ShellKind.SH, "FreeBSD", Platform.FREEBSD, 64))) {
            assertFalse(line.contains("Linux"), line);
            assertFalse(line.contains("x86_64"), line);
        }
    }

    @Test
    void of_pointsToAproposAndManIntro() {
        final String joined = String.join("\n", words(installed(ShellKind.SH, "FreeBSD", Platform.FREEBSD, 64)));
        assertTrue(joined.contains("apropos"), joined);
        assertTrue(joined.contains("man intro"), joined);
        assertTrue(joined.contains("Tab"), joined);
    }

    @Test
    void of_pointsToPkgInfoForWhatIsInstalled() {
        final String joined = String.join("\n", words(installed(ShellKind.SH, "FreeBSD", Platform.FREEBSD, 64)));
        assertTrue(joined.contains("pkg info"), joined);
    }

    /** The three discovery lines name their command in cyan, above a dimmed tip, top to bottom. */
    @Test
    void of_setsTheThreeDiscoveryLinesInCyanAboveADimTip() {
        final List<CliLine> lines = ConsoleGreeting.of(installed(ShellKind.SH, "FreeBSD", Platform.FREEBSD, 64));
        final CliLine findsACommand = lines.get(6);
        assertEquals("apropos", findsACommand.spans().get(3).text().english());
        assertEquals(CliStyle.CYAN, findsACommand.spans().get(3).style());
        final CliLine learnsTheSystem = lines.get(7);
        assertEquals("man intro", learnsTheSystem.spans().get(3).text().english());
        assertEquals(CliStyle.CYAN, learnsTheSystem.spans().get(3).style());
        final CliLine everyCommand = lines.get(8);
        assertEquals("Tab", everyCommand.spans().get(3).text().english());
        assertEquals(CliStyle.CYAN, everyCommand.spans().get(3).style());
        assertEquals(CliStyle.DIM, lines.get(10).style());
    }

    /** A label longer than English's own must still leave the three commands starting on the same column. */
    @Test
    void of_alignsTheThreeCommandsOnTheSameColumnInEveryLanguage() {
        assertColumnsAlign(ITextLanguage.ENGLISH);
        assertColumnsAlign(PT_BR);
    }

    private static void assertColumnsAlign(final ITextLanguage language) {
        final List<CliLine> lines = ConsoleGreeting.of(installed(ShellKind.SH, "FreeBSD", Platform.FREEBSD, 64));
        final int findsACommand = cyanColumn(lines.get(6), language);
        final int learnsTheSystem = cyanColumn(lines.get(7), language);
        final int everyCommand = cyanColumn(lines.get(8), language);
        assertEquals(findsACommand, learnsTheSystem, "the second line's command drifted off the first's column");
        assertEquals(findsACommand, everyCommand, "the third line's command drifted off the first's column");
    }

    /** Where the cyan run of that line begins, once every run before it is in that language. */
    private static int cyanColumn(final CliLine line, final ITextLanguage language) {
        int column = 0;
        for (final CliRun run : line.resolve(language)) {
            if (run.style() == CliStyle.CYAN) {
                return column;
            }
            column += run.text().length();
        }
        throw new IllegalStateException("no cyan run on this line: " + line);
    }

    @Test
    void of_greetsALinuxWithItsKernelInBrackets() {
        final List<String> said = words(installed(ShellKind.BASH, "Fedora", Platform.LINUX, 64));
        assertEquals("Fedora desk tty1", said.get(0));
        assertEquals("desk login: player", said.get(2));
        assertTrue(said.contains("Welcome to Fedora (Linux 6.8-jsc x86_64)"), said.toString());
    }

    @Test
    void of_greetsSystemVWithTheMachineTheLoginAndWhereItsHelpIs() {
        final List<String> said = words(installed(ShellKind.SH, "UNIX System V", Platform.UNIX, 16));
        assertEquals("desk Console Login: player", said.get(0));
        assertEquals("Type help for the UNIX system on-line help.", said.get(1));
        assertEquals("", said.get(said.size() - 1));
        assertEquals(3, said.size());
    }

    @Test
    void of_logsRootInOnAnInstallerMedium() {
        final List<String> said = words(new ConsoleIdentity(null, true, "archiso", "Arch Linux live",
                Platform.LINUX, 64));
        assertEquals("Arch Linux live installation medium (tty1)", said.get(0));
        assertEquals("archiso login: root (automatic login)", said.get(2));
    }

    @Test
    void of_saysNothingWhereThereIsNoUnixPrompt() {
        assertTrue(ConsoleGreeting.of(ConsoleIdentity.NONE).isEmpty());
        assertTrue(ConsoleGreeting.of(new ConsoleIdentity(null, false, "", "MC-DOS", Platform.MC_DOS, 16)).isEmpty());
    }

    @Test
    void of_endsOnABlankLineSoThePromptStandsApart() {
        final List<String> said = words(installed(ShellKind.SH, "FreeBSD", Platform.FREEBSD, 64));
        assertEquals("", said.get(said.size() - 1));
    }

    /** The console of an installed system on a machine called {@code desk}. */
    private static ConsoleIdentity installed(final ShellKind shell, final String system, final Platform platform,
                                             final int bits) {
        return new ConsoleIdentity(shell, false, "desk", system, platform, bits);
    }

    private static List<String> words(final ConsoleIdentity console) {
        return ConsoleGreeting.of(console).stream().map(CliLine::text).toList();
    }

    /** One of the mod's own language files, answering each key with the sentence it gives, or null. */
    private static ITextLanguage shipped(final String file) {
        final String json;
        try {
            json = Files.readString(Path.of("src", "main", "resources", "assets", "jsc", "lang", file),
                    StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return key -> {
            final Matcher entry = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
                    .matcher(json);
            return entry.find() ? entry.group(1) : null;
        };
    }
}
