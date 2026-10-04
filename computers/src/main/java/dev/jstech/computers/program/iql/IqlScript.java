/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * A script of the network's language as a studio holds it: statements one after another, each ended by a semicolon,
 * comments running from two dashes to the end of their line, and template parameters written as
 * {@code <name, type, default>} waiting for a value. Pure logic, no Minecraft.
 *
 * <p>A semicolon inside quotes, or inside the braces of a procedure's body, ends nothing: a procedure is one statement
 * however many it holds. A comment is blanked out of the statement it sits in rather than cut, so every character of
 * a statement stands where it stood in the script and an error found in it can be pointed at in the editor.
 */
public final class IqlScript {

    /** A template parameter: two commas between angle brackets, no brackets inside. */
    private static final Pattern PARAMETER = Pattern.compile("<([^<>,]+),([^<>,]*),([^<>]*)>");
    /** The words of the language beside its verbs: clauses, conditions, saved objects and their triggers. */
    private static final Set<String> KEYWORDS = Set.of("SHOW", "FROM", "TO", "WHERE", "IF", "ORDER", "BY", "LIMIT",
            "SET", "OFFER", "WITH", "ASC", "DESC", "AND", "OR", "NOT", "ALL", "CONTAINS", "HAS", "LIKE", "IN", "ON",
            "CREATE", "VIEW", "PROCEDURE", "PROC", "JOB", "AS", "EVERY", "WHEN", "EXEC", "CALL", "TOP", "BUS",
            "INTERFACE", "REDSTONE", "OUT");

    private IqlScript() {
    }

    /**
     * One statement of a script.
     *
     * @param text  what is sent to the network: the statement without its semicolon, comments blanked, ends trimmed
     * @param start where its first character stands in the script
     * @param end   where its semicolon stands, or the script's length for a last statement that has none
     */
    public record Statement(String text, int start, int end) {
    }

    /** A template parameter, as {@code <name, type, default>} writes it. */
    public record Parameter(String name, String type, String fallback) {
    }

    /** The statements of {@code script}, in order; one that is empty or only a comment is left out. */
    public static List<Statement> split(final String script) {
        final List<Statement> out = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        int start = -1;
        int depth = 0;
        char quote = 0;
        boolean comment = false;
        final int n = script.length();
        for (int i = 0; i < n; i++) {
            final char c = script.charAt(i);
            if (comment) {
                comment = c != '\n';
                current.append(comment ? ' ' : '\n');
                continue;
            }
            if (quote != 0) {
                current.append(c);
                quote = c == quote ? 0 : quote;
                continue;
            }
            if (c == '-' && i + 1 < n && script.charAt(i + 1) == '-') {
                comment = true;
                current.append(' ');
                continue;
            }
            if (c == ';' && depth == 0) {
                add(out, current, start, i);
                current.setLength(0);
                start = -1;
                continue;
            }
            if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
            }
            if (start < 0 && !Character.isWhitespace(c)) {
                start = i;
            }
            current.append(c);
        }
        add(out, current, start, n);
        return out;
    }

    /** The statement of {@code script} the caret at {@code offset} stands in, or after; null for none. */
    @Nullable
    public static Statement at(final String script, final int offset) {
        Statement last = null;
        for (final Statement statement : split(script)) {
            if (offset <= statement.end()) {
                return statement;
            }
            last = statement;
        }
        return last;
    }

    /**
     * Where token {@code index} of {@code statement} lies, as {@code {first, pastLast}} counted in the statement's
     * characters, quotes included; null when the statement has no such token.
     */
    @Nullable
    public static int[] tokenSpan(final String statement, final int index) {
        final List<int[]> spans = IqlLexer.spans(statement);
        return index >= 0 && index < spans.size() ? spans.get(index) : null;
    }

    /**
     * Whether running {@code statement} loses something for good, which a studio asks about first: items dropped,
     * everything of an item sent out of the network, or a saved view, procedure or job dropped.
     */
    public static boolean destructive(final String statement) {
        final IqlParseResult parsed = IqlParser.tryParse(statement);
        if (parsed.definition() != null) {
            return parsed.definition().verb() == IqlDefinition.Verb.DROP;
        }
        final IqlOperation operation = parsed.operation();
        if (operation == null) {
            return false;
        }
        return operation.verb() == IqlVerb.DROP
                || operation.verb() == IqlVerb.DELETE && operation.quantity() == IqlOperation.ALL;
    }

    /** The template parameters of {@code text}, each once, in the order they first appear. */
    public static List<Parameter> parameters(final String text) {
        final Map<String, Parameter> found = new LinkedHashMap<>();
        final Matcher matcher = PARAMETER.matcher(text);
        while (matcher.find()) {
            final String name = matcher.group(1).strip();
            found.putIfAbsent(name, new Parameter(name, matcher.group(2).strip(), matcher.group(3).strip()));
        }
        return List.copyOf(found.values());
    }

    /** {@code text} with each template parameter replaced by its value in {@code values}, else its default. */
    public static String fill(final String text, final Map<String, String> values) {
        final Matcher matcher = PARAMETER.matcher(text);
        final StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            final String name = matcher.group(1).strip();
            final String value = values.getOrDefault(name, matcher.group(3).strip());
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** A table as comma-separated values, a line for the columns and one for each row, quoted where it must be. */
    public static String csv(final List<String> columns, final List<List<String>> rows) {
        final StringBuilder out = new StringBuilder();
        csvLine(out, columns);
        for (final List<String> row : rows) {
            out.append('\n');
            csvLine(out, row);
        }
        return out.toString();
    }

    /**
     * A table as text, every column as wide as its widest cell and two spaces between them, with a line of dashes
     * under the columns: what Results to Text shows.
     */
    public static String textTable(final List<String> columns, final List<List<String>> rows) {
        final int[] widths = new int[columns.size()];
        for (int c = 0; c < columns.size(); c++) {
            widths[c] = columns.get(c).length();
            for (final List<String> row : rows) {
                if (c < row.size()) {
                    widths[c] = Math.max(widths[c], row.get(c).length());
                }
            }
        }
        final StringBuilder out = new StringBuilder();
        padLine(out, columns, widths);
        out.append('\n');
        final List<String> rules = new ArrayList<>();
        for (final int width : widths) {
            rules.add("-".repeat(width));
        }
        padLine(out, rules, widths);
        for (final List<String> row : rows) {
            out.append('\n');
            padLine(out, row, widths);
        }
        return out.toString();
    }

    /** Whether {@code word} is one of the language's own words, which an editor colours as such. */
    public static boolean keyword(final String word) {
        final String upper = word.toUpperCase(Locale.ROOT);
        return IqlVerb.fromKeyword(upper).isPresent() || KEYWORDS.contains(upper);
    }

    private static void add(final List<Statement> out, final StringBuilder current, final int start, final int end) {
        final String text = current.toString().strip();
        if (!text.isEmpty()) {
            out.add(new Statement(text, start, end));
        }
    }

    private static void csvLine(final StringBuilder out, final List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            final String cell = cells.get(i);
            if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                out.append('"').append(cell.replace("\"", "\"\"")).append('"');
            } else {
                out.append(cell);
            }
        }
    }

    private static void padLine(final StringBuilder out, final List<String> cells, final int[] widths) {
        final StringBuilder line = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            final String cell = c < cells.size() ? cells.get(c) : "";
            line.append(cell).append(" ".repeat(widths[c] - cell.length()));
            if (c < widths.length - 1) {
                line.append("  ");
            }
        }
        out.append(line.toString().stripTrailing());
    }
}
