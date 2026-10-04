/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What the owner of an IQL Server Management Studio chose in its Options and Query Options, and the files it opened
 * last, kept as a settings file on the computer it runs on, a line for each choice, the way a program keeps its
 * settings. A line the studio does not know, or a value it cannot read, leaves that choice as it comes. Pure.
 */
final class IsmsSettings {

    boolean askFirst = true;
    boolean stopOnError = true;
    boolean lineNumbers = true;
    boolean queryAtStart = true;
    Results results = Results.GRID;
    final List<String> recent = new ArrayList<>();

    /** The file the choices are kept in, at the top of the computer's disk. */
    static final String FILE = "isms.cfg";
    /** How many files Recent Files lists. */
    static final int MOST_RECENT = 6;
    private static final String RECENT_SEPARATOR = "|";

    /** The choices a settings file holds, each as it comes where the file says nothing of it. */
    static IsmsSettings read(final String text) {
        final Map<String, String> lines = new LinkedHashMap<>();
        for (final String line : text.split("\n")) {
            final int eq = line.indexOf('=');
            if (eq > 0) {
                lines.put(line.substring(0, eq).strip(), line.substring(eq + 1).strip());
            }
        }
        final IsmsSettings settings = new IsmsSettings();
        settings.askFirst = flag(lines.get("ask_first"), settings.askFirst);
        settings.stopOnError = flag(lines.get("stop_on_error"), settings.stopOnError);
        settings.lineNumbers = flag(lines.get("line_numbers"), settings.lineNumbers);
        settings.queryAtStart = flag(lines.get("query_at_start"), settings.queryAtStart);
        settings.results = Results.named(lines.get("results"), settings.results);
        final String recent = lines.get("recent");
        if (recent != null && !recent.isEmpty()) {
            for (final String path : recent.split("\\" + RECENT_SEPARATOR)) {
                if (!path.isBlank() && settings.recent.size() < MOST_RECENT) {
                    settings.recent.add(path.strip());
                }
            }
        }
        return settings;
    }

    /** The settings file that holds these choices. */
    String write() {
        final Map<String, String> lines = new LinkedHashMap<>();
        lines.put("ask_first", Boolean.toString(askFirst));
        lines.put("stop_on_error", Boolean.toString(stopOnError));
        lines.put("line_numbers", Boolean.toString(lineNumbers));
        lines.put("query_at_start", Boolean.toString(queryAtStart));
        lines.put("results", results.word());
        lines.put("recent", String.join(RECENT_SEPARATOR, recent));
        final StringBuilder out = new StringBuilder();
        lines.forEach((key, value) -> out.append(key).append('=').append(value).append('\n'));
        return out.toString();
    }

    /** Puts {@code path} at the top of Recent Files, once, dropping the oldest past the most it lists. */
    void opened(final String path) {
        recent.remove(path);
        recent.add(0, path);
        while (recent.size() > MOST_RECENT) {
            recent.remove(recent.size() - 1);
        }
    }

    private static boolean flag(final String value, final boolean otherwise) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        return !"false".equalsIgnoreCase(value) && otherwise;
    }

    /** Where a query's results go: a grid, text, or a file on the computer. */
    enum Results {
        GRID("grid"),
        TEXT("text"),
        FILE("file");

        private final String word;

        Results(final String word) {
            this.word = word;
        }

        /** The word the settings file keeps it as. */
        String word() {
            return word;
        }

        /** The next of the three, after the last the first, as the toolbar's button steps through them. */
        Results next() {
            return switch (this) {
                case GRID -> TEXT;
                case TEXT -> FILE;
                case FILE -> GRID;
            };
        }

        /** The one the settings file calls {@code word}, or {@code otherwise} for a word none is called. */
        static Results named(final String word, final Results otherwise) {
            if (word == null) {
                return otherwise;
            }
            final String lower = word.toLowerCase(Locale.ROOT);
            for (final Results results : List.of(GRID, TEXT, FILE)) {
                if (results.word.equals(lower)) {
                    return results;
                }
            }
            return otherwise;
        }
    }
}
