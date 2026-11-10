/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * How a value lies in raw memory: how many bytes it takes, and where each field of a struct sits.
 *
 * <p>A struct is laid out as C# lays out one of values: its fields in the order they are written, each at a multiple
 * of its own size, a struct's own being that of its widest field, and the whole rounded up to that. So {@code sizeof}
 * reads what it would read in C#, and a pointer moved by one moves past exactly one value.
 *
 * <p>Only the compiler knows this. The machine reads and writes bytes at addresses it is handed, and every address it
 * is handed was worked out here.
 */
public final class Layout {

    private final TypeRules rules;

    /** What a pointer, a long and a double take, and the most any field is aligned to. */
    private static final int WIDEST = 8;

    public Layout(final TypeRules rules) {
        this.rules = rules;
    }

    /**
     * Whether a value of that type can lie in raw memory: a number, a bool, a character, an enum's value, a pointer, or
     * a struct whose fields are all of those.
     */
    public boolean holdsOnlyValues(final ITypeSymbol type) {
        return this.holdsOnlyValues(type, new HashSet<>());
    }

    /**
     * Whether a pointer may point at that type: int, long, byte, nothing in particular, or a struct of values
     * (computers/docs/design/sigma.md).
     */
    public boolean pointable(final ITypeSymbol type) {
        if (type == ITypeSymbol.Primitive.INT || type == ITypeSymbol.Primitive.LONG
                || type == ITypeSymbol.Primitive.BYTE || type == ITypeSymbol.Primitive.VOID) {
            return true;
        }
        final NamedType named = this.rules.named(type);
        return named != null && named.kind() == NamedType.Kind.STRUCT && this.holdsOnlyValues(type);
    }

    /** How many bytes a value of that type takes in raw memory, or 0 for one that cannot lie there. */
    public int sizeOf(final ITypeSymbol type) {
        if (type instanceof ITypeSymbol.PointerType) {
            return WIDEST;
        }
        if (type instanceof ITypeSymbol.Primitive primitive) {
            return switch (primitive) {
                case BYTE, BOOL -> 1;
                case CHAR -> 2;
                case INT, FLOAT -> 4;
                case LONG, DOUBLE -> WIDEST;
                case VOID -> 0;
            };
        }
        final NamedType named = this.rules.named(type);
        if (named == null || !this.holdsOnlyValues(type)) {
            return 0;
        }
        if (named.kind() == NamedType.Kind.ENUM) {
            return 4;
        }
        int end = 0;
        for (final IMemberSymbol field : this.fieldsOf(named)) {
            end = align(end, this.alignOf(typeOf(field))) + this.sizeOf(typeOf(field));
        }
        return align(end, this.alignOf(type));
    }

    /** The type of what a field or a property of a struct holds. */
    public static ITypeSymbol typeOf(final IMemberSymbol field) {
        return switch (field) {
            case IMemberSymbol.FieldSymbol held -> held.type();
            case IMemberSymbol.PropertySymbol held -> held.type();
            default -> ITypeSymbol.Special.ERROR;
        };
    }

    /** Where the field of that name sits from the start of the struct, or -1 for no field of it. */
    public int offsetOf(final NamedType struct, final String name) {
        int end = 0;
        for (final IMemberSymbol field : this.fieldsOf(struct)) {
            final int at = align(end, this.alignOf(typeOf(field)));
            if (field.name().equals(name)) {
                return at;
            }
            end = at + this.sizeOf(typeOf(field));
        }
        return -1;
    }

    /** What lies in raw memory of a struct, in order: its fields and its properties, as an object of it holds them. */
    public List<IMemberSymbol> fieldsOf(final NamedType struct) {
        final List<IMemberSymbol> held = new ArrayList<>();
        for (final IMemberSymbol member : struct.members()) {
            final boolean kept = member instanceof IMemberSymbol.FieldSymbol
                    || member instanceof IMemberSymbol.PropertySymbol;
            if (kept && !member.isStatic()) {
                held.add(member);
            }
        }
        return held;
    }

    private boolean holdsOnlyValues(final ITypeSymbol type, final Set<NamedType> open) {
        if (type instanceof ITypeSymbol.PointerType) {
            return true;
        }
        if (type instanceof ITypeSymbol.Primitive primitive) {
            return primitive != ITypeSymbol.Primitive.VOID;
        }
        final NamedType named = this.rules.named(type);
        if (named == null || named.kind() == NamedType.Kind.ENUM) {
            return named != null;
        }
        // A struct that holds itself would never end, so one met again on the way down holds no values.
        if (named.kind() != NamedType.Kind.STRUCT || !open.add(named)) {
            return false;
        }
        for (final IMemberSymbol field : this.fieldsOf(named)) {
            if (!this.holdsOnlyValues(typeOf(field), open)) {
                return false;
            }
        }
        open.remove(named);
        return true;
    }

    /* A value sits at a multiple of its own size; a struct at a multiple of its widest field's. */
    private int alignOf(final ITypeSymbol type) {
        final NamedType named = this.rules.named(type);
        if (named == null || named.kind() != NamedType.Kind.STRUCT) {
            return Math.max(1, Math.min(WIDEST, this.sizeOf(type)));
        }
        int widest = 1;
        for (final IMemberSymbol field : this.fieldsOf(named)) {
            widest = Math.max(widest, this.alignOf(typeOf(field)));
        }
        return widest;
    }

    private static int align(final int at, final int to) {
        return (at + to - 1) / to * to;
    }
}
