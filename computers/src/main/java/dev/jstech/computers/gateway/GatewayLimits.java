/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import java.util.Map;

/**
 * How much a computer on the other side may say in one answer.
 *
 * <p>The other side is another mod's computer running somebody's Lua: what it sends back is not ours to
 * trust. An answer is measured before anything of it reaches a program of ours, so a computer that reads
 * a thirty megabyte file and hands the whole thing over is refused at the door rather than after the
 * memory has already been taken. The measuring walks the answer once, counting as it goes, and stops at
 * the first thing that is too much, so a refusal costs no more than the limit itself.
 *
 * <p>Walking with a depth limit is also what makes a table that contains itself harmless: a run of
 * tables that deep is refused before anything follows it round for ever.
 */
public final class GatewayLimits {

    /** The longest piece of text an answer may carry, in characters. */
    public static final int TEXT = 64 * 1024;
    /** The most a list or a map in an answer may hold. */
    public static final int ITEMS = 4096;
    /** How deep an answer may nest. */
    public static final int DEPTH = 8;
    /** The most an answer may weigh in all, counted as characters and one for every other value. */
    public static final int TOTAL = 256 * 1024;

    private GatewayLimits() {
    }

    /** Why an answer was refused, or null when it is within every limit. */
    public static String refuse(final Object answer) {
        final long[] weighed = {0L};
        return walk(answer, 1, weighed);
    }

    private static String walk(final Object value, final int depth, final long[] weighed) {
        if (depth > DEPTH) {
            return "the answer is nested deeper than " + DEPTH;
        }
        weighed[0]++;
        if (weighed[0] > TOTAL) {
            return "the answer is larger than " + TOTAL;
        }
        return switch (value) {
            case String text -> {
                if (text.length() > TEXT) {
                    yield "a piece of text in the answer is longer than " + TEXT;
                }
                weighed[0] += text.length();
                yield weighed[0] > TOTAL ? "the answer is larger than " + TOTAL : null;
            }
            case Map<?, ?> table -> {
                if (table.size() > ITEMS) {
                    yield "the answer holds more than " + ITEMS + " things at once";
                }
                for (final Map.Entry<?, ?> entry : table.entrySet()) {
                    final String key = walk(entry.getKey(), depth + 1, weighed);
                    if (key != null) {
                        yield key;
                    }
                    final String held = walk(entry.getValue(), depth + 1, weighed);
                    if (held != null) {
                        yield held;
                    }
                }
                yield null;
            }
            case Object[] several -> {
                if (several.length > ITEMS) {
                    yield "the answer holds more than " + ITEMS + " things at once";
                }
                for (final Object one : several) {
                    final String held = walk(one, depth + 1, weighed);
                    if (held != null) {
                        yield held;
                    }
                }
                yield null;
            }
            case null, default -> null;
        };
    }
}
