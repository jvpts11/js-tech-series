/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import java.util.Locale;

/**
 * The systematic names IUPAC gives an element before it is named: a root for each digit of its atomic number and
 * "-ium" ({@code ununennium} for 119), and a symbol of each root's first letter ({@code Uue}). The final "i" of "bi"
 * and "tri" goes before "-ium", and the last "n" of "enn" before "nil".
 *
 * <p>Pure: no game types.
 */
public final class SystematicNames {

    private static final String[] ROOTS = {"nil", "un", "bi", "tri", "quad", "pent", "hex", "sept", "oct", "enn"};

    private SystematicNames() {
    }

    /** The systematic name of the element of atomic number {@code number}, in lower case: {@code ununennium}. */
    public static String name(final int number) {
        final String digits = Integer.toString(number);
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            final String root = ROOTS[digits.charAt(i) - '0'];
            // "enn" before "nil" loses an n: ennil, not ennnil.
            if (root.equals("nil") && out.toString().endsWith("enn")) {
                out.setLength(out.length() - 1);
            }
            out.append(root);
        }
        // "bi" and "tri" lose their i before -ium: bium, trium.
        if (out.charAt(out.length() - 1) == 'i') {
            out.setLength(out.length() - 1);
        }
        return out.append("ium").toString();
    }

    /** {@link #name(int)} written as a name, its first letter capital: {@code Ununennium}. */
    public static String title(final int number) {
        final String name = name(number);
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    /** The systematic symbol of the element of atomic number {@code number}: {@code Uue} for 119. */
    public static String symbol(final int number) {
        final String digits = Integer.toString(number);
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            out.append(ROOTS[digits.charAt(i) - '0'].charAt(0));
        }
        return out.substring(0, 1).toUpperCase(Locale.ROOT) + out.substring(1);
    }
}
