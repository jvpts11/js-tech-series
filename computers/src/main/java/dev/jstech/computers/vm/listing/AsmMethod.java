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
 *
 * <p>A method with exceptions carries the stretches it protects and what answers each, innermost first, and the line
 * of the source each instruction came from, which is where an exception says it was thrown. The second is not what
 * the method is either, and is left out of being equal as the listing's lines are.
 *
 * @param handlers  the protected stretches and what answers each, innermost first
 * @param positions the line of the source each instruction came from, or none when the listing does not say
 */
public record AsmMethod(String name, String returns, List<String> parameters, boolean isStatic,
                        int slots, List<Instruction> body, List<Integer> lines, List<Handler> handlers,
                        List<Integer> positions) {

    /**
     * One protected stretch of the method and what answers an exception thrown inside it: a catch, which takes the
     * exceptions of {@code type} and of every type standing on it, or a finally, run whichever way the stretch is left.
     *
     * @param start   the label the stretch starts at
     * @param end     the label just after it
     * @param type    the exceptions a catch takes, or null for a finally
     * @param handler the label the catch or the finally starts at
     * @param through the label just after it
     */
    public record Handler(String start, String end, String type, String handler, String through) {

        /** Whether this is a finally, which runs however the stretch is left, rather than a catch. */
        public boolean cleanup() {
            return this.type == null;
        }

        /** How the handler is written on its line of the listing, after {@code .try}. */
        public String write() {
            return this.start + " " + this.end + (this.cleanup() ? " finally " : " catch " + this.type + " ")
                    + this.handler + " " + this.through;
        }
    }

    /**
     * The name every constructor is written under. It starts with a dot, which no name in a source can, so it is never
     * a method a program wrote, and every type's constructors share it; a call to one reads back with the owner
     * before the double dot ({@code call Tests.Base..ctor(int) -> void}).
     */
    public static final String CONSTRUCTOR = ".ctor";

    /** The name of the static method that puts a type's static fields in place before anything else of it runs. */
    public static final String TYPE_SET_UP = ".cctor";

    /**
     * The most places a method can need, as many as a Java method may have locals. Every frame of a call holds this
     * many, in the memory of the machine running the game, so a listing asking for more is refused when it is read.
     */
    public static final int MOST_SLOTS = 65_535;

    public AsmMethod {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(returns, "returns");
        parameters = List.copyOf(parameters);
        body = body == null ? null : List.copyOf(body);
        lines = lines == null ? List.of() : List.copyOf(lines);
        handlers = handlers == null ? List.of() : List.copyOf(handlers);
        positions = positions == null ? List.of() : List.copyOf(positions);
        if (slots < 0 || slots > MOST_SLOTS) {
            throw new IllegalArgumentException("a method cannot need " + slots + " places");
        }
    }

    /** A method read from a listing, with the lines its instructions were read from and no exceptions about it. */
    public AsmMethod(final String name, final String returns, final List<String> parameters, final boolean isStatic,
                     final int slots, final List<Instruction> body, final List<Integer> lines) {
        this(name, returns, parameters, isStatic, slots, body, lines, List.of(), List.of());
    }

    /** A method that was not read from a listing, as a compiler makes one, with no lines to remember. */
    public AsmMethod(final String name, final String returns, final List<String> parameters, final boolean isStatic,
                     final int slots, final List<Instruction> body) {
        this(name, returns, parameters, isStatic, slots, body, List.of());
    }

    /** The same method with the stretches it protects and the source lines its instructions came from. */
    public AsmMethod protecting(final List<Handler> protecting, final List<Integer> from) {
        return new AsmMethod(this.name, this.returns, this.parameters, this.isStatic, this.slots, this.body,
                this.lines, protecting, from);
    }

    /** The line of the source instruction {@code index} came from, or 0 when the listing does not say. */
    public int positionOf(final int index) {
        return index >= 0 && index < this.positions.size() ? this.positions.get(index) : 0;
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
                && this.parameters.equals(method.parameters) && Objects.equals(this.body, method.body)
                && this.handlers.equals(method.handlers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.returns, this.parameters, this.isStatic, this.slots, this.body,
                this.handlers);
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
        private final List<Handler> handlers = new ArrayList<>();
        /** The line of the listing each handler was read from. */
        private final List<Integer> handlerLines = new ArrayList<>();
        private final List<Integer> positions = new ArrayList<>();
        /** The line of the source the instructions being read now came from, as the last {@code .line} said. */
        private int position;

        public Builder(final String name, final String returns, final List<String> parameters,
                       final boolean isStatic, final int slots, final boolean hasBody) {
            this.name = name;
            this.returns = returns;
            this.parameters = List.copyOf(parameters);
            this.isStatic = isStatic;
            this.slots = slots;
            this.hasBody = hasBody;
        }

        /** Whether the header was written with a slots line, which is what gives a method a body to hold. */
        public boolean hasBody() {
            return this.hasBody;
        }

        /** How many places a running call of the method has: its slots, or its parameters when those are more. */
        public int placeCount() {
            return Math.max(this.slots, this.parameters.size());
        }

        /** Adds one line, with the line of the listing it came from. */
        public void add(final Instruction instruction, final int line) {
            this.body.add(instruction);
            this.lines.add(line);
            this.positions.add(this.position);
        }

        /** Says the instructions read from here on came from that line of the source. */
        public void at(final int sourceLine) {
            this.position = Math.max(0, sourceLine);
        }

        /** Adds a protected stretch, with the line of the listing it was read from. */
        public void addHandler(final Handler handler, final int line) {
            this.handlers.add(handler);
            this.handlerLines.add(line);
        }

        /** The protected stretches read so far, innermost first. */
        public List<Handler> handlers() {
            return List.copyOf(this.handlers);
        }

        /** The line of the listing handler {@code index} was read from. */
        public int handlerLineOf(final int index) {
            return this.handlerLines.get(index);
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
            final boolean placed = this.positions.stream().anyMatch(position -> position > 0);
            return new AsmMethod(this.name, this.returns, this.parameters, this.isStatic,
                    this.slots, this.hasBody ? this.body : null, this.hasBody ? this.lines : List.of(),
                    this.handlers, this.hasBody && placed ? this.positions : List.of());
        }
    }
}
