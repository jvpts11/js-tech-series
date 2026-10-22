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
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Reads a USD layer written as text, {@code .usda}: its metadata, its prims with their specifiers, types, metadata and
 * children, and their properties (attributes with their types, values, samples over time, connections and metadata;
 * relationships with their targets). Variant sets are read past, the model taken as it stands by default. Pure.
 */
final class UsdaParser {

    private final String name;
    private final String text;
    private int at;

    private static final Set<String> SPECIFIERS = Set.of("def", "over", "class");
    private static final Set<String> QUALIFIERS = Set.of("uniform", "varying", "custom", "config");
    private static final Set<String> LIST_EDITS = Set.of("prepend", "append", "add", "delete", "reorder");

    private UsdaParser(final String name, final String text) {
        this.name = name;
        this.text = text;
    }

    /** Whether {@code bytes} begin as a USD text layer does. */
    static boolean isText(final byte[] bytes) {
        return bytes.length >= 5 && bytes[0] == '#' && bytes[1] == 'u' && bytes[2] == 's' && bytes[3] == 'd'
                && bytes[4] == 'a';
    }

    /**
     * The layer a text file holds, as the tree of its prims under its root, the root carrying the layer's metadata.
     *
     * @throws ModelFormatException when a part of it does not read
     */
    static UsdPrim parse(final String name, final String text) {
        final UsdaParser parser = new UsdaParser(name, text);
        final UsdPrim root = new UsdPrim("", "/");
        try {
            parser.layer(root);
        } catch (final IndexOutOfBoundsException | NumberFormatException broken) {
            throw parser.broken("something that reads (" + broken.getMessage() + ")");
        }
        return root;
    }

    private void layer(final UsdPrim root) {
        final int line = this.text.indexOf('\n');
        this.at = line < 0 ? this.text.length() : line + 1;
        skip();
        if (peek() == '(') {
            metadata(root.metadata);
        }
        while (true) {
            skip();
            if (this.at >= this.text.length()) {
                return;
            }
            prim(root);
        }
    }

    /* A prim: its specifier, its type if it has one, its name, its metadata, and its body. */
    private void prim(final UsdPrim parent) {
        final String specifier = word();
        if (!SPECIFIERS.contains(specifier)) {
            throw broken("def, over or class, not " + specifier);
        }
        skip();
        String type = "";
        if (peek() != '"') {
            type = word();
            skip();
        }
        final UsdPrim prim = parent.child(string());
        prim.specifier = specifier;
        if (!type.isEmpty()) {
            prim.type = type;
        }
        skip();
        if (peek() == '(') {
            metadata(prim.metadata);
            skip();
        }
        expect('{');
        while (true) {
            skip();
            final char next = peek();
            if (next == '}') {
                this.at++;
                return;
            }
            final int mark = this.at;
            final String first = word();
            if (SPECIFIERS.contains(first)) {
                this.at = mark;
                prim(prim);
            } else if (first.equals("variantSet")) {
                variantSet();
            } else {
                this.at = mark;
                property(prim);
            }
        }
    }

    /* A property: an attribute of a type, or a relationship; its value, and its metadata. */
    private void property(final UsdPrim prim) {
        String word = word();
        while (QUALIFIERS.contains(word) || LIST_EDITS.contains(word)) {
            skip();
            word = word();
        }
        final boolean relationship = word.equals("rel");
        String type = relationship ? "" : word;
        skip();
        if (!relationship && peek() == '[') {
            expect('[');
            expect(']');
            type += "[]";
            skip();
        }
        String propertyName = name();
        String suffix = "";
        for (final String known : List.of(".connect", ".timeSamples")) {
            if (propertyName.endsWith(known)) {
                suffix = known;
                propertyName = propertyName.substring(0, propertyName.length() - known.length());
            }
        }
        final UsdPrim.Property property = prim.property(propertyName);
        property.relationship = relationship;
        if (!type.isEmpty()) {
            property.typeName = type;
        }
        skip();
        if (this.at < this.text.length() && peek() == '=') {
            this.at++;
            skip();
            if (suffix.equals(".timeSamples")) {
                samples(property);
            } else {
                final Object value = value();
                if (relationship || suffix.equals(".connect")) {
                    targets(value, property.targets);
                } else {
                    property.value = value;
                }
            }
            skip();
        }
        if (this.at < this.text.length() && peek() == '(') {
            metadata(property.metadata);
        }
    }

