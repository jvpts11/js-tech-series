/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.core.text.Text;
import java.util.Locale;

/**
 * The lines that redraw themselves: a fetcher's bar, a package manager's, a counter running up to its total.
 *
 * <p>Every one of them is sized to the terminal, because the real ones are. A real tool asks the terminal how
 * wide it is and lays its bar out in what is left after the name and the figures, which is why the same
 * download looks different in a narrow window. These are laid out for the columns a monitor has, and none of
 * them is ever longer than that, so none of them ever wraps and redraws two rows instead of one.
 *
 * <p>The bars and their figures are the tools' own drawing and read the same in every language. Where a line
 * carries a tool's words, a counted step or a remark, the words are the player's language and what follows them
 * is set at its column once they are, so a longer translation moves nothing after it out of line.
 */
final class Bars {

    /** The columns these are laid out for, which is as many as a monitor's glass has. */
    static final int COLUMNS = TermBuffer.MONITOR_COLUMNS;

    /** How wide a counted step's words are set, which is where its bar begins. */
    private static final int STEP_COLUMNS = 30;

    /** How many columns the verdict at the end of a remark takes: a space, then {@code [ ok ]}. */
    private static final int VERDICT_COLUMNS = 7;

    private Bars() {
    }

    /**
     * The fetcher's line: the file, how far along, the bar, how much has come, how fast, and how long is left.
     *
     * @param progress from 0 to 1
     * @param seconds  how long the whole fetch takes
     */
    static CliLine fetch(final String file, final double progress, final double megabytes, final double rate,
                         final double seconds) {
        final String tail = progress >= 1.0
                ? "in " + Math.max(1, Math.round(seconds)) + "s"
                : "eta " + Math.max(1, (long) Math.ceil(seconds * (1.0 - progress))) + "s";
        return CliLine.plain(Text.literal(String.format(Locale.ROOT, "%-17s %3d%%%s %7s %5.1fMB/s %s",
                cut(file, 17), (int) Math.floor(progress * 100), arrow(progress, 14),
                String.format(Locale.ROOT, "%.2fM", megabytes * progress), rate, tail)));
    }

    /** The package manager's retrieval line: the package, its size, the rate, and a bar of hashes. */
    static CliLine retrieve(final String file, final double progress, final double mebibytes, final double rate) {
        return CliLine.plain(Text.literal(String.format(Locale.ROOT, " %-22s %5.1f MiB %5.1f MiB/s %s %3d%%",
                cut(file, 22), mebibytes * progress, rate, hashes(progress, 10),
                (int) Math.floor(progress * 100))));
    }

    /**
     * A counted line: which of how many, what is being done to it, and a bar.
     *
     * <p>The counter is as wide as the total, so a column of these stays straight from the first to the last.
     *
     * @param what the step, in the tool's words
     */
    static CliLine counted(final int at, final int of, final Text what, final double progress) {
        final int width = String.valueOf(of).length();
        final String count = String.format(Locale.ROOT, "(%" + width + "d/%d) ", at, of);
        final String bar = String.format(Locale.ROOT, "%s %3d%%", hashes(progress, 12),
                (int) Math.floor(progress * 100));
        return CliLine.of(CliSpan.plain(count), CliSpan.plain(what),
                CliSpan.pad(count.length() + STEP_COLUMNS + 1), CliSpan.plain(bar));
    }

    /**
     * A label and a count running up to its total, replaced at the end by the word the tool ends on.
     *
     * @param label the tool's words for what it is counting through
     * @param done  the tool's word for having got to the end
     */
    static CliLine counting(final Text label, final double progress, final int total, final Text done) {
        if (progress >= 1.0) {
            return Tint.line(label, " ", done);
        }
        final int width = String.valueOf(total).length();
        final String count = String.format(Locale.ROOT, "%" + width + "d/%d", (int) Math.floor(progress * total),
                total);
        return Tint.line(label, " ", count);
    }

    /**
     * A remark with its verdict at the right-hand edge: the star, the words, and {@code [ ok ]} in the last
     * columns, the brackets in one colour and the word in another.
     */
    static CliLine ok(final Text text) {
        return CliLine.of(Tint.star(), CliSpan.plain(text), CliSpan.pad(COLUMNS - VERDICT_COLUMNS),
                CliSpan.plain(" "), Tint.blue("["), CliSpan.plain(" "), Tint.green(Text.literal("ok")),
                CliSpan.plain(" "), Tint.blue("]"));
    }

    /** A bar of equals signs led by an arrowhead, which is the fetcher's. */
    private static String arrow(final double progress, final int width) {
        final int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * width);
        final StringBuilder out = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            out.append(i < filled - 1 || (i < filled && progress >= 1.0) ? '=' : i == filled - 1 ? '>' : ' ');
        }
        return out.append(']').toString();
    }

    /** A bar of hashes over dashes, which is the package manager's. */
    private static String hashes(final double progress, final int width) {
        final int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, progress)) * width);
        return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "]";
    }

    /** A name cut to the room a column gives it, which is what the real tools do to a long one. */
    private static String cut(final String text, final int room) {
        return text.length() <= room ? text : text.substring(0, room);
    }
}
