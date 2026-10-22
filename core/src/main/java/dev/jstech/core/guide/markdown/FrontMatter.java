/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The header of a manual's Markdown file: the lines between two of three dashes at its very top, each a name, a colon
 * and its value, a list written in square brackets or with commas, and the body under it. A file with no header is all
 * body. Pure.
 *
 * <pre>{@code
 * ---
 * section: mymod:machines
 * order: 3
 * covers: [mymod:kiln, mymod:kiln_door]
 * ---
 * # Kiln
 * }</pre>
 *
 * @param fields   each name the header gives, with its value as written
 * @param body     what follows the header
 * @param bodyLine the line of the file the body starts on, from 1
 */
public record FrontMatter(Map<String, String> fields, String body, int bodyLine) {

    private static final String FENCE = "---";
    private static final char QUOTE = '"';
    private static final char APOSTROPHE = '\'';

    public FrontMatter {
        fields = Map.copyOf(fields);
    }

    /** The header and the body of {@code text}; a header left open is read as all body. */
    public static FrontMatter of(final String text) {
        final List<String> lines = text.lines().toList();
        if (lines.isEmpty() || !lines.get(0).strip().equals(FENCE)) {
            return new FrontMatter(Map.of(), text, 1);
        }
        final Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 1; i < lines.size(); i++) {
            final String line = lines.get(i);
            if (line.strip().equals(FENCE)) {
                return new FrontMatter(fields, String.join("\n", lines.subList(i + 1, lines.size())), i + 2);
            }
            final int colon = line.indexOf(':');
            if (colon > 0 && !line.strip().startsWith("#")) {
                fields.put(line.substring(0, colon).strip(), unquoted(line.substring(colon + 1).strip()));
            }
        }
        return new FrontMatter(Map.of(), text, 1);
    }

    /** The value of {@code name}, if the header gives it and it is not empty. */
    public Optional<String> get(final String name) {
        return Optional.ofNullable(this.fields.get(name)).filter(value -> !value.isEmpty());
    }

    /** The value of {@code name}, or {@code otherwise}. */
    public String get(final String name, final String otherwise) {
        return this.get(name).orElse(otherwise);
    }

    /** The whole number {@code name} gives, or {@code otherwise} when it gives none or no number. */
    public int number(final String name, final int otherwise) {
        try {
            return this.get(name).map(Integer::parseInt).orElse(otherwise);
        } catch (final NumberFormatException notANumber) {
            return otherwise;
        }
    }

    /** Whether {@code name} says yes ({@code true} or {@code yes}), or {@code otherwise} when it says nothing. */
    public boolean yes(final String name, final boolean otherwise) {
        return this.get(name).map(value -> value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"))
                .orElse(otherwise);
    }

    /** The list {@code name} gives, written {@code [a, b]} or {@code a, b}; empty when it gives none. */
    public List<String> list(final String name) {
        return listOf(this.get(name, ""));
    }

    /** The items of a list written {@code [a, b]} or {@code a, b}, each trimmed, quotes taken off. */
    static List<String> listOf(final String written) {
        String inside = written.strip();
        if (inside.startsWith("[") && inside.endsWith("]")) {
            inside = inside.substring(1, inside.length() - 1);
        }
        final List<String> items = new ArrayList<>();
        for (final String item : inside.split(",")) {
            final String value = unquoted(item.strip());
            if (!value.isEmpty()) {
                items.add(value);
            }
        }
        return List.copyOf(items);
    }

    private static String unquoted(final String value) {
        if (value.length() >= 2 && (value.charAt(0) == QUOTE && value.charAt(value.length() - 1) == QUOTE
                || value.charAt(0) == APOSTROPHE && value.charAt(value.length() - 1) == APOSTROPHE)) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
