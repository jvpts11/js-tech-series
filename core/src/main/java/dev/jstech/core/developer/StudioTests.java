/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Which tests are a thing's: those whose name names it. The game writes a GameTest's name as its class and its method,
 * lower case and with no gaps ({@code testkitgametests.testkitmachine_works}), so both are compared with only their
 * letters and digits: the thing {@code test_kit_machine} is named by that test, and by every test of a class
 * {@code TestKitMachineTests}.
 */
public final class StudioTests {

    private StudioTests() {
    }

    /**
     * The names among {@code tests} that name the thing whose id's path is {@code path}, in their order; a path in
     * folders ({@code machine/compressor}) is named by its last part.
     */
    public static List<String> naming(final Collection<String> tests, final String path) {
        return index(tests).naming(path);
    }

    /** The tests' names made ready to be asked of many things, each squeezed once. */
    public static Index index(final Collection<String> tests) {
        final List<String> names = List.copyOf(tests);
        return new Index(names, names.stream().map(StudioTests::squeezed).toList());
    }

    /** Only the letters and digits of a name, in lower case. */
    static String squeezed(final String name) {
        final StringBuilder out = new StringBuilder(name.length());
        for (final char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * The tests' names, each beside its letters and digits.
     *
     * @param names    the names, in their order
     * @param squeezed each name's letters and digits, at the same place
     */
    public record Index(List<String> names, List<String> squeezed) {

        public Index {
            names = List.copyOf(names);
            squeezed = List.copyOf(squeezed);
            if (names.size() != squeezed.size()) {
                throw new IllegalArgumentException("each name is squeezed once: " + names.size() + " names, "
                        + squeezed.size() + " squeezed");
            }
        }

        /** The names that name the thing whose id's path is {@code path}, as {@link StudioTests#naming} says. */
        public List<String> naming(final String path) {
            final String thing = StudioTests.squeezed(path.substring(path.lastIndexOf('/') + 1));
            if (thing.isEmpty()) {
                return List.of();
            }
            final List<String> out = new ArrayList<>();
            for (int i = 0; i < this.names.size(); i++) {
                if (this.squeezed.get(i).contains(thing)) {
                    out.add(this.names.get(i));
                }
            }
            return out;
        }
    }
}