    private static void targets(@Nullable final Object value, final List<String> into) {
        if (value instanceof String path) {
            into.add(path);
        } else if (value instanceof List<?> list) {
            list.forEach(each -> into.add(String.valueOf(each)));
        }
    }

    /* Samples over time: times and their values, as { 0: v, 24: w }. */
    private void samples(final UsdPrim.Property property) {
        expect('{');
        while (true) {
            skip();
            if (peek() == '}') {
                this.at++;
                return;
            }
            final double time = number();
            skip();
            expect(':');
            skip();
            property.samples.put(time, value());
            skip();
            if (peek() == ',') {
                this.at++;
            }
        }
    }

    /* A variant set, read past: the model is taken as it stands by default. */
    private void variantSet() {
        skip();
        string();
        skip();
        expect('=');
        skip();
        skipBalanced('{', '}');
    }

    /* Metadata in parentheses: names and values, list edits, and a bare string, the documentation. */
    private void metadata(final Map<String, Object> into) {
        expect('(');
        while (true) {
            skip();
            final char next = peek();
            if (next == ')') {
                this.at++;
                return;
            }
            if (next == '"' || next == '\'') {
                into.put("documentation", string());
                continue;
            }
            String key = name();
            while (LIST_EDITS.contains(key)) {
                skip();
                key = name();
            }
            skip();
            if (peek() == '=') {
                this.at++;
                skip();
                into.put(key, value());
            }
            skip();
            if (this.at < this.text.length() && peek() == ';') {
                this.at++;
            }
        }
    }

