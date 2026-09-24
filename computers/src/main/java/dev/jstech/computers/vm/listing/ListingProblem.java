/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Objects;

/**
 * One thing wrong with a listing, at one place in its text. A listing with any problem does not run.
 *
 * <p>Line and column are both one-based, because they are read by a person counting lines in an editor.
 *
 * @param line    the line the problem is on
 * @param column  the column it starts at
 * @param code    the code a player quotes, for example {@code A4003}
 * @param message what is wrong, as a sentence read in the player's language
 */
@TextHolder
public record ListingProblem(int line, int column, String code, Text message) {

    private static final TextKey LINE = TextKey.of("jsc.vm.listing.problem", "(%s,%s): error %s: %s");

    public ListingProblem {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("line and column are one-based: " + line + "," + column);
        }
    }

    /** The one-line form, for example {@code (4,1): error A4003: 'nonsense' is not an instruction}. */
    public Text text() {
        return LINE.with(this.line, this.column, this.code, this.message);
    }

    /** The same line in English, the form it takes as data. */
    public String format() {
        return this.text().english();
    }

    @Override
    public String toString() {
        return this.format();
    }
}
