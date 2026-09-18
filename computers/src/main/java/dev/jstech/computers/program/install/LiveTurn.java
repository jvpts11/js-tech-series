/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.tty.ITtyProcess;
import dev.jstech.computers.program.tty.TtyScript;
import dev.jstech.computers.program.tty.TtyScriptProcess;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * What came of one line typed at a live medium's shell.
 *
 * <p>Three things can come of one. It can be answered at once, in so many lines or in none, which is most of
 * them. It can be refused, in the tool's own words. Or it can start a tool that takes time, which says what
 * it has to say at once and then holds the terminal until it is done; nothing such a tool is for has happened
 * when this is handed back, because a tool does what it was run for at its end.
 *
 * @param ok       whether the line was accepted, which decides the colour of what it printed
 * @param lines    what is printed at once
 * @param tool     the tool left running in front of the terminal, or null when the line simply answered
 * @param complete whether this was the line that ends the installation and starts the machine on its new system
 */
public record LiveTurn(boolean ok, List<CliLine> lines, @Nullable ITtyProcess tool, boolean complete) {

    public LiveTurn {
        lines = List.copyOf(lines);
    }

    /** Accepted, and answered with those lines. */
    public static LiveTurn said(final String... lines) {
        return new LiveTurn(true, plain(lines, CliStyle.PLAIN), null, false);
    }

    /** Accepted, and answered with lines that carry colours of their own. */
    public static LiveTurn said(final List<CliLine> lines) {
        return new LiveTurn(true, lines, null, false);
    }

    /** Accepted without a word, which is how most of these tools say yes. */
    public static LiveTurn silent() {
        return new LiveTurn(true, List.of(), null, false);
    }

    /** Refused, in the tool's own words. */
    public static LiveTurn refused(final String... lines) {
        return new LiveTurn(false, plain(lines, CliStyle.ERROR), null, false);
    }

    /** Accepted, and left running: the script is played in front of the terminal until it is over. */
    public static LiveTurn running(final TtyScript script) {
        return new LiveTurn(true, List.of(), new TtyScriptProcess(script), false);
    }

    /** Accepted, and left running as a tool that is more than a script, such as one that is talked to. */
    public static LiveTurn running(final ITtyProcess tool) {
        return new LiveTurn(true, List.of(), tool, false);
    }

    /** The line that ends the installation. */
    public static LiveTurn finished(final String... lines) {
        return new LiveTurn(true, plain(lines, CliStyle.PLAIN), null, true);
    }

    /** What was printed at once, as plain text, for whatever only wants to read it. */
    public String text() {
        final StringBuilder out = new StringBuilder();
        for (final CliLine line : this.lines) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append(line.text());
        }
        return out.toString();
    }

    private static List<CliLine> plain(final String[] lines, final CliStyle style) {
        final List<CliLine> out = new ArrayList<>(lines.length);
        for (final String line : lines) {
            out.add(new CliLine(line, style));
        }
        return out;
    }
}
