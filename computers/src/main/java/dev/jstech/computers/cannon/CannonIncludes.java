/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Lua files a Cannon source includes, found before it is compiled.
 *
 * <p>Whoever compiles a program has to hand the compiler the files it includes, so they are looked
 * for in the text first, a line at a time, the way the parser will read them: {@code include}, a file
 * name in quotes, a semicolon. An included file is found beside the file that includes it.
 */
public final class CannonIncludes {

    private static final Pattern INCLUDE = Pattern.compile("^\\s*include\\s+\"((?:[^\"\\\\]|\\\\.)*)\"\\s*;");

    private CannonIncludes() {
    }

    /** The files the source includes, as written, in the order written. */
    public static List<String> scan(final String text) {
        final List<String> found = new ArrayList<>();
        for (final String line : text.split("\n")) {
            final Matcher match = INCLUDE.matcher(line);
            if (match.find()) {
                found.add(match.group(1).replace("\\\\", "\\").replace("\\\"", "\""));
            }
        }
        return found;
    }

    /**
     * Where an included file is: as written when it names a whole path, otherwise in the folder of
     * the file that includes it, written with the separator that file's path uses.
     */
    public static String beside(final String including, final String included) {
        if (included.startsWith("/") || included.startsWith("\\") || (included.length() > 1 && included.charAt(1) == ':')) {
            return included;
        }
        final int slash = Math.max(including.lastIndexOf('/'), including.lastIndexOf('\\'));
        if (slash < 0) {
            return included;
        }
        final char separator = including.charAt(slash);
        return including.substring(0, slash + 1) + (separator == '\\' ? included.replace('/', '\\') : included);
    }
}
