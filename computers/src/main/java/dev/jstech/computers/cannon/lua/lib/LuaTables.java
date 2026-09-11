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
 * The table library: tables as sequences.
 *
 * <p>A sort with a comparator is a Lua function called once per comparison, so it runs as a merge
 * sort whose every step is a continuation: the state of the merge lives in an object between calls,
 * and the runtime carries it on when the comparator returns. Without a comparator the sort is done
 * here, on numbers or text.
 */
final class LuaTables {

    private static final String TABLE = "T";
    private static final String COMPARE = "Cmp";
    private static final String COUNT = "N";
    private static final String WIDTH = "Width";
    private static final String LEFT = "Left";
    private static final String MID = "Mid";
    private static final String RIGHT = "Right";
    private static final String I = "I";
    private static final String J = "J";
    private static final String K = "K";
    private static final String SOURCE = "Src";
    private static final String TARGET = "Dst";

    private LuaTables() {
    }

    static void register() {
        LuaLib.define("table.insert", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "insert", arguments, line);
            final Values.Table table = args.table(0);
            final long size = table.length();
            if (arguments.length == 2) {
                table.put(size + 1, args.at(1));
            } else if (arguments.length == 3) {
                final long position = args.integer(1);
                if (position < 1 || position > size + 1) {
                    throw args.bad(1, "position out of bounds");
                }
                for (long i = size; i >= position; i--) {
                    table.put(i + 1, table.get(i));
                }
                table.put(position, args.at(2));
            } else {
                throw context.error("wrong number of arguments to 'insert'", line);
            }
            context.resized(table, line);
            return null;
        });
        LuaLib.define("table.remove", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "remove", arguments, line);
            final Values.Table table = args.table(0);
            final long size = table.length();
            final long position = args.integer(1, size);
            if (arguments.length > 1 && size + 1 != position && (position < 1 || position > size + 1)) {
                throw args.bad(1, "position out of bounds");
            }
            final Object removed = table.get(position);
            for (long i = position; i < size; i++) {
                table.put(i, table.get(i + 1));
            }
            if (position <= size) {
                table.put(size, null);
            }
            context.resized(table, line);
            return removed;
        });
        LuaLib.define("table.concat", (context, target, arguments, line) -> {
            final LuaArgs args = new LuaArgs(context, "concat", arguments, line);
            final Values.Table table = args.table(0);
            final String separator = args.text(1, "");
            final long from = args.integer(2, 1L);
            final long to = args.integer(3, table.length());
            final StringBuilder out = new StringBuilder();
            for (long i = from; i <= to; i++) {
                final String piece = LuaValues.asText(table.get(i));
                if (piece == null) {
                    throw context.error("invalid value (at index " + i + ") in table for 'concat'", line);
                }
                if (i > from) {
                    out.append(separator);
                }
                out.append(piece);
            }
            return context.text(out.toString(), line);
        });
        LuaLib.define("table.unpack", LuaTables::unpack);
        LuaLib.define("table.pack", (context, target, arguments, line) -> {
            final Values.Table made = context.table(line);
            for (int i = 0; i < arguments.length; i++) {
                made.put((long) (i + 1), arguments[i]);
            }
            made.put(context.text("n", line), (long) arguments.length);
            context.resized(made, line);
            return made;
        });
        LuaLib.define("table.sort", LuaTables::sort);
        LuaLib.continueWith("table.sort.step", (context, state, result, line) ->
                mergeStep(context, state, LuaValues.truth(LuaBase.first(context, result)), line));
    }

    static Object unpack(final ILuaContext context, final Object target, final Object[] arguments,
                         final int line) {
        final LuaArgs args = new LuaArgs(context, "unpack", arguments, line);
        final Values.Table table = args.table(0);
        final long from = args.integer(1, 1L);
        final long to = args.integer(2, table.length());
        if (to - from >= 1_000_000) {
            throw context.error("too many results to unpack", line);
        }
        final List<Object> values = new ArrayList<>();
        for (long i = from; i <= to; i++) {
            values.add(table.get(i));
        }
        return context.values(values, line);
    }

    private static Object sort(final ILuaContext context, final Object target, final Object[] arguments,
                               final int line) {
        final LuaArgs args = new LuaArgs(context, "sort", arguments, line);
        final Values.Table table = args.table(0);
        final int count = (int) Math.min(Integer.MAX_VALUE, table.length());
        if (!args.has(1)) {
            final List<Object> items = new ArrayList<>(count);
            for (long i = 1; i <= count; i++) {
                items.add(table.get(i));
            }
            try {
                items.sort((left, right) -> plainCompare(context, left, right, line));
            } catch (final IllegalArgumentException inconsistent) {
                throw context.error("invalid order function for sorting", line);
            }
            for (int i = 0; i < count; i++) {
                table.put((long) (i + 1), items.get(i));
            }
            context.resized(table, line);
            return null;
        }
        final Values.DelegateValue compare = args.function(1);
        if (count < 2) {
            return null;
        }
        final Values.Obj state = context.state("table.sort.step", null, line);
        state.set(TABLE, table);
        state.set(COMPARE, compare);
        state.set(COUNT, (long) count);
        final List<Object> items = new ArrayList<>(count);
        for (long i = 1; i <= count; i++) {
            items.add(table.get(i));
        }
        state.set(SOURCE, context.values(items, line));
        final List<Object> empty = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            empty.add(null);
        }
        state.set(TARGET, context.values(empty, line));
        state.set(WIDTH, 1L);
        state.set(LEFT, 0L);
        beginMerge(state);
        return mergeStep(context, state, null, line);
    }

    private static int plainCompare(final ILuaContext context, final Object left, final Object right,
                                    final int line) {
        if (LuaNumbers.isNumber(left) && LuaNumbers.isNumber(right)) {
            return LuaNumbers.less(left, right) ? -1 : LuaNumbers.less(right, left) ? 1 : 0;
        }
        if (left instanceof String a && right instanceof String b) {
            return a.compareTo(b);
        }
        final String one = LuaValues.typeName(left);
        final String other = LuaValues.typeName(right);
        throw context.error(one.equals(other) ? "attempt to compare two " + one + " values"
                : "attempt to compare " + one + " with " + other, line);
    }

    /* Sets the bounds of the next run to merge: [left, mid) with [mid, right). */
    private static void beginMerge(final Values.Obj state) {
        final long width = (Long) state.get(WIDTH);
        final long left = (Long) state.get(LEFT);
        final long count = (Long) state.get(COUNT);
        final long mid = Math.min(left + width, count);
        final long right = Math.min(left + 2 * width, count);
        state.set(MID, mid);
        state.set(RIGHT, right);
        state.set(I, left);
        state.set(J, mid);
        state.set(K, left);
    }

    /**
     * Runs the merge until the next comparison is needed, then asks for it; {@code answer} is what
     * the last comparison said, or null when none was asked yet.
     */
    private static Object mergeStep(final ILuaContext context, final Values.Obj state, final Boolean answer,
                                    final int line) {
        final Values.Arr source = (Values.Arr) state.get(SOURCE);
        final Values.Arr target = (Values.Arr) state.get(TARGET);
        Boolean pending = answer;
        while (true) {
            long i = (Long) state.get(I);
            long j = (Long) state.get(J);
            long k = (Long) state.get(K);
            final long mid = (Long) state.get(MID);
            final long right = (Long) state.get(RIGHT);
            if (i < mid && j < right) {
                if (pending == null) {
                    // Whether the right one goes first: asked as cmp(b, a), so equal ones keep their order.
                    return new LuaCall(state.get(COMPARE),
                            new Object[] {source.get((int) j, line), source.get((int) i, line)}, state);
                }
                if (pending) {
                    target.set((int) k, source.get((int) j, line), line);
                    j++;
                } else {
                    target.set((int) k, source.get((int) i, line), line);
                    i++;
                }
                pending = null;
                state.set(I, i);
                state.set(J, j);
                state.set(K, k + 1);
                continue;
            }
            while (i < mid) {
                target.set((int) k++, source.get((int) i++, line), line);
            }
            while (j < right) {
                target.set((int) k++, source.get((int) j++, line), line);
            }
            final long count = (Long) state.get(COUNT);
            final long width = (Long) state.get(WIDTH);
            final long nextLeft = (Long) state.get(LEFT) + 2 * width;
            if (nextLeft < count) {
                state.set(LEFT, nextLeft);
                beginMerge(state);
                continue;
            }
            // One pass is over: what was merged becomes the source of the next, twice as wide.
            state.set(SOURCE, target);
            state.set(TARGET, source);
            if (width * 2 >= count) {
                final Values.Table table = (Values.Table) state.get(TABLE);
                for (int n = 0; n < count; n++) {
                    table.put((long) (n + 1), target.get(n, line));
                }
                context.resized(table, line);
                return context.values(List.of(), line);
            }
            state.set(WIDTH, width * 2);
            state.set(LEFT, 0L);
            beginMerge(state);
            return mergeStep(context, state, null, line);
        }
    }
}
