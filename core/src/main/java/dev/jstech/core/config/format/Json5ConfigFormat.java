/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * JSON5, the JSON a person writes by hand: comments, keys without quotes, a comma after the last item, text in single
 * quotes, hexadecimal numbers, Infinity and NaN. It is read as the JSON5 specification reads it and written with
 * {@code //} comments above what they describe; the game carries no JSON5 library, so both are done here.
 */
public final class Json5ConfigFormat implements IConfigFormat {

    private static final String INDENT = "  ";
    /** A key that may be written without quotes: an identifier of the plain letters. */
    private static final Pattern BARE_KEY = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*");

    Json5ConfigFormat() {
    }

    @Override
    public String extension() {
        return "json5";
    }

    @Override
    public boolean keepsComments() {
        return true;
    }

    @Override
    public Map<String, Object> read(final byte[] file) throws ConfigFormatException {
        final Reader reader = new Reader(new String(file, StandardCharsets.UTF_8));
        reader.skipSpace();
        if (reader.atEnd()) {
            return new LinkedHashMap<>();
        }
        if (reader.peek() != '{') {
            throw new ConfigFormatException(reader.place() + "not a JSON5 object at the top of the file");
        }
        final Object top = reader.value();
        reader.skipSpace();
        if (!reader.atEnd()) {
            throw new ConfigFormatException(reader.place() + "more after the end of the object");
        }
        return PlainValues.map((Map<?, ?>) top);
    }

    @Override
    public byte[] write(final Map<String, Object> values, final IConfigComments comments) {
        final StringBuilder out = new StringBuilder();
        for (final String line : comments.at(List.of())) {
            out.append(line.isEmpty() ? "//" : "// " + line).append('\n');
        }
        writeObject(out, values, List.of(), comments, 0);
        out.append('\n');
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void writeObject(final StringBuilder out, final Map<?, ?> map, final List<String> at,
                                    final IConfigComments comments, final int depth) {
        if (map.isEmpty()) {
            out.append("{}");
            return;
        }
        out.append("{\n");
        int written = 0;
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
            final String key = String.valueOf(entry.getKey());
            final List<String> path = new ArrayList<>(at);
            path.add(key);
            final List<String> lines = comments.at(path);
            if (!lines.isEmpty() && written > 0) {
                // A described value stands apart from the one above it.
                out.append('\n');
            }
            for (final String line : lines) {
                out.append(INDENT.repeat(depth + 1)).append(line.isEmpty() ? "//" : "// " + line).append('\n');
            }
            out.append(INDENT.repeat(depth + 1)).append(BARE_KEY.matcher(key).matches() ? key : quote(key))
                    .append(": ");
            writeValue(out, entry.getValue(), path, comments, depth + 1);
            written++;
            out.append(written < map.size() ? ",\n" : "\n");
        }
        out.append(INDENT.repeat(depth)).append('}');
    }

    private static void writeValue(final StringBuilder out, final Object value, final List<String> path,
                                   final IConfigComments comments, final int depth) {
        if (value instanceof Map<?, ?> map) {
            writeObject(out, map, path, comments, depth);
        } else if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                out.append("[]");
                return;
            }
            out.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                out.append(INDENT.repeat(depth + 1));
                writeValue(out, list.get(i), path, IConfigComments.NONE, depth + 1);
                out.append(i + 1 < list.size() ? ",\n" : "\n");
            }
            out.append(INDENT.repeat(depth)).append(']');
        } else if (value instanceof Double number) {
            out.append(number.isNaN() ? "NaN" : number.isInfinite() ? (number > 0 ? "Infinity" : "-Infinity")
                    : number.toString());
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else {
            out.append(quote(String.valueOf(value)));
        }
    }

    /** Text in double quotes, with what cannot stand in them as it is escaped. */
    static String quote(final String text) {
        final StringBuilder out = new StringBuilder(text.length() + 2).append('"');
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c < 0x20 || c == ' ' || c == ' ') {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    /** Reads JSON5 text, a character at a time, into plain values; each mistake says the line and column it is at. */
    private static final class Reader {

        private final String text;
        private int at;

        Reader(final String text) {
            this.text = text;
        }

        boolean atEnd() {
            return this.at >= this.text.length();
        }

        char peek() {
            return this.text.charAt(this.at);
        }

        /** Steps over white space and comments, which JSON5 allows between any two tokens. */
        void skipSpace() throws ConfigFormatException {
            while (!atEnd()) {
                final char c = peek();
                if (Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '﻿') {
                    this.at++;
                } else if (this.text.startsWith("//", this.at)) {
                    while (!atEnd() && peek() != '\n' && peek() != '\r' && peek() != ' ' && peek() != ' ') {
                        this.at++;
                    }
                } else if (this.text.startsWith("/*", this.at)) {
                    final int end = this.text.indexOf("*/", this.at + 2);
                    if (end < 0) {
                        throw new ConfigFormatException(place() + "a comment that never ends");
                    }
                    this.at = end + 2;
                } else {
                    return;
                }
            }
        }

        Object value() throws ConfigFormatException {
            skipSpace();
            if (atEnd()) {
                throw new ConfigFormatException(place() + "the file ends where a value should be");
            }
            final char c = peek();
            if (c == '{') {
                return object();
            }
            if (c == '[') {
                return array();
            }
            if (c == '"' || c == '\'') {
                return string();
            }
            if (this.text.startsWith("true", this.at)) {
                this.at += 4;
                return Boolean.TRUE;
            }
            if (this.text.startsWith("false", this.at)) {
                this.at += 5;
                return Boolean.FALSE;
            }
            if (this.text.startsWith("null", this.at)) {
                this.at += 4;
                return null;
            }
            return number();
        }

        private Map<String, Object> object() throws ConfigFormatException {
            this.at++;
            final Map<String, Object> out = new LinkedHashMap<>();
            while (true) {
                skipSpace();
                if (atEnd()) {
                    throw new ConfigFormatException(place() + "an object that never closes");
                }
                if (peek() == '}') {
                    this.at++;
                    return out;
                }
                final String key = peek() == '"' || peek() == '\'' ? string() : identifier();
                skipSpace();
                if (atEnd() || peek() != ':') {
                    throw new ConfigFormatException(place() + "a ':' after the key '" + key + "'");
                }
                this.at++;
                final Object value = value();
                if (value != null) {
                    out.put(key, value);
                }
                skipSpace();
                if (!atEnd() && peek() == ',') {
                    this.at++;
                } else if (atEnd() || peek() != '}') {
                    throw new ConfigFormatException(place() + "a ',' or a '}' after the value of '" + key + "'");
                }
            }
        }

        private List<Object> array() throws ConfigFormatException {
            this.at++;
            final List<Object> out = new ArrayList<>();
            while (true) {
                skipSpace();
                if (atEnd()) {
                    throw new ConfigFormatException(place() + "a list that never closes");
                }
                if (peek() == ']') {
                    this.at++;
                    return out;
                }
                final Object value = value();
                if (value != null) {
                    out.add(value);
                }
                skipSpace();
                if (!atEnd() && peek() == ',') {
                    this.at++;
                } else if (atEnd() || peek() != ']') {
                    throw new ConfigFormatException(place() + "a ',' or a ']' after an item of a list");
                }
            }
        }

        private String identifier() throws ConfigFormatException {
            final int start = this.at;
            while (!atEnd() && (Character.isJavaIdentifierPart(peek()) || peek() == '$')) {
                this.at++;
            }
            if (start == this.at || !(Character.isJavaIdentifierStart(this.text.charAt(start))
                    || this.text.charAt(start) == '$')) {
                this.at = start;
                throw new ConfigFormatException(place() + "a key, in quotes or as a name");
            }
            return this.text.substring(start, this.at);
        }

        private String string() throws ConfigFormatException {
            final char quote = peek();
            this.at++;
            final StringBuilder out = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw new ConfigFormatException(place() + "text that never closes");
                }
                final char c = this.text.charAt(this.at++);
                if (c == quote) {
                    return out.toString();
                }
                if (c == '\n' || c == '\r') {
                    throw new ConfigFormatException(place() + "a line break inside text; write it as \\n");
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw new ConfigFormatException(place() + "text that never closes");
                }
                final char escaped = this.text.charAt(this.at++);
                switch (escaped) {
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'v' -> out.append('\u000B');
                    case '0' -> out.append('\0');
                    case 'x' -> out.append((char) hex(2));
                    case 'u' -> out.append((char) hex(4));
                    case '\n', ' ', ' ' -> {
                        // A backslash at the end of a line continues the text on the next one.
                    }
                    case '\r' -> {
                        if (!atEnd() && peek() == '\n') {
                            this.at++;
                        }
                    }
                    default -> out.append(escaped);
                }
            }
        }

        private int hex(final int digits) throws ConfigFormatException {
            if (this.at + digits > this.text.length()) {
                throw new ConfigFormatException(place() + "an escape cut short");
            }
            try {
                final int value = Integer.parseInt(this.text.substring(this.at, this.at + digits), 16);
                this.at += digits;
                return value;
            } catch (final NumberFormatException e) {
                throw new ConfigFormatException(place() + "an escape that is not hexadecimal");
            }
        }

        private Number number() throws ConfigFormatException {
            final int start = this.at;
            boolean negative = false;
            if (peek() == '+' || peek() == '-') {
                negative = peek() == '-';
                this.at++;
            }
            if (this.text.startsWith("Infinity", this.at)) {
                this.at += 8;
                return negative ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            }
            if (this.text.startsWith("NaN", this.at)) {
                this.at += 3;
                return Double.NaN;
            }
            if (this.text.startsWith("0x", this.at) || this.text.startsWith("0X", this.at)) {
                this.at += 2;
                final int digits = this.at;
                while (!atEnd() && Character.digit(peek(), 16) >= 0) {
                    this.at++;
                }
                if (digits == this.at) {
                    throw new ConfigFormatException(place() + "a hexadecimal number with no digits");
                }
                final long value = Long.parseUnsignedLong(this.text.substring(digits, this.at), 16);
                return JsonConfigFormat.whole(negative ? -value : value);
            }
            while (!atEnd() && (Character.isDigit(peek()) || peek() == '.' || peek() == 'e' || peek() == 'E'
                    || ((peek() == '+' || peek() == '-')
                    && (this.text.charAt(this.at - 1) == 'e' || this.text.charAt(this.at - 1) == 'E')))) {
                this.at++;
            }
            String written = this.text.substring(start, this.at);
            if (written.isEmpty() || written.equals("+") || written.equals("-")) {
                this.at = start;
                throw new ConfigFormatException(place()
                        + "a value: an object, a list, text, a number, true, false or null");
            }
            if (written.startsWith("+")) {
                written = written.substring(1);
            }
            try {
                return JsonConfigFormat.number(written);
            } catch (final NumberFormatException e) {
                this.at = start;
                throw new ConfigFormatException(place() + "a number that cannot be read: " + written);
            }
        }

        /** Where the reader is, as a mistake found there begins: the line and the column, counted from one. */
        String place() {
            int line = 1;
            int column = 1;
            for (int i = 0; i < Math.min(this.at, this.text.length()); i++) {
                if (this.text.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            return "line " + line + ", column " + column + ": ";
        }
    }
}
