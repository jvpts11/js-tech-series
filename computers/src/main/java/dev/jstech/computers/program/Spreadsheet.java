/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/**
 * A sheet of cells: what is typed in them, what they work out to, and how they ask the network.
 *
 * <p>Bounded on purpose at 26 columns by 99 rows. A sheet here is for keeping an eye on a base, not for
 * running a business, and a bound means the whole thing can be worked out in one pass without anybody
 * wondering whether a large sheet will cost the game its tick.
 *
 * <p>Besides the arithmetic a spreadsheet has always done, a cell may ask what the network holds. Those
 * are the ones that make it worth having here rather than on paper, and they are answered by whatever the
 * machine last said rather than by asking the world, so working the sheet out is pure and instant.
 *
 * <p>This class carries no Minecraft dependency, so the whole of it runs under plain JUnit.
 */
public final class Spreadsheet {

    /** How wide a sheet is, which is A to Z. */
    public static final int COLUMNS = 26;

    /** How tall a sheet is. */
    public static final int ROWS = 99;

    /** What a cell shows when it refers to itself, directly or round a ring of other cells. */
    public static final String CIRCULAR = "#LOOP";

    /** What a cell shows when what was typed in it is not something it can work out. */
    public static final String ERROR = "#ERR";

    /** What a cell shows when it asks the network for something the machine has not said anything about. */
    public static final String UNKNOWN = "#NONE";

    /**
     * What the machine last said about the network, which is what the asking functions are answered from.
     *
     * <p>An interface rather than a map so the app can hand over its own snapshot without copying it, and
     * so a test can answer whatever it likes without a network anywhere near it.
     */
    public interface INetworkFacts {

        /** How many of a thing the network holds, or -1 when nothing is known about it. */
        long quantity(String item);

        /** How much room the network has left, or -1 when that is not known. */
        long free();

        /** How many servers are on the network, or -1 when that is not known. */
        long servers();
    }

    /** Nothing is known, which is what a sheet on a machine that is on no network works from. */
    public static final INetworkFacts NOTHING_KNOWN = new INetworkFacts() {

        @Override
        public long quantity(final String item) {
            return -1L;
        }

        @Override
        public long free() {
            return -1L;
        }

        @Override
        public long servers() {
            return -1L;
        }
    };

    private final String[][] cells = new String[ROWS][COLUMNS];
    /**
     * What each cell came out as last time, so a sheet drawn sixty times a second is worked out once.
     *
     * <p>A window asks every cell it can see what it shows, in every frame. A cell holding a sum over a
     * column walks that column, and a cell that reads another cell walks into it, so a sheet with a few
     * formulas in it would be thousands of evaluations a frame without this. It is thrown away whole
     * whenever anything is typed or the machine says something new, which is the only time an answer can
     * have changed.
     */
    private final String[][] shown = new String[ROWS][COLUMNS];
    private INetworkFacts facts = NOTHING_KNOWN;
    /** Why the last cell that could not be worked out failed, which is what that cell then shows. */
    private String lastError = ERROR;

    public Spreadsheet() {
        for (final String[] row : cells) {
            Arrays.fill(row, "");
        }
    }

    /** Says what the machine last reported, which every asking cell is worked out from. */
    public void setFacts(final INetworkFacts source) {
        this.facts = source == null ? NOTHING_KNOWN : source;
        forget();
    }

    /**
     * Says the answers may have changed.
     *
     * <p>Called for anything typed and for anything the machine says, because a cell can read any other
     * cell and a change anywhere can be felt anywhere else. Working out which few cells actually moved is
     * a bigger machine than a sheet this size is worth.
     */
    public void forget() {
        for (final String[] row : shown) {
            Arrays.fill(row, null);
        }
    }

    /** What was typed into a cell, which is what an editor puts back in the bar above the sheet. */
    public String raw(final int row, final int column) {
        return inside(row, column) ? cells[row][column] : "";
    }

    /** Types into a cell. */
    public void set(final int row, final int column, final String text) {
        if (inside(row, column)) {
            cells[row][column] = text == null ? "" : text;
            forget();
        }
    }

