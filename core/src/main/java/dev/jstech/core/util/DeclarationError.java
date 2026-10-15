/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.util;

import java.util.Objects;
import org.jetbrains.annotations.Nullable;

/**
 * Something a mod declared that cannot stand, found while the game loads: which mod, which declaration, what is wrong
 * with it, and how to put it right. Its message is one paragraph a person can act on, the way the programmers' pages
 * end with what can go wrong, rather than a bare line over a stack of calls:
 *
 * <pre>{@code
 * throw DeclarationError.of("mymod", "the block mymod:press", "has no name",
 *         "give it one with named(\"Press\") before register()");
 * }</pre>
 *
 * <p>reads "mymod declares the block mymod:press, which has no name. To fix it, give it one with named("Press")
 * before register()." The mod is left out where the declaration does not know it, and the game's own report of the
 * failed load names the mod around it.
 */
public final class DeclarationError extends IllegalStateException {

    @Nullable
    private final String mod;
    private final String declaration;
    private final String problem;
    private final String fix;

    private DeclarationError(@Nullable final String mod, final String declaration, final String problem,
                             final String fix) {
        super((mod == null ? sentenceStart(declaration) + " " + problem
                : mod + " declares " + declaration + ", which " + problem) + ". To fix it, " + fix + ".");
        this.mod = mod;
        this.declaration = declaration;
        this.problem = problem;
        this.fix = fix;
    }

    /**
     * The error in {@code declaration}, made by {@code mod} (null when the declaration does not know it):
     * {@code problem} says what is wrong in words that follow "which", {@code fix} what to do, both without a final
     * full stop.
     */
    public static DeclarationError of(@Nullable final String mod, final String declaration, final String problem,
                                      final String fix) {
        return new DeclarationError(mod, Objects.requireNonNull(declaration, "declaration"),
                Objects.requireNonNull(problem, "problem"), Objects.requireNonNull(fix, "fix"));
    }

    /** The mod that made the declaration, or null when it is not known. */
    public @Nullable String mod() {
        return mod;
    }

    /** What was declared, as the message names it. */
    public String declaration() {
        return declaration;
    }

    /** What is wrong with it. */
    public String problem() {
        return problem;
    }

    /** How to put it right. */
    public String fix() {
        return fix;
    }

    /** The declaration's words as the start of a sentence: its first letter in capitals. */
    private static String sentenceStart(final String declaration) {
        return Character.toUpperCase(declaration.charAt(0)) + declaration.substring(1);
    }
}
