/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import java.util.ArrayList;
import java.util.List;

/**
 * A type, once it has been resolved to the thing it names.
 *
 * <p>A type reference in the tree is text; a type symbol is the type itself, shared by every place
 * that mentions it. Two extra members exist so one mistake does not cascade into ten: {@link
 * Special#ERROR} stands for a type that could not be resolved and is convertible to and from
 * everything, and {@link Special#NULL} is the type of the null literal, which fits any reference.
 */
public sealed interface ITypeSymbol
        permits ITypeSymbol.Primitive, ITypeSymbol.Special, ITypeSymbol.ArrayType, ITypeSymbol.PointerType,
                ITypeSymbol.GenericType, ITypeSymbol.TypeParameter, ITypeSymbol.TupleType, NamedType {

    /** How the type is written, which is how a diagnostic quotes it. */
    String describe();

    /** The value types built into the language. */
    enum Primitive implements ITypeSymbol {
        INT("int"),
        LONG("long"),
        FLOAT("float"),
        DOUBLE("double"),
        BOOL("bool"),
        CHAR("char"),
        VOID("void"),
        /** A whole number from 0 to 255, which came with pointers; byte is a word only where a type is written. */
        BYTE("byte");

        private final String text;

        Primitive(final String text) {
            this.text = text;
        }

        @Override
        public String describe() {
            return this.text;
        }

        /** Whether arithmetic applies to this type. */
        public boolean isNumeric() {
            return this != BOOL && this != VOID;
        }

        /** The primitive written that way, or null. */
        public static Primitive written(final String text) {
            for (final Primitive primitive : values()) {
                if (primitive.text.equals(text)) {
                    return primitive;
                }
            }
            return null;
        }
    }

    /** The two types that exist for the compiler's own sake rather than the player's. */
    enum Special implements ITypeSymbol {
        ERROR("?"),
        NULL("null");

        private final String text;

        Special(final String text) {
            this.text = text;
        }

        @Override
        public String describe() {
            return this.text;
        }
    }

    /** An array of something. */
    record ArrayType(ITypeSymbol element) implements ITypeSymbol {

        @Override
        public String describe() {
            return this.element.describe() + "[]";
        }
    }

    /**
     * A pointer: the address of a value of the type it points at, which is {@code void} for one that says nothing about
     * what is there. Only unsafe code holds one (computers/docs/design/sigma.md).
     */
    record PointerType(ITypeSymbol pointee) implements ITypeSymbol {

        @Override
        public String describe() {
            return this.pointee.describe() + "*";
        }

        /** Whether it says nothing about what it points at, so nothing is read through it without a cast. */
        public boolean isVoid() {
            return this.pointee == Primitive.VOID;
        }
    }

    /** One of the built-in collections with its arguments filled in. */
    record GenericType(NamedType definition, List<ITypeSymbol> arguments) implements ITypeSymbol {

        public GenericType {
            arguments = List.copyOf(arguments);
        }

        @Override
        public String describe() {
            final StringBuilder text = new StringBuilder(this.definition.name()).append('<');
            for (int i = 0; i < this.arguments.size(); i++) {
                text.append(i > 0 ? ", " : "").append(this.arguments.get(i).describe());
            }
            return text.append('>').toString();
        }
    }

    /**
     * A stand-in for one of a built-in collection's arguments, as it appears in that collection's
     * own members before a use site says what it holds.
     */
    record TypeParameter(String name, int index) implements ITypeSymbol {

        @Override
        public String describe() {
            return this.name;
        }
    }

    /**
     * A tuple: the struct the compiler makes for its places' types, with the names the place it was written in gives
     * them. Two tuples of the same types are one type whatever their places are called, as C#'s are, so only the
     * struct counts for being the same, and a tuple is written down under the struct's name.
     *
     * @param names what each place is called where this tuple was written, empty for one called nothing
     */
    record TupleType(NamedType struct, List<String> names) implements ITypeSymbol {

        public TupleType {
            names = List.copyOf(names);
        }

        /** The field a place is read through: ItemN for the Nth, which every place is, or the name it was given. */
        public String fieldOf(final String name) {
            final int at = name.isEmpty() ? -1 : this.names.indexOf(name);
            return at < 0 ? name : NamedType.ITEM + (at + 1);
        }

        /** The types of the places, in order. */
        public List<ITypeSymbol> places() {
            final List<ITypeSymbol> places = new ArrayList<>();
            for (final IMemberSymbol member : this.struct.members()) {
                if (member instanceof IMemberSymbol.FieldSymbol field) {
                    places.add(field.type());
                }
            }
            return places;
        }

        @Override
        public String describe() {
            return this.struct.describe();
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof TupleType tuple && tuple.struct == this.struct;
        }

        @Override
        public int hashCode() {
            return this.struct.hashCode();
        }
    }
}
