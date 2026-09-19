/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import java.util.Locale;

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
    SIGMA("Sigma", "Σ", "jsc:sigma_subset", "sg", "sgproj", "scc"),
    /** Everything the language has. */
    SIGMA_SHARP("Sigma Sharp", "Σ#", "jsc:sigma", "sgs", "sgsproj", "sgsc");

    private final String displayName;
    private final String mark;
    private final String id;
    private final String sourceExtension;
    private final String projectExtension;
    private final String compiler;

    LanguageLevel(final String displayName, final String mark, final String id, final String sourceExtension,
                  final String projectExtension, final String compiler) {
        this.displayName = displayName;
        this.mark = mark;
        this.id = id;
        this.sourceExtension = sourceExtension;
        this.projectExtension = projectExtension;
        this.compiler = compiler;
    }

    /** The command that compiles a source of this language at a prompt. */
    public String compiler() {
        return this.compiler;
    }

    /** The name a message uses, since a player reads this and not the constant. */
    public String displayName() {
        return this.displayName;
    }

    /** The name the way it is written wherever the letter fits: on a menu, a filter, a file's kind. */
    public String mark() {
        return this.mark;
    }

    /** The id the language is registered under, which is what a project file names its language by. */
    public String id() {
        return this.id;
    }

    /** What a source file of this language ends in, without the dot. */
    public String sourceExtension() {
        return this.sourceExtension;
    }

    /** What a project of this language keeps itself in, without the dot. */
    public String projectExtension() {
        return this.projectExtension;
    }

    /** The language a file of that name is written in, going by how it ends, or null when it is neither. */
    public static LanguageLevel ofSource(final String path) {
        if (path == null) {
            return null;
        }
        final String lower = path.toLowerCase(Locale.ROOT);
        for (final LanguageLevel level : values()) {
            if (lower.endsWith("." + level.sourceExtension)) {
                return level;
            }
        }
        return null;
    }

    /** The language registered under that id, or null when it is somebody else's. */
    public static LanguageLevel ofId(final String id) {
        for (final LanguageLevel level : values()) {
            if (level.id.equals(id)) {
                return level;
            }
        }
        return null;
    }

    /** Whether this is the whole language, so nothing is refused for being outside it. */
    public boolean full() {
        return this == SIGMA_SHARP;
    }
}