    /** Whether a cell asks the network rather than only holding what was typed. */
    public boolean isLive(final int row, final int column) {
        final String text = raw(row, column);
        if (!text.startsWith("=")) {
            return false;
        }
        final String upper = text.toUpperCase(Locale.ROOT);
        return upper.contains("QTY(") || upper.contains("FREE(") || upper.contains("SERVERS(");
    }

    /** What a cell shows: what was typed, or what the formula in it works out to. */
    public String display(final int row, final int column) {
        final String text = raw(row, column);
        if (!text.startsWith("=")) {
            return text;
        }
        if (!inside(row, column)) {
            return text;
        }
        final String already = shown[row][column];
        if (already != null) {
            return already;
        }
        final Double value = evaluate(row, column, new HashSet<>());
        final String out = value == null ? lastError : number(value);
        shown[row][column] = out;
        return out;
    }

    /** How many cells on the sheet ask the network. */
    public int liveCells() {
        int total = 0;
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if (isLive(row, column)) {
                    total++;
                }
            }
        }
        return total;
    }

    /** The whole sheet as comma separated text, which is the file it is kept in. */
    public String toCsv() {
        final StringBuilder out = new StringBuilder();
        final int lastRow = lastUsedRow();
        final int lastColumn = lastUsedColumn();
        for (int row = 0; row <= lastRow; row++) {
            for (int column = 0; column <= lastColumn; column++) {
                if (column > 0) {
                    out.append(',');
                }
                out.append(escape(cells[row][column]));
            }
            out.append('\n');
        }
        return out.toString();
    }

    /** Reads a sheet back out of comma separated text. */
    public void fromCsv(final String csv) {
        for (final String[] row : cells) {
            Arrays.fill(row, "");
        }
        forget();
        if (csv == null || csv.isEmpty()) {
            return;
        }
        final String[] lines = csv.split("\n", -1);
        for (int row = 0; row < lines.length && row < ROWS; row++) {
            final List<String> values = splitCsvLine(lines[row]);
            for (int column = 0; column < values.size() && column < COLUMNS; column++) {
                cells[row][column] = values.get(column);
            }
        }
    }

    /** The name of a column, which is the letter a player types in a formula. */
    public static String columnName(final int column) {
        return String.valueOf((char) ('A' + Math.floorMod(column, COLUMNS)));
    }

    /** The name of a cell, as a formula refers to it. */
    public static String cellName(final int row, final int column) {
        return columnName(column) + (row + 1);
    }

    /**
     * Works a cell out, or null when it cannot be.
     *
     * <p>The set of cells already being worked out is carried down, which is what catches a cell that
     * refers to itself round a ring of others: without it that is an endless walk rather than an answer.
     */
    private Double evaluate(final int row, final int column, final Set<Integer> visiting) {
        if (!inside(row, column)) {
            lastError = ERROR;
            return null;
        }
        final int key = row * COLUMNS + column;
        if (!visiting.add(key)) {
            lastError = CIRCULAR;
            return null;
        }
        try {
            final String text = cells[row][column];
            if (!text.startsWith("=")) {
                return plainNumber(text);
            }
            return new Parser(text.substring(1), visiting).parse();
        } finally {
            visiting.remove(key);
        }
    }

    /** A cell holding a plain number counts as that number in a sum; anything else counts as nothing. */
    private static Double plainNumber(final String text) {
        if (text.isBlank()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(text.trim().replace(",", ""));
        } catch (final NumberFormatException notANumber) {
            return 0.0;
        }
    }

    /** Written without a decimal part when it has none, which is what a count should look like. */
    private static String number(final double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value) && Math.abs(value) < 1e15) {
            return String.format(Locale.ROOT, "%,d", (long) value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private int lastUsedRow() {
        for (int row = ROWS - 1; row >= 0; row--) {
            for (int column = 0; column < COLUMNS; column++) {
                if (!cells[row][column].isEmpty()) {
                    return row;
                }
            }
        }
        return 0;
    }

    private int lastUsedColumn() {
        for (int column = COLUMNS - 1; column >= 0; column--) {
            for (int row = 0; row < ROWS; row++) {
                if (!cells[row][column].isEmpty()) {
                    return column;
                }
            }
        }
        return 0;
    }

    private static String escape(final String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** Splits one line, respecting quotes, which is what a value holding a comma needs. */
    private static List<String> splitCsvLine(final String line) {
        final List<String> out = new ArrayList<>();
        final StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            final char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        out.add(current.toString());
        return out;
    }

    private static boolean inside(final int row, final int column) {
        return row >= 0 && row < ROWS && column >= 0 && column < COLUMNS;
    }

    /**
     * Reads a formula and works it out as it goes.
     *
     * <p>The ordinary four operations with the ordinary precedence, brackets, cell names, ranges inside
     * the few functions that take them, and the three that ask the network. Anything it does not
     * understand stops it, and the cell says so rather than guessing.
     */
    private final class Parser {

        private final String text;
        private final Set<Integer> visiting;
        private int at;

        Parser(final String formula, final Set<Integer> alreadyVisiting) {
            this.text = formula;
            this.visiting = alreadyVisiting;
        }

        Double parse() {
            final Double value = sum();
            if (value == null) {
                return null;
            }
            skipSpace();
            if (at < text.length()) {
                lastError = ERROR;
                return null;
            }
            return value;
        }

        private Double sum() {
            Double left = product();
            while (left != null) {
                skipSpace();
                if (at >= text.length()) {
                    return left;
                }
                final char c = text.charAt(at);
                if (c != '+' && c != '-') {
                    return left;
                }
                at++;
                final Double right = product();
                if (right == null) {
                    return null;
                }
                left = c == '+' ? left + right : left - right;
            }
            return null;
        }

        private Double product() {
            Double left = unary();
            while (left != null) {
                skipSpace();
                if (at >= text.length()) {
                    return left;
                }
                final char c = text.charAt(at);
                if (c != '*' && c != '/') {
                    return left;
                }
                at++;
                final Double right = unary();
                if (right == null) {
                    return null;
                }
                if (c == '/' && right == 0.0) {
                    lastError = ERROR;
                    return null;
                }
                left = c == '*' ? left * right : left / right;
            }
            return null;
        }

        private Double unary() {
            skipSpace();
            if (at < text.length() && text.charAt(at) == '-') {
                at++;
                final Double value = unary();
                return value == null ? null : -value;
            }
            return atom();
        }

        private Double atom() {
            skipSpace();
            if (at >= text.length()) {
                lastError = ERROR;
                return null;
            }
            final char c = text.charAt(at);
            if (c == '(') {
                at++;
                final Double inner = sum();
                skipSpace();
                if (inner == null || at >= text.length() || text.charAt(at) != ')') {
                    lastError = ERROR;
                    return null;
                }
                at++;
                return inner;
            }
            if (Character.isDigit(c) || c == '.') {
                return literal();
            }
            if (Character.isLetter(c)) {
                return nameOrCall();
            }
            lastError = ERROR;
            return null;
        }

        private Double literal() {
            final int start = at;
            while (at < text.length() && (Character.isDigit(text.charAt(at)) || text.charAt(at) == '.')) {
                at++;
            }
            try {
                return Double.parseDouble(text.substring(start, at));
            } catch (final NumberFormatException notANumber) {
                lastError = ERROR;
                return null;
            }
        }

        /** Either a cell such as {@code B4}, or a call such as {@code SUM(A1:A9)} or {@code QTY("iron")}. */
        private Double nameOrCall() {
            final int start = at;
            while (at < text.length() && Character.isLetterOrDigit(text.charAt(at))) {
                at++;
            }
            final String word = text.substring(start, at);
            skipSpace();
            if (at < text.length() && text.charAt(at) == '(') {
                at++;
                return call(word.toUpperCase(Locale.ROOT));
            }
            return cellValue(word);
        }

        private Double cellValue(final String word) {
            final int[] spot = parseCell(word);
            if (spot == null) {
                lastError = ERROR;
                return null;
            }
            return evaluate(spot[0], spot[1], visiting);
        }

        private Double call(final String name) {
            switch (name) {
                case "QTY" -> {
                    return network(name);
                }
                case "FREE", "SERVERS" -> {
                    skipSpace();
                    if (at >= text.length() || text.charAt(at) != ')') {
                        lastError = ERROR;
                        return null;
                    }
                    at++;
                    final long value = name.equals("FREE") ? facts.free() : facts.servers();
                    if (value < 0) {
                        lastError = UNKNOWN;
                        return null;
                    }
                    return (double) value;
                }
                case "SUM", "MIN", "MAX", "AVG", "COUNT" -> {
                    return overRange(name);
                }
                default -> {
                    lastError = ERROR;
                    return null;
                }
            }
        }

        /** {@code QTY("iron_ingot")}: the one function that takes a name rather than numbers. */
        private Double network(final String name) {
            skipSpace();
            if (at >= text.length() || text.charAt(at) != '"') {
                lastError = ERROR;
                return null;
            }
            at++;
            final int start = at;
            while (at < text.length() && text.charAt(at) != '"') {
                at++;
            }
            if (at >= text.length()) {
                lastError = ERROR;
                return null;
            }
            final String item = text.substring(start, at);
            at++;
            skipSpace();
            if (at >= text.length() || text.charAt(at) != ')') {
                lastError = ERROR;
                return null;
            }
            at++;
            final long held = facts.quantity(item);
            if (held < 0) {
                lastError = UNKNOWN;
                return null;
            }
            return (double) held;
        }

        /** {@code SUM(A1:A9)} and its neighbours, which walk a rectangle of cells. */
        private Double overRange(final String name) {
            final List<Double> values = new ArrayList<>();
            skipSpace();
            final int start = at;
            while (at < text.length() && text.charAt(at) != ')') {
                at++;
            }
            if (at >= text.length()) {
                lastError = ERROR;
                return null;
            }
            final String inside = text.substring(start, at).trim();
            at++;
            final int colon = inside.indexOf(':');
            if (colon < 0) {
                lastError = ERROR;
                return null;
            }
            final int[] from = parseCell(inside.substring(0, colon).trim());
            final int[] to = parseCell(inside.substring(colon + 1).trim());
            if (from == null || to == null) {
                lastError = ERROR;
                return null;
            }
            for (int row = Math.min(from[0], to[0]); row <= Math.max(from[0], to[0]); row++) {
                for (int column = Math.min(from[1], to[1]); column <= Math.max(from[1], to[1]); column++) {
                    final Double value = evaluate(row, column, visiting);
                    if (value == null) {
                        return null;
                    }
                    values.add(value);
                }
            }
            return reduce(name, values);
        }

        private Double reduce(final String name, final List<Double> values) {
            if (values.isEmpty()) {
                return 0.0;
            }
            final ToDoubleFunction<List<Double>> how = switch (name) {
                case "SUM" -> list -> list.stream().mapToDouble(Double::doubleValue).sum();
                case "MIN" -> list -> list.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                case "MAX" -> list -> list.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
                case "AVG" -> list -> list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                default -> list -> list.size();
            };
            return how.applyAsDouble(values);
        }

        /** A cell name such as {@code B4} as a row and a column, or null when it is not one. */
        private int[] parseCell(final String word) {
            if (word.length() < 2 || !Character.isLetter(word.charAt(0))) {
                return null;
            }
            final int column = Character.toUpperCase(word.charAt(0)) - 'A';
            final int row;
            try {
                row = Integer.parseInt(word.substring(1)) - 1;
            } catch (final NumberFormatException notANumber) {
                return null;
            }
            return inside(row, column) ? new int[] {row, column} : null;
        }

        private void skipSpace() {
            while (at < text.length() && text.charAt(at) == ' ') {
                at++;
            }
        }
    }
}
