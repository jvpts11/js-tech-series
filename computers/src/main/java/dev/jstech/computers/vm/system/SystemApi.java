/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

import java.util.ArrayList;
import java.util.List;

/**
 * The types of the language's library and of the machine a program runs on, declared once: what the compiler lets a
 * program write and what an editor offers.
 *
 * <p>Only declarations live here, with nothing of the runtime or the world in them, so the compiler and the editors
 * read them with no machine anywhere; what answers each call is bound to it elsewhere. The language's own core (text,
 * the two collections, the delegates and the entry point) is declared by the compiler itself.
 */
public final class SystemApi {

    private static final String UTILS = "System.Utils";

    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String FLOAT = "float";
    private static final String DOUBLE = "double";
    private static final String BOOL = "bool";
    private static final String STRING = "string";
    private static final String OBJECT = "object";

    private static final List<TypeSpec> TYPES = List.of(math(), convert());

    private SystemApi() {
    }

    /** Every type, in the order it was declared. */
    public static List<TypeSpec> types() {
        return TYPES;
    }

    /** The type of that name, or null when the system declares none. */
    public static TypeSpec type(final String name) {
        for (final TypeSpec type : TYPES) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    private static TypeSpec math() {
        final Members math = new Members("Math");
        math.pure(INT, "Abs", INT);
        math.pure(DOUBLE, "Abs", DOUBLE);
        math.pure(INT, "Min", INT, INT);
        math.pure(DOUBLE, "Min", DOUBLE, DOUBLE);
        math.pure(INT, "Max", INT, INT);
        math.pure(DOUBLE, "Max", DOUBLE, DOUBLE);
        math.pure(INT, "Clamp", INT, INT, INT);
        math.pure(DOUBLE, "Clamp", DOUBLE, DOUBLE, DOUBLE);
        math.pure(DOUBLE, "Floor", DOUBLE);
        math.pure(DOUBLE, "Ceil", DOUBLE);
        math.pure(DOUBLE, "Round", DOUBLE);
        math.pure(DOUBLE, "Sqrt", DOUBLE);
        math.pure(DOUBLE, "Pow", DOUBLE, DOUBLE);
        return new TypeSpec(UTILS, "Math", math.methods);
    }

    private static TypeSpec convert() {
        final Members convert = new Members("Convert");
        convert.pure(INT, "ToInt", STRING);
        convert.pure(LONG, "ToLong", STRING);
        convert.pure(FLOAT, "ToFloat", STRING);
        convert.pure(DOUBLE, "ToDouble", STRING);
        convert.pure(BOOL, "ToBool", STRING);
        convert.pure(STRING, "ToString", OBJECT);
        /*
         * The Try forms answer whether the text was a value and hand the value out sideways, for a program that would
         * rather ask again than stop on a line somebody mistyped.
         */
        convert.pure(BOOL, "TryInt", STRING, "out " + INT);
        convert.pure(BOOL, "TryLong", STRING, "out " + LONG);
        convert.pure(BOOL, "TryDouble", STRING, "out " + DOUBLE);
        convert.pure(BOOL, "TryBool", STRING, "out " + BOOL);
        return new TypeSpec(UTILS, "Convert", convert.methods);
    }

    /** Gathers the members of one type, in the order they are declared. */
    private static final class Members {

        private final String owner;
        private final List<MethodSpec> methods = new ArrayList<>();

        Members(final String owner) {
            this.owner = owner;
        }

        /** A method called on the type that the language answers with nothing but what it hands over. */
        void pure(final String returns, final String name, final String... parameters) {
            this.methods.add(new MethodSpec(new MemberId(this.owner, name, List.of(parameters)), returns, true,
                    MemberKind.PURE, CallCost.FREE));
        }
    }
}
