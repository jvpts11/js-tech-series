/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import java.io.Serial;

/**
 * What a manual's Markdown file does not say right, and on which of its lines: kept with what was read, never thrown,
 * and written to the log by whoever reads the manuals, so the rest of the file and of the manual still opens.
 */
public final class ManualFileException extends RuntimeException {

    private final int line;

    @Serial
    private static final long serialVersionUID = 1L;

    /** That {@code what} is wrong on {@code line} of the file, counted from 1, or 0 for the file as a whole. */
    public ManualFileException(final int line, final String what) {
        super(what, null, false, false);
        this.line = line;
    }

    /** The line of the file it is on, from 1, or 0 for the file as a whole. */
    public int line() {
        return this.line;
    }

    /** It, at {@code file}: the file and the line, then what is wrong. */
    public String at(final String file) {
        return this.line > 0 ? file + ":" + this.line + ": " + this.getMessage() : file + ": " + this.getMessage();
    }
}
