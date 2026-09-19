/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.Locale;

/**
 * The little pieces of text every family of commands writes the same way, so a number printed by the network reads
 * like a number printed by a drive.
 */
final class CliText {

    private CliText() {
    }

    /** A number with its thousands apart, as every listing writes one: {@code 18,742}. */
    static String group(final long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }
}
