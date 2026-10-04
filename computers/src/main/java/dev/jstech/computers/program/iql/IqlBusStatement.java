/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.program.iql.IqlLexer.Token;
import dev.jstech.computers.program.iql.IqlLexer.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One {@code SET BUS} statement: the bus it names and the one setting it changes, in the same words a bus's window
 * shows under Software. Pure, so it is tested without the game; what it changes is applied by the engine, and only
 * where the bus's era can be set to it.
 *
 * <pre>
 * SET BUS 'Ore in' ON | OFF
 * SET BUS 'Ore in' MODE CONTINUOUS | MODE ON DEMAND
 * SET BUS 'Ore in' FILTER ONLY iron_ore, coal | FILTER ALL BUT dirt | FILTER NONE | FILTER TAG c:ores, c:logs
 * SET BUS 'Ore in' MATCH FUZZY | MATCH EXACT
 * SET BUS 'Ore in' KEEP 16 MAX 64 | MAX 64 | KEEP iron_ore 16 MAX iron_ore 64
 * SET BUS 'Ore in' PRIORITY 5
 * SET BUS 'Ore in' WHEN STOCK iron_ore &lt; 512 | WHEN STOCK TAG c:ores &lt; 4096 | WHEN TIME BETWEEN 18:00 AND 06:00
 * SET BUS 'Ore in' AFTER BUS 'Coal in'
 * SET BUS 'Chest wall' ACCESS READ AND WRITE | ACCESS READ ONLY | ACCESS WRITE ONLY
 * </pre>
 *
 * @param bus    the name of the bus
 * @param change what it sets
 */
public record IqlBusStatement(String bus, Change change) {

    /** A quantity the statement leaves as it is. */
    public static final int UNCHANGED = -1;

    /** What a statement sets. */
    public sealed interface Change permits Power, Mode, Filter, Tags, Match, Quantities, ItemQuantities, Priority,
            Stock, After, Hours, Access {
    }

    /** On or off. */
    public record Power(boolean on) implements Change {
    }

    /** Moving all the time, or only while its cable has a redstone signal. */
    public record Mode(boolean onDemand) implements Change {
    }

    /** What the filter lists, by the ids as typed (a fluid after its kind: {@code FLUID water}), and which way. */
    public record Filter(boolean allBut, List<String> items) implements Change {

        public Filter {
            items = List.copyOf(items);
        }
    }

    /** The item tags it lists besides its slots. */
    public record Tags(List<String> tags) implements Change {

        public Tags {
            tags = List.copyOf(tags);
        }
    }

    /** Exact, or loose: an item whatever its damage and components. */
    public record Match(boolean fuzzy) implements Change {
    }

    /** What the chest keeps and the most a move takes, for the whole bus; {@link #UNCHANGED} leaves one. */
    public record Quantities(int keep, int max) implements Change {
    }

    /** The same for one listed item. */
    public record ItemQuantities(String item, int keep, int max) implements Change {
    }

    /** Which of the network's buses goes first, or which storage it fills first. */
    public record Priority(int value) implements Change {
    }

    /** Wait while the network holds {@code below} or more of an item, or of a tag after a {@code #}. */
    public record Stock(String subject, long below) implements Change {
    }

    /** Wait for the named bus to finish. */
    public record After(String bus) implements Change {
    }

    /** Move only between these hours of the day, past midnight when {@code to} comes first. */
    public record Hours(int from, int to) implements Change {
    }

    /** Which way the network may use an External Storage Bus's inventory. */
    public record Access(int access) implements Change {
    }

    /** Whether {@code input} is a {@code SET BUS} statement, which this reads rather than the action parser. */
    public static boolean isSetBus(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        return tokens.size() >= 2 && word(tokens.get(0), "SET") && word(tokens.get(1), "BUS");
    }

    /** Reads a {@code SET BUS} statement. */
    public static IqlBusStatement parse(final String input) {
        return new Reader(IqlLexer.lex(input)).statement();
    }

