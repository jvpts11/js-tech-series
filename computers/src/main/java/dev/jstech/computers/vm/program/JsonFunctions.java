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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;

/**
 * JSON read into a tree the program holds (computers/docs/design/sigma.md): every value a JsonValue, an object of the
 * program's own made only of what any object holds (a kind, and a text, a number, a truth, a list or a map), so a tree
 * weighs what it holds and is saved with the program like anything else. A value is read with its indexers,
 * {@code doc["items"][0]}, which are its Get and At; reading a member or an element that is not there gives null.
 */
@TextHolder
final class JsonFunctions {

    /** The type a program writes for one value of a tree. */
    static final String TYPE = "JsonValue";
    /** What kind of value it is, as a program reads it. */
    static final String KIND = "Kind";
    static final String OBJECT = "object";
    static final String ARRAY = "array";
    static final String STRING = "string";
    static final String NUMBER = "number";
    static final String BOOL = "bool";
    static final String NULL = "null";
    /** What a value holds by its kind: a text, a number, a truth, its elements, its members. */
    static final String TEXT = "Text";
    static final String VALUE = "Number";
    static final String TRUTH = "Truth";
    static final String ITEMS = "Items";
    static final String FIELDS = "Fields";

    private static final String WRITTEN_TEXT = "string";
    private static final String WHOLE = "int";
    /** What one value weighs, what it holds aside: the header, its kind and what it holds. */
    private static final long VALUE_BYTES = Heap.HEADER + 2L * Heap.REFERENCE;
    /** A whole number as large as a real one holds exactly, beyond which a number is written as a real one. */
    private static final double EXACT = 1e15;

    private static final TextKey NOT_JSON = TextKey.of("jsc.vm.json.not_json",
            "this is not JSON: it stops being JSON at character %s");
    private static final TextKey NOT_A = TextKey.of("jsc.vm.json.not_a", "a JSON %s is not %s");
    private static final TextKey NO_VALUE = TextKey.of("jsc.vm.json.no_value", "there is no JsonValue here");
    private static final TextKey A_TEXT = TextKey.of("jsc.vm.json.a_text", "a text");
    private static final TextKey A_NUMBER = TextKey.of("jsc.vm.json.a_number", "a number");
    private static final TextKey A_TRUTH = TextKey.of("jsc.vm.json.a_truth", "true or false");

    private JsonFunctions() {
    }

    static void register(final IntrinsicRegistry.Builder registry) {
        registry.onType("Json", "Parse", TYPE, (context, target, arguments, line) -> {
            try {
                return made(context, JsonReader.read(String.valueOf(arguments[0])), line);
            } catch (final JsonReader.NotJson wrong) {
                throw new Halt(Halt.Reason.BAD_CAST, line, NOT_JSON.with(wrong.where()), Halt.Thrown.FORMAT);
            }
        }, WRITTEN_TEXT);
        registry.onObject(TYPE, "Get", TYPE, (context, target, arguments, line) ->
                value(target, line).get(FIELDS) instanceof Values.MapValue fields
                        ? fields.entries().get(String.valueOf(arguments[0])) : null, WRITTEN_TEXT);
        registry.onObject(TYPE, "At", TYPE, (context, target, arguments, line) ->
                value(target, line).get(ITEMS) instanceof Values.ListValue items
                        ? items.get(Numbers.toInt(arguments[0]), line) : null, WHOLE);
        registry.onObject(TYPE, "Has", "bool", (context, target, arguments, line) ->
                value(target, line).get(FIELDS) instanceof Values.MapValue fields
                        && fields.entries().containsKey(String.valueOf(arguments[0])), WRITTEN_TEXT);
        registry.onObject(TYPE, "Keys", "List<string>", (context, target, arguments, line) -> {
            final Values.ListValue keys = new Values.ListValue();
            if (value(target, line).get(FIELDS) instanceof Values.MapValue fields) {
                keys.items().addAll(fields.entries().keySet());
            }
            return context.allocate(keys, keys.bytes(), line);
        });
        registry.onObject(TYPE, "AsString", WRITTEN_TEXT, (context, target, arguments, line) ->
                asString(context, value(target, line), line));
        registry.onObject(TYPE, "AsNumber", "double", (context, target, arguments, line) ->
                asNumber(value(target, line), line));
        registry.onObject(TYPE, "AsBool", "bool", (context, target, arguments, line) -> {
            final Values.Obj value = value(target, line);
            if (value.get(TRUTH) instanceof Boolean truth) {
                return truth;
            }
            throw notA(value, A_TRUTH, line);
        });
    }

