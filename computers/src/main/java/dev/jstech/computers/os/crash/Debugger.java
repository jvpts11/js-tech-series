/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OpenBSD's kernel debugger at {@code ddb>}, after a panic (computers/docs/design/operating-systems.md): the machine
 * waits there, never rebooting by itself, and answers {@code show panic}, {@code ps} and {@code trace} from what the
 * page it stopped on holds, until {@code boot reboot} starts it over.
 *
 * <p>Pure: what it prints is worked out from the page, so a line typed is answered the same on every monitor.
 */
public final class Debugger {

    /** The prompt the debugger waits at. */
    public static final String PROMPT = "ddb> ";

    /* What the page's lines mark the trace with: each frame names where it was, the trace's last line its end. */
    private static final String FRAME = ") at ";
    private static final String TRACE_END = "end trace frame:";

    private Debugger() {
    }

    /**
     * What ddb answers to a line typed at its prompt on that page, or nothing for a line it takes in silence.
     *
     * @param line what was typed, without the prompt
     */
    public static Answer answer(final CrashPage page, final String line) {
        final String typed = line.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return switch (typed) {
            case "" -> Answer.lines(List.of());
            case "show panic" -> Answer.lines(List.of(page.stopCode()));
            case "ps" -> Answer.lines(List.of(page.stopLine().split("\n")));
            case "trace", "bt" -> Answer.lines(trace(page));
            case "boot reboot" -> Answer.reboot();
            default -> Answer.lines(List.of("No such command"));
        };
    }

    /** The trace the page printed when it stopped, from its first frame to its end. */
    private static List<String> trace(final CrashPage page) {
        final List<String> out = new ArrayList<>();
        for (final String line : page.lines()) {
            if (line.contains(FRAME) || line.startsWith(TRACE_END)) {
                out.add(line);
            }
        }
        return out;
    }

    /**
     * What a line typed at ddb did.
     *
     * @param printed what it printed, a line each
     * @param reboots whether it starts the machine over, which is the one way out
     */
    public record Answer(List<String> printed, boolean reboots) {

        public Answer {
            printed = List.copyOf(printed);
        }

        static Answer lines(final List<String> printed) {
            return new Answer(printed, false);
        }

        static Answer reboot() {
            // What ddb prints as it starts the machine over, which is the last thing on the console.
            return new Answer(List.of("syncing disks... done", "rebooting..."), true);
        }
    }
}