    /**
     * Reads one setting as a {@code SET BUS} statement writes it after the bus's name, from {@code tokens}, which hold
     * the setting and nothing after it: what {@code SET ROUTER} sets on a router.
     */
    static Change setting(final List<Token> tokens) {
        final Reader reader = new Reader(tokens);
        final Change change = reader.setting();
        reader.end();
        return change;
    }

    /**
     * The hours of a {@code TIME BETWEEN 18:00 AND 06:00} spec, from and to, or null when it is not one: what a job
     * that switches a bus on for some hours of the day is set by.
     */
    public static int[] hoursOf(final String spec) {
        final List<Token> tokens = IqlLexer.lex(spec == null ? "" : spec);
        if (tokens.size() != 5 || !word(tokens.get(0), "TIME") || !word(tokens.get(1), "BETWEEN")
                || !word(tokens.get(3), "AND")) {
            return null;
        }
        try {
            return new int[] {hour(tokens.get(2).text()), hour(tokens.get(4).text())};
        } catch (final IqlError e) {
            return null;
        }
    }

    /* An hour of the day as written: 18:00, or 18. */
    private static int hour(final String written) {
        final String digits = written.contains(":") ? written.substring(0, written.indexOf(':')) : written;
        try {
            final int hour = Integer.parseInt(digits);
            if (hour >= 0 && hour < 24) {
                return hour;
            }
        } catch (final NumberFormatException e) {
            // Falls through to the refusal below.
        }
        throw IqlError.of(IqlError.NOT_AN_HOUR, written);
    }

    private static boolean word(final Token token, final String keyword) {
        return (token.type() == Type.WORD) && token.text().equalsIgnoreCase(keyword);
    }

    /* Reads the tokens of one statement left to right. */
    private static final class Reader {

        private final List<Token> tokens;
        private int pos;

        Reader(final List<Token> tokens) {
            this.tokens = tokens;
        }

        IqlBusStatement statement() {
            keyword("SET");
            keyword("BUS");
            if (pos >= tokens.size()) {
                throw IqlError.of(IqlError.BUS_NEEDS_NAME);
            }
            final String name = tokens.get(pos++).text();
            final Change change = setting();
            end();
            return new IqlBusStatement(name, change);
        }

        /* The setting from here on: its word, then what it takes. */
        Change setting() {
            if (pos >= tokens.size()) {
                throw IqlError.of(IqlError.BUS_NEEDS_SETTING);
            }
            final Token setting = tokens.get(pos++);
            return switch (setting.text().toUpperCase(Locale.ROOT)) {
                case "ON" -> new Power(true);
                case "OFF" -> new Power(false);
                case "MODE" -> mode();
                case "FILTER" -> filter();
                case "MATCH" -> new Match(either("FUZZY", "EXACT"));
                case "KEEP" -> quantities(true);
                case "MAX" -> quantities(false);
                case "PRIORITY" -> new Priority((int) number());
                case "WHEN" -> when();
                case "AFTER" -> {
                    keyword("BUS");
                    yield new After(next(IqlError.BUS_NEEDS_NAME).text());
                }
                case "ACCESS" -> access();
                default -> throw IqlError.of(IqlError.BUS_UNKNOWN_SETTING, setting.text());
            };
        }

        /* Nothing may follow the statement. */
        void end() {
            if (pos < tokens.size()) {
                throw IqlError.of(IqlError.UNEXPECTED_TOKEN, tokens.get(pos).text());
            }
        }

        private Change mode() {
            if (peek("CONTINUOUS")) {
                pos++;
                return new Mode(false);
            }
            keyword("ON");
            keyword("DEMAND");
            return new Mode(true);
        }

        private Change filter() {
            if (peek("TAG")) {
                pos++;
                return new Tags(list());
            }
            if (peek("NONE")) {
                pos++;
                return new Filter(false, List.of());
            }
            if (peek("ALL")) {
                pos++;
                keyword("BUT");
                return new Filter(true, list());
            }
            keyword("ONLY");
            return new Filter(false, list());
        }

