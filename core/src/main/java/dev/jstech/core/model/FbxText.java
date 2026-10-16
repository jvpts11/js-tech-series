/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads the text form of FBX into the same records as the binary one: each record a name and a colon, its properties
 * after it, split by commas, and the records inside it between braces; an array written {@code *count { a: ... }};
 * a semicolon starting a comment to the end of the line.
 */
final class FbxText {

    private final String name;
    private final String text;
    private int at;
    /* Whether the last thing of a property list was a comma, so that the list goes on past the line's end. */
    private boolean lastComma;

    FbxText(final String name, final String text) {
        this.name = name;
        this.text = text;
    }

    /** The records at the top of the text, in their order. */
    List<FbxNode> nodes() {
        final List<FbxNode> out = new ArrayList<>();
        while (true) {
            skipBlank();
            if (this.at >= this.text.length()) {
                return out;
            }
            out.add(node());
        }
    }

    /* A record: its name, its colon, its properties, and the records between its braces if it has any. */
    private FbxNode node() {
        final String recordName = word();
        skipBlank();
        expect(':');
        final List<Object> properties = new ArrayList<>();
        final List<FbxNode> children = new ArrayList<>();
        this.lastComma = false;
        while (true) {
            skipInline();
            if (this.at >= this.text.length()) {
                break;
            }
            final char next = this.text.charAt(this.at);
            if (next == '\n' || next == '\r') {
                // A property list goes on past the end of a line only after a comma.
                if (!properties.isEmpty() && endsWithComma()) {
                    skipBlank();
                    continue;
                }
                break;
            }
            if (next == '{') {
                this.at++;
                children.addAll(block());
                break;
            }
            if (next == '}') {
                break;
            }
            if (next == ',') {
                this.at++;
                this.lastComma = true;
                continue;
            }
            this.lastComma = false;
            if (next == '*') {
                this.at++;
                number();
                skipBlank();
                expect('{');
                properties.add(arrayBlock());
                continue;
            }
            properties.add(value());
        }
        return new FbxNode(recordName, properties, children);
    }

    private boolean endsWithComma() {
        return this.lastComma;
    }

    /* The records between a brace and the brace that closes it. */
    private List<FbxNode> block() {
        final List<FbxNode> out = new ArrayList<>();
        while (true) {
            skipBlank();
            if (this.at >= this.text.length()) {
                throw new ModelFormatException("the FBX file " + this.name + " has a brace never closed");
            }
            if (this.text.charAt(this.at) == '}') {
                this.at++;
                return out;
            }
            out.add(node());
        }
    }

    /* An array's numbers, written {@code a: 1,2,3} before the brace that closes it. */
    private Object arrayBlock() {
        skipBlank();
        final List<Double> numbers = new ArrayList<>();
        boolean whole = true;
        if (this.text.charAt(this.at) != '}') {
            word();
            skipBlank();
            expect(':');
            while (true) {
                skipBlank();
                final char next = this.text.charAt(this.at);
                if (next == '}') {
                    break;
                }
                if (next == ',') {
                    this.at++;
                    continue;
                }
                final String written = numberText();
                whole &= written.indexOf('.') < 0 && written.indexOf('e') < 0 && written.indexOf('E') < 0;
                numbers.add(Double.parseDouble(written));
            }
        }
        skipBlank();
        expect('}');
        if (whole) {
            final long[] out = new long[numbers.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = numbers.get(i).longValue();
            }
            return out;
        }
        final double[] out = new double[numbers.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = numbers.get(i);
        }
        return out;
    }

    /* A quoted text, a number, or a bare word as the {@code Y} and {@code T} of flags. */
    private Object value() {
        final char next = this.text.charAt(this.at);
        if (next == '"') {
            final int end = this.text.indexOf('"', this.at + 1);
            if (end < 0) {
                throw new ModelFormatException("the FBX file " + this.name + " has a quote never closed");
            }
            final String quoted = this.text.substring(this.at + 1, end);
            this.at = end + 1;
            return names(quoted);
        }
        if (next == '-' || next == '+' || next == '.' || Character.isDigit(next)) {
            return number();
        }
        return word();
    }

    /* The text form writes an object's name as {@code Class::Name}, the binary one as {@code Name, 0, 1, Class}. */
    private static String names(final String quoted) {
        final int split = quoted.indexOf("::");
        return split < 0 ? quoted : quoted.substring(split + 2) + "\u0000\u0001" + quoted.substring(0, split);
    }

    private Object number() {
        final String written = numberText();
        if (written.indexOf('.') < 0 && written.indexOf('e') < 0 && written.indexOf('E') < 0) {
            return Long.parseLong(written);
        }
        return Double.parseDouble(written);
    }

    private String numberText() {
        final int start = this.at;
        while (this.at < this.text.length()) {
            final char c = this.text.charAt(this.at);
            if (Character.isDigit(c) || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                this.at++;
            } else {
                break;
            }
        }
        if (start == this.at) {
            throw new ModelFormatException("the FBX file " + this.name + " has no number where one belongs");
        }
        return this.text.substring(start, this.at);
    }

    private String word() {
        final int start = this.at;
        while (this.at < this.text.length()) {
            final char c = this.text.charAt(this.at);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '|' || c == '-') {
                this.at++;
            } else {
                break;
            }
        }
        if (start == this.at) {
            throw new ModelFormatException("the FBX file " + this.name + " has no name where one belongs");
        }
        return this.text.substring(start, this.at);
    }

    private void expect(final char wanted) {
        if (this.at >= this.text.length() || this.text.charAt(this.at) != wanted) {
            throw new ModelFormatException("the FBX file " + this.name + " lacks a " + wanted + " where one belongs");
        }
        this.at++;
    }

    /* Spaces, tabs and comments, but not the end of the line. */
    private void skipInline() {
        while (this.at < this.text.length()) {
            final char c = this.text.charAt(this.at);
            if (c == ' ' || c == '\t') {
                this.at++;
            } else if (c == ';') {
                skipComment();
            } else {
                return;
            }
        }
    }

    private void skipBlank() {
        while (this.at < this.text.length()) {
            final char c = this.text.charAt(this.at);
            if (Character.isWhitespace(c)) {
                this.at++;
            } else if (c == ';') {
                skipComment();
            } else {
                return;
            }
        }
    }

    private void skipComment() {
        while (this.at < this.text.length() && this.text.charAt(this.at) != '\n') {
            this.at++;
        }
    }
}
