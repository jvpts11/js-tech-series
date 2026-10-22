/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The entities of a STEP file, its exchange structure as ISO 10303-21 writes it: each instance by its number, its
 * type and its parameters (numbers, strings, enumerations, references to other instances, lists, typed values, and the
 * unset {@code $} and derived {@code *}); a complex instance, of several types at once, as the list of its parts.
 * Comments are skipped; the header says nothing the Core draws. Pure.
 */
final class StepFile {

    private final Map<Integer, Entity> entities;

    private StepFile(final Map<Integer, Entity> entities) {
        this.entities = entities;
    }

    /**
     * The entities of {@code text}.
     *
     * @throws ModelFormatException when it is not a STEP file, or a part of it does not read
     */
    static StepFile parse(final String name, final String text) {
        final int data = text.indexOf("DATA;");
        if (!text.startsWith("ISO-10303-21") && !text.stripLeading().startsWith("ISO-10303-21") || data < 0) {
            throw new ModelFormatException("the file " + name + " is not a STEP file: it has no DATA section");
        }
        final Parser parser = new Parser(name, text, data + "DATA;".length());
        return new StepFile(parser.entities());
    }

    /** The instance numbered {@code id}, if there is one. */
    Optional<Entity> get(final int id) {
        return Optional.ofNullable(this.entities.get(id));
    }

    /** The instance a reference names; none for one not a reference or naming none. */
    @Nullable
    Entity at(@Nullable final Object reference) {
        return reference instanceof Ref ref ? this.entities.get(ref.id()) : null;
    }

    /** Every instance, by its number. */
    Map<Integer, Entity> all() {
        return this.entities;
    }

    /** A reference to the instance numbered {@code id}. */
    record Ref(int id) {
    }

    /** An enumeration's value, as {@code .T.} or {@code .MILLI.}, without its dots. */
    record Enum(String value) {
    }

    /** A value of a type named before it, as {@code LENGTH_MEASURE(25.4)}. */
    record Typed(String type, Object value) {
    }

    /** The unset parameter {@code $} and the derived {@code *}. */
    enum Missing {
        UNSET, DERIVED
    }

    /**
     * An instance: its number, its type and its parameters; or, for a complex one, the list of its parts, each of a
     * type with its own parameters.
     *
     * @param id    its number
     * @param type  its type, or the first part's for a complex one
     * @param args  its parameters, or the first part's
     * @param parts the parts of a complex instance; empty for a simple one
     */
    record Entity(int id, String type, List<Object> args, List<Entity> parts) {

        Entity {
            args = List.copyOf(args);
            parts = List.copyOf(parts);
        }

        /** Whether it is of {@code wanted}, or has a part of it. */
        boolean is(final String wanted) {
            return part(wanted) != null;
        }

        /** Its part of type {@code wanted}: itself for a simple one of that type; none for one without. */
        @Nullable
        Entity part(final String wanted) {
            if (this.parts.isEmpty()) {
                return this.type.equals(wanted) ? this : null;
            }
            for (final Entity part : this.parts) {
                if (part.type.equals(wanted)) {
                    return part;
                }
            }
            return null;
        }

        /** Its parameter {@code index}, or the unset one past its last. */
        Object arg(final int index) {
            return index < this.args.size() ? this.args.get(index) : Missing.UNSET;
        }

        /** Its parameter {@code index} as a number. */
        double number(final int index) {
            return StepFile.number(arg(index));
        }

        /** Its parameter {@code index} as a list; empty for one that is not. */
        List<Object> list(final int index) {
            return arg(index) instanceof List<?> list ? List.copyOf(list) : List.of();
        }

        /** Its parameter {@code index} as true or false, as {@code .T.} and {@code .F.}. */
        boolean flag(final int index) {
            return arg(index) instanceof Enum value && value.value().equals("T");
        }

        /** Its parameter {@code index} as a string; empty for one that is not. */
        String string(final int index) {
            return arg(index) instanceof String value ? value : "";
        }
    }

    /** A parameter as a number: a number, or a typed one's value. */
    static double number(final Object value) {
        if (value instanceof Double number) {
            return number;
        }
        if (value instanceof Typed typed) {
            return number(typed.value());
        }
        throw new IllegalArgumentException("a number was wanted, not " + value);
    }

    /* Reads the instances of the DATA section, one by one. */
    private static final class Parser {

        private final String name;
        private final String text;
        private int at;

        private Parser(final String name, final String text, final int at) {
            this.name = name;
            this.text = text;
            this.at = at;
        }

        Map<Integer, Entity> entities() {
            final Map<Integer, Entity> out = new HashMap<>();
            while (true) {
                skip();
                if (this.at >= this.text.length() || this.text.startsWith("ENDSEC", this.at)) {
                    return out;
                }
                expect('#');
                final int id = (int) whole();
                skip();
                expect('=');
                skip();
                final Entity entity;
                if (peek() == '(') {
                    this.at++;
                    final List<Entity> parts = new ArrayList<>();
                    skip();
                    while (peek() != ')') {
                        final String type = keyword();
                        parts.add(new Entity(id, type, arguments(), List.of()));
                        skip();
                    }
                    this.at++;
                    if (parts.isEmpty()) {
                        throw broken("a complex instance of no parts");
                    }
                    entity = new Entity(id, parts.getFirst().type(), parts.getFirst().args(), parts);
                } else {
                    entity = new Entity(id, keyword(), arguments(), List.of());
                }
                skip();
                expect(';');
                out.put(id, entity);
            }
        }

