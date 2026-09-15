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
 * One method of the assembly: what it takes, what it gives back, how many places it needs, and the
 * lines it runs.
 *
 * <p>Parameters and locals share one numbered set of places, parameters first, so an instruction
 * only ever needs one number to reach either. {@code slots} is how many of them the method needs at
 * once, which is what the runtime allocates when the method is entered.
 *
 * <p>A method with no body is a signature an interface asks for, or the shape a delegate stands for.
 *
 * <p>A method read from a listing remembers the line each instruction was written on, so a problem found once the
 * program is made ready to run can still say where it was written. Where the lines were written says nothing about
 * what the method is, so two methods with the same lines are equal wherever they came from.
 */
public record AsmMethod(String name, String returns, List<String> parameters, boolean isStatic,
                        int slots, List<Instruction> body, List<Integer> lines) {

    /**
     * The name every constructor is written under. It starts with a dot, which no name in a source can, so it is never
     * a method a program wrote, and every type's constructors share it; a call to one reads back with the owner
     * before the double dot ({@code call Tests.Base..ctor(int) -> void}).
     */
    public static final String CONSTRUCTOR = ".ctor";

    /** The name of the static method that puts a type's static fields in place before anything else of it runs. */
    public static final String TYPE_SET_UP = ".cctor";

    public AsmMethod {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(returns, "returns");
        parameters = List.copyOf(parameters);
        body = body == null ? null : List.copyOf(body);
        lines = lines == null ? List.of() : List.copyOf(lines);
        if (slots < 0) {
            throw new IllegalArgumentException("a method cannot need a negative number of places");
        }
    }

    /** A method that was not read from a listing, as a compiler makes one, with no lines to remember. */
    public AsmMethod(final String name, final String returns, final List<String> parameters, final boolean isStatic,
                     final int slots, final List<Instruction> body) {
        this(name, returns, parameters, isStatic, slots, body, List.of());
    }

    /** Whether the method has lines of its own. */
    public boolean hasBody() {
        return this.body != null;
    }

    /** How the method is written on its own line, without the places or the body. */
    public String signature() {
        return this.returns + " " + this.name + "(" + String.join(", ", this.parameters) + ")";
    }

    /** The line of the listing instruction {@code index} was read from, or 0 when the method was not read from one. */
    public int lineOf(final int index) {
        return index >= 0 && index < this.lines.size() ? this.lines.get(index) : 0;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof AsmMethod method && this.isStatic == method.isStatic && this.slots == method.slots
                && this.name.equals(method.name) && this.returns.equals(method.returns)
                && this.parameters.equals(method.parameters) && Objects.equals(this.body, method.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.returns, this.parameters, this.isStatic, this.slots, this.body);
    }

    /**
     * Gathers a method as its lines are read, one at a time, keeping the line each came from so a
     * branch to a label that is never marked can be reported where it was written.
     */
    public static final class Builder {

        private final String name;
        private final String returns;
        private final List<String> parameters;
        private final boolean isStatic;
        private final int slots;
        private final boolean hasBody;
        private final List<Instruction> body = new ArrayList<>();
        private final List<Integer> lines = new ArrayList<>();

        public Builder(final String name, final String returns, final List<String> parameters,
                       final boolean isStatic, final int slots, final boolean hasBody) {
            this.name = name;
            this.returns = returns;
            this.parameters = List.copyOf(parameters);
            this.isStatic = isStatic;
            this.slots = slots;
            this.hasBody = hasBody;
        }

        /** Adds one line, with the line of the listing it came from. */
        public void add(final Instruction instruction, final int line) {
            this.body.add(instruction);
            this.lines.add(line);
        }

        /** The labels a branch could name, and the line each branch is on. */
        public List<Instruction> instructions() {
            return List.copyOf(this.body);
        }

        /** The line of the listing instruction {@code index} came from. */
        public int lineOf(final int index) {
            return this.lines.get(index);
        }

        /** The finished method. */
        public AsmMethod build() {
            return new AsmMethod(this.name, this.returns, this.parameters, this.isStatic,
                    this.slots, this.hasBody ? this.body : null, this.hasBody ? this.lines : List.of());
        }
    }
}
