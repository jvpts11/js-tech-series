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

/**
 * What the language does with its two collections, a list and a map.
 *
 * <p>Every call is made on the collection itself, and whatever it did, the collection weighs afterwards what it then
 * holds, so a list that grows is paid for as it grows.
 */
final class CollectionFunctions {

    private static final String LIST = "List";
    private static final String MAP = "Map";
    private static final String NOTHING = "void";
    private static final String FLAG = "bool";
    private static final String WHOLE = "int";

    private CollectionFunctions() {
    }

    static void register(final IntrinsicRegistry.Builder registry) {
        registry.onObject(LIST, "Add", NOTHING, onList((context, held, arguments, line) -> {
            held.items().add(arguments[0]);
            return null;
        }), "T");
        registry.onObject(LIST, "Insert", NOTHING, onList((context, held, arguments, line) -> {
            held.insert(Numbers.toInt(arguments[0]), arguments[1], line);
            return null;
        }), WHOLE, "T");
        registry.onObject(LIST, "RemoveAt", NOTHING, onList((context, held, arguments, line) -> {
            held.removeAt(Numbers.toInt(arguments[0]), line);
            return null;
        }), WHOLE);
        registry.onObject(LIST, "Remove", FLAG, onList((context, held, arguments, line) ->
                held.items().remove(arguments[0])), "T");
        registry.onObject(LIST, "Clear", NOTHING, onList((context, held, arguments, line) -> {
            held.items().clear();
            return null;
        }));
        registry.onObject(LIST, "Contains", FLAG, onList((context, held, arguments, line) ->
                held.items().contains(arguments[0])), "T");
        registry.onObject(LIST, "IndexOf", WHOLE, onList((context, held, arguments, line) ->
                held.items().indexOf(arguments[0])), "T");
        registry.onObject(LIST, "Get", "T", onList((context, held, arguments, line) ->
                held.get(Numbers.toInt(arguments[0]), line)), WHOLE);
        registry.onObject(LIST, "Set", NOTHING, onList((context, held, arguments, line) -> {
            held.set(Numbers.toInt(arguments[0]), arguments[1], line);
            return null;
        }), WHOLE, "T");
        registry.onObject(LIST, "Sort", NOTHING, onList((context, held, arguments, line) -> {
            held.items().sort(CollectionFunctions::compare);
            return null;
        }));

        registry.onObject(MAP, "Put", NOTHING, onMap((context, held, arguments, line) -> {
            held.entries().put(arguments[0], arguments[1]);
            return null;
        }), "K", "V");
        registry.onObject(MAP, "Get", "V", onMap((context, held, arguments, line) ->
                held.entries().get(arguments[0])), "K");
        registry.onObject(MAP, "ContainsKey", FLAG, onMap((context, held, arguments, line) ->
                held.entries().containsKey(arguments[0])), "K");
        registry.onObject(MAP, "TryGet", FLAG, onMap((context, held, arguments, line) -> {
            final Object found = held.entries().get(arguments[0]);
            arguments[1] = found == null ? 0 : found;
            return found != null;
        }), "K", "out V");
        registry.onObject(MAP, "Remove", FLAG, onMap((context, held, arguments, line) ->
                held.entries().remove(arguments[0]) != null), "K");
        registry.onObject(MAP, "Keys", "List<K>", onMap((context, held, arguments, line) ->
                listOf(context, held.entries().keySet(), line)));
        registry.onObject(MAP, "Values", "List<V>", onMap((context, held, arguments, line) ->
                listOf(context, held.entries().values(), line)));
    }

    /** A call on a list: made on anything that is not one, it halts. */
    private static IPureFunction onList(final IListCall call) {
        return (context, target, arguments, line) -> {
            if (!(target instanceof Values.ListValue held)) {
                throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no list here");
            }
            final Object answer = call.call(context, held, arguments, line);
            context.resize(held, held.bytes(), line);
            return answer;
        };
    }

    /** A call on a map: made on anything that is not one, it halts. */
    private static IPureFunction onMap(final IMapCall call) {
        return (context, target, arguments, line) -> {
            if (!(target instanceof Values.MapValue held)) {
                throw new Halt(Halt.Reason.NO_OBJECT, line, "there is no map here");
            }
            final Object answer = call.call(context, held, arguments, line);
            context.resize(held, held.bytes(), line);
            return answer;
        };
    }

    private static Object listOf(final IPureContext context, final Iterable<Object> values, final int line) {
        final Values.ListValue made = new Values.ListValue();
        context.allocate(made, made.bytes(), line);
        for (final Object value : values) {
            made.items().add(value);
        }
        context.resize(made, made.bytes(), line);
        return made;
    }

    /*
     * One order over everything a list can hold, or the sort gives up part way through: numbers by value
     * come first, then text in order, then anything else, which keeps the place it had.
     */
    private static int compare(final Object left, final Object right) {
        final int kinds = Integer.compare(rank(left), rank(right));
        if (kinds != 0) {
            return kinds;
        }
        return switch (rank(left)) {
            case 0 -> Numbers.compare(left, right);
            case 1 -> ((String) left).compareTo((String) right);
            default -> 0;
        };
    }

    /** Where a kind of value falls in a sort: 0 for a number, 1 for text, 2 for anything else. */
    private static int rank(final Object value) {
        if (value instanceof Number || value instanceof Character) {
            return 0;
        }
        return value instanceof String ? 1 : 2;
    }

    @FunctionalInterface
    private interface IListCall {
        Object call(IPureContext context, Values.ListValue held, Object[] arguments, int line);
    }

    @FunctionalInterface
    private interface IMapCall {
        Object call(IPureContext context, Values.MapValue held, Object[] arguments, int line);
    }
}
