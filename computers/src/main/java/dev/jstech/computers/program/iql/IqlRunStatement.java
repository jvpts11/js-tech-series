/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.program.iql.IqlLexer.Token;
import dev.jstech.computers.program.iql.IqlLexer.Type;
import dev.jstech.core.jstechseries.operation.OperationPriority;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One {@code RUN} statement: a request of a kind of Operation another mod registered, for a machine of the network to
 * carry out. It names the kind by its id, the machine when it is given one, the request's fields by name, and the
 * priority it is queued at. Pure, so it is tested without the game; the Mainframe that runs it finds the kind, reads
 * the fields as the kind says its requests are written, and gives the request to the machine.
 *
 * <pre>
 * RUN jstests:accelerate WITH seconds = 3
 * RUN jsindustrial:accel_run ON 'Cyclotron-3fa2c1' WITH beam = 'proton', energy = 20 PRIORITY HIGH
 * RUN jsindustrial:valve_set ON '12, 64, -3' WITH open = true
 * </pre>
 *
 * @param kind     the id of the kind of Operation, as {@code mod:kind}
 * @param machine  the machine it is for, by the name it answers to on the network or by its position; empty for
 *                 whichever able machine is least busy
 * @param fields   what the request carries, by the fields' names, in the order written
 * @param priority the level it is queued at
 */
public record IqlRunStatement(String kind, String machine, List<Field> fields, OperationPriority priority) {

    /** The most fields one request is given, more than any kind of Operation has. */
    public static final int MOST_FIELDS = 32;

    public IqlRunStatement {
        fields = List.copyOf(fields);
    }

    /**
     * One field given after {@code WITH}.
     *
     * @param name  the field's name, as the kind calls it
     * @param value what it is given: a {@link Long} or a {@link Double} for a number, a {@link Boolean} for
     *              {@code true} or {@code false}, words otherwise
     */
    public record Field(String name, Object value) {

        /** The field as the line of what was asked shows it: its name, then its value. */
        public String said() {
            return this.name + " " + this.value;
        }
    }

    /** Whether {@code input} is a {@code RUN} statement, which this reads rather than the action parser. */
    public static boolean isRun(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        return !tokens.isEmpty() && word(tokens.getFirst(), "RUN");
    }

    /** Reads a {@code RUN} statement. */
    public static IqlRunStatement parse(final String input) {
        final List<Token> tokens = commasApart(IqlLexer.lex(input == null ? "" : input));
        if (tokens.size() < 2) {
            throw IqlError.of(IqlError.RUN_NEEDS_KIND);
        }
        final Token kind = tokens.get(1);
        if (kind.type() != Type.WORD && kind.type() != Type.STRING || !kind.text().matches("[a-z0-9_]+:[a-z0-9_/]+")) {
            throw IqlError.of(IqlError.NOT_A_KIND, kind.text());
        }
        String machine = "";
        final List<Field> fields = new ArrayList<>();
        OperationPriority priority = OperationPriority.DEFAULT;
        final Set<String> said = new HashSet<>();
        int at = 2;
        while (at < tokens.size()) {
            final Token clause = tokens.get(at);
            if (!isClause(clause)) {
                throw IqlError.of(IqlError.UNEXPECTED_TOKEN, clause.text());
            }
            final String keyword = clause.text().toUpperCase(Locale.ROOT);
            if (!said.add(keyword)) {
                throw IqlError.of(IqlError.CLAUSE_TWICE, keyword);
            }
            at++;
            switch (keyword) {
                case "ON" -> {
                    if (at >= tokens.size() || tokens.get(at).type() == Type.OPERATOR || isClause(tokens.get(at))) {
                        throw IqlError.of(IqlError.EXPECTED, IqlError.A_MACHINE);
                    }
                    machine = tokens.get(at++).text();
                }
                case "WITH" -> at = readFields(tokens, at, fields);
                default -> {
                    if (at >= tokens.size()) {
                        throw IqlError.of(IqlError.EXPECTED, IqlError.A_LEVEL);
                    }
                    final Token level = tokens.get(at++);
                    priority = OperationPriority.fromKeyword(level.text())
                            .orElseThrow(() -> IqlError.of(IqlError.UNKNOWN_PRIORITY, level.text()));
                }
            }
        }
        return new IqlRunStatement(kind.text(), machine, fields, priority);
    }

