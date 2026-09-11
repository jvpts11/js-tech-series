/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The string library, patterns included.
 *
 * <p>Text is text as Java holds it, one character per place, and a program that asks for bytes gets
 * the character's number. {@code gsub} with a function to call for every match runs match by match,
 * carrying what it has built so far between the calls; {@code gmatch} hands back an iterator that
 * keeps its place in an object of its own.
 */
final class LuaStrings {

    private static final String SUBJECT = "S";
    private static final String PATTERN = "P";
    private static final String POSITION = "Pos";
    private static final String REPLACEMENT = "Repl";
    private static final String OUT = "Out";
    private static final String COUNT = "Count";
    private static final String LIMIT = "Max";
    private static final String MATCH_START = "From";
    private static final String MATCH_END = "To";
    private static final int COST_PER_CHARACTERS = 64;

    private LuaStrings() {
    }

    static void register() {
        LuaLib.define("string.len", (context, target, arguments, line) ->
                (long) new LuaArgs(context, "len", arguments, line).text(0).length());
        LuaLib.define("string.sub", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "sub", arguments, line);
            final String text = args.text(0);
            final int length = text.length();
            long from = args.integer(1, 1L);
            long to = args.integer(2, -1L);
            if (from < 0) {
                from = Math.max(length + from + 1, 1);
            } else if (from == 0) {
                from = 1;
            }
            if (to < 0) {
                to = length + to + 1;
            } else if (to > length) {
                to = length;
            }
            if (from > to) {
                return context.text("", line);
            }
            return context.text(text.substring((int) from - 1, (int) to), line);
        });
        LuaLib.define("string.upper", (context, target, arguments, line) ->
                context.text(new LuaArgs(context, "upper", arguments, line).text(0).toUpperCase(Locale.ROOT), line));
        LuaLib.define("string.lower", (context, target, arguments, line) ->
                context.text(new LuaArgs(context, "lower", arguments, line).text(0).toLowerCase(Locale.ROOT), line));
        LuaLib.define("string.reverse", (context, target, arguments, line) ->
                context.text(new StringBuilder(new LuaArgs(context, "reverse", arguments, line).text(0))
                        .reverse().toString(), line));
        LuaLib.define("string.rep", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "rep", arguments, line);
            final String text = args.text(0);
            final long times = args.integer(1);
            final String separator = args.text(2, "");
            if (times <= 0) {
                return context.text("", line);
            }
            final long size = (text.length() + separator.length()) * times;
            if (size > 1_000_000) {
                throw context.error("resulting string too large", line);
            }
            final StringBuilder out = new StringBuilder((int) size);
            for (long i = 0; i < times; i++) {
                if (i > 0) {
                    out.append(separator);
                }
                out.append(text);
            }
            return context.text(out.toString(), line);
        });
        LuaLib.define("string.byte", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "byte", arguments, line);
            final String text = args.text(0);
            long from = args.integer(1, 1L);
            long to = args.integer(2, from);
            if (from < 0) {
                from = text.length() + from + 1;
            }
            if (to < 0) {
                to = text.length() + to + 1;
            }
            from = Math.max(from, 1);
            to = Math.min(to, text.length());
            final List<Object> codes = new ArrayList<>();
            for (long i = from; i <= to; i++) {
                codes.add((long) text.charAt((int) i - 1));
            }
            return context.values(codes, line);
        });
        LuaLib.define("string.char", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "char", arguments, line);
            final StringBuilder out = new StringBuilder(arguments.length);
            for (int i = 0; i < arguments.length; i++) {
                final long code = args.integer(i);
                if (code < 0 || code > 0xFFFF) {
                    throw args.bad(i, "value out of range");
                }
                out.append((char) code);
            }
            return context.text(out.toString(), line);
        });
        LuaLib.define("string.find", (context, target, arguments, line) -> find(context, arguments, true, line));
        LuaLib.define("string.match", (context, target, arguments, line) -> find(context, arguments, false, line));
        LuaLib.define("string.gmatch", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "gmatch", arguments, line);
            final Values.Obj state = context.state("string.gmatch", null, line);
            state.set(SUBJECT, context.text(args.text(0), line));
            state.set(PATTERN, context.text(args.text(1), line));
            state.set(POSITION, 0L);
            return context.function("string.gmatch.next", state, line);
        });
        LuaLib.hidden("string.gmatch.next", (context, target, arguments, line) -> {
            final Values.Obj state = (Values.Obj) target;
            final String subject = String.valueOf(state.get(SUBJECT));
            final String pattern = String.valueOf(state.get(PATTERN));
            final long position = (Long) state.get(POSITION);
            if (position > subject.length()) {
                return null;
            }
            final LuaPatterns.Match match = matched(context, subject, pattern, (int) position, line);
            if (match == null) {
                state.set(POSITION, (long) subject.length() + 1);
                return null;
            }
            // An empty match still moves on, or the same empty match would come back forever.
            state.set(POSITION, (long) (match.end() == match.start() ? match.end() + 1 : match.end()));
            return context.values(onHeap(context, match.results(subject), line), line);
        });
        LuaLib.define("string.gsub", LuaStrings::gsub);
        LuaLib.continueWith("string.gsub.next", (context, state, result, line) -> {
            final String subject = String.valueOf(state.get(SUBJECT));
            final int from = (int) (long) (Long) state.get(MATCH_START);
            final int to = (int) (long) (Long) state.get(MATCH_END);
            final Object value = LuaBase.first(context, result);
            final String piece;
            if (!LuaValues.truth(value)) {
                piece = subject.substring(from, to);
            } else {
                piece = LuaValues.asText(value);
                if (piece == null) {
                    throw context.error("invalid replacement value (a " + LuaValues.typeName(value) + ")", line);
                }
            }
            state.set(OUT, context.text(state.get(OUT) + piece, line));
            state.set(COUNT, (Long) state.get(COUNT) + 1);
            return gsubFrom(context, state, line);
        });
        LuaLib.define("string.format", LuaStrings::format);
    }

    private static Object find(final ILuaContext context, final Object[] arguments, final boolean find,
                               final int line) {
        final LuaArgs args = new LuaArgs(context, find ? "find" : "match", arguments, line);
        final String subject = args.text(0);
        final String pattern = args.text(1);
        long init = args.integer(2, 1L);
        if (init < 0) {
            init = Math.max(subject.length() + init + 1, 1);
        } else if (init == 0) {
            init = 1;
        }
        if (init > subject.length() + 1) {
            return null;
        }
        final boolean plain = find && LuaValues.truth(args.at(3));
        if (find && (plain || LuaPatterns.isPlain(pattern))) {
            final int at = subject.indexOf(pattern, (int) init - 1);
            if (at < 0) {
                return null;
            }
            return context.values(List.of((long) at + 1, (long) at + pattern.length()), line);
        }
        final LuaPatterns.Match match = matched(context, subject, pattern, (int) init - 1, line);
        if (match == null) {
            return null;
        }
        final List<Object> answer = new ArrayList<>();
        if (find) {
            answer.add((long) match.start() + 1);
            answer.add((long) match.end());
            answer.addAll(onHeap(context, match.captures(), line));
        } else {
            answer.addAll(onHeap(context, match.results(subject), line));
        }
        return context.values(answer, line);
    }

    private static LuaPatterns.Match matched(final ILuaContext context, final String subject, final String pattern,
                                             final int from, final int line) {
        try {
            return LuaPatterns.find(subject, pattern, from);
        } catch (final LuaPatterns.BadPattern bad) {
            throw context.error(bad.getMessage(), line);
        }
    }

    private static List<Object> onHeap(final ILuaContext context, final List<Object> captures, final int line) {
        final List<Object> made = new ArrayList<>(captures.size());
        for (final Object capture : captures) {
            made.add(capture instanceof String text ? context.text(text, line) : capture);
        }
        return made;
    }

    private static Object gsub(final ILuaContext context, final Object target, final Object[] arguments,
                               final int line) {
        final LuaArgs args = new LuaArgs(context, "gsub", arguments, line);
        final String subject = args.text(0);
        final String pattern = args.text(1);
        final Object replacement = args.at(2);
        if (!(replacement instanceof String || LuaNumbers.isNumber(replacement)
                || replacement instanceof Values.Table || replacement instanceof Values.DelegateValue)) {
            throw args.bad(2, "string/function/table expected");
        }
        final Values.Obj state = context.state("string.gsub.next", null, line);
        state.set(SUBJECT, context.text(subject, line));
        state.set(PATTERN, context.text(pattern, line));
        state.set(REPLACEMENT, replacement instanceof String ? context.text((String) replacement, line)
                : LuaNumbers.isNumber(replacement) ? context.text(LuaNumbers.format(replacement), line)
                : replacement);
        state.set(POSITION, 0L);
        state.set(OUT, context.text("", line));
        state.set(COUNT, 0L);
        state.set(LIMIT, args.has(3) ? args.integer(3) : Long.MAX_VALUE);
        return gsubFrom(context, state, line);
    }

    /** Replaces match after match until a function has to be called, or the subject runs out. */
    private static Object gsubFrom(final ILuaContext context, final Values.Obj state, final int line) {
        final String subject = String.valueOf(state.get(SUBJECT));
        final String pattern = String.valueOf(state.get(PATTERN));
        final Object replacement = state.get(REPLACEMENT);
        final boolean anchored = !pattern.isEmpty() && pattern.charAt(0) == '^';
        int position = (int) (long) (Long) state.get(POSITION);
        long count = (Long) state.get(COUNT);
        final long limit = (Long) state.get(LIMIT);
        final StringBuilder out = new StringBuilder(String.valueOf(state.get(OUT)));
        while (count < limit && position <= subject.length()) {
            final LuaPatterns.Match match = matched(context, subject, pattern, position, line);
            if (match == null || match.start() != position && anchored) {
                break;
            }
            // Text before the match is kept as it is.
            out.append(subject, position, match.start());
            if (replacement instanceof Values.DelegateValue function) {
                state.set(OUT, context.text(out.toString(), line));
                state.set(COUNT, count);
                state.set(MATCH_START, (long) match.start());
                state.set(MATCH_END, (long) match.end());
                state.set(POSITION, (long) (match.end() == match.start() ? match.end() + 1 : match.end()));
                if (match.end() == match.start() && match.start() < subject.length()) {
                    /*
                     * An empty match keeps the character it sits before, which has to be put after the
                     * replacement, so it is remembered as the next stretch to copy.
                     */
                    state.set(POSITION, (long) match.end());
                }
                return new LuaCall(function, onHeap(context, match.results(subject), line).toArray(), state);
            }
            out.append(expandReplacement(context, subject, match, replacement, line));
            count++;
            if (match.end() > match.start()) {
                position = match.end();
            } else {
                if (position < subject.length()) {
                    out.append(subject.charAt(position));
                }
                position++;
            }
            if (anchored) {
                break;
            }
        }
        if (position < subject.length()) {
            out.append(subject, position, subject.length());
        }
        final String made = out.toString();
        return context.values(List.of(context.text(made, line), count), line);
    }

    private static String expandReplacement(final ILuaContext context, final String subject,
                                            final LuaPatterns.Match match, final Object replacement,
                                            final int line) {
        final List<Object> results = match.results(subject);
        if (replacement instanceof Values.Table table) {
            final Object value = table.get(results.getFirst());
            if (!LuaValues.truth(value)) {
                return subject.substring(match.start(), match.end());
            }
            final String text = LuaValues.asText(value);
            if (text == null) {
                throw context.error("invalid replacement value (a " + LuaValues.typeName(value) + ")", line);
            }
            return text;
        }
        final String template = String.valueOf(replacement);
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < template.length(); i++) {
            final char c = template.charAt(i);
            if (c != '%') {
                out.append(c);
                continue;
            }
            if (++i >= template.length()) {
                throw context.error("invalid use of '%' in replacement string", line);
            }
            final char what = template.charAt(i);
            if (what == '%') {
                out.append('%');
            } else if (what == '0') {
                out.append(subject, match.start(), match.end());
            } else if (Character.isDigit(what)) {
                final int index = what - '1';
                if (index >= results.size()) {
                    throw context.error("invalid capture index %" + (index + 1) + " in replacement string", line);
                }
                out.append(LuaValues.plainString(results.get(index)));
            } else {
                throw context.error("invalid use of '%' in replacement string", line);
            }
        }
        return out.toString();
    }

    // format

    private static Object format(final ILuaContext context, final Object target, final Object[] arguments,
                                 final int line) {
        final LuaArgs args = new LuaArgs(context, "format", arguments, line);
        final String template = args.text(0);
        final StringBuilder out = new StringBuilder();
        int next = 1;
        for (int i = 0; i < template.length(); i++) {
            final char c = template.charAt(i);
            if (c != '%') {
                out.append(c);
                continue;
            }
            if (i + 1 < template.length() && template.charAt(i + 1) == '%') {
                out.append('%');
                i++;
                continue;
            }
            final int start = i;
            i++;
            while (i < template.length() && "-+ #0".indexOf(template.charAt(i)) >= 0) {
                i++;
            }
            while (i < template.length() && Character.isDigit(template.charAt(i))) {
                i++;
            }
            if (i < template.length() && template.charAt(i) == '.') {
                i++;
                while (i < template.length() && Character.isDigit(template.charAt(i))) {
                    i++;
                }
            }
            if (i >= template.length()) {
                throw context.error("invalid conversion '" + template.substring(start) + "' to 'format'", line);
            }
            final char conversion = template.charAt(i);
            final String spec = template.substring(start, i);
            if (next >= arguments.length && conversion != '%') {
                throw args.bad(next, "no value");
            }
            final int index = next++;
            switch (conversion) {
                case 'd', 'i' -> out.append(String.format(Locale.ROOT, spec + "d", args.integer(index)));
                case 'u' -> out.append(String.format(Locale.ROOT, spec + "d", Math.abs(args.integer(index))));
                case 'c' -> out.append((char) args.integer(index));
                case 'x', 'X', 'o' -> out.append(String.format(Locale.ROOT, spec + conversion, args.integer(index)));
                case 'e', 'E', 'f', 'F' -> out.append(String.format(Locale.ROOT,
                        spec + Character.toLowerCase(conversion), args.real(index)));
                case 'g', 'G' -> out.append(general(spec, conversion, args.real(index)));
                case 'a', 'A' -> out.append(String.format(Locale.ROOT, spec + conversion, args.real(index)));
                case 's' -> out.append(String.format(Locale.ROOT, spec + "s", stringOf(args.at(index))));
                case 'q' -> out.append(quoted(String.valueOf(stringOf(args.at(index)))));
                default -> throw context.error("invalid conversion '%" + conversion + "' to 'format'", line);
            }
        }
        return context.text(out.toString(), line);
    }

    private static String stringOf(final Object value) {
        return LuaValues.plainString(value);
    }

    /* Java's %g keeps trailing zeros and never chooses the fixed form the way C does, so it is done here. */
    private static String general(final String spec, final char conversion, final double value) {
        int precision = 6;
        final int dot = spec.indexOf('.');
        if (dot >= 0) {
            final String digits = spec.substring(dot + 1);
            precision = digits.isEmpty() ? 0 : Integer.parseInt(digits);
        }
        String body = Double.isNaN(value) ? "nan" : Double.isInfinite(value) ? (value < 0 ? "-inf" : "inf")
                : LuaNumbers.general(value, precision == 0 ? 1 : precision);
        if (conversion == 'G') {
            body = body.toUpperCase(Locale.ROOT);
        }
        final String flags = dot >= 0 ? spec.substring(1, dot) : spec.substring(1);
        int width = 0;
        final StringBuilder digits = new StringBuilder();
        boolean left = false;
        boolean plus = false;
        for (int i = 0; i < flags.length(); i++) {
            final char c = flags.charAt(i);
            if (Character.isDigit(c) && (c != '0' || !digits.isEmpty())) {
                digits.append(c);
            } else if (c == '-') {
                left = true;
            } else if (c == '+') {
                plus = true;
            }
        }
        if (!digits.isEmpty()) {
            width = Integer.parseInt(digits.toString());
        }
        if (plus && value >= 0) {
            body = "+" + body;
        }
        final StringBuilder out = new StringBuilder(body);
        while (out.length() < width) {
            if (left) {
                out.append(' ');
            } else {
                out.insert(0, ' ');
            }
        }
        return out.toString();
    }

    private static String quoted(final String text) {
        final StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\0' -> out.append("\\0");
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
}
