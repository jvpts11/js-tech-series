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

/** What the language does with text: joining it, cutting it, searching it and changing it. */
@TextHolder
final class TextFunctions {

    private static final String TEXT = IntrinsicTypes.TEXT;
    private static final TextKey NO_START = TextKey.of("jsc.vm.text_functions.no_start",
            "there is no place %s to start from in a string of %s");
    private static final TextKey NO_RUN = TextKey.of("jsc.vm.text_functions.no_run",
            "there are no %s characters from place %s in a string of %s");

    private TextFunctions() {
    }

    static void register(final IntrinsicRegistry.Builder registry) {
        // Joining is written by the compiler for "+" on text, with whatever the two sides are.
        registry.onType(TEXT, "Concat", TEXT, (context, target, arguments, line) ->
                context.text(String.valueOf(arguments[0]) + String.valueOf(arguments[1]), line), "T", "U");
        // Written by the compiler where a character is joined to text, since a character runs as its number.
        registry.onType(TEXT, "FromChar", TEXT, (context, target, arguments, line) ->
                context.text(String.valueOf((char) Numbers.toInt(arguments[0])), line), "char");
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
        String result = String.valueOf(arguments[0]);
        for (int i = 1; i < arguments.length; i++) {
            result = result.replace("{" + (i - 1) + "}", String.valueOf(arguments[i]));
        }
        return context.text(result, line);
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
