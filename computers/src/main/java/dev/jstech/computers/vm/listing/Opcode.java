/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Every instruction of the assembly, and the one operand each of them takes.
 *
 * <p>The machine works on a stack: an instruction takes what it needs off the top and leaves its
 * result there. That is what keeps the listing short enough to read down a screen, and it is why
 * there is no instruction here that names two places at once.
 *
 * <p>Every instruction costs one from a process's budget for the tick, except a call into the
 * library or into the world, which costs what that call is documented to cost.
 *
 * <p>{@code ldfn} is the one that makes a handler: it takes the object a method belongs to off the
 * stack and leaves a delegate bound to it, which is what a method handed over without brackets, and
 * what a lambda, both come to in the end.
 */
public enum Opcode {

    LDC_I4("ldc.i4", OperandShape.I4),
    LDC_I8("ldc.i8", OperandShape.I8),
    LDC_R4("ldc.r4", OperandShape.R4),
    LDC_R8("ldc.r8", OperandShape.R8),
    LDSTR("ldstr", OperandShape.TEXT),
    LDNULL("ldnull", OperandShape.NONE),

    LDLOC("ldloc", OperandShape.SLOT),
    STLOC("stloc", OperandShape.SLOT),
    LDFLD("ldfld", OperandShape.FIELD),
    STFLD("stfld", OperandShape.FIELD),
    LDSFLD("ldsfld", OperandShape.FIELD),
    STSFLD("stsfld", OperandShape.FIELD),
    LDTHIS("ldthis", OperandShape.NONE),

    ADD("add", OperandShape.NONE),
    SUB("sub", OperandShape.NONE),
    MUL("mul", OperandShape.NONE),
    DIV("div", OperandShape.NONE),
    REM("rem", OperandShape.NONE),
    NEG("neg", OperandShape.NONE),
    AND("and", OperandShape.NONE),
    OR("or", OperandShape.NONE),
    XOR("xor", OperandShape.NONE),
    NOT("not", OperandShape.NONE),
    SHL("shl", OperandShape.NONE),
    SHR("shr", OperandShape.NONE),

    CONV_I4("conv.i4", OperandShape.NONE),
    CONV_I8("conv.i8", OperandShape.NONE),
    CONV_R4("conv.r4", OperandShape.NONE),
    CONV_R8("conv.r8", OperandShape.NONE),

    CEQ("ceq", OperandShape.NONE),
    CLT("clt", OperandShape.NONE),
    CGT("cgt", OperandShape.NONE),
    BR("br", OperandShape.LABEL),
    BRTRUE("brtrue", OperandShape.LABEL),
    BRFALSE("brfalse", OperandShape.LABEL),
    BEQ("beq", OperandShape.LABEL),
    BNE("bne", OperandShape.LABEL),
    BLT("blt", OperandShape.LABEL),
    BLE("ble", OperandShape.LABEL),
    BGT("bgt", OperandShape.LABEL),
    BGE("bge", OperandShape.LABEL),

    NEWOBJ("newobj", OperandShape.CONSTRUCTOR),
    NEWARR("newarr", OperandShape.TYPE),
    LDELEM("ldelem", OperandShape.NONE),
    STELEM("stelem", OperandShape.NONE),
    LDLEN("ldlen", OperandShape.NONE),
    DISPOSE("dispose", OperandShape.NONE),
    /** Takes the lock of the object on top of the stack, waiting its turn if another thread holds it. */
    MONITOR_ENTER("monitor.enter", OperandShape.NONE),
    /** Lets go of the lock of the object on top of the stack. */
    MONITOR_EXIT("monitor.exit", OperandShape.NONE),
    CASTCLASS("castclass", OperandShape.TYPE),
    ISINST("isinst", OperandShape.TYPE),

    CALL("call", OperandShape.METHOD),
    CALLVIRT("callvirt", OperandShape.METHOD),
    LDFN("ldfn", OperandShape.METHOD),
    SYS("sys", OperandShape.TEXT),
    RET("ret", OperandShape.NONE),

    POP("pop", OperandShape.NONE),
    DUP("dup", OperandShape.NONE),
    /** Replaces the struct on top of the stack with a copy of it: what storing or handing over a value does. */
    COPY("copy", OperandShape.NONE);

    /** What kind of operand an instruction takes, if any. */
    public enum OperandShape {
        NONE,
        I4,
        I8,
        R4,
        R8,
        TEXT,
        SLOT,
        LABEL,
        FIELD,
        METHOD,
        CONSTRUCTOR,
        TYPE
    }

    private static final Map<String, Opcode> BY_NAME;

    static {
        final Map<String, Opcode> names = new HashMap<>();
        for (final Opcode opcode : values()) {
            names.put(opcode.text, opcode);
        }
        BY_NAME = Collections.unmodifiableMap(names);
    }

    private final String text;
    private final OperandShape shape;

    Opcode(final String text, final OperandShape shape) {
        this.text = text;
        this.shape = shape;
    }

    /** The instruction written that way, or null. */
    public static Opcode written(final String text) {
        return BY_NAME.get(text);
    }

    /** How the instruction is written in a listing. */
    public String text() {
        return this.text;
    }

    /** What it takes after its name. */
    public OperandShape shape() {
        return this.shape;
    }

    /** Whether it takes anything at all. */
    public boolean takesOperand() {
        return this.shape != OperandShape.NONE;
    }
}
