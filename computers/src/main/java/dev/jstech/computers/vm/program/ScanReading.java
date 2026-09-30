/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One value read off what is left of a typed line, the way C's scanf reads one: a character as it comes, anything
 * else once the spaces before it are skipped, a whole number, a number, or a word. What follows the value is left for
 * the next read.
 *
 * <p>Where C would stop on text that is not the value asked for and leave it there, to be tripped over by every read
 * after it, the line it was on is let go instead, and the read says it found nothing: a program asking again gets
 * the next line typed, not the same mistake.
 */
final class ScanReading {

    private static final Pattern WHOLE = Pattern.compile("[+-]?\\d+");
    private static final Pattern REAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
    private static final Pattern WORD = Pattern.compile("\\S+");

    private ScanReading() {
    }

    /**
     * A value read, or null when what was typed was not one, and what is left of the line after it.
     *
     * @param value what was read: an Integer, a Long, a Double, a String or a Character, or null
     * @param rest  what is left of the line, still ending in its line break, or nothing when the line is used up
     */
    record Read(Object value, String rest) {
    }

    /**
     * Reads a value of {@code kind} (int, long, double, string or char) off {@code line}, which ends in its line
     * break as what is left of a line always does.
     */
    static Read of(final String kind, final String line) {
        if ("char".equals(kind)) {
            return line.isEmpty() ? new Read('\n', "") : new Read(line.charAt(0), line.substring(1));
        }
        int start = 0;
        while (start < line.length() && Character.isWhitespace(line.charAt(start))) {
            start++;
        }
        final Pattern shape = switch (kind) {
            case "int", "long" -> WHOLE;
            case "double" -> REAL;
            default -> WORD;
        };
        final Matcher found = shape.matcher(line).region(start, line.length());
        if (!found.lookingAt()) {
            return new Read(null, "");
        }
        final String text = found.group();
        final Object value;
        try {
            value = switch (kind) {
                case "int" -> Integer.parseInt(text);
                case "long" -> Long.parseLong(text);
                case "double" -> Double.parseDouble(text);
                default -> text;
            };
        } catch (final NumberFormatException tooBig) {
            return new Read(null, "");
        }
        return new Read(value, line.substring(found.end()));
    }
}