    /** What was asked, as a log's row shows it: each field and its value, after commas. */
    public String asked() {
        final List<String> each = new ArrayList<>(this.fields.size());
        for (final Field field : this.fields) {
            each.add(field.said());
        }
        return String.join(", ", each);
    }

    /*
     * Reads the fields after WITH, from {@code at}, into {@code into}: name = value, a comma before each after the
     * first, up to the next clause or the end. Returns where it stopped.
     */
    private static int readFields(final List<Token> tokens, final int from, final List<Field> into) {
        int at = from;
        final Set<String> names = new HashSet<>();
        while (true) {
            if (at + 2 >= tokens.size()) {
                throw IqlError.of(IqlError.RUN_FIELD_NEEDS_VALUE, at < tokens.size() ? tokens.get(at).text() : "");
            }
            final Token name = tokens.get(at);
            final Token equals = tokens.get(at + 1);
            final Token value = tokens.get(at + 2);
            if (name.type() != Type.WORD || !equals.text().equals("=") || value.type() == Type.OPERATOR
                    || isComma(value)) {
                throw IqlError.of(IqlError.RUN_FIELD_NEEDS_VALUE, name.text());
            }
            if (!names.add(name.text())) {
                throw IqlError.of(IqlError.FIELD_TWICE, name.text());
            }
            if (into.size() >= MOST_FIELDS) {
                throw IqlError.of(IqlError.UNEXPECTED_TOKEN, name.text());
            }
            into.add(new Field(name.text(), readValue(value)));
            at += 3;
            if (at < tokens.size() && isComma(tokens.get(at))) {
                at++;
                continue;
            }
            return at;
        }
    }

    /* What a value token stands for: a number, a truth, or the words as they are. */
    private static Object readValue(final Token token) {
        if (token.type() == Type.STRING) {
            return token.text();
        }
        final String text = token.text();
        if (text.matches("-?\\d+")) {
            try {
                return Long.parseLong(text);
            } catch (final NumberFormatException tooBig) {
                return text;
            }
        }
        if (text.matches("-?\\d+\\.\\d+")) {
            return Double.parseDouble(text);
        }
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(text);
        }
        return text;
    }

    /*
     * The lexer keeps a comma inside the word it is written against (a value, as in {@code 3,}); the fields are
     * told apart by it, so each comma is made a token of its own.
     */
    private static List<Token> commasApart(final List<Token> tokens) {
        final List<Token> out = new ArrayList<>(tokens.size());
        for (final Token token : tokens) {
            if (token.type() == Type.STRING || token.text().indexOf(',') < 0) {
                out.add(token);
                continue;
            }
            final String text = token.text();
            int start = 0;
            for (int i = 0; i <= text.length(); i++) {
                if (i == text.length() || text.charAt(i) == ',') {
                    if (i > start) {
                        final String part = text.substring(start, i);
                        out.add(new Token(part.matches("-?\\d+%?") ? Type.NUMBER : Type.WORD, part));
                    }
                    if (i < text.length()) {
                        out.add(new Token(Type.WORD, ","));
                    }
                    start = i + 1;
                }
            }
        }
        return out;
    }

    /* Whether {@code token} opens one of the statement's clauses, ON, WITH or PRIORITY. */
    private static boolean isClause(final Token token) {
        return word(token, "ON") || word(token, "WITH") || word(token, "PRIORITY");
    }

    private static boolean isComma(final Token token) {
        return token.type() == Type.WORD && token.text().equals(",");
    }

    private static boolean word(final Token token, final String keyword) {
        return token.type() == Type.WORD && token.text().equalsIgnoreCase(keyword);
    }
}
