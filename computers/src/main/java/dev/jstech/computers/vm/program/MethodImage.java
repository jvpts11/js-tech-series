/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One method made ready to run: its lines in an array, and for each line whatever it would otherwise have to look up
 * while it runs.
 *
 * <p>A branch holds the line it lands on, a call holds what it reaches and which of its arguments are filled in rather
 * than handed over, a {@code new} holds the constructor it runs, and a field holds whether it is the program's own and
 * what reads it when it is not. All of it is worked out once, when the program loads, so running a line is reading it.
 */
public final class MethodImage {

    private static final Instruction[] NO_CODE = new Instruction[0];
    private static final Handler[] NO_HANDLERS = new Handler[0];

    private final String owner;
    private final String name;
    private final List<String> parameters;
    private final String returns;
    private final boolean isStatic;
    private final int slots;
    private final Instruction[] code;
    private final boolean[] outs;
    private final boolean gives;
    /** For each line, where a branch on it lands. */
    private final int[] jumps;
    /** For each line, the call, the creation or the field it reaches, as the program loaded it. */
    private final Object[] sites;
    /** For each line, the line of the listing it was read from, when the method was read from one. */
    private final AsmMethod written;
    /** The protected stretches as the method's own lines, innermost first, worked out when the program loads. */
    private Handler[] handlers = NO_HANDLERS;

    /**
     * One protected stretch of the method, as its own lines: the stretch from {@code start} up to {@code end}, and the
     * catch or the finally that answers it from {@code handler} up to {@code through}.
     *
     * @param type the exceptions a catch takes, or null for a finally
     */
    record Handler(int start, int end, String type, int handler, int through) {

        /** Whether this is a finally, run however its stretch is left. */
        boolean cleanup() {
            return this.type == null;
        }

        /** Whether the line at that place is inside the protected stretch. */
        boolean covers(final int line) {
            return line >= this.start && line < this.end;
        }

        /** Whether the line at that place is inside the catch or the finally itself. */
        boolean runs(final int line) {
            return line >= this.handler && line < this.through;
        }
    }

    MethodImage(final String owner, final AsmMethod method) {
        this.owner = owner;
        this.name = method.name();
        this.parameters = method.parameters();
        this.returns = method.returns();
        this.isStatic = method.isStatic();
        this.slots = method.slots();
        this.code = method.hasBody() ? method.body().toArray(NO_CODE) : NO_CODE;
        this.outs = outsOf(this.parameters);
        this.gives = !"void".equals(this.returns);
        this.jumps = new int[this.code.length];
        this.sites = new Object[this.code.length];
        this.written = method;
    }

    /** Works out what every line reaches, once every type of the program knows its methods. */
    void resolve(final ProgramImage program) {
        final Map<String, Integer> labels = new HashMap<>();
        for (int i = 0; i < this.code.length; i++) {
            if (this.code[i].label() != null) {
                labels.put(this.code[i].label(), i);
            }
        }
        for (int i = 0; i < this.code.length; i++) {
            switch (this.code[i].operand()) {
                // A label nothing carries ends the method; the reader never lets such a listing load in any case.
                case IOperand.Label label -> this.jumps[i] = labels.getOrDefault(label.name(), this.code.length);
                // A region is the method's, by its label: two methods may both have one called done.
                case IOperand.Region region -> this.sites[i] = this.describe() + ":" + region.name();
                case IOperand.RegionJump region -> {
                    this.jumps[i] = labels.getOrDefault(region.label(), this.code.length);
                    this.sites[i] = this.describe() + ":" + region.name();
                }
                case IOperand.Method called -> this.sites[i] = program.callSite(called);
                case IOperand.Constructor made -> this.sites[i] = program.creation(made);
                case IOperand.Field field -> this.sites[i] = program.valueSite(field, this.code[i].opcode());
                case null, default -> {
                }
            }
        }
        final List<AsmMethod.Handler> written = this.written.handlers();
        this.handlers = new Handler[written.size()];
        for (int i = 0; i < written.size(); i++) {
            final AsmMethod.Handler one = written.get(i);
            this.handlers[i] = new Handler(labels.getOrDefault(one.start(), this.code.length),
                    labels.getOrDefault(one.end(), this.code.length), one.type(),
                    labels.getOrDefault(one.handler(), this.code.length),
                    labels.getOrDefault(one.through(), this.code.length));
        }
    }

    /** How many protected stretches the method has. */
    int handlerCount() {
        return this.handlers.length;
    }

    /** The protected stretch at that place in the method's list, innermost first. */
    Handler handler(final int index) {
        return this.handlers[index];
    }

    /** The line of the source the line at {@code index} came from, or 0 when the listing does not say. */
    int positionOf(final int index) {
        return this.written.positionOf(index);
    }

    /** The column of the statement the line at {@code index} came from, or 0 when the listing does not say. */
    int columnOf(final int index) {
        return this.written.columnOf(index);
    }

    /** The variables the source named, as a build for the debugger lists them; none for any other build. */
    List<AsmMethod.Local> locals() {
        return this.written.debug().locals();
    }

    /** The type the method belongs to. */
    public String owner() {
        return this.owner;
    }

    /** What the method is called. */
    public String name() {
        return this.name;
    }

    /** The types it takes, with {@code out} in front of the ones it fills in. */
    public List<String> parameters() {
        return this.parameters;
    }

    /** The type it gives back. */
    public String returns() {
        return this.returns;
    }

    /** Whether it belongs to the type rather than to one of its objects. */
    public boolean isStatic() {
        return this.isStatic;
    }

    /** How many places it keeps its parameters and its locals in. */
    public int slots() {
        return this.slots;
    }

    /** How many lines it runs. */
    public int length() {
        return this.code.length;
    }

    /** The line at that place. */
    public Instruction instruction(final int index) {
        return this.code[index];
    }

    /** Whether it has lines to run, which a method an interface only asks for does not. */
    public boolean hasCode() {
        return this.code.length > 0;
    }

    /** Whether the parameter at that place is one the method fills in. */
    public boolean fillsIn(final int index) {
        return this.outs[index];
    }

    /** Whether it hands anything back. */
    public boolean gives() {
        return this.gives;
    }

    /** How the method is written where it is called. */
    public String describe() {
        return this.owner + "." + this.name + "(" + String.join(", ", this.parameters) + ")";
    }

    /** The line of the listing the line at {@code index} was read from, or 0 when the method was not read from one. */
    int lineOf(final int index) {
        return this.written.lineOf(index);
    }

    /** Where a branch on the line at {@code index} lands. */
    int jump(final int index) {
        return this.jumps[index];
    }

    /** The region the line at {@code index} names, as the process keeps it: by this method and its label. */
    String region(final int index) {
        return (String) this.sites[index];
    }

    /** The call the line at {@code index} makes. */
    ProgramImage.CallSite call(final int index) {
        return (ProgramImage.CallSite) this.sites[index];
    }

    /** The object the line at {@code index} makes. */
    ProgramImage.Creation creation(final int index) {
        return (ProgramImage.Creation) this.sites[index];
    }

    /** The field the line at {@code index} reads or writes. */
    ProgramImage.ValueSite value(final int index) {
        return (ProgramImage.ValueSite) this.sites[index];
    }

    /** Which of those parameters a call fills in rather than hands over. */
    static boolean[] outsOf(final List<String> parameters) {
        final boolean[] outs = new boolean[parameters.size()];
        for (int i = 0; i < outs.length; i++) {
            outs[i] = parameters.get(i).startsWith("out ");
        }
        return outs;
    }
}
