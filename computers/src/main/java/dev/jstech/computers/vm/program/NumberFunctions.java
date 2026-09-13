/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.IPureContext;
import dev.jstech.computers.vm.system.IPureFunction;
import dev.jstech.computers.vm.system.IntrinsicRegistry;
import java.util.Locale;

/** What the language does with numbers: the arithmetic of Math, reading a value out of text, and counting ticks. */
final class NumberFunctions {

    private static final String MATH = "Math";
    private static final String CONVERT = "Convert";
    private static final String TEXT = "string";
    private static final String WHOLE = "int";
    private static final String REAL = "double";
    private static final String FLAG = "bool";

    private NumberFunctions() {
    }

    static void register(final IntrinsicRegistry.Builder registry) {
        /*
         * The whole-number form of each and its real-number form share one function, which answers by what it was
         * actually handed: a real value is answered as a real.
         */
        registry.onType(MATH, "Abs", WHOLE, NumberFunctions::abs, WHOLE);
        registry.onType(MATH, "Abs", REAL, NumberFunctions::abs, REAL);
        registry.onType(MATH, "Min", WHOLE, NumberFunctions::min, WHOLE, WHOLE);
        registry.onType(MATH, "Min", REAL, NumberFunctions::min, REAL, REAL);
        registry.onType(MATH, "Max", WHOLE, NumberFunctions::max, WHOLE, WHOLE);
        registry.onType(MATH, "Max", REAL, NumberFunctions::max, REAL, REAL);
        registry.onType(MATH, "Clamp", WHOLE, NumberFunctions::clamp, WHOLE, WHOLE, WHOLE);
        registry.onType(MATH, "Clamp", REAL, NumberFunctions::clamp, REAL, REAL, REAL);
        registry.onType(MATH, "Floor", REAL, (context, target, arguments, line) ->
                Math.floor(Numbers.toDouble(arguments[0])), REAL);
        registry.onType(MATH, "Ceil", REAL, (context, target, arguments, line) ->
                Math.ceil(Numbers.toDouble(arguments[0])), REAL);
        registry.onType(MATH, "Round", REAL, (context, target, arguments, line) ->
                (double) Math.round(Numbers.toDouble(arguments[0])), REAL);
        registry.onType(MATH, "Sqrt", REAL, (context, target, arguments, line) ->
                Math.sqrt(Numbers.toDouble(arguments[0])), REAL);
        registry.onType(MATH, "Pow", REAL, (context, target, arguments, line) ->
                Math.pow(Numbers.toDouble(arguments[0]), Numbers.toDouble(arguments[1])), REAL, REAL);

        registry.onType(CONVERT, "ToInt", WHOLE, (context, target, arguments, line) ->
                number("ToInt", String.valueOf(arguments[0]), line), TEXT);
        registry.onType(CONVERT, "ToLong", "long", (context, target, arguments, line) ->
                number("ToLong", String.valueOf(arguments[0]), line), TEXT);
        registry.onType(CONVERT, "ToFloat", "float", (context, target, arguments, line) ->
                number("ToFloat", String.valueOf(arguments[0]), line), TEXT);
        registry.onType(CONVERT, "ToDouble", REAL, (context, target, arguments, line) ->
                number("ToDouble", String.valueOf(arguments[0]), line), TEXT);
        registry.onType(CONVERT, "ToBool", FLAG, (context, target, arguments, line) ->
                truth(String.valueOf(arguments[0]), line), TEXT);
        registry.onType(CONVERT, "ToString", TEXT, (context, target, arguments, line) ->
                context.text(String.valueOf(arguments[0]), line), "object");
        /*
         * The value goes out sideways and the answer says whether it is worth anything: a program asking again is a
         * program that never stopped on a mistyped line.
         */
        registry.onType(CONVERT, "TryInt", FLAG, attempt((text, line) -> Integer.parseInt(text.trim()), 0),
                TEXT, "out int");
        registry.onType(CONVERT, "TryLong", FLAG, attempt((text, line) -> Long.parseLong(text.trim()), 0L),
                TEXT, "out long");
        registry.onType(CONVERT, "TryDouble", FLAG, attempt((text, line) -> Double.parseDouble(text.trim()), 0.0d),
                TEXT, "out double");
        registry.onType(CONVERT, "TryBool", FLAG, attempt(NumberFunctions::truth, false), TEXT, "out bool");

        registry.onType("Time", "Ticks", "long", (context, target, arguments, line) ->
                Numbers.toLong(arguments[0]) * 20L, WHOLE);
    }

    /** The number {@code text} says, read the way {@code name} (ToInt, ToLong, ToFloat, ToDouble) asks, or a halt. */
    static Object number(final String name, final String text, final int line) {
        try {
            return switch (name) {
                case "ToInt" -> Integer.parseInt(text.trim());
                case "ToLong" -> Long.parseLong(text.trim());
                case "ToFloat" -> Float.parseFloat(text.trim());
                case "ToDouble" -> Double.parseDouble(text.trim());
                default -> throw new Halt(Halt.Reason.NO_SUCH_MEMBER, line, "Convert has no " + name);
            };
        } catch (final NumberFormatException notANumber) {
            throw new Halt(Halt.Reason.BAD_CAST, line, "'" + text + "' is not a number");
        }
    }

    /** What a line says when it is asked for yes or no: the usual spellings of either, or a halt. */
    static boolean truth(final String text, final int line) {
        return switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "y", "on", "1" -> true;
            case "false", "no", "n", "off", "0" -> false;
            default -> throw new Halt(Halt.Reason.BAD_CAST, line, "'" + text + "' is not true or false");
        };
    }

    private static IPureFunction attempt(final IReading reading, final Object none) {
        return (context, target, arguments, line) -> {
            try {
                arguments[1] = reading.read(String.valueOf(arguments[0]), line);
                return true;
            } catch (final NumberFormatException | Halt notAValue) {
                arguments[1] = none;
                return false;
            }
        };
    }

    private static Object abs(final IPureContext context, final Object target, final Object[] arguments,
                              final int line) {
        final Object first = arguments[0];
        return isReal(first) ? (Object) Math.abs(Numbers.toDouble(first)) : (Object) Math.abs(Numbers.toInt(first));
    }

    private static Object min(final IPureContext context, final Object target, final Object[] arguments,
                              final int line) {
        return isReal(arguments[0])
                ? (Object) Math.min(Numbers.toDouble(arguments[0]), Numbers.toDouble(arguments[1]))
                : (Object) Math.min(Numbers.toInt(arguments[0]), Numbers.toInt(arguments[1]));
    }

    private static Object max(final IPureContext context, final Object target, final Object[] arguments,
                              final int line) {
        return isReal(arguments[0])
                ? (Object) Math.max(Numbers.toDouble(arguments[0]), Numbers.toDouble(arguments[1]))
                : (Object) Math.max(Numbers.toInt(arguments[0]), Numbers.toInt(arguments[1]));
    }

    private static Object clamp(final IPureContext context, final Object target, final Object[] arguments,
                                final int line) {
        return isReal(arguments[0])
                ? (Object) Math.min(Math.max(Numbers.toDouble(arguments[0]), Numbers.toDouble(arguments[1])),
                        Numbers.toDouble(arguments[2]))
                : (Object) Math.min(Math.max(Numbers.toInt(arguments[0]), Numbers.toInt(arguments[1])),
                        Numbers.toInt(arguments[2]));
    }

    private static boolean isReal(final Object value) {
        return value instanceof Double || value instanceof Float;
    }

    @FunctionalInterface
    private interface IReading {
        Object read(String text, int line);
    }
}
