/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An installation written down: one line per thing remembered, as a name and a value.
 *
 * <p>Named rather than counted on purpose. A sequence this long grows, and a state written as a row of
 * unlabelled fields would mean every new thing remembered shifts everything after it and throws away what was
 * saved before. By name, a state written by an older build simply says less, and what it does not say takes
 * the value a fresh installation would have. Plain text, so the class that keeps it stays free of the game.
 */
final class LiveSaved {

    private final Map<String, String> values = new LinkedHashMap<>();

    /** Separates one remembered thing from the next; a value carrying one is escaped rather than cut. */
    private static final char LINE = '\n';

    LiveSaved() {
    }

    /** What a saved state says, by name; anything it does not name is simply absent. */
    static LiveSaved read(final String written) {
        final LiveSaved saved = new LiveSaved();
        if (written == null || written.isEmpty()) {
            return saved;
        }
        for (final String line : written.split(String.valueOf(LINE), -1)) {
            final int split = line.indexOf('=');
            if (split > 0) {
                saved.values.put(line.substring(0, split), unescape(line.substring(split + 1)));
            }
        }
        return saved;
    }

    void put(final String name, final String value) {
        this.values.put(name, value == null ? "" : value);
    }

    void put(final String name, final boolean value) {
        this.values.put(name, value ? "1" : "0");
    }

    boolean flag(final String name) {
        return "1".equals(this.values.get(name));
    }

    String text(final String name, final String fallback) {
        return this.values.getOrDefault(name, fallback);
    }

    int number(final String name, final int fallback) {
        try {
            return Integer.parseInt(this.values.getOrDefault(name, Integer.toString(fallback)));
        } catch (final NumberFormatException wrong) {
            return fallback;
        }
    }

    Map<String, String> all() {
        return this.values;
    }

    String write() {
        final StringBuilder out = new StringBuilder();
        for (final Map.Entry<String, String> one : this.values.entrySet()) {
            if (!out.isEmpty()) {
                out.append(LINE);
            }
            out.append(one.getKey()).append('=').append(escape(one.getValue()));
        }
        return out.toString();
    }

    /** A value with the two characters that would break a line written so they cannot. */
    private static String escape(final String value) {
        return value.replace("\\", "\\\\").replace("\n", "\\n");
    }

    private static String unescape(final String value) {
        final StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }
            final char next = value.charAt(++i);
            out.append(next == 'n' ? '\n' : next);
        }
        return out.toString();
    }
}