    /** How many members, elements or characters {@code value} holds; nothing for a number, a truth or null. */
    static int count(final Values.Obj value) {
        if (value.get(FIELDS) instanceof Values.MapValue fields) {
            return fields.entries().size();
        }
        if (value.get(ITEMS) instanceof Values.ListValue items) {
            return items.size();
        }
        return value.get(TEXT) instanceof String text ? text.length() : 0;
    }

    /* The program's own copy of what was read, every value of it on the program's heap. */
    private static Values.Obj made(final IPureContext context, final JsonReader.INode node, final int line) {
        final Values.Obj made = new Values.Obj(TYPE);
        switch (node) {
            case JsonReader.Text text -> {
                made.set(KIND, STRING);
                made.set(TEXT, context.text(text.value(), line));
            }
            case JsonReader.Num number -> {
                made.set(KIND, NUMBER);
                made.set(VALUE, number.value());
            }
            case JsonReader.Truth truth -> {
                made.set(KIND, BOOL);
                made.set(TRUTH, truth.value());
            }
            case JsonReader.Nothing nothing -> made.set(KIND, NULL);
            case JsonReader.Items items -> {
                made.set(KIND, ARRAY);
                final Values.ListValue list = new Values.ListValue();
                for (final JsonReader.INode item : items.values()) {
                    list.items().add(made(context, item, line));
                }
                made.set(ITEMS, context.allocate(list, list.bytes(), line));
            }
            case JsonReader.Fields fields -> {
                made.set(KIND, OBJECT);
                final Values.MapValue map = new Values.MapValue();
                for (final Map.Entry<String, JsonReader.INode> member : fields.members().entrySet()) {
                    map.entries().put(context.text(member.getKey(), line), made(context, member.getValue(), line));
                }
                made.set(FIELDS, context.allocate(map, map.bytes(), line));
            }
        }
        return context.allocate(made, VALUE_BYTES, line);
    }

    /* A value as a text: a text as it is, a number, true or false, and null as null; an object or an array is none. */
    private static String asString(final IPureContext context, final Values.Obj value, final int line) {
        final Object kind = value.get(KIND);
        if (STRING.equals(kind)) {
            return String.valueOf(value.get(TEXT));
        }
        if (value.get(VALUE) instanceof Double number) {
            final boolean whole = number == Math.rint(number) && Math.abs(number) < EXACT;
            return context.text(whole ? Long.toString(number.longValue()) : Double.toString(number), line);
        }
        if (value.get(TRUTH) instanceof Boolean truth) {
            return context.text(truth.toString(), line);
        }
        if (NULL.equals(kind)) {
            return context.text(NULL, line);
        }
        throw notA(value, A_TEXT, line);
    }

    /* A value as a number: a number as it is, and a text that writes one; anything else is none. */
    private static double asNumber(final Values.Obj value, final int line) {
        if (value.get(VALUE) instanceof Double number) {
            return number;
        }
        if (value.get(TEXT) instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (final NumberFormatException wrong) {
                throw notA(value, A_NUMBER, line);
            }
        }
        throw notA(value, A_NUMBER, line);
    }

    private static Halt notA(final Values.Obj value, final TextKey wanted, final int line) {
        return new Halt(Halt.Reason.BAD_CAST, line, NOT_A.with(value.get(KIND), wanted.text()), Halt.Thrown.FORMAT);
    }

    private static Values.Obj value(final Object target, final int line) {
        if (!(target instanceof Values.Obj value) || !TYPE.equals(value.type())) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, NO_VALUE.text());
        }
        return value;
    }
}
