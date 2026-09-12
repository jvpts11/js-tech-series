/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

import dev.jstech.computers.cannon.run.Values;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What crosses between a program of ours and a ComputerCraft computer.
 *
 * <p>The two sides hold values differently: over there everything countable is one kind of number and
 * everything else is a table; here a whole number is whole, a list is a list and a map is a map. This is
 * the whole of the translation, and it knows nothing of ComputerCraft itself, so it can be checked on its
 * own.
 *
 * <p>Going over: a number becomes a Lua number, a letter becomes a piece of text, a list becomes a table
 * counted from one, a map becomes a table with its own keys. Coming back: a number that is whole comes
 * back whole, a table that is a run from one comes back a list, and any other table comes back a map.
 */
public final class GatewayValues {

    private GatewayValues() {
    }

    /** A run of our values as the other side takes them. */
    public static List<Object> toLuaAll(final List<Object> ours) {
        final List<Object> out = new ArrayList<>(ours.size());
        for (final Object one : ours) {
            out.add(toLua(one));
        }
        return out;
    }

    /** One of our values as the other side takes it. */
    public static Object toLua(final Object ours) {
        return switch (ours) {
            case null -> null;
            case Values.ListValue list -> {
                final Map<Object, Object> table = new LinkedHashMap<>();
                for (int i = 0; i < list.items().size(); i++) {
                    table.put((double) (i + 1), toLua(list.items().get(i)));
                }
                yield table;
            }
            case Values.MapValue map -> {
                final Map<Object, Object> table = new LinkedHashMap<>();
                for (final Map.Entry<Object, Object> entry : map.entries().entrySet()) {
                    table.put(toLua(entry.getKey()), toLua(entry.getValue()));
                }
                yield table;
            }
            case Character letter -> String.valueOf(letter);
            case Number number -> number.doubleValue();
            default -> ours;
        };
    }

    /** What the other side answered, as a value one of our programs can hold. */
    public static Object fromLua(final Object theirs) {
        return switch (theirs) {
            case null -> null;
            case Object[] several -> {
                final Values.ListValue list = new Values.ListValue();
                for (final Object one : several) {
                    list.items().add(fromLua(one));
                }
                yield list;
            }
            case Map<?, ?> table -> fromTable(table);
            case Number number -> whole(number);
            case Boolean flag -> flag;
            case String text -> text;
            default -> String.valueOf(theirs);
        };
    }

    /* A Lua number is one kind of number; one that stands for a whole comes back whole. */
    private static Object whole(final Number number) {
        final double value = number.doubleValue();
        if (Double.isNaN(value) || Double.isInfinite(value) || value != Math.rint(value)
                || Math.abs(value) > 9.007199254740992E15) {
            return value;
        }
        return (long) value;
    }

    /* A table that is a run from one is a list; anything else keeps its keys. */
    private static Object fromTable(final Map<?, ?> table) {
        final List<Object> run = new ArrayList<>();
        for (int i = 1; i <= table.size(); i++) {
            final Object at = table.containsKey((double) i) ? table.get((double) i) : table.get((long) i);
            if (at == null) {
                run.clear();
                break;
            }
            run.add(fromLua(at));
        }
        if (!table.isEmpty() && run.size() == table.size()) {
            final Values.ListValue list = new Values.ListValue();
            list.items().addAll(run);
            return list;
        }
        final Values.MapValue map = new Values.MapValue();
        for (final Map.Entry<?, ?> entry : table.entrySet()) {
            map.entries().put(fromLua(entry.getKey()), fromLua(entry.getValue()));
        }
        return map;
    }
}
