/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import java.util.Objects;

/**
 * One thing wrong with a listing, at one place in its text. A listing with any problem does not run.
 *
 * <p>Line and column are both one-based, because they are read by a person counting lines in an editor.
 *
 * @param line    the line the problem is on
 * @param column  the column it starts at
 * @param code    the code a player quotes, for example {@code C4003}
 * @param message what is wrong, in words
 */
public record ListingProblem(int line, int column, String code, String message) {

    public ListingProblem {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("line and column are one-based: " + line + "," + column);
        }
    }

    /** The one-line form, for example {@code (4,1): error C4003: 'nonsense' is not an instruction}. */
    public String format() {
        return "(" + this.line + "," + this.column + "): error " + this.code + ": " + this.message;
    }

    @Override
    public String toString() {
        return this.format();
    }
}
