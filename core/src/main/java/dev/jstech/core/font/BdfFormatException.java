/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.font;

/** A text that breaks the BDF format, refused with the line where it stops being a font. */
public final class BdfFormatException extends IllegalArgumentException {

    private final int line;

    /** @param why what is wrong at that line */
    public BdfFormatException(final int line, final String why) {
        super("not a BDF font at line " + line + ": " + why);
        this.line = line;
    }

    /** The line of the text, counted from one, where it stops being a font. */
    public int line() {
        return line;
    }
}
