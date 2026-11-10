/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * How the debugger writes a value out where it stops: as the source would write it where it can. A number is a number,
 * text is in double quotes, a character in single quotes, a bool is true or false, an address is in hexadecimal, and an
 * object is its type with its first few fields, one level deep, so a value that holds a great deal is still one line.
 *
 * <p>The runtime keeps a bool and a character as numbers, so the type the source gave the variable says which they are.
 */
final class DebugValues {

    /** How many fields of an object, and how many items of a list or an array, are written before the rest is cut. */
    private static final int MOST_PARTS = 4;
    /** How long a piece of text may be before it is cut, so a file read into a string is not written out whole. */
    private static final int MOST_CHARACTERS = 80;

    private DebugValues() {
    }

    /** The value as the debugger writes it, given the type the source declared it with. */
    static String describe(final Object value, final String type) {
        return write(value, type, true);
    }

    /** A type as it is shown: its own name without the namespaces it is in, where it has no type arguments. */
    static String shortType(final String type) {
        if (type == null || type.isEmpty() || type.indexOf('<') >= 0) {
            return type == null ? "" : type;
        }
        final int dot = type.lastIndexOf('.');
        return dot < 0 ? type : type.substring(dot + 1);
    }

    private static String write(final Object value, final String type, final boolean deep) {
        final String declared = type == null ? "" : type;
        if (value == null) {
            return "null";
        }
        if (value instanceof Number number) {
            return number(number, declared);
        }
        if (value instanceof Boolean truth) {
            return truth.toString();
        }
        if (value instanceof Character letter) {
            return "'" + letter + "'";
        }
        if (value instanceof String text) {
            return quoted(text);
        }
        if (value instanceof Values.Obj object) {
            return deep ? object(object) : shortType(object.type());
        }
        if (value instanceof Values.Arr array) {
            return shortType(array.element()) + "[" + array.length() + "]"
                    + (deep ? " " + items(array.all()) : "");
        }
        if (value instanceof Values.ListValue list) {
            return "List (" + list.items().size() + ")" + (deep ? " " + items(list.items()) : "");
        }
        if (value instanceof Values.MapValue map) {
            return "Dictionary (" + map.entries().size() + ")";
        }
        if (value instanceof Values.DelegateValue delegate) {
            return shortType(delegate.type());
        }
        return String.valueOf(value);
    }

    /* A number as the type it was declared with says it is: a bool, a character, an address, or a number. */
    private static String number(final Number number, final String type) {
        if ("bool".equals(type)) {
            return number.longValue() != 0 ? "true" : "false";
        }
        if ("char".equals(type)) {
            return "'" + (char) number.intValue() + "'";
        }
        if (type.endsWith("*")) {
            final String address = Long.toHexString(number.longValue()).toUpperCase(Locale.ROOT);
            return number.longValue() == 0 ? "null" : "0x" + address;
        }
        return String.valueOf(number);
    }

    /* An object: its type, then its first fields between braces, each written one level deep. */
    private static String object(final Values.Obj object) {
        final List<String> parts = new ArrayList<>();
        int left = MOST_PARTS;
        for (final Map.Entry<String, Object> field : object.all().entrySet()) {
            if (left-- == 0) {
                parts.add("...");
                break;
            }
            parts.add(field.getKey() + " = " + write(field.getValue(), "", false));
        }
        final String type = shortType(object.type());
        return parts.isEmpty() ? type : type + " {" + String.join(", ", parts) + "}";
    }

    private static String items(final List<Object> items) {
        final List<String> parts = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i == MOST_PARTS) {
                parts.add("...");
                break;
            }
            parts.add(write(items.get(i), "", false));
        }
        return "{" + String.join(", ", parts) + "}";
    }

    private static String quoted(final String text) {
        final String cut = text.length() > MOST_CHARACTERS ? text.substring(0, MOST_CHARACTERS) + "..." : text;
        return "\"" + cut.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
