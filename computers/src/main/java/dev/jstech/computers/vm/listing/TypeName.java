/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The name of a type, as a listing writes it and a machine reads it back.
 *
 * <p>A line of assembly is made almost entirely of names, and until now they were all the same kind of thing:
 * the type something belongs to, the member of it being reached, and the types going in and coming back were
 * four pieces of text side by side. Nothing stopped one being handed over in another's place, and nothing would
 * have noticed: the listing writes, the machine reads it, and the call is simply one nothing answers.
 *
 * <p>So the ones that name types say so. The member's own name stays text, because that is what it is, and the
 * two can no longer be confused for one another by standing next to each other.
 *
 * <p>It holds the name exactly as it is written, qualified or not, since that is what both ends compare.
 */
public record TypeName(String value) {

    public TypeName {
        Objects.requireNonNull(value, "a type name is never nothing");
        if (value.isBlank()) {
            throw new IllegalArgumentException("a type name is never blank");
        }
    }

    /** The same, letting nothing through as nothing: a field of the type around it names no owner. */
    public static TypeName of(final String value) {
        return value == null ? null : new TypeName(value);
    }

    /** Several at once, for the types a method takes. */
    public static List<TypeName> all(final List<String> values) {
        final List<TypeName> names = new ArrayList<>(values.size());
        for (final String value : values) {
            names.add(new TypeName(value));
        }
        return names;
    }

    /** Back to plain text, for whoever compares names rather than carrying them. */
    public static List<String> written(final List<TypeName> names) {
        final List<String> values = new ArrayList<>(names.size());
        for (final TypeName name : names) {
            values.add(name.value());
        }
        return values;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