        /* A parenthesised list of parameters. */
        private List<Object> arguments() {
            skip();
            expect('(');
            final List<Object> out = new ArrayList<>();
            skip();
            if (peek() == ')') {
                this.at++;
                return out;
            }
            while (true) {
                out.add(value());
                skip();
                final char next = take();
                if (next == ')') {
                    return out;
                }
                if (next != ',') {
                    throw broken("a ',' or ')' between parameters, not '" + next + "'");
                }
            }
        }

        private Object value() {
            skip();
            final char next = peek();
            switch (next) {
                case '#' -> {
                    this.at++;
                    return new Ref((int) whole());
                }
                case '\'' -> {
                    return string();
                }
                case '.' -> {
                    if (this.at + 1 < this.text.length() && Character.isDigit(this.text.charAt(this.at + 1))) {
                        return real();
                    }
                    this.at++;
                    final int end = this.text.indexOf('.', this.at);
                    if (end < 0) {
                        throw broken("an enumeration that does not end");
                    }
                    final String value = this.text.substring(this.at, end);
                    this.at = end + 1;
                    return new Enum(value);
                }
                case '$' -> {
                    this.at++;
                    return Missing.UNSET;
                }
                case '*' -> {
                    this.at++;
                    return Missing.DERIVED;
                }
                case '(' -> {
                    return arguments();
                }
                case '"' -> {
                    // Binary written in hex; nothing the Core draws holds one.
                    final int end = this.text.indexOf('"', this.at + 1);
                    this.at = end < 0 ? this.text.length() : end + 1;
                    return "";
                }
                default -> {
                    if (Character.isLetter(next) || next == '!') {
                        final String type = keyword();
                        final List<Object> inner = arguments();
                        return new Typed(type, inner.isEmpty() ? Missing.UNSET : inner.getFirst());
                    }
                    return real();
                }
            }
        }

        private String string() {
            expect('\'');
            final StringBuilder out = new StringBuilder();
            while (true) {
                if (this.at >= this.text.length()) {
                    throw broken("a string that does not end");
                }
                final char next = this.text.charAt(this.at++);
                if (next == '\'') {
                    if (this.at < this.text.length() && this.text.charAt(this.at) == '\'') {
                        out.append('\'');
                        this.at++;
                        continue;
                    }
                    return out.toString();
                }
                out.append(next);
            }
        }

        private Double real() {
            final int start = this.at;
            while (this.at < this.text.length() && "+-.0123456789Ee".indexOf(this.text.charAt(this.at)) >= 0) {
                this.at++;
            }
            if (start == this.at) {
                throw broken("a parameter, not '" + peek() + "'");
            }
            String written = this.text.substring(start, this.at);
            // STEP writes "1." and "1.E-07", which Java reads once a digit follows the point.
            if (written.endsWith(".")) {
                written += "0";
            }
            written = written.replace(".E", ".0E").replace(".e", ".0e");
            try {
                return Double.parseDouble(written);
            } catch (final NumberFormatException wrong) {
                throw broken("a number that does not read: " + written);
            }
        }

        private long whole() {
            final int start = this.at;
            while (this.at < this.text.length() && Character.isDigit(this.text.charAt(this.at))) {
                this.at++;
            }
            if (start == this.at) {
                throw broken("a number");
            }
            return Long.parseLong(this.text.substring(start, this.at));
        }

        private String keyword() {
            skip();
            final int start = this.at;
            if (peek() == '!') {
                this.at++;
            }
            while (this.at < this.text.length() && (Character.isLetterOrDigit(this.text.charAt(this.at))
                    || this.text.charAt(this.at) == '_' || this.text.charAt(this.at) == '-')) {
                this.at++;
            }
            if (start == this.at) {
                throw broken("a type's name");
            }
            return this.text.substring(start, this.at).toUpperCase(Locale.ROOT);
        }

        /* Skips white space and comments. */
        private void skip() {
            while (this.at < this.text.length()) {
                final char next = this.text.charAt(this.at);
                if (Character.isWhitespace(next)) {
                    this.at++;
                } else if (next == '/' && this.text.startsWith("/*", this.at)) {
                    final int end = this.text.indexOf("*/", this.at + 2);
                    this.at = end < 0 ? this.text.length() : end + 2;
                } else {
                    return;
                }
            }
        }

        private char peek() {
            if (this.at >= this.text.length()) {
                throw broken("more, where the file ends");
            }
            return this.text.charAt(this.at);
        }

        private char take() {
            final char next = peek();
            this.at++;
            return next;
        }

        private void expect(final char wanted) {
            if (take() != wanted) {
                throw broken("'" + wanted + "'");
            }
        }

        private ModelFormatException broken(final String wanted) {
            final int line = (int) this.text.substring(0, Math.min(this.at, this.text.length())).chars()
                    .filter(c -> c == '\n').count() + 1;
            return new ModelFormatException("the STEP file " + this.name + " does not read at line " + line
                    + ": it wants " + wanted);
        }
    }
}
