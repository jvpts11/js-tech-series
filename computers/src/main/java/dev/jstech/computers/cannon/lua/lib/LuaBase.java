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

/**
 * The functions every Lua program has without asking: printing, types, tables as sequences,
 * metatables, protected calls and errors.
 *
 * <p>{@code print} and {@code tostring} ask a value's {@code __tostring} when it has one, which is
 * a Lua function, so they go one argument at a time and carry on through a continuation whenever
 * one has to be called.
 */
final class LuaBase {

    private static final String PRINT_ARGUMENTS = "Args";
    private static final String PRINT_INDEX = "Index";
    private static final String PRINT_OUT = "Out";
    private static final String HANDLER = "Handler";

    private LuaBase() {
    }

    static void register() {
        LuaLib.define("print", (context, target, arguments, line) -> {
            final Values.Obj state = context.state("print.next", null, line);
            state.set(PRINT_ARGUMENTS, context.values(java.util.Arrays.asList(arguments), line));
            state.set(PRINT_INDEX, 0L);
            state.set(PRINT_OUT, context.text("", line));
            return printFrom(context, state, line);
        });
        LuaLib.continueWith("print.next", (context, state, result, line) -> {
            final Object first = first(context, result);
            if (!(first instanceof String text)) {
                throw context.error("'__tostring' must return a string", line);
            }
            state.set(PRINT_OUT, context.text(state.get(PRINT_OUT) + text, line));
            state.set(PRINT_INDEX, (Long) state.get(PRINT_INDEX) + 1);
            return printFrom(context, state, line);
        });
        LuaLib.define("tostring", (context, target, arguments, line) -> {
            final Object value = arguments.length > 0 ? arguments[0] : null;
            final Values.Table meta = context.metatableOf(value);
            final Object handler = meta == null ? null : meta.get("__tostring");
            if (handler != null) {
                return new LuaCall(handler, new Object[] {value}, context.state("tostring.done", null, line));
            }
            return value instanceof String ? value : context.text(LuaValues.plainString(value), line);
        });
        LuaLib.continueWith("tostring.done", (context, state, result, line) -> {
            final Object first = first(context, result);
            if (!(first instanceof String)) {
                throw context.error("'__tostring' must return a string", line);
            }
            return first;
        });
        LuaLib.define("tonumber", LuaBase::toNumber);
        LuaLib.define("type", (context, target, arguments, line) -> {
            if (arguments.length == 0) {
                throw context.error("bad argument #1 to 'type' (value expected)", line);
            }
            return context.text(LuaValues.typeName(arguments[0]), line);
        });
        LuaLib.define("pairs", (context, target, arguments, line) -> {
            final Object value = arguments.length > 0 ? arguments[0] : null;
            final Values.Table meta = context.metatableOf(value);
            final Object handler = meta == null ? null : meta.get("__pairs");
            if (handler != null) {
                return new LuaCall(handler, new Object[] {value}, context.state("pairs.done", null, line));
            }
            final LuaArgs args = new LuaArgs(context, "pairs", arguments, line);
            final Values.Table table = args.table(0);
            final List<Object> three = new ArrayList<>(3);
            three.add(context.function("next", null, line));
            three.add(table);
            three.add(null);
            return context.values(three, line);
        });
        LuaLib.continueWith("pairs.done", (context, state, result, line) -> {
            final List<Object> all = context.expand(result);
            final List<Object> three = new ArrayList<>(3);
            for (int i = 0; i < 3; i++) {
                three.add(i < all.size() ? all.get(i) : null);
            }
            return context.values(three, line);
        });
        LuaLib.define("next", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "next", arguments, line);
            final Values.Table table = args.table(0);
            final Object key = args.at(1);
            if (key != null && !table.knows(key)) {
                throw context.error("invalid key to 'next'", line);
            }
            return pair(context, table, table.nextKey(key), line);
        });
        LuaLib.define("ipairs", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "ipairs", arguments, line);
            if (!args.has(0)) {
                throw args.expected(0, "table");
            }
            return context.values(List.of(context.function("ipairs.next", null, line), args.at(0), 0L), line);
        });
        LuaLib.hidden("ipairs.next", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "ipairs", arguments, line);
            final Values.Table table = args.table(0);
            final long next = args.integer(1) + 1;
            final Object value = table.get(next);
            return value == null ? null : context.values(List.of(next, value), line);
        });
        LuaLib.define("select", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "select", arguments, line);
            if ("#".equals(args.at(0))) {
                return (long) (arguments.length - 1);
            }
            final long index = args.integer(0);
            if (index == 0) {
                throw args.bad(0, "index out of range");
            }
            final int rest = arguments.length - 1;
            final int from = index > 0 ? (int) Math.min(index, rest + 1) : Math.max(0, rest + (int) index + 1);
            if (index < 0 && -index > rest) {
                throw args.bad(0, "index out of range");
            }
            return context.values(java.util.Arrays.asList(args.from(from)), line);
        });
        LuaLib.define("rawget", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "rawget", arguments, line);
            return args.table(0).get(args.at(1));
        });
        LuaLib.define("rawset", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "rawset", arguments, line);
            final Values.Table table = args.table(0);
            if (args.at(1) == null) {
                throw context.error("table index is nil", line);
            }
            table.put(args.at(1), args.at(2));
            context.resized(table, line);
            return table;
        });
        LuaLib.define("rawequal", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "rawequal", arguments, line);
            return LuaValues.rawEqual(args.at(0), args.at(1));
        });
        LuaLib.define("rawlen", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "rawlen", arguments, line);
            if (args.at(0) instanceof String text) {
                return (long) text.length();
            }
            return args.table(0).length();
        });
        LuaLib.define("setmetatable", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "setmetatable", arguments, line);
            final Values.Table table = args.table(0);
            if (args.has(1) && !(args.at(1) instanceof Values.Table)) {
                throw args.bad(1, "nil or table expected");
            }
            if (table.metatable() != null && table.metatable().get("__metatable") != null) {
                throw context.error("cannot change a protected metatable", line);
            }
            table.setMetatable(args.at(1) instanceof Values.Table meta ? meta : null);
            return table;
        });
        LuaLib.define("getmetatable", (context, target, arguments, line) -> {
            final Values.Table meta = context.metatableOf(arguments.length > 0 ? arguments[0] : null);
            if (meta == null) {
                return null;
            }
            final Object shown = meta.get("__metatable");
            return shown != null ? shown : meta;
        });
        LuaLib.define("pcall", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "pcall", arguments, line);
            if (!args.has(0)) {
                throw args.expected(0, "value");
            }
            return new LuaCall(args.at(0), args.from(1), context.state("pcall.done", "pcall.failed", line));
        });
        LuaLib.continueWith("pcall.done", (context, state, result, line) -> {
            final List<Object> answer = new ArrayList<>();
            answer.add(true);
            answer.addAll(context.expand(result));
            return context.values(answer, line);
        });
        LuaLib.continueWith("pcall.failed", (context, state, raised, line) -> {
            final List<Object> answer = new ArrayList<>(2);
            answer.add(false);
            answer.add(raised);
            return context.values(answer, line);
        });
        LuaLib.define("xpcall", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "xpcall", arguments, line);
            if (!args.has(0)) {
                throw args.expected(0, "value");
            }
            final Values.Obj state = context.state("pcall.done", "xpcall.failed", line);
            state.set(HANDLER, args.at(1));
            return new LuaCall(args.at(0), args.from(2), state);
        });
        LuaLib.continueWith("xpcall.failed", (context, state, raised, line) -> {
            final Object handler = state.get(HANDLER);
            if (handler == null) {
                final List<Object> answer = new ArrayList<>(2);
                answer.add(false);
                answer.add(raised);
                return context.values(answer, line);
            }
            return new LuaCall(handler, new Object[] {raised}, context.state("xpcall.handled", null, line));
        });
        LuaLib.continueWith("xpcall.handled", (context, state, result, line) -> {
            final List<Object> answer = new ArrayList<>(2);
            answer.add(false);
            answer.add(first(context, result));
            return context.values(answer, line);
        });
        LuaLib.define("error", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "error", arguments, line);
            final Object value = args.at(0);
            final long level = args.integer(1, 1L);
            if (value instanceof String message && level > 0) {
                throw context.raise(context.text(context.where((int) level, line) + message, line), line);
            }
            throw context.raise(value, line);
        });
        LuaLib.define("assert", (context, target, arguments, line) -> {
            if (arguments.length == 0) {
                throw context.error("bad argument #1 to 'assert' (value expected)", line);
            }
            if (LuaValues.truth(arguments[0])) {
                return context.values(java.util.Arrays.asList(arguments), line);
            }
            if (arguments.length > 1) {
                throw context.raise(arguments[1], line);
            }
            throw context.raise(context.text("assertion failed!", line), line);
        });
        LuaLib.define("unpack", LuaTables::unpack);
        LuaLib.define("collectgarbage", (context, target, arguments, line) -> {
            final String what = arguments.length > 0 && arguments[0] instanceof String option ? option : "collect";
            return switch (what) {
                case "count" -> context.heldBytes() / 1024.0;
                case "collect" -> {
                    context.collect();
                    yield 0L;
                }
                default -> 0L;
            };
        });
        LuaLib.intrinsic("load");
        LuaLib.intrinsic("loadstring");
        LuaLib.intrinsic("coroutine.create");
        LuaLib.intrinsic("coroutine.resume");
        LuaLib.intrinsic("coroutine.yield");
        LuaLib.intrinsic("coroutine.wrap");
        LuaLib.intrinsic("coroutine.status");
        LuaLib.intrinsic("coroutine.running");
        LuaLib.intrinsic("coroutine.isyieldable");
        LuaLib.constant("_VERSION", "Lua 5.2");
    }

    static Object first(final ILuaContext context, final Object result) {
        final List<Object> all = context.expand(result);
        return all.isEmpty() ? null : all.getFirst();
    }

    private static Object printFrom(final ILuaContext context, final Values.Obj state, final int line) {
        final List<Object> values = context.expand(state.get(PRINT_ARGUMENTS));
        int index = (int) (long) (Long) state.get(PRINT_INDEX);
        final StringBuilder out = new StringBuilder(String.valueOf(state.get(PRINT_OUT)));
        for (; index < values.size(); index++) {
            final Object value = values.get(index);
            if (index > 0) {
                out.append('\t');
            }
            final Values.Table meta = context.metatableOf(value);
            final Object handler = meta == null ? null : meta.get("__tostring");
            if (handler != null) {
                state.set(PRINT_OUT, context.text(out.toString(), line));
                state.set(PRINT_INDEX, (long) index);
                return new LuaCall(handler, new Object[] {value}, state);
            }
            out.append(LuaValues.plainString(value));
        }
        context.print(out.toString());
        return context.values(List.of(), line);
    }

    private static Object toNumber(final ILuaContext context, final Object target, final Object[] arguments,
                                   final int line) {
        final LuaArgs args = new LuaArgs(context, "tonumber", arguments, line);
        if (!args.has(1)) {
            if (arguments.length == 0) {
                throw args.expected(0, "value");
            }
            return LuaNumbers.toNumber(args.at(0));
        }
        final long base = args.integer(1);
        if (base < 2 || base > 36) {
            throw args.bad(1, "base out of range");
        }
        final String text = args.text(0);
        return LuaNumbers.parse(text, (int) base);
    }

    private static Object pair(final ILuaContext context, final Values.Table table, final Object key,
                               final int line) {
        if (key == null) {
            return null;
        }
        return context.values(List.of(key, table.get(key)), line);
    }
}
