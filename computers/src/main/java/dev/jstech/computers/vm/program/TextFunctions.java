/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IPureContext;
import dev.jstech.computers.vm.system.IntrinsicRegistry;
import dev.jstech.computers.vm.system.IntrinsicTypes;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import java.util.regex.Pattern;

/** What the language does with text, joining it, cutting it, searching it and changing it, and with one character. */
@TextHolder
final class TextFunctions {

    private static final String TEXT = IntrinsicTypes.TEXT;
    /** A single character, whose type is only reached for what the language says about one. */
    private static final String CHAR = "char";
    private static final TextKey NO_START = TextKey.of("jsc.vm.text_functions.no_start",
            "there is no place %s to start from in a string of %s");
    private static final TextKey NO_RUN = TextKey.of("jsc.vm.text_functions.no_run",
            "there are no %s characters from place %s in a string of %s");
    private static final TextKey NOT_A_HOLE = TextKey.of("jsc.vm.text_functions.not_a_hole",
            "'%s' is no printf hole this machine writes");

    private TextFunctions() {
    }

    static void register(final IntrinsicRegistry.Builder registry) {
        // Joining is written by the compiler for "+" on text, with whatever the two sides are.
        registry.onType(TEXT, "Concat", TEXT, (context, target, arguments, line) ->
                context.text(String.valueOf(arguments[0]) + String.valueOf(arguments[1]), line), "T", "U");
        // Written by the compiler where a character is joined to text, since a character runs as its number.
        registry.onType(TEXT, "FromChar", TEXT, (context, target, arguments, line) ->
                context.text(String.valueOf((char) Numbers.toInt(arguments[0])), line), "char");
        // Written by the compiler for a printf hole with a width, a precision, flags or a base.
        registry.onType(TEXT, "Printf", TEXT, TextFunctions::printf, TEXT, "object");
        // Character by character, and only which way it goes: -1, 0 or 1, whatever the difference was.
        registry.onType(TEXT, "Compare", "int", (context, target, arguments, line) ->
                Integer.signum(String.valueOf(arguments[0]).compareTo(String.valueOf(arguments[1]))), TEXT, TEXT);
        registry.onType(CHAR, "ToUpper", CHAR, (context, target, arguments, line) ->
                Character.toUpperCase(letter(arguments[0])), CHAR);
        registry.onType(CHAR, "ToLower", CHAR, (context, target, arguments, line) ->
                Character.toLowerCase(letter(arguments[0])), CHAR);
        registry.onType(CHAR, "IsDigit", "bool", (context, target, arguments, line) ->
                Character.isDigit(letter(arguments[0])), CHAR);
        registry.onType(CHAR, "IsLetter", "bool", (context, target, arguments, line) ->
                Character.isLetter(letter(arguments[0])), CHAR);
        registry.onType(CHAR, "IsWhiteSpace", "bool", (context, target, arguments, line) ->
                Character.isWhitespace(letter(arguments[0])), CHAR);
        registry.onType(TEXT, "Format", TEXT, TextFunctions::format, TEXT, "object");
        registry.onType(TEXT, "Format", TEXT, TextFunctions::format, TEXT, "object", "object");
        registry.onObject(TEXT, "Substring", TEXT, TextFunctions::substring, "int");
        registry.onObject(TEXT, "Substring", TEXT, TextFunctions::substring, "int", "int");
        registry.onObject(TEXT, "IndexOf", "int", (context, target, arguments, line) ->
                String.valueOf(target).indexOf(String.valueOf(arguments[0])), TEXT);
        registry.onObject(TEXT, "Contains", "bool", (context, target, arguments, line) ->
                String.valueOf(target).contains(String.valueOf(arguments[0])), TEXT);
        registry.onObject(TEXT, "StartsWith", "bool", (context, target, arguments, line) ->
                String.valueOf(target).startsWith(String.valueOf(arguments[0])), TEXT);
        registry.onObject(TEXT, "EndsWith", "bool", (context, target, arguments, line) ->
                String.valueOf(target).endsWith(String.valueOf(arguments[0])), TEXT);
        registry.onObject(TEXT, "ToUpper", TEXT, (context, target, arguments, line) ->
                context.text(String.valueOf(target).toUpperCase(Locale.ROOT), line));
        registry.onObject(TEXT, "ToLower", TEXT, (context, target, arguments, line) ->
                context.text(String.valueOf(target).toLowerCase(Locale.ROOT), line));
        registry.onObject(TEXT, "Trim", TEXT, (context, target, arguments, line) ->
                context.text(String.valueOf(target).strip(), line));
        registry.onObject(TEXT, "Replace", TEXT, (context, target, arguments, line) -> context.text(
                String.valueOf(target).replace(String.valueOf(arguments[0]), String.valueOf(arguments[1])), line),
                TEXT, TEXT);
        registry.onObject(TEXT, "Split", "List<string>", TextFunctions::split, "char");
    }

