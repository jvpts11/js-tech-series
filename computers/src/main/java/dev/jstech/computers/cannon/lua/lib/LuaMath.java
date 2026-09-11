/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import java.util.List;

/** The math library, whole numbers and reals alike. */
final class LuaMath {

    private LuaMath() {
    }

    static void register() {
        LuaLib.constant("math.pi", Math.PI);
        LuaLib.constant("math.huge", Double.POSITIVE_INFINITY);
        LuaLib.constant("math.maxinteger", Long.MAX_VALUE);
        LuaLib.constant("math.mininteger", Long.MIN_VALUE);
        LuaLib.define("math.abs", (context, target, arguments, line) -> {
            final Object number = new LuaArgs(context, "abs", arguments, line).number(0);
            return number instanceof Long whole ? (Object) Math.abs(whole) : (Object) Math.abs((Double) number);
        });
        LuaLib.define("math.floor", (context, target, arguments, line) -> {
            final Object number = new LuaArgs(context, "floor", arguments, line).number(0);
            return number instanceof Long ? number : whole(Math.floor((Double) number));
        });
        LuaLib.define("math.ceil", (context, target, arguments, line) -> {
            final Object number = new LuaArgs(context, "ceil", arguments, line).number(0);
            return number instanceof Long ? number : whole(Math.ceil((Double) number));
        });
        LuaLib.define("math.sqrt", (context, target, arguments, line) ->
                Math.sqrt(new LuaArgs(context, "sqrt", arguments, line).real(0)));
        LuaLib.define("math.pow", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "pow", arguments, line);
            return Math.pow(args.real(0), args.real(1));
        });
        LuaLib.define("math.exp", (context, target, arguments, line) ->
                Math.exp(new LuaArgs(context, "exp", arguments, line).real(0)));
        LuaLib.define("math.log", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "log", arguments, line);
            final double value = args.real(0);
            if (!args.has(1)) {
                return Math.log(value);
            }
            final double base = args.real(1);
            if (base == 10) {
                return Math.log10(value);
            }
            if (base == 2) {
                return Math.log(value) / Math.log(2);
            }
            return Math.log(value) / Math.log(base);
        });
        LuaLib.define("math.sin", (context, target, arguments, line) ->
                Math.sin(new LuaArgs(context, "sin", arguments, line).real(0)));
        LuaLib.define("math.cos", (context, target, arguments, line) ->
                Math.cos(new LuaArgs(context, "cos", arguments, line).real(0)));
        LuaLib.define("math.tan", (context, target, arguments, line) ->
                Math.tan(new LuaArgs(context, "tan", arguments, line).real(0)));
        LuaLib.define("math.asin", (context, target, arguments, line) ->
                Math.asin(new LuaArgs(context, "asin", arguments, line).real(0)));
        LuaLib.define("math.acos", (context, target, arguments, line) ->
                Math.acos(new LuaArgs(context, "acos", arguments, line).real(0)));
        LuaLib.define("math.atan", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "atan", arguments, line);
            return Math.atan2(args.real(0), args.has(1) ? args.real(1) : 1.0);
        });
        LuaLib.define("math.deg", (context, target, arguments, line) ->
                Math.toDegrees(new LuaArgs(context, "deg", arguments, line).real(0)));
        LuaLib.define("math.rad", (context, target, arguments, line) ->
                Math.toRadians(new LuaArgs(context, "rad", arguments, line).real(0)));
        LuaLib.define("math.fmod", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "fmod", arguments, line);
            final Object a = args.number(0);
            final Object b = args.number(1);
            if (a instanceof Long whole && b instanceof Long by) {
                if (by == 0) {
                    throw args.bad(1, "zero");
                }
                return whole % by;
            }
            return LuaNumbers.toDouble(a) % LuaNumbers.toDouble(b);
        });
        LuaLib.define("math.modf", (context, target, arguments, line) -> {
            final double value = new LuaArgs(context, "modf", arguments, line).real(0);
            final double whole = value >= 0 ? Math.floor(value) : Math.ceil(value);
            final double fraction = Double.isInfinite(value) ? 0.0 : value - whole;
            return context.values(List.of(whole(whole) instanceof Long asWhole ? (Object) (double) asWhole
                    : (Object) whole, fraction), line);
        });
        LuaLib.define("math.max", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "max", arguments, line);
            Object best = args.number(0);
            for (int i = 1; i < arguments.length; i++) {
                final Object other = args.number(i);
                if (LuaNumbers.less(best, other)) {
                    best = other;
                }
            }
            return best;
        });
        LuaLib.define("math.min", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "min", arguments, line);
            Object best = args.number(0);
            for (int i = 1; i < arguments.length; i++) {
                final Object other = args.number(i);
                if (LuaNumbers.less(other, best)) {
                    best = other;
                }
            }
            return best;
        });
        LuaLib.define("math.tointeger", (context, target, arguments, line) ->
                arguments.length > 0 ? LuaNumbers.toInteger(arguments[0]) : null);
        LuaLib.define("math.type", (context, target, arguments, line) -> {
            final Object value = arguments.length > 0 ? arguments[0] : null;
            if (value instanceof Long) {
                return context.text("integer", line);
            }
            if (value instanceof Double) {
                return context.text("float", line);
            }
            return null;
        });
        LuaLib.define("math.ult", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "ult", arguments, line);
            return Long.compareUnsigned(args.integer(0), args.integer(1)) < 0;
        });
        LuaLib.define("math.random", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "random", arguments, line);
            if (arguments.length == 0) {
                return context.random().nextDouble();
            }
            final long low = arguments.length == 1 ? 1 : args.integer(0);
            final long high = arguments.length == 1 ? args.integer(0) : args.integer(1);
            if (low > high) {
                throw args.bad(arguments.length == 1 ? 0 : 1, "interval is empty");
            }
            return low + (long) (context.random().nextDouble() * (high - low + 1));
        });
        LuaLib.define("math.randomseed", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "randomseed", arguments, line);
            context.seed(Double.doubleToLongBits(args.real(0)));
            return null;
        });
    }

    /** A real that is whole comes back as the whole number, as floor and ceil give one. */
    private static Object whole(final double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value) && value >= -0x1p63 && value < 0x1p63) {
            return (long) value;
        }
        return value;
    }
}
