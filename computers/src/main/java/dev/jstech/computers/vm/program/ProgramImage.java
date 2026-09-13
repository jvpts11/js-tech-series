/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmType;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Shape;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A program made ready to run.
 *
 * <p>The listing is text, and text is what a player reads, not what a machine should chase down a line at a time. So
 * it is worked out once, when the program loads: every branch knows the line it lands on, every call knows the method
 * of the program it reaches (or that the runtime answers it), every {@code new} knows its constructor and what the
 * object weighs, and every type knows the methods it inherits and every type it is. From then on a line looks nothing
 * up by its name.
 *
 * <p>Nothing in it changes once it is made, so every process running the same program could share one.
 */
public final class ProgramImage {

    /**
     * A call as the program loaded it.
     *
     * @param named      the call as the listing writes it, for what the runtime answers by name
     * @param direct     the method of the program the call reaches on the type it names, or null when the program has
     *                   none and the runtime answers the call instead
     * @param signature  the number of the shape the call is written with, or null when no type of the program declares
     *                   a method of that shape
     * @param outs       which of its arguments the call fills in rather than hands over
     * @param constructs whether the call runs a constructor, which always runs on the type it names
     */
    record CallSite(IOperand.Method named, MethodImage direct, Integer signature, boolean[] outs, boolean constructs) {
    }

    /**
     * A {@code new} as the program loaded it.
     *
     * @param made        the object as the listing writes it, for what the runtime makes itself
     * @param type        the type of the program it makes, or null when the runtime brings that type
     * @param constructor the constructor that runs, or null when the type has none taking those arguments
     * @param outs        which of its arguments are filled in rather than handed over
     */
    record Creation(IOperand.Constructor made, TypeImage type, MethodImage constructor, boolean[] outs) {
    }

    private final Map<String, TypeImage> types = new LinkedHashMap<>();
    /** Every method shape a type of the program declares, numbered in the order they were first met. */
    private final Map<String, Integer> signatures = new HashMap<>();
    private final String entryPoint;
    private final Shape shape;

    private ProgramImage(final AsmProgram program) {
        this.entryPoint = program.entryPoint();
        this.shape = program.shape();
        for (final AsmType type : program.types()) {
            this.types.put(type.name(), new TypeImage(type, this.signatures));
        }
        for (final TypeImage type : this.types.values()) {
            type.link(this.types);
        }
        for (final TypeImage type : this.types.values()) {
            type.complete(this.types, this.signatures);
        }
        for (final TypeImage type : this.types.values()) {
            type.resolve(this);
        }
    }

    /** Makes a program ready to run. */
    public static ProgramImage of(final AsmProgram program) {
        return new ProgramImage(program);
    }

    /** The type of that name, or null when the runtime provides it instead of the program. */
    public TypeImage type(final String name) {
        return this.types.get(name);
    }

    /** The class the runtime starts from, or null for a library. */
    public String entryPoint() {
        return this.entryPoint;
    }

    /** Whether this is a program that runs at a terminal or one that stays up. */
    public Shape shape() {
        return this.shape;
    }

    /** Every type the program declares. */
    public List<TypeImage> types() {
        return List.copyOf(this.types.values());
    }

    /**
     * The method of that shape on that type or on one it stands on, or null.
     *
     * <p>For what is named at run time rather than written on a line: a handler, a thread's body, a frame read back
     * from a snapshot.
     */
    public MethodImage method(final String owner, final String name, final List<String> parameters) {
        final TypeImage type = this.types.get(owner);
        return type == null ? null : type.method(this.signatures.get(key(name, parameters)));
    }

    /** Whether a type is, or stands on, another. */
    public boolean isA(final String type, final String other) {
        if (type == null || other == null) {
            return false;
        }
        if (type.equals(other)) {
            return true;
        }
        final TypeImage known = this.types.get(type);
        return known != null && known.isA(other);
    }

    CallSite callSite(final IOperand.Method called) {
        final Integer signature = this.signatures.get(key(called.name(), called.parameters()));
        final TypeImage owner = this.types.get(called.owner());
        return new CallSite(called, owner == null ? null : owner.method(signature), signature,
                MethodImage.outsOf(called.parameters()), AsmMethod.CONSTRUCTOR.equals(called.name()));
    }

    Creation creation(final IOperand.Constructor made) {
        final TypeImage type = this.types.get(made.owner());
        return new Creation(made, type, type == null ? null : type.constructor(made.parameters().size()),
                MethodImage.outsOf(made.parameters()));
    }

    /** How a method shape is written, which is what tells two overloads apart. */
    static String key(final String name, final List<String> parameters) {
        return name + "(" + String.join(", ", parameters) + ")";
    }
}
