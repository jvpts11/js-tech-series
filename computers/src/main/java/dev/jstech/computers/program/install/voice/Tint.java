/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;

/**
 * The colours the tools of a by-hand installation print in, by the names a terminal has for them.
 *
 * <p>These tools colour parts of a line, and which part is which colour is as much what they look like as the
 * words are: the green arrows a merge opens every line with, the blue brackets round a green {@code ok}, a
 * package's flags in red beside the ones it was built without in blue. Writing a line down here reads the way
 * the line looks, a run at a time.
 */
final class Tint {

    private Tint() {
    }

    static CliSpan green(final String text) {
        return new CliSpan(text, CliStyle.OK);
    }

    static CliSpan blue(final String text) {
        return new CliSpan(text, CliStyle.BLUE);
    }

    static CliSpan cyan(final String text) {
        return new CliSpan(text, CliStyle.CYAN);
    }

    static CliSpan yellow(final String text) {
        return new CliSpan(text, CliStyle.WARN);
    }

    static CliSpan red(final String text) {
        return new CliSpan(text, CliStyle.ERROR);
    }

    static CliSpan dim(final String text) {
        return new CliSpan(text, CliStyle.DIM);
    }

    static CliSpan bright(final String text) {
        return new CliSpan(text, CliStyle.BRIGHT);
    }

    /**
     * A line put together from runs and plain words, in order.
     *
     * @param parts each either a {@link CliSpan} or anything else, which is written plain
     */
    static CliLine line(final Object... parts) {
        final CliLine.Builder out = CliLine.build();
        for (final Object part : parts) {
            if (part instanceof CliSpan span) {
                out.add(span.text(), span.style());
            } else if (part instanceof CliLine whole) {
                out.add(whole);
            } else {
                out.plain(String.valueOf(part));
            }
        }
        return out.done();
    }

    /** The package manager's arrows, which it opens nearly every line with. */
    static CliSpan arrows() {
        return green(">>>");
    }

    /** The star a line of the package manager's own remarks opens with. */
    static CliSpan star() {
        return green(" * ");
    }
}
