/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Puts arguments into a sentence the way the game's language files do: {@code %s} takes the next argument,
 * {@code %2$s} the second whatever its place, and {@code %%} is a percent sign.
 *
 * <p>Forgiving where the game throws: a sentence a translator got slightly wrong still reads, an argument that is not
 * there leaves nothing in its place, and anything else after a percent sign is left as it is written.
 */
public final class TextFormat {

    private static final Pattern MARK = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    private TextFormat() {
    }

    /** The sentence with the arguments put in. */
    public static String apply(final String pattern, final List<String> args) {
        if (pattern.indexOf('%') < 0) {
            return pattern;
        }
        final StringBuilder out = new StringBuilder(pattern.length() + 16);
        final Matcher mark = MARK.matcher(pattern);
        int written = 0;
        int next = 0;
        while (mark.find()) {
            out.append(pattern, written, mark.start());
            written = mark.end();
            final String kind = mark.group(2);
            if ("%".equals(kind) && mark.group(1) == null) {
                out.append('%');
            } else if ("s".equals(kind)) {
                final int index = mark.group(1) != null ? Integer.parseInt(mark.group(1)) - 1 : next++;
                if (index >= 0 && index < args.size()) {
                    out.append(args.get(index));
                }
            } else {
                out.append(mark.group());
            }
        }
        return out.append(pattern, written, pattern.length()).toString();
    }
}
