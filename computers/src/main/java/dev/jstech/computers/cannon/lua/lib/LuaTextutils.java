/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ComputerCraft's {@code textutils}: tables written as Lua and as JSON and read back, times, columns.
 *
 * <p>Reading a table back does not run the text as a program, the way ComputerCraft's own does with
 * {@code load}: a small reader takes the literal apart instead, so a program that saves its state
 * every tick does not compile a new chunk every tick.
 */
final class LuaTextutils {

    private static final String JSON_NULL = "json_null";

    private LuaTextutils() {
    }

    static void register() {
        LuaLib.define("textutils.serialize", LuaTextutils::serialize);
        LuaLib.define("textutils.serialise", LuaTextutils::serialize);
        LuaLib.define("textutils.unserialize", LuaTextutils::unserialize);
        LuaLib.define("textutils.unserialise", LuaTextutils::unserialize);
        LuaLib.define("textutils.serializeJSON", LuaTextutils::serializeJson);
        LuaLib.define("textutils.serialiseJSON", LuaTextutils::serializeJson);
        LuaLib.define("textutils.unserializeJSON", LuaTextutils::unserializeJson);
        LuaLib.define("textutils.unserialiseJSON", LuaTextutils::unserializeJson);
        LuaLib.define("textutils.formatTime", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "formatTime", arguments, line);
            final double time = args.real(0);
            final boolean twentyFour = LuaValues.truth(args.at(1));
            final int hour = (int) Math.floor(time) % 24;
            final int minute = (int) Math.floor((time - Math.floor(time)) * 60);
            if (twentyFour) {
                return context.text(String.format(Locale.ROOT, "%d:%02d", hour, minute), line);
            }
            final int twelve = hour % 12 == 0 ? 12 : hour % 12;
            return context.text(String.format(Locale.ROOT, "%d:%02d %s", twelve, minute, hour < 12 ? "AM" : "PM"),
                    line);
        });
        LuaLib.define("textutils.urlEncode", (context, target, arguments, line) -> {
            final String text = new LuaArgs(context, "urlEncode", arguments, line).text(0);
            final StringBuilder out = new StringBuilder();
            for (final byte b : text.getBytes(StandardCharsets.UTF_8)) {
                final char c = (char) (b & 0xFF);
                if (Character.isLetterOrDigit(c) && c < 128 || "-_.~".indexOf(c) >= 0) {
                    out.append(c);
                } else if (c == ' ') {
                    out.append('+');
                } else {
                    out.append('%').append(String.format(Locale.ROOT, "%02X", b & 0xFF));
                }
            }
            return context.text(out.toString(), line);
        });
        LuaLib.define("textutils.slowWrite", (context, target, arguments, line) -> {
            context.write(new LuaArgs(context, "slowWrite", arguments, line).text(0));
            return null;
        });
        LuaLib.define("textutils.slowPrint", (context, target, arguments, line) -> {
            context.print(new LuaArgs(context, "slowPrint", arguments, line).text(0));
            return null;
        });
        LuaLib.define("textutils.pagedPrint", (context, target, arguments, line) -> {
            context.print(new LuaArgs(context, "pagedPrint", arguments, line).text(0));
            return 0L;
        });
        LuaLib.define("textutils.tabulate", LuaTextutils::tabulate);
        LuaLib.define("textutils.pagedTabulate", LuaTextutils::tabulate);
        LuaLib.define("textutils.complete", (context, target, arguments, line) -> context.table(line));
        LuaLib.constant("textutils.empty_json_array", "[]");
        LuaLib.constant("textutils.json_null", JSON_NULL);
    }

    // Lua literals

    private static Object serialize(final ILuaContext context, final Object target, final Object[] arguments,
                                    final int line) {
        final Object value = arguments.length > 0 ? arguments[0] : null;
        boolean compact = false;
        if (arguments.length > 1 && arguments[1] instanceof Values.Table options) {
            compact = LuaValues.truth(options.get("compact"));
        }
        final StringBuilder out = new StringBuilder();
        writeLua(context, value, out, "", compact, new IdentityHashMap<>(), line);
        return context.text(out.toString(), line);
    }

    private static void writeLua(final ILuaContext context, final Object value, final StringBuilder out,
                                 final String indent, final boolean compact, final Map<Object, Boolean> open,
                                 final int line) {
        if (value == null) {
            out.append("nil");
        } else if (value instanceof Boolean flag) {
            out.append(flag);
        } else if (value instanceof Long whole) {
            out.append(whole);
        } else if (value instanceof Double real) {
            if (real.isNaN()) {
                out.append("0/0");
            } else if (real.isInfinite()) {
                out.append(real > 0 ? "1/0" : "-1/0");
            } else {
                out.append(LuaNumbers.general(real, 17));
            }
        } else if (value instanceof String text) {
            out.append(quoted(text));
        } else if (value instanceof Values.Table table) {
            if (open.containsKey(table)) {
                throw context.error("Cannot serialize table with repeated entries", line);
            }
            open.put(table, true);
            final String inner = indent + "  ";
            final String separator = compact ? "," : ",\n";
            out.append(compact ? "{" : "{\n");
            final long run = table.length();
            for (long i = 1; i <= run; i++) {
                out.append(compact ? "" : inner);
                writeLua(context, table.get(i), out, inner, compact, open, line);
                out.append(separator);
            }
            for (Object key = table.nextKey(null); key != null; key = table.nextKey(key)) {
                if (key instanceof Long index && index >= 1 && index <= run) {
                    continue;
                }
                out.append(compact ? "" : inner);
                if (key instanceof String name && isName(name)) {
                    out.append(name).append(compact ? "=" : " = ");
                } else {
                    out.append(compact ? "[" : "[ ");
                    writeLua(context, key, out, inner, compact, open, line);
                    out.append(compact ? "]=" : " ] = ");
                }
                writeLua(context, table.get(key), out, inner, compact, open, line);
                out.append(separator);
            }
            out.append(compact ? "" : indent).append('}');
            open.remove(table);
        } else {
            throw context.error("Cannot serialize type " + LuaValues.typeName(value), line);
        }
    }

    private static boolean isName(final String text) {
        if (text.isEmpty() || !(Character.isLetter(text.charAt(0)) || text.charAt(0) == '_')
                || dev.jstech.computers.cannon.lua.LuaTokenKind.keyword(text) != null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (!(c < 128 && (Character.isLetterOrDigit(c) || c == '_'))) {
                return false;
            }
        }
        return true;
    }

    static String quoted(final String text) {
        final StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32 || c == 127) {
                        out.append('\\').append((int) c);
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private static Object unserialize(final ILuaContext context, final Object target, final Object[] arguments,
                                      final int line) {
        final String text = new LuaArgs(context, "unserialize", arguments, line).text(0);
        try {
            final LiteralReader reader = new LiteralReader(context, text, line);
            final Object value = reader.value();
            reader.skip();
            return reader.atEnd() ? value : null;
        } catch (final IllegalArgumentException notALiteral) {
            return null;
        }
    }

    /** Reads the literals {@code serialize} writes: tables, text, numbers, true, false and nil. */
    private static final class LiteralReader {
        private final ILuaContext context;
        private final String text;
        private final int line;
        private int at;

        LiteralReader(final ILuaContext context, final String text, final int line) {
            this.context = context;
            this.text = text;
            this.line = line;
        }

        boolean atEnd() {
            return this.at >= this.text.length();
        }

        void skip() {
            while (!this.atEnd()) {
                final char c = this.text.charAt(this.at);
                if (Character.isWhitespace(c)) {
                    this.at++;
                } else if (this.text.startsWith("--", this.at)) {
                    while (!this.atEnd() && this.text.charAt(this.at) != '\n') {
                        this.at++;
                    }
                } else {
                    return;
                }
            }
        }

        private char peek() {
            if (this.atEnd()) {
                throw new IllegalArgumentException("end of text");
            }
            return this.text.charAt(this.at);
        }

        Object value() {
            this.skip();
            final char c = this.peek();
            if (c == '{') {
                return this.table();
            }
            if (c == '"' || c == '\'') {
                return this.context.text(this.string(), this.line);
            }
            if (c == '[' && this.at + 1 < this.text.length() && (this.text.charAt(this.at + 1) == '['
                    || this.text.charAt(this.at + 1) == '=')) {
                return this.context.text(this.longString(), this.line);
            }
            if (Character.isLetter(c) || c == '_') {
                final int start = this.at;
                while (!this.atEnd() && (Character.isLetterOrDigit(this.text.charAt(this.at))
                        || this.text.charAt(this.at) == '_')) {
                    this.at++;
                }
                return switch (this.text.substring(start, this.at)) {
                    case "true" -> Boolean.TRUE;
                    case "false" -> Boolean.FALSE;
                    case "nil" -> null;
                    default -> throw new IllegalArgumentException("not a literal");
                };
            }
            return this.number();
        }

        private Object number() {
            final int start = this.at;
            if (this.peek() == '-' || this.peek() == '+') {
                this.at++;
            }
            while (!this.atEnd() && (Character.isLetterOrDigit(this.text.charAt(this.at))
                    || this.text.charAt(this.at) == '.'
                    || (this.text.charAt(this.at - 1) == 'e' || this.text.charAt(this.at - 1) == 'E')
                    && (this.text.charAt(this.at) == '-' || this.text.charAt(this.at) == '+'))) {
                this.at++;
            }
            final String written = this.text.substring(start, this.at);
            // What serialize writes for the reals that have no digits.
            if (!this.atEnd() && this.text.charAt(this.at) == '/') {
                this.at++;
                final int after = this.at;
                while (!this.atEnd() && Character.isDigit(this.text.charAt(this.at))) {
                    this.at++;
                }
                final Object top = LuaNumbers.parse(written);
                final Object bottom = LuaNumbers.parse(this.text.substring(after, this.at));
                if (top == null || bottom == null) {
                    throw new IllegalArgumentException("not a number");
                }
                return LuaNumbers.toDouble(top) / LuaNumbers.toDouble(bottom);
            }
            final Object number = LuaNumbers.parse(written);
            if (number == null) {
                throw new IllegalArgumentException("not a number");
            }
            return number;
        }

        private String string() {
            final char quote = this.text.charAt(this.at++);
            final StringBuilder out = new StringBuilder();
            while (true) {
                final char c = this.peek();
                this.at++;
                if (c == quote) {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                final char escaped = this.peek();
                this.at++;
                switch (escaped) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case 'r' -> out.append('\r');
                    case 'a' -> out.append('');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'v' -> out.append('');
                    case '\n' -> out.append('\n');
                    default -> {
                        if (Character.isDigit(escaped)) {
                            int value = escaped - '0';
                            for (int i = 0; i < 2 && !this.atEnd() && Character.isDigit(this.peek()); i++) {
                                value = value * 10 + (this.text.charAt(this.at++) - '0');
                            }
                            out.append((char) value);
                        } else {
                            out.append(escaped);
                        }
                    }
                }
            }
        }

        private String longString() {
            int level = 0;
            this.at++;
            while (this.peek() == '=') {
                level++;
                this.at++;
            }
            this.at++;
            final String close = "]" + "=".repeat(level) + "]";
            final int end = this.text.indexOf(close, this.at);
            if (end < 0) {
                throw new IllegalArgumentException("unfinished long string");
            }
            String body = this.text.substring(this.at, end);
            if (body.startsWith("\n")) {
                body = body.substring(1);
            }
            this.at = end + close.length();
            return body;
        }

        private Values.Table table() {
            this.at++;
            final Values.Table made = this.context.table(this.line);
            long position = 1;
            while (true) {
                this.skip();
                if (this.peek() == '}') {
                    this.at++;
                    this.context.resized(made, this.line);
                    return made;
                }
                if (this.peek() == '[' && this.at + 1 < this.text.length() && this.text.charAt(this.at + 1) != '['
                        && this.text.charAt(this.at + 1) != '=') {
                    this.at++;
                    final Object key = this.value();
                    this.skip();
                    this.expect(']');
                    this.skip();
                    this.expect('=');
                    made.put(key, this.value());
                } else if (Character.isLetter(this.peek()) || this.peek() == '_') {
                    final int mark = this.at;
                    while (!this.atEnd() && (Character.isLetterOrDigit(this.peek()) || this.peek() == '_')) {
                        this.at++;
                    }
                    final String name = this.text.substring(mark, this.at);
                    this.skip();
                    if (!this.atEnd() && this.peek() == '=') {
                        this.at++;
                        made.put(this.context.text(name, this.line), this.value());
                    } else {
                        this.at = mark;
                        made.put(position++, this.value());
                    }
                } else {
                    made.put(position++, this.value());
                }
                this.skip();
                if (this.peek() == ',' || this.peek() == ';') {
                    this.at++;
                }
            }
        }

        private void expect(final char c) {
            if (this.peek() != c) {
                throw new IllegalArgumentException("expected " + c);
            }
            this.at++;
        }
    }

    // JSON

    private static Object serializeJson(final ILuaContext context, final Object target, final Object[] arguments,
                                        final int line) {
        final Object value = arguments.length > 0 ? arguments[0] : null;
        final boolean bareKeys = arguments.length > 1 && LuaValues.truth(arguments[1]);
        final StringBuilder out = new StringBuilder();
        writeJson(context, value, out, bareKeys, new IdentityHashMap<>(), line);
        return context.text(out.toString(), line);
    }

    private static void writeJson(final ILuaContext context, final Object value, final StringBuilder out,
                                  final boolean bareKeys, final Map<Object, Boolean> open, final int line) {
        if (value == null || JSON_NULL.equals(value)) {
            out.append("null");
        } else if (value instanceof Boolean || value instanceof Long) {
            out.append(value);
        } else if (value instanceof Double real) {
            out.append(real.isNaN() || real.isInfinite() ? "null" : LuaNumbers.general(real, 17));
        } else if (value instanceof String text) {
            out.append(jsonString(text));
        } else if (value instanceof Values.Table table) {
            if (open.containsKey(table)) {
                throw context.error("Cannot serialize table with repeated entries", line);
            }
            open.put(table, true);
            final long run = table.length();
            int keys = 0;
            for (Object key = table.nextKey(null); key != null; key = table.nextKey(key)) {
                keys++;
            }
            if (keys == run) {
                out.append('[');
                for (long i = 1; i <= run; i++) {
                    if (i > 1) {
                        out.append(',');
                    }
                    writeJson(context, table.get(i), out, bareKeys, open, line);
                }
                out.append(']');
            } else {
                out.append('{');
                boolean first = true;
                for (Object key = table.nextKey(null); key != null; key = table.nextKey(key)) {
                    if (!(key instanceof String) && !LuaNumbers.isNumber(key)) {
                        continue;
                    }
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    final String name = LuaValues.plainString(key);
                    out.append(bareKeys && isName(name) ? name : jsonString(name)).append(':');
                    writeJson(context, table.get(key), out, bareKeys, open, line);
                }
                out.append('}');
            }
            open.remove(table);
        } else {
            throw context.error("Cannot serialize type " + LuaValues.typeName(value), line);
        }
    }

    private static String jsonString(final String text) {
        final StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 32) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private static Object unserializeJson(final ILuaContext context, final Object target, final Object[] arguments,
                                          final int line) {
        final String text = new LuaArgs(context, "unserializeJSON", arguments, line).text(0);
        boolean keepNull = false;
        if (arguments.length > 1 && arguments[1] instanceof Values.Table options) {
            keepNull = LuaValues.truth(options.get("parse_null"));
        }
        try {
            final JsonReader reader = new JsonReader(context, text, keepNull, line);
            final Object value = reader.value();
            reader.skip();
            if (!reader.atEnd()) {
                throw new IllegalArgumentException("trailing text at " + (reader.at + 1));
            }
            return value;
        } catch (final IllegalArgumentException | StringIndexOutOfBoundsException wrong) {
            return context.values(java.util.Arrays.asList(null, context.text(String.valueOf(wrong.getMessage()),
                    line)), line);
        }
    }

    /** Reads JSON into tables: arrays run from one, objects keyed by their names. */
    private static final class JsonReader {
        private final ILuaContext context;
        private final String text;
        private final boolean keepNull;
        private final int line;
        private int at;

        JsonReader(final ILuaContext context, final String text, final boolean keepNull, final int line) {
            this.context = context;
            this.text = text;
            this.keepNull = keepNull;
            this.line = line;
        }

        boolean atEnd() {
            return this.at >= this.text.length();
        }

        void skip() {
            while (!this.atEnd() && Character.isWhitespace(this.text.charAt(this.at))) {
                this.at++;
            }
        }

        Object value() {
            this.skip();
            final char c = this.text.charAt(this.at);
            switch (c) {
                case '{' -> {
                    this.at++;
                    final Values.Table made = this.context.table(this.line);
                    this.skip();
                    if (this.text.charAt(this.at) == '}') {
                        this.at++;
                        return made;
                    }
                    while (true) {
                        this.skip();
                        final String key = this.string();
                        this.skip();
                        this.expect(':');
                        final Object value = this.value();
                        if (value != null) {
                            made.put(this.context.text(key, this.line), value);
                        }
                        this.skip();
                        if (this.text.charAt(this.at) == ',') {
                            this.at++;
                            continue;
                        }
                        this.expect('}');
                        this.context.resized(made, this.line);
                        return made;
                    }
                }
                case '[' -> {
                    this.at++;
                    final Values.Table made = this.context.table(this.line);
                    this.skip();
                    if (this.text.charAt(this.at) == ']') {
                        this.at++;
                        return made;
                    }
                    long position = 1;
                    while (true) {
                        made.put(position++, this.value());
                        this.skip();
                        if (this.text.charAt(this.at) == ',') {
                            this.at++;
                            continue;
                        }
                        this.expect(']');
                        this.context.resized(made, this.line);
                        return made;
                    }
                }
                case '"' -> {
                    return this.context.text(this.string(), this.line);
                }
                default -> {
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
                        return this.keepNull ? this.context.text(JSON_NULL, this.line) : null;
                    }
                    final int start = this.at;
                    while (!this.atEnd() && "+-0123456789.eE".indexOf(this.text.charAt(this.at)) >= 0) {
                        this.at++;
                    }
                    final Object number = LuaNumbers.parse(this.text.substring(start, this.at));
                    if (number == null) {
                        throw new IllegalArgumentException("unexpected character at " + (start + 1));
                    }
                    return number;
                }
            }
        }

        private String string() {
            this.expect('"');
            final StringBuilder out = new StringBuilder();
            while (true) {
                final char c = this.text.charAt(this.at++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                final char escaped = this.text.charAt(this.at++);
                switch (escaped) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    case 'r' -> out.append('\r');
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'u' -> {
                        out.append((char) Integer.parseInt(this.text.substring(this.at, this.at + 4), 16));
                        this.at += 4;
                    }
                    default -> out.append(escaped);
                }
            }
        }

        private void expect(final char c) {
            if (this.atEnd() || this.text.charAt(this.at) != c) {
                throw new IllegalArgumentException("expected '" + c + "' at " + (this.at + 1));
            }
            this.at++;
        }
    }

    // columns

    private static Object tabulate(final ILuaContext context, final Object target, final Object[] arguments,
                                   final int line) {
        final List<List<String>> rows = new ArrayList<>();
        int widest = 0;
        for (final Object argument : arguments) {
            if (argument instanceof Values.Table table) {
                final List<String> row = new ArrayList<>();
                for (long i = 1; i <= table.length(); i++) {
                    final String cell = LuaValues.plainString(table.get(i));
                    row.add(cell);
                    widest = Math.max(widest, cell.length());
                }
                rows.add(row);
            }
        }
        final int columnWidth = widest + 1;
        final int columns = Math.max(1, LuaTerminal.WIDTH / Math.max(1, columnWidth));
        for (final List<String> row : rows) {
            final StringBuilder out = new StringBuilder();
            for (int i = 0; i < row.size(); i++) {
                if (i > 0 && i % columns == 0) {
                    context.print(out.toString().stripTrailing());
                    out.setLength(0);
                }
                out.append(String.format(Locale.ROOT, "%-" + columnWidth + "s", row.get(i)));
            }
            context.print(out.toString().stripTrailing());
        }
        return null;
    }
}
