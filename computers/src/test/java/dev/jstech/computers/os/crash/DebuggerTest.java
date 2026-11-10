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
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.os.Platform;
import java.util.List;
import org.junit.jupiter.api.Test;

class DebuggerTest {

    private static final long UP = 20L * 60L * 12L;
    private static final long RAM = 256L * 1_048_576L;

    @Test
    void answer_showPanicSaysWhatThePageStoppedOn() {
        final CrashPage page = openBsdPage();
        assertEquals(List.of(page.stopCode()), Debugger.answer(page, "show   panic").printed());
    }

    @Test
    void answer_psIsTheWholeTableARowALine() {
        final CrashPage page = openBsdPage();
        final Debugger.Answer ps = Debugger.answer(page, "ps");
        assertEquals(List.of(page.stopLine().split("\n")), ps.printed());
        assertFalse(ps.reboots());
    }

    @Test
    void answer_traceIsTheFramesThePagePrinted() {
        final List<String> trace = Debugger.answer(openBsdPage(), "trace").printed();
        assertFalse(trace.isEmpty());
        assertEquals(trace, Debugger.answer(openBsdPage(), "bt").printed(), "bt is the same trace");
    }

    @Test
    void answer_bootRebootIsTheOneWayOut() {
        assertTrue(Debugger.answer(openBsdPage(), "boot reboot").reboots());
        assertEquals(List.of("No such command"), Debugger.answer(openBsdPage(), "reboot").printed(),
                "ddb never restarts the machine by itself, nor for a word it does not know");
    }

    private static CrashPage openBsdPage() {
        return CrashPages.of(CrashStyle.DDB, Platform.OPENBSD, CrashCause.MEMORY, CrashPages.Facts.of("harbour",
                "OpenBSD", "MF ATX Legacy Motherboard (Socket 939)", "Velocion", "init", UP, RAM,
                CrashPages.Moment.memory("firefox", 12, List.of())));
    }
}
