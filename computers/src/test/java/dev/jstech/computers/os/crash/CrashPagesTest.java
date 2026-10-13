/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.Platform;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CrashPagesTest {

    /** An hour, twelve minutes and three seconds up, in ticks. */
    private static final long UP = (3600L + 12 * 60L + 3L) * 20L;
    private static final long RAM = 512L * 1_048_576L;

    @Test
    void styleFor_givesEachFramesEditionItsOwnPage() {
        assertEquals(CrashStyle.FRAMES_95, CrashPages.styleFor(Platform.FRAMES, 1));
        assertEquals(CrashStyle.FRAMES_XP, CrashPages.styleFor(Platform.FRAMES, 2));
        assertEquals(CrashStyle.FRAMES_7, CrashPages.styleFor(Platform.FRAMES, 3));
        assertEquals(CrashStyle.FRAMES_10, CrashPages.styleFor(Platform.FRAMES, 4));
        assertEquals(CrashStyle.FRAMES_11, CrashPages.styleFor(Platform.FRAMES, 5));
    }

    @Test
    void styleFor_putsEveryUnixKernelOnItsConsoleAndTheEarliestSystemsOnNone() {
        assertEquals(CrashStyle.CONSOLE, CrashPages.styleFor(Platform.LINUX, 0));
        assertEquals(CrashStyle.CONSOLE, CrashPages.styleFor(Platform.FREEBSD, 0));
        assertEquals(CrashStyle.CONSOLE, CrashPages.styleFor(Platform.UNIX, 0));
        assertNull(CrashPages.styleFor(Platform.MC_DOS, 0));
        assertNull(CrashPages.styleFor(Platform.MC_NET, 0));
    }

    @Test
    void of_framesNamesTheStopCodeOfTheCause() {
        assertEquals("MEMORY_MANAGEMENT", page(CrashStyle.FRAMES_XP, Platform.FRAMES, CrashCause.MEMORY,
                CrashPages.Moment.memory("Paint", 0, List.of())).stopCode());
        assertEquals("MACHINE_CHECK_EXCEPTION", page(CrashStyle.FRAMES_7, Platform.FRAMES, CrashCause.HARDWARE,
                CrashPages.Moment.hardware("Stratix Layer DDR-512")).stopCode());
        assertEquals("WHEA_UNCORRECTABLE_ERROR", page(CrashStyle.FRAMES_10, Platform.FRAMES, CrashCause.HARDWARE,
                CrashPages.Moment.hardware("Stratix Layer DDR-512")).stopCode());
        assertEquals("0E", page(CrashStyle.FRAMES_95, Platform.FRAMES, CrashCause.MEMORY,
                CrashPages.Moment.memory("Paint", 0, List.of())).stopCode());
        assertEquals("12", page(CrashStyle.FRAMES_95, Platform.FRAMES, CrashCause.HARDWARE,
                CrashPages.Moment.hardware("x")).stopCode());
    }

    @Test
    void of_newerFramesNameThePartTakenOutAndOnlyForTheHardware() {
        final CrashPage hardware = page(CrashStyle.FRAMES_11, Platform.FRAMES, CrashCause.HARDWARE,
                CrashPages.Moment.hardware("Stratix Layer DDR5-32768"));
        assertEquals("Stratix Layer DDR5-32768", hardware.failedPart());
        assertTrue(hardware.red());
        final CrashPage memory = page(CrashStyle.FRAMES_11, Platform.FRAMES, CrashCause.MEMORY,
                CrashPages.Moment.memory("Paint", 0, List.of()));
        assertEquals("", memory.failedPart());
        assertFalse(memory.red());
    }

    @Test
    void of_linuxMemoryKillsFirstThenPanicsAndCountsItsReboot() {
        final List<String> lines = page(CrashStyle.CONSOLE, Platform.LINUX, CrashCause.MEMORY,
                CrashPages.Moment.memory("soundfoundryd", 412, List.of(new CrashPages.Victim(4211, "paint", 512))))
                .lines();
        final String reason = "Kernel panic - not syncing: System is deadlocked on memory";
        assertTrue(lines.get(0).contains("Out of memory: Killed process 4211 (paint)"), lines.get(0));
        assertTrue(lines.stream().anyMatch(l -> l.endsWith(reason)));
        assertTrue(lines.stream().anyMatch(l -> l.contains("PID: 412 Comm: soundfoundryd Not tainted 6.8-jsc #1")));
        assertTrue(lines.stream().anyMatch(l -> l.endsWith("Rebooting in 10 seconds..")));
        assertTrue(lines.getLast().endsWith("---[ end " + reason + " ]---"));
    }

    @Test
    void of_linuxKillingInitPanicsWithTheSignalItDiedOf() {
        final List<String> lines = page(CrashStyle.CONSOLE, Platform.LINUX, CrashCause.INIT_KILLED,
                CrashPages.Moment.initKilled("player@box:~$ kill -9 1", 9)).lines();
        assertEquals("player@box:~$ kill -9 1", lines.get(0), "the line as the console showed it");
        assertTrue(lines.stream().anyMatch(l -> l.endsWith("Attempted to kill init! exitcode=0x00000009")));
        assertTrue(lines.stream().anyMatch(l -> l.contains("PID: 1 Comm: systemd")));
    }

    @Test
    void of_freeBsdGoesNowhereWithoutItsInitAndOffersToHoldItsReboot() {
        final CrashPage page = page(CrashStyle.CONSOLE, Platform.FREEBSD, CrashCause.INIT_KILLED,
                CrashPages.Moment.initKilled("root@box:~ # kill 1", 15));
        assertTrue(page.holdsOnKey());
        assertTrue(page.lines().contains("init died (signal 15, exit 0)"));
        assertTrue(page.lines().contains("panic: Going nowhere without my init!"));
        assertTrue(page.lines().contains("Uptime: 1h12m3s"));
        assertEquals("Automatic reboot in 15 seconds - press a key on the console to abort", page.lines().getLast());
    }

    @Test
    void of_freeBsdMemorySaysHowMuchWasAllocated() {
        final List<String> lines = page(CrashStyle.CONSOLE, Platform.FREEBSD, CrashCause.MEMORY,
                CrashPages.Moment.memory("paint", 900, List.of(new CrashPages.Victim(3902, "knot", 64)))).lines();
        assertEquals("pid 3902 (knot), jid 0, uid 0, was killed: out of swap space", lines.get(0));
        assertTrue(lines.contains("panic: kmem_malloc(16384): kmem_map too small: " + RAM + " total allocated"));
    }

    @Test
    void of_systemVPanicsInItsOwnWords() {
        final List<String> lines = page(CrashStyle.CONSOLE, Platform.UNIX, CrashCause.HARDWARE,
                CrashPages.Moment.hardware("Stratix Layer SIMM-16")).lines();
        assertEquals("PANIC: memory parity error", lines.get(0));
        assertEquals("Automatic reboot in progress...", lines.getLast());
        assertFalse(page(CrashStyle.CONSOLE, Platform.UNIX, CrashCause.MEMORY,
                CrashPages.Moment.memory("ed", 214, List.of())).holdsOnKey());
    }

    @Test
    void crashPage_keepsTheLastLinesWhenAKernelSaysTooMuch() {
        final List<String> lines = new ArrayList<>();
        for (int i = 0; i < CrashPage.MOST_LINES + 5; i++) {
            lines.add("line " + i);
        }
        final CrashPage page = new CrashPage(CrashStyle.CONSOLE, CrashCause.MEMORY, "", "", "", lines, 10, false);
        assertEquals(CrashPage.MOST_LINES, page.lines().size());
        assertEquals("line " + (CrashPage.MOST_LINES + 4), page.lines().getLast());
    }

    @Test
    void killLine_isEachKernelsOwnLineForWhatItEnded() {
        final CrashPages.Victim victim = new CrashPages.Victim(1234, "calc", 64);
        final String linux = CrashPages.killLine(Platform.LINUX, UP, victim);
        assertTrue(linux.startsWith("[ 4323.338190] Out of memory: Killed process 1234 (calc) total-vm:"), linux);
        assertEquals("pid 1234 (calc), jid 0, uid 0, was killed: out of swap space",
                CrashPages.killLine(Platform.FREEBSD, UP, victim));
        assertEquals("NOTICE: out of memory: process 1234 (calc) killed", CrashPages.killLine(Platform.UNIX, UP, victim));
    }

    private static CrashPage page(final CrashStyle style, final Platform platform, final CrashCause cause,
                                  final CrashPages.Moment moment) {
        return CrashPages.of(style, platform, cause, CrashPages.Facts.of("box", "Debian",
                "MF ATX Legacy Motherboard (Socket 939)", "Velocion", "systemd", UP, RAM, moment));
    }
}
