/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

/**
 * What a cast and a type test make of a value: the value as the type asked for, or a halt that says why it is not one,
 * and whether a value is of a type at all.
 */
final class TypeChecks {

    private final ProgramImage program;

    TypeChecks(final ProgramImage program) {
        this.program = program;
    }

    /**
     * The value as {@code type}. A number, or an object holding one, comes back as the kind of number asked for; an
     * object or nothing comes back as it is when it is of that type or stands on it.
     *
     * @throws Halt with {@link Halt.Reason#BAD_CAST} when the value is not one
     */
    Object cast(final Object value, final String type, final int line) {
        final PrimitiveKind primitive = PrimitiveKind.of(type);
        if (primitive != null) {
            // An object holding a number gives it back as the kind asked for, whichever kind it holds.
            if (value instanceof Number || value instanceof Character) {
                return primitive.convert(value);
            }
            if (primitive == PrimitiveKind.BOOL && value instanceof Boolean) {
                return value;
            }
            throw new Halt(Halt.Reason.BAD_CAST, line, value == null ? "there is nothing here to make a " + type
                    : "this is not a " + type);
        }
        if (value == null || this.isOfType(value, type)) {
            return value;
        }
        throw new Halt(Halt.Reason.BAD_CAST, line, "this is not a " + type);
    }

    /** Whether the value is of {@code type} or stands on it; nothing is never of any type. */
    boolean isInstance(final Object value, final String type) {
        return value != null && this.isOfType(value, type);
    }

    private boolean isOfType(final Object value, final String type) {
        if (value instanceof Values.Obj object) {
            return this.program.isA(object.type(), type);
        }
        if (value instanceof Values.DelegateValue delegate) {
            return delegate.type().equals(type);
        }
        return switch (type) {
            case "string" -> value instanceof String;
            case "object" -> true;
            case "int" -> value instanceof Integer;
            case "long" -> value instanceof Long;
            case "float" -> value instanceof Float;
            case "double" -> value instanceof Double;
            case "bool" -> value instanceof Boolean;
            case "char" -> value instanceof Character;
            default -> value instanceof Values.Arr array && type.equals(array.element() + "[]")
                    || value instanceof Values.ListValue && type.startsWith("List<")
                    || value instanceof Values.MapValue && type.startsWith("Map<");
        };
    }

    /** The kinds of value a cast can take a number out of an object as. */
    private enum PrimitiveKind {
        INT, LONG, FLOAT, DOUBLE, CHAR, BOOL;

        static PrimitiveKind of(final String written) {
            return switch (written) {
                case "int" -> INT;
                case "long" -> LONG;
                case "float" -> FLOAT;
                case "double" -> DOUBLE;
                case "char" -> CHAR;
                case "bool" -> BOOL;
                default -> null;
            };
        }

        Object convert(final Object value) {
            return switch (this) {
                case INT -> Numbers.toInt(value);
                case LONG -> Numbers.toLong(value);
                case FLOAT -> Numbers.toFloat(value);
                case DOUBLE -> Numbers.toDouble(value);
                case CHAR -> (char) Numbers.toInt(value);
                case BOOL -> value;
            };
        }
    }
}
