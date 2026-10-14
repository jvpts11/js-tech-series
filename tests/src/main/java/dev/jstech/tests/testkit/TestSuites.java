/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The suites the series' tests are split into: one per mod, and one for the tests that need several mods at once. A
 * test belongs to the suite its class's package names ({@code dev.jstech.tests.gametest.computers} holds J's
 * Computers' GameTests), so a change to one mod is proved by that mod's suite instead of by every test of the series.
 * The whole battery still runs at the end of a stretch of work and before a release, and that is where a change to J's
 * Core that broke another mod shows.
 *
 * <p>{@code -PgtMods=core,industrial} picks the GameTests' suites and {@code -PctMods=computers} the client tests'.
 */
public final class TestSuites {

    /** Every suite, by the last part of its package. */
    public static final List<String> ALL = List.of("core", "computers", "industrial", "series");

    private TestSuites() {
    }

    /** The suite a test class belongs to, or {@code null} for a class outside every suite's package. */
    public static String of(final Class<?> type) {
        return of(type.getName());
    }

    /** The suite of a class by its full name, or {@code null} for one outside every suite's package. */
    public static String of(final String className) {
        final int dot = className.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        final String pkg = className.substring(0, dot);
        final String last = pkg.substring(pkg.lastIndexOf('.') + 1);
        return ALL.contains(last) ? last : null;
    }

    /**
     * The suites a comma-separated list asks for, in the order given; empty when it asks for none, which means every
     * suite.
     *
     * @throws IllegalArgumentException when a name is no suite, so a mistyped one stops the run instead of running a
     *                                  smaller battery that reads as passed
     */
    public static Set<String> asked(final String list) {
        final Set<String> suites = new LinkedHashSet<>();
        for (final String part : list.split(",")) {
            final String name = part.strip().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                continue;
            }
            if (!ALL.contains(name)) {
                throw new IllegalArgumentException("there is no test suite named \"" + name + "\"; the suites are "
                        + String.join(", ", ALL));
            }
            suites.add(name);
        }
        return suites;
    }

    /** Whether a class of {@code suite} runs when {@code asked} were asked for (none asked runs every suite). */
    public static boolean runs(final String suite, final Set<String> asked) {
        return asked.isEmpty() || asked.contains(suite);
    }
}