    private static Object format(final IPureContext context, final Object target, final Object[] arguments,
                                 final int line) {
        final String pattern = String.valueOf(arguments[0]);
        final StringBuilder result = new StringBuilder(pattern.length());
        int at = 0;
        while (at < pattern.length()) {
            final int end = placeholderEnd(pattern, at, arguments.length - 1);
            if (end < 0) {
                result.append(pattern.charAt(at++));
                continue;
            }
            // What is inserted is never read again, so an argument that contains "{1}" keeps it as written.
            final int index = Integer.parseInt(pattern.substring(at + 1, end));
            result.append(arguments[index + 1]);
            at = end + 1;
        }
        return context.text(result.toString(), line);
    }

    /* Where the closing brace is when a {n} naming one of the arguments starts at that place, otherwise -1. */
    private static int placeholderEnd(final String pattern, final int start, final int count) {
        if (pattern.charAt(start) != '{') {
            return -1;
        }
        int end = start + 1;
        while (end < pattern.length() && pattern.charAt(end) >= '0' && pattern.charAt(end) <= '9') {
            end++;
        }
        if (end == start + 1 || end >= pattern.length() || pattern.charAt(end) != '}' || end - start > 10) {
            return -1;
        }
        return Long.parseLong(pattern.substring(start + 1, end)) < count ? end : -1;
    }

    /** A character as it runs, which is its number, as the character it is. */
    private static char letter(final Object value) {
        return (char) Numbers.toInt(value);
    }

    /**
     * A value put in a printf hole the way C puts one. The hole was read when the program was compiled, so one that
     * does not read here was written by hand into a listing, and stops the program saying so.
     */
    private static Object printf(final IPureContext context, final Object target, final Object[] arguments,
                                 final int line) {
        final HoleFormat hole = HoleFormat.read(String.valueOf(arguments[0]));
        if (hole == null) {
            throw new Halt(Halt.Reason.REFUSED, line, NOT_A_HOLE.with(String.valueOf(arguments[0])));
        }
        return context.text(hole.apply(arguments[1]), line);
    }

    /**
     * The piece of a string a Substring asks for: from a place to the end, or so many characters from it.
     *
     * <p>The place and the count are checked against the string first, so a program asking for more than is there
     * stops with a message saying what it asked for, and never hands the runtime a range it cannot take.
     */
    private static Object substring(final IPureContext context, final Object target, final Object[] arguments,
                                    final int line) {
        final String value = String.valueOf(target);
        final int start = Numbers.toInt(arguments[0]);
        final boolean toEnd = arguments.length == 1;
        final long length = toEnd ? (long) value.length() - start : Numbers.toInt(arguments[1]);
        if (start < 0 || start > value.length() || length < 0 || start + length > value.length()) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, toEnd
                    ? NO_START.with(start, value.length())
                    : NO_RUN.with(length, start, value.length()));
        }
        return context.text(value.substring(start, (int) (start + length)), line);
    }

    private static Object split(final IPureContext context, final Object target, final Object[] arguments,
                                final int line) {
        final Values.ListValue made = new Values.ListValue();
        context.allocate(made, made.bytes(), line);
        for (final String part : String.valueOf(target).split(Pattern.quote(String.valueOf(arguments[0])), -1)) {
            made.items().add(context.text(part, line));
        }
        context.resize(made, made.bytes(), line);
        return made;
    }
}
