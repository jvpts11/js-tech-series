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
    private static final Map<String, Integer> LIBRARY_TYPES = Map.of("Sound", 2, "Speaker", 2, "char", 2);
    /**
     * The members that came after the first version on a type that was already there, each written as its owner,
     * its name and the types it takes, with the version it came in.
     */
    private static final Map<String, Integer> LIBRARY_MEMBERS = Map.ofEntries(
            Map.entry("string.Compare(string, string)", 2), Map.entry("Convert.ToString(int, int)", 2),
            Map.entry("Convert.ToInt(string, int)", 2), Map.entry("Console.Print(char)", 2),
            Map.entry("Console.Read()", 2), Map.entry("Console.Scan(out int)", 2),
            Map.entry("Console.Scan(out long)", 2), Map.entry("Console.Scan(out double)", 2),
            Map.entry("Console.Scan(out string)", 2), Map.entry("Console.Scan(out char)", 2));
    /**
     * The types the smaller language's library took in later than the full one had them, with the version each
     * came in there: the full language always had Random, and the smaller one gained it with the old rand.
     */
    private static final Map<String, Integer> SUBSET_LATER = Map.of("Random", 2);

    private SigmaVersions() {
    }

    /**
     * The version a type of the library came in, in {@code level}; a type that has always been there came in the
     * first.
     */
    public static int sinceType(final LanguageLevel level, final String name) {
        final Integer later = level.full() ? null : SUBSET_LATER.get(name);
        return later != null ? later : LIBRARY_TYPES.getOrDefault(name, FIRST);
    }

    /**
     * The version a member of the library came in, in {@code level}: the version of its type, or a later one when
     * the member was added to a type that was already there.
     *
     * @param takes the types it takes, as a signature writes them between its brackets: {@code int, int}
     */
    public static int sinceMember(final LanguageLevel level, final String owner, final String name,
                                  final String takes) {
        return Math.max(sinceType(level, owner),
                LIBRARY_MEMBERS.getOrDefault(owner + "." + name + "(" + takes + ")", FIRST));
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
