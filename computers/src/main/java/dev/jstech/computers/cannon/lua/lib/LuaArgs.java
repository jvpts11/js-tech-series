/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import dev.jstech.computers.cannon.run.Values;

/**
 * The arguments a library function was given, checked the way the language checks them.
 *
 * <p>Every complaint reads as Lua's would ({@code bad argument #2 to 'insert' (number expected,
 * got nil)}), because a program written against the real library reads its errors by that shape.
 */
final class LuaArgs {

    private final ILuaContext context;
    private final String function;
    private final Object[] arguments;
    private final int line;

    LuaArgs(final ILuaContext context, final String function, final Object[] arguments, final int line) {
        this.context = context;
        this.function = function;
        this.arguments = arguments;
        this.line = line;
    }

    int count() {
        return this.arguments.length;
    }

    Object at(final int index) {
        return index < this.arguments.length ? this.arguments[index] : null;
    }

    boolean has(final int index) {
        return index < this.arguments.length && this.arguments[index] != null;
    }

    RuntimeException bad(final int index, final String message) {
        return this.context.error("bad argument #" + (index + 1) + " to '" + this.function + "' (" + message + ")",
                this.line);
    }

    RuntimeException expected(final int index, final String kind) {
        return this.bad(index, kind + " expected, got " + (index < this.arguments.length
                ? LuaValues.typeName(this.arguments[index]) : "no value"));
    }

    Values.Table table(final int index) {
        if (this.at(index) instanceof Values.Table table) {
            return table;
        }
        throw this.expected(index, "table");
    }

    Object number(final int index) {
        final Object number = LuaNumbers.toNumber(this.at(index));
        if (number == null) {
            throw this.expected(index, "number");
        }
        return number;
    }

    double real(final int index) {
        return LuaNumbers.toDouble(this.number(index));
    }

    long integer(final int index) {
        final Object value = this.at(index);
        final Long whole = LuaNumbers.toInteger(value);
        if (whole == null) {
            if (LuaNumbers.toNumber(value) != null) {
                throw this.bad(index, "number has no integer representation");
            }
            throw this.expected(index, "number");
        }
        return whole;
    }

    long integer(final int index, final long fallback) {
        return this.has(index) ? this.integer(index) : fallback;
    }

    String text(final int index) {
        final String text = LuaValues.asText(this.at(index));
        if (text == null) {
            throw this.expected(index, "string");
        }
        return text;
    }

    String text(final int index, final String fallback) {
        return this.has(index) ? this.text(index) : fallback;
    }

    Values.DelegateValue function(final int index) {
        if (this.at(index) instanceof Values.DelegateValue function) {
            return function;
        }
        throw this.expected(index, "function");
    }

    /** Everything from that argument on. */
    Object[] from(final int index) {
        if (index >= this.arguments.length) {
            return new Object[0];
        }
        final Object[] rest = new Object[this.arguments.length - index];
        System.arraycopy(this.arguments, index, rest, 0, rest.length);
        return rest;
    }
}
