/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

/**
 * How much of the language a source is allowed to be.
 *
 * <p>There are two languages and one compiler. The smaller one is what the earliest machines can be programmed
 * in at all, and it is a true subset: everything written in it is also the full language, so a program carried
 * from an old machine to a new one compiles there untouched, and nothing has to be ported forward.
 *
 * <p>The cut is not about what is hard to compile. It is about what those machines were: no threads, no
 * collections of their own, nothing that hands a piece of code around as a value. What is left is a class, a
 * struct, an array, a loop and a call, which is what the people who owned them had.
 */
public enum LanguageLevel {

    /** The subset the earliest machines take. */
    SIGMA("Sigma"),
    /** Everything the language has. */
    SIGMA_SHARP("Sigma Sharp");

    private final String displayName;

    LanguageLevel(final String displayName) {
        this.displayName = displayName;
    }

    /** The name a message uses, since a player reads this and not the constant. */
    public String displayName() {
        return this.displayName;
    }

    /** Whether this is the whole language, so nothing is refused for being outside it. */
    public boolean full() {
        return this == SIGMA_SHARP;
    }
}
