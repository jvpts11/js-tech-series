/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import java.util.Map;

/**
 * The versions of the language, one number for both: Σ N is the subset of Σ# N, so a project has a single number
 * and a program carried from Σ to Σ# keeps it.
 *
 * <p>A version only ever adds. What the first version took, every later one takes and compiles to the same listing,
 * so a newer compiler never breaks an older program; what a version holds a program to is only what came after it.
 * The number a build is held to is the newest the installed compiler knows, lowered by the project or at the
 * prompt, and never the other way: a compiler cannot be asked for a version it does not have.
 */
public final class SigmaVersions {

    /** The language as it first shipped under these names. */
    public static final int FIRST = 1;
    /** The newest version this compiler knows, which is what a build is held to when nothing lowers it. */
    public static final int NEWEST = 2;

    /** The types of the library that came after the first version, with the version each came in. */
    private static final Map<String, Integer> LIBRARY_TYPES = Map.of("Sound", 2, "Speaker", 2);

    private SigmaVersions() {
    }

    /** The version a type of the library came in; a type that has always been there came in the first. */
    public static int sinceType(final String name) {
        return LIBRARY_TYPES.getOrDefault(name, FIRST);
    }

    /**
     * The version a build asked for {@code asked} is held to: the newest when it asks for none (zero or less), and
     * never past the newest or below the first.
     */
    public static int held(final int asked) {
        return asked <= 0 ? NEWEST : Math.max(FIRST, Math.min(NEWEST, asked));
    }

    /** Whether {@code asked} is a version this compiler has. */
    public static boolean known(final int asked) {
        return asked >= FIRST && asked <= NEWEST;
    }

    /**
     * The version a compiler installed at {@code packageVersion} knows, which is its major number: sgsc 2.0 is Σ# 2.
     * A package with no version on record predates the record and is taken at the newest, as is one whose number
     * cannot be read.
     */
    public static int ofPackage(final String packageVersion) {
        if (packageVersion == null || packageVersion.isBlank()) {
            return NEWEST;
        }
        final int dot = packageVersion.indexOf('.');
        try {
            return held(Integer.parseInt(dot < 0 ? packageVersion.trim() : packageVersion.substring(0, dot).trim()));
        } catch (final NumberFormatException unreadable) {
            return NEWEST;
        }
    }
}
