/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;
import java.util.Locale;

/**
 * What every Lua value is and says about itself, without asking its metatable anything.
 *
 * <p>Nothing is null, a table is a {@link Values.Table}, a function is a delegate, a coroutine is an
 * object of the runtime's own type, numbers are longs and doubles, and text is text. What is left
 * is something the program holds but did not make, which Lua calls userdata.
 */
public final class LuaValues {

    /** The type of the object that stands for a coroutine. */
    public static final String COROUTINE = "Lua.0coroutine";

    private LuaValues() {
    }

    /** The name {@code type} gives the value. */
    public static String typeName(final Object value) {
        if (value == null) {
            return "nil";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (LuaNumbers.isNumber(value)) {
            return "number";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Values.Table) {
            return "table";
        }
        if (value instanceof Values.DelegateValue) {
            return "function";
        }
        if (value instanceof Values.Obj object && COROUTINE.equals(object.type())) {
            return "thread";
        }
        return "userdata";
    }

    /** Whether the value counts as true: everything but nil and false does. */
    public static boolean truth(final Object value) {
        return value != null && !Boolean.FALSE.equals(value);
    }

    /** Whether the two values are the same without any metamethod asked: numbers by what they say. */
    public static boolean rawEqual(final Object left, final Object right) {
        if (left == null || right == null) {
            return left == right;
        }
        if (LuaNumbers.isNumber(left) && LuaNumbers.isNumber(right)) {
            return LuaNumbers.equal(left, right);
        }
        if (left instanceof String || left instanceof Boolean) {
            return left.equals(right);
        }
        return left == right;
    }

    /** The value as {@code tostring} would write it when no metatable has a say. */
    public static String plainString(final Object value) {
        if (value == null) {
            return "nil";
        }
        if (value instanceof String text) {
            return text;
        }
        if (LuaNumbers.isNumber(value)) {
            return LuaNumbers.format(value);
        }
        if (value instanceof Boolean flag) {
            return flag ? "true" : "false";
        }
        return typeName(value) + ": " + address(value);
    }

    /** A number that tells two things of one kind apart while both are alive, written the way Lua does. */
    public static String address(final Object value) {
        return String.format(Locale.ROOT, "0x%08x", System.identityHashCode(value));
    }

    /** The value as text when it is text or a number, as the string library takes it; null otherwise. */
    public static String asText(final Object value) {
        if (value instanceof String text) {
            return text;
        }
        if (LuaNumbers.isNumber(value)) {
            return LuaNumbers.format(value);
        }
        return null;
    }
}
