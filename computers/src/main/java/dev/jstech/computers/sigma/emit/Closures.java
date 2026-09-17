/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.lower.Captures;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.vm.listing.AsmType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The objects a method's lambdas keep their variables in, as types the program carries.
 *
 * <p>Which variables those are was worked out before anything was written down; what is left is where they
 * go. One object per method rather than one per lambda, because two lambdas of the same method that use the
 * same variable must see each other's writes, and it is made when the method starts so that both the method
 * and its lambdas read and write through the same one.
 *
 * <p>The name begins with a digit, so nothing a player writes can ever collide with it, and it is given out
 * here, in the order the classes are written, so that a program written twice comes out the same.
 */
final class Closures {

    /** The types written for the closures of the class being emitted, in the order they were made. */
    private final List<AsmType> written = new ArrayList<>();
    private final Map<String, AsmType> byName = new LinkedHashMap<>();

    private int count;

    /** The object a method's lambdas share, or nothing when there was nothing for them to keep. */
    Closure forMethod(final NamedType type, final Captures kept) {
        if (kept == null) {
            return null;
        }
        this.count++;
        final Closure closure = new Closure("0closure" + this.count, kept.fields(), kept.holdsThis());
        final AsmType made = new AsmType(AsmType.Kind.CLASS, closure.type());
        if (kept.holdsThis()) {
            made.addField(new AsmType.Field(Closure.OUTER, type.qualifiedName(), false));
        }
        for (final Map.Entry<IBinding.Variable, String> field : kept.fields().entrySet()) {
            made.addField(new AsmType.Field(field.getValue(), field.getKey().type().describe(), false));
        }
        this.written.add(made);
        this.byName.put(closure.type(), made);
        return closure;
    }

    /** The types made since the last forgetting, for the program to carry. */
    List<AsmType> written() {
        return this.written;
    }

    /** The type of that name, for a lambda's own method to be added to. */
    AsmType typeOf(final String name) {
        return this.byName.get(name);
    }

    /** Lets go of what was made for one class, once that class has taken it. */
    void forget() {
        this.written.clear();
        this.byName.clear();
    }
}