        /* KEEP n [MAX m], MAX m, or the same for one item: KEEP iron_ore 16 MAX iron_ore 64. */
        private Change quantities(final boolean keepFirst) {
            if (pos < tokens.size() && tokens.get(pos).type() == Type.WORD) {
                final String item = tokens.get(pos++).text();
                final int first = (int) number();
                int second = UNCHANGED;
                if (keepFirst && peek("MAX")) {
                    pos++;
                    final String again = next(IqlError.AN_ITEM).text();
                    if (!again.equalsIgnoreCase(item)) {
                        throw IqlError.of(IqlError.UNEXPECTED_TOKEN, again);
                    }
                    second = (int) number();
                }
                return keepFirst ? new ItemQuantities(item, first, second) : new ItemQuantities(item, UNCHANGED,
                        first);
            }
            final int first = (int) number();
            if (keepFirst && peek("MAX")) {
                pos++;
                return new Quantities(first, (int) number());
            }
            return keepFirst ? new Quantities(first, UNCHANGED) : new Quantities(UNCHANGED, first);
        }

        private Change when() {
            if (peek("TIME")) {
                pos++;
                keyword("BETWEEN");
                final int from = hour(next(IqlError.NOT_AN_HOUR).text());
                keyword("AND");
                return new Hours(from, hour(next(IqlError.NOT_AN_HOUR).text()));
            }
            keyword("STOCK");
            final boolean tag = peek("TAG");
            if (tag) {
                pos++;
            }
            final String subject = next(IqlError.AN_ITEM).text();
            final Token below = next("<");
            if (below.type() != Type.OPERATOR || !"<".equals(below.text())) {
                throw IqlError.of(IqlError.UNEXPECTED_TOKEN, below.text());
            }
            return new Stock(tag ? "#" + subject : subject, number());
        }

        private Change access() {
            if (peek("READ")) {
                pos++;
                if (peek("ONLY")) {
                    pos++;
                    return new Access(BusSettings.READ_ONLY);
                }
                keyword("AND");
                keyword("WRITE");
                return new Access(BusSettings.READ_WRITE);
            }
            keyword("WRITE");
            keyword("ONLY");
            return new Access(BusSettings.WRITE_ONLY);
        }

        /*
         * The rest of the statement as a list: words split on commas, a fluid or a chemical kept with its kind
         * before it ({@code FLUID water}).
         */
        private List<String> list() {
            final List<String> items = new ArrayList<>();
            String kind = null;
            while (pos < tokens.size()) {
                for (final String part : tokens.get(pos++).text().split(",")) {
                    final String id = part.strip();
                    if (id.isEmpty()) {
                        continue;
                    }
                    if (kind == null && (id.equalsIgnoreCase("FLUID") || id.equalsIgnoreCase("CHEMICAL"))) {
                        kind = id.toUpperCase(Locale.ROOT);
                        continue;
                    }
                    items.add(kind == null ? id : kind + " " + id);
                    kind = null;
                }
            }
            if (items.isEmpty()) {
                throw IqlError.of(IqlError.EXPECTED, IqlError.A_LIST);
            }
            return items;
        }

        private boolean either(final String yes, final String no) {
            if (peek(yes)) {
                pos++;
                return true;
            }
            keyword(no);
            return false;
        }

        private long number() {
            final Token token = next(IqlError.A_COUNT);
            if (token.type() != Type.NUMBER) {
                throw IqlError.of(IqlError.EXPECTED_GOT, IqlError.A_COUNT, token.text());
            }
            try {
                return Long.parseLong(token.text());
            } catch (final NumberFormatException e) {
                throw IqlError.of(IqlError.EXPECTED_GOT, IqlError.A_COUNT, token.text());
            }
        }

        private Token next(final Object what) {
            if (pos >= tokens.size()) {
                throw IqlError.of(IqlError.EXPECTED, what);
            }
            return tokens.get(pos++);
        }

        private void keyword(final String keyword) {
            if (!peek(keyword)) {
                throw IqlError.of(IqlError.EXPECTED, keyword);
            }
            pos++;
        }

        private boolean peek(final String keyword) {
            return pos < tokens.size() && word(tokens.get(pos), keyword);
        }
    }
}
