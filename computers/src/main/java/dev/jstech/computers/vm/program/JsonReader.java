/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a JSON text into a tree, as RFC 8259 writes one: objects, arrays, texts, numbers, true, false and null. It
 * holds nothing of the program, so what a program is handed is made from this tree by whoever reads it for one.
 *
 * <p>A tree nested deeper than {@link #DEEPEST} is refused rather than read, so a text cannot make the reader go as
 * deep as the server's own stack.
 */
public final class JsonReader {

    private final String text;
    private int at;

    /** How deep objects and arrays may nest inside each other. */
    public static final int DEEPEST = 64;
    private static final String NUMBER_CHARACTERS = "+-0123456789.eE";
    private static final String SPACE = " \t\r\n";
    private static final int HEX_DIGITS = 4;
    private static final int HEX = 16;

    /** One value of the tree. */
    public sealed interface INode permits Text, Num, Truth, Nothing, Items, Fields {
    }

    /** A text. */
    public record Text(String value) implements INode {
    }

    /** A number, held as a real one. */
    public record Num(double value) implements INode {
    }

    /** True or false. */
    public record Truth(boolean value) implements INode {
    }

    /** JSON's null. */
    public record Nothing() implements INode {
    }

    /** An array: its values in order. */
    public record Items(List<INode> values) implements INode {
    }

    /** An object: its members in the order they were written, a name written twice keeping its last value. */
    public record Fields(Map<String, INode> members) implements INode {
    }

    /** A text that is not JSON, and the character it stopped being JSON at, counted from nought. */
    public static final class NotJson extends RuntimeException {

        private final int where;

        NotJson(final int where) {
            super(null, null, false, false);
            this.where = where;
        }

        /** The character the text stopped being JSON at. */
        public int where() {
            return this.where;
        }
    }

    private JsonReader(final String text) {
        this.text = text;
    }

    /** The tree {@code text} holds, or {@link NotJson} saying where it stops being JSON. */
    public static INode read(final String text) {
        final JsonReader reader = new JsonReader(text);
        reader.space();
        final INode value = reader.value(0);
        reader.space();
        if (reader.at < text.length()) {
            throw new NotJson(reader.at);
        }
        return value;
    }

    private INode value(final int depth) {
        if (this.at >= this.text.length()) {
            throw new NotJson(this.at);
        }
        return switch (this.text.charAt(this.at)) {
            case '{' -> this.object(depth + 1);
            case '[' -> this.array(depth + 1);
            case '"' -> new Text(this.string());
            case 't' -> this.word("true", new Truth(true));
            case 'f' -> this.word("false", new Truth(false));
            case 'n' -> this.word("null", new Nothing());
            default -> this.number();
        };
    }

    private INode object(final int depth) {
        this.deepen(depth);
        this.at++;
        final Map<String, INode> members = new LinkedHashMap<>();
        this.space();
        if (this.next('}')) {
            return new Fields(members);
        }
        while (true) {
            this.space();
            if (this.at >= this.text.length() || this.text.charAt(this.at) != '"') {
                throw new NotJson(this.at);
            }
            final String name = this.string();
            this.space();
            this.expect(':');
            this.space();
            members.put(name, this.value(depth));
            this.space();
            if (this.next('}')) {
                return new Fields(members);
            }
            this.expect(',');
        }
    }

    private INode array(final int depth) {
        this.deepen(depth);
        this.at++;
        final List<INode> values = new ArrayList<>();
        this.space();
        if (this.next(']')) {
            return new Items(values);
        }
        while (true) {
            this.space();
            values.add(this.value(depth));
            this.space();
            if (this.next(']')) {
                return new Items(values);
            }
            this.expect(',');
        }
    }

    private String string() {
        this.at++;
        final StringBuilder out = new StringBuilder();
        while (this.at < this.text.length()) {
            final char c = this.text.charAt(this.at++);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                if (c < ' ') {
                    throw new NotJson(this.at - 1);
                }
                out.append(c);
            } else {
                out.append(this.escaped());
            }
        }
        throw new NotJson(this.at);
    }

    /* The character an escape inside a text stands for, the backslash read already. */
    private char escaped() {
        if (this.at >= this.text.length()) {
            throw new NotJson(this.at);
        }
        final char escape = this.text.charAt(this.at++);
        return switch (escape) {
            case '"', '\\', '/' -> escape;
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> this.unicode();
            default -> throw new NotJson(this.at - 1);
        };
    }

    private char unicode() {
        if (this.at + HEX_DIGITS > this.text.length()) {
            throw new NotJson(this.at);
        }
        try {
            final char c = (char) Integer.parseInt(this.text.substring(this.at, this.at + HEX_DIGITS), HEX);
            this.at += HEX_DIGITS;
            return c;
        } catch (final NumberFormatException wrong) {
            throw new NotJson(this.at);
        }
    }

    private INode number() {
        final int from = this.at;
        while (this.at < this.text.length() && NUMBER_CHARACTERS.indexOf(this.text.charAt(this.at)) >= 0) {
            this.at++;
        }
        if (from == this.at) {
            throw new NotJson(from);
        }
        try {
            return new Num(Double.parseDouble(this.text.substring(from, this.at)));
        } catch (final NumberFormatException wrong) {
            throw new NotJson(from);
        }
    }

    private INode word(final String word, final INode value) {
        if (!this.text.startsWith(word, this.at)) {
            throw new NotJson(this.at);
        }
        this.at += word.length();
        return value;
    }

    private void deepen(final int depth) {
        if (depth > DEEPEST) {
            throw new NotJson(this.at);
        }
    }

    private boolean next(final char wanted) {
        if (this.at < this.text.length() && this.text.charAt(this.at) == wanted) {
            this.at++;
            return true;
        }
        return false;
    }

    private void expect(final char wanted) {
        if (!this.next(wanted)) {
            throw new NotJson(this.at);
        }
    }

    private void space() {
        while (this.at < this.text.length() && SPACE.indexOf(this.text.charAt(this.at)) >= 0) {
            this.at++;
        }
    }
}
