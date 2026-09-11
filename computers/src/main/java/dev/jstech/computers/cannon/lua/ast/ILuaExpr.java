/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.ast;

import dev.jstech.computers.cannon.lua.LuaTokenKind;
import java.util.List;

/** An expression of Lua. */
public sealed interface ILuaExpr extends ILuaNode {

    /** {@code nil}. */
    record Nil(int line, int column) implements ILuaExpr {
    }

    /** {@code true} or {@code false}. */
    record Bool(boolean value, int line, int column) implements ILuaExpr {
    }

    /** A number: a long or a double. */
    record Number(Object value, int line, int column) implements ILuaExpr {
    }

    /** A string, escapes already applied. */
    record Text(String value, int line, int column) implements ILuaExpr {
    }

    /** {@code ...}. */
    record Vararg(int line, int column) implements ILuaExpr {
    }

    /** A name: a local, an upvalue or a global, which the resolver decides. */
    record Name(String identifier, int line, int column) implements ILuaExpr {
    }

    /** {@code target[key]}, which {@code target.name} also is. */
    record Index(ILuaExpr target, ILuaExpr key, int line, int column) implements ILuaExpr {
    }

    /** {@code callee(arguments)}. */
    record Call(ILuaExpr callee, List<ILuaExpr> arguments, int line, int column) implements ILuaExpr {

        public Call {
            arguments = List.copyOf(arguments);
        }
    }

    /** {@code target:name(arguments)}: the target is looked up once and passed first. */
    record MethodCall(ILuaExpr target, String name, List<ILuaExpr> arguments, int line, int column)
            implements ILuaExpr {

        public MethodCall {
            arguments = List.copyOf(arguments);
        }
    }

    /** {@code function (...) ... end}. */
    record Function(LuaFunctionBody body, int line, int column) implements ILuaExpr {
    }

    /** {@code left op right} for every operator but {@code and} and {@code or}. */
    record Binary(LuaTokenKind operator, ILuaExpr left, ILuaExpr right, int line, int column) implements ILuaExpr {
    }

    /** {@code left and right}, or {@code left or right}: the right side runs only when needed. */
    record Logical(boolean and, ILuaExpr left, ILuaExpr right, int line, int column) implements ILuaExpr {
    }

    /** {@code not x}, {@code -x} or {@code #x}. */
    record Unary(LuaTokenKind operator, ILuaExpr operand, int line, int column) implements ILuaExpr {
    }

    /** {@code (inner)}: one value, however many the inside gives. */
    record Paren(ILuaExpr inner, int line, int column) implements ILuaExpr {
    }

    /** {@code { fields }}. */
    record Table(List<Field> fields, int line, int column) implements ILuaExpr {

        public Table {
            fields = List.copyOf(fields);
        }
    }

    /** One entry of a table constructor: a keyed one, or a positional one when the key is null. */
    record Field(ILuaExpr key, ILuaExpr value, int line, int column) implements ILuaNode {
    }
}
