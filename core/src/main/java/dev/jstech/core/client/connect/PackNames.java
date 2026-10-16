/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The name a rule of OptiFine and Continuity asks a block that can be named to have, as those packs write names: the
 * name itself, {@code pattern:} with {@code *} for any text and {@code ?} for any letter, {@code ipattern:} the same
 * with no care for case, and {@code regex:} and {@code iregex:} for an expression. A block with no name meets none.
 */
final class PackNames {

    private static final Map<String, Pattern> PATTERNS = new ConcurrentHashMap<>();
    private static final Pattern NEVER = Pattern.compile("(?!)");

    private PackNames() {
    }

    /** Whether the block in the middle of {@code around} has the name {@code wanted} asks. */
    static boolean matches(final String wanted, final PackSurroundings around) {
        return !around.name().isEmpty() && pattern(wanted).matcher(around.name()).matches();
    }

    private static Pattern pattern(final String wanted) {
        return PATTERNS.computeIfAbsent(wanted, PackNames::compile);
    }

    private static Pattern compile(final String wanted) {
        try {
            if (wanted.startsWith("regex:")) {
                return Pattern.compile(wanted.substring("regex:".length()));
            }
            if (wanted.startsWith("iregex:")) {
                return Pattern.compile(wanted.substring("iregex:".length()), Pattern.CASE_INSENSITIVE);
            }
            if (wanted.startsWith("pattern:")) {
                return Pattern.compile(glob(wanted.substring("pattern:".length())));
            }
            if (wanted.startsWith("ipattern:")) {
                return Pattern.compile(glob(wanted.substring("ipattern:".length()).toLowerCase(Locale.ROOT)),
                        Pattern.CASE_INSENSITIVE);
            }
            return Pattern.compile(Pattern.quote(wanted));
        } catch (final PatternSyntaxException e) {
            return NEVER;
        }
    }

    /* A pattern of * and ? as an expression, everything else as it is. */
    private static String glob(final String pattern) {
        final StringBuilder out = new StringBuilder();
        for (final char c : pattern.toCharArray()) {
            switch (c) {
                case '*' -> out.append(".*");
                case '?' -> out.append('.');
                default -> out.append(Pattern.quote(Character.toString(c)));
            }
        }
        return out.toString();
    }
}
