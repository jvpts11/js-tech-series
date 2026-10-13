/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit;

import dev.jstech.core.client.gui.logic.TextDocument;

/**
 * How many lines a file has, the way a terminal editor without a status line of its own counts them: the
 * empty line after the last one's end is where the file stops, and not a line of it.
 *
 * <p>Every editor here that says a count on opening or writing a file counts it the same way, so it lives
 * once rather than being copied wherever one of them needs it.
 */
public final class TtyLineCount {

    private TtyLineCount() {
    }

    /** The document's line count, the last empty line left out. */
    public static int of(final TextDocument doc) {
        final int count = doc.lineCount();
        return count > 0 && doc.line(count - 1).isEmpty() ? count - 1 : count;
    }
}