    /*
     * A value: a number, a string, an asset, a path, a tuple, a list, a dictionary (read past), or a word (true,
     * false, None, a token).
     */
    @Nullable
    private Object value() {
        skip();
        final char next = peek();
        if (next == '(') {
            return tuple();
        }
        if (next == '[') {
            return list();
        }
        if (next == '{') {
            skipBalanced('{', '}');
            return null;
        }
        if (next == '"' || next == '\'') {
            return string();
        }
        if (next == '@') {
            return asset();
        }
        if (next == '<') {
            final int end = this.text.indexOf('>', this.at);
            final String path = this.text.substring(this.at + 1, end);
            this.at = end + 1;
            return path;
        }
        if (next == '-' || next == '+' || next == '.' || Character.isDigit(next)) {
            return number();
        }
        final String word = name();
        return switch (word) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            case "None" -> null;
            case "inf" -> Double.POSITIVE_INFINITY;
            case "nan" -> Double.NaN;
            default -> word;
        };
    }

    /* A tuple: numbers as an array of doubles, tuples of tuples (a matrix) as one array row by row. */
    private Object tuple() {
        expect('(');
        final List<Object> items = new ArrayList<>();
        while (true) {
            skip();
            if (peek() == ')') {
                this.at++;
                break;
            }
            items.add(value());
            skip();
            if (peek() == ',') {
                this.at++;
            }
        }
        return numbers(items);
    }

    /* A list: numbers as an array of doubles, tuples as an array of arrays, the rest as a list. */
    private Object list() {
        expect('[');
        final List<Object> items = new ArrayList<>();
        while (true) {
            skip();
            if (peek() == ']') {
                this.at++;
                break;
            }
            items.add(value());
            skip();
            if (peek() == ',') {
                this.at++;
            }
        }
        if (!items.isEmpty() && items.stream().allMatch(item -> item instanceof Double)) {
            return numbers(items);
        }
        if (!items.isEmpty() && items.stream().allMatch(item -> item instanceof double[])) {
            final double[][] out = new double[items.size()][];
            for (int i = 0; i < out.length; i++) {
                out[i] = (double[]) items.get(i);
            }
            return out;
        }
        return items;
    }

    private static Object numbers(final List<Object> items) {
        int count = 0;
        for (final Object item : items) {
            count += item instanceof double[] row ? row.length : 1;
        }
        final double[] out = new double[count];
        int k = 0;
        for (final Object item : items) {
            if (item instanceof double[] row) {
                System.arraycopy(row, 0, out, k, row.length);
                k += row.length;
            } else {
                out[k++] = item instanceof Double number ? number : item instanceof Boolean flag && flag ? 1 : 0;
            }
        }
        return out;
    }

    private UsdPrim.Asset asset() {
        final boolean triple = this.text.startsWith("@@@", this.at);
        final String fence = triple ? "@@@" : "@";
        final int start = this.at + fence.length();
        final int end = this.text.indexOf(fence, start);
        if (end < 0) {
            throw broken("an asset path that ends");
        }
        this.at = end + fence.length();
        // A path to a prim of the asset may follow it.
        if (this.at < this.text.length() && this.text.charAt(this.at) == '<') {
            this.at = this.text.indexOf('>', this.at) + 1;
        }
        return new UsdPrim.Asset(this.text.substring(start, end));
    }

    private String string() {
        final char quote = peek();
        final boolean triple = this.text.startsWith(String.valueOf(quote).repeat(3), this.at);
        final String fence = triple ? String.valueOf(quote).repeat(3) : String.valueOf(quote);
        this.at += fence.length();
        final StringBuilder out = new StringBuilder();
        while (!this.text.startsWith(fence, this.at)) {
            if (this.at >= this.text.length()) {
                throw broken("a string that ends");
            }
            char next = this.text.charAt(this.at++);
            if (next == '\\' && this.at < this.text.length()) {
                next = this.text.charAt(this.at++);
                next = switch (next) {
                    case 'n' -> '\n';
                    case 't' -> '\t';
                    default -> next;
                };
            }
            out.append(next);
        }
        this.at += fence.length();
        return out.toString();
    }

    private double number() {
        final int start = this.at;
        while (this.at < this.text.length() && "+-.0123456789eE".indexOf(this.text.charAt(this.at)) >= 0) {
            this.at++;
        }
        if (start == this.at) {
            throw broken("a number");
        }
        return Double.parseDouble(this.text.substring(start, this.at));
    }

    /* A word: letters, digits and underscores. */
    private String word() {
        final int start = this.at;
        while (this.at < this.text.length() && (Character.isLetterOrDigit(this.text.charAt(this.at))
                || this.text.charAt(this.at) == '_')) {
            this.at++;
        }
        if (start == this.at) {
            throw broken("a word, not '" + peek() + "'");
        }
        return this.text.substring(start, this.at);
    }

    /* A name: a word with the colons and dots of a namespaced property. */
    private String name() {
        final int start = this.at;
        while (this.at < this.text.length() && (Character.isLetterOrDigit(this.text.charAt(this.at))
                || "_:.".indexOf(this.text.charAt(this.at)) >= 0)) {
            this.at++;
        }
        if (start == this.at) {
            throw broken("a name, not '" + peek() + "'");
        }
        return this.text.substring(start, this.at);
    }

    /* Skips from an opening mark past the one that closes it, strings and all. */
    private void skipBalanced(final char open, final char close) {
        expect(open);
        int depth = 1;
        while (depth > 0) {
            final char next = peek();
            if (next == '"' || next == '\'') {
                string();
                continue;
            }
            if (next == '#') {
                skip();
                continue;
            }
            this.at++;
            if (next == open) {
                depth++;
            } else if (next == close) {
                depth--;
            }
        }
    }

    /* Skips white space and comments. */
    private void skip() {
        while (this.at < this.text.length()) {
            final char next = this.text.charAt(this.at);
            if (Character.isWhitespace(next)) {
                this.at++;
            } else if (next == '#') {
                final int end = this.text.indexOf('\n', this.at);
                this.at = end < 0 ? this.text.length() : end + 1;
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

    private void expect(final char wanted) {
        skip();
        if (peek() != wanted) {
            throw broken("'" + wanted + "', not '" + peek() + "'");
        }
        this.at++;
    }

    private ModelFormatException broken(final String wanted) {
        int line = 1;
        for (int i = 0; i < Math.min(this.at, this.text.length()); i++) {
            if (this.text.charAt(i) == '\n') {
                line++;
            }
        }
        return new ModelFormatException("the USD file " + this.name + " does not read at line " + line + ": it wants "
                + wanted);
    }
}
