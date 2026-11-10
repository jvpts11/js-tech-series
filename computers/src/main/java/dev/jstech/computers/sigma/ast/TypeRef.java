/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.ast;

import java.util.List;
import java.util.Objects;

/**
 * A type as it was written, before anything has been resolved.
 *
 * <p>The front end only records the shape: the name, the arguments a built-in collection was given,
 * and how many pairs of brackets followed it. Whether the name exists, and whether a collection was
 * given the right number of arguments, is settled once the symbol table exists. A tuple, {@code (int count,
 * string name)}, is written with the name {@link #TUPLE}: its places are the arguments, and what each is called
 * is in {@code names}, empty where it was not called anything.
 *
 * @param names what each place of a tuple is called, empty for one called nothing; no names for any other type
 */
public record TypeRef(String name, List<TypeRef> arguments, int arrayRank, List<String> names, int line,
                      int column) implements INode {

    /** The name a tuple is written with, which no type a program names can have. */
    public static final String TUPLE = "()";
    /** The name a pointer is written with, whose one argument is what it points at. */
    public static final String POINTER = "*";

    public TypeRef {
        Objects.requireNonNull(name, "name");
        arguments = List.copyOf(arguments);
        names = List.copyOf(names);
        if (arrayRank < 0) {
            throw new IllegalArgumentException("array rank cannot be negative: " + arrayRank);
        }
    }

    /** A type that is not a tuple. */
    public TypeRef(final String name, final List<TypeRef> arguments, final int arrayRank, final int line,
                   final int column) {
        this(name, arguments, arrayRank, List.of(), line, column);
    }

    /** A plain named type with no arguments and no brackets. */
    public static TypeRef named(final String name, final int line, final int column) {
        return new TypeRef(name, List.of(), 0, line, column);
    }

    /** A tuple of those places, called those names, with that many pairs of brackets after it. */
    public static TypeRef tuple(final List<TypeRef> places, final List<String> names, final int arrayRank,
                                final int line, final int column) {
        return new TypeRef(TUPLE, places, arrayRank, names, line, column);
    }

    /** A pointer to that type, with that many pairs of brackets after it. */
    public static TypeRef pointer(final TypeRef pointee, final int arrayRank, final int line, final int column) {
        return new TypeRef(POINTER, List.of(pointee), arrayRank, line, column);
    }

    /** Whether this is a pointer to something. */
    public boolean isPointer() {
        return POINTER.equals(this.name);
    }

    /** The same type with that many pairs of brackets after it. */
    public TypeRef withArrayRank(final int rank) {
        return new TypeRef(this.name, this.arguments, rank, this.names, this.line, this.column);
    }

    /** Whether this is an array of something. */
    public boolean isArray() {
        return this.arrayRank > 0;
    }

    /** Whether this is a tuple. */
    public boolean isTuple() {
        return TUPLE.equals(this.name);
    }

    /** The same type with one fewer pair of brackets, for reading an array's element type. */
    public TypeRef elementType() {
        if (this.arrayRank == 0) {
            throw new IllegalStateException(this.name + " is not an array");
        }
        return new TypeRef(this.name, this.arguments, this.arrayRank - 1, this.names, this.line, this.column);
    }

    /** How the type was written, for a diagnostic to quote back. */
    public String describe() {
        final StringBuilder text = new StringBuilder();
        if (this.isTuple()) {
            text.append('(');
            for (int i = 0; i < this.arguments.size(); i++) {
                final String called = i < this.names.size() ? this.names.get(i) : "";
                text.append(i > 0 ? ", " : "").append(this.arguments.get(i).describe())
                        .append(called.isEmpty() ? "" : " " + called);
            }
            text.append(')');
        } else if (this.isPointer()) {
            text.append(this.arguments.getFirst().describe()).append('*');
        } else {
            text.append(this.name);
            if (!this.arguments.isEmpty()) {
                text.append('<');
                for (int i = 0; i < this.arguments.size(); i++) {
                    text.append(i > 0 ? ", " : "").append(this.arguments.get(i).describe());
                }
                text.append('>');
            }
        }
        text.append("[]".repeat(this.arrayRank));
        return text.toString();
    }
}
