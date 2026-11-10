/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.sigma.sem.Layout;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.sigma.sem.TypeRules;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reading and writing a value through an address, which is what every place in raw memory comes down to.
 *
 * <p>A number, a bool, a character, an enum's value and a pointer are one instruction each, as wide as the value is. A
 * struct is its fields one by one, each at the offset the layout gives it: read into a fresh struct of its own, or
 * written from one. The instructions go wherever they are handed, so the blocks a method makes before it starts can be
 * written once the method is known.
 */
final class RawAccess {

    private final Layout layout;
    private final TypeRules rules;

    RawAccess(final Layout layout, final TypeRules rules) {
        this.layout = layout;
        this.rules = rules;
    }

    /** Reads what lies at the address on top of the stack, leaving the value in its place. */
    void read(final ITypeSymbol type, final MethodBody body, final Consumer<Instruction> out) {
        final NamedType struct = this.struct(type);
        if (struct == null) {
            out.accept(Instruction.of(readOf(type)));
            return;
        }
        final int address = body.hidden();
        out.accept(Instruction.of(Opcode.STLOC, new IOperand.Slot(address)));
        out.accept(Instruction.of(Opcode.NEWOBJ, new IOperand.Constructor(type.describe(), List.of())));
        for (final IMemberSymbol field : this.layout.fieldsOf(struct)) {
            out.accept(Instruction.of(Opcode.DUP));
            this.fieldAddress(address, struct, field, out);
            this.read(Layout.typeOf(field), body, out);
            out.accept(Instruction.of(Opcode.STFLD, new IOperand.Field(body.ownerOf(field), field.name())));
        }
    }

    /** Writes the value on top of the stack at the address beneath it, taking both off. */
    void write(final ITypeSymbol type, final MethodBody body, final Consumer<Instruction> out) {
        final NamedType struct = this.struct(type);
        if (struct == null) {
            out.accept(Instruction.of(writeOf(type)));
            return;
        }
        final int value = body.hidden();
        final int address = body.hidden();
        out.accept(Instruction.of(Opcode.STLOC, new IOperand.Slot(value)));
        out.accept(Instruction.of(Opcode.STLOC, new IOperand.Slot(address)));
        for (final IMemberSymbol field : this.layout.fieldsOf(struct)) {
            this.fieldAddress(address, struct, field, out);
            out.accept(Instruction.of(Opcode.LDLOC, new IOperand.Slot(value)));
            out.accept(Instruction.of(Opcode.LDFLD, new IOperand.Field(body.ownerOf(field), field.name())));
            this.write(Layout.typeOf(field), body, out);
        }
    }

    /** Moves the address on top of the stack past that many values of the type a pointer points at. */
    void step(final ITypeSymbol pointee, final Consumer<Instruction> out) {
        final int size = this.layout.sizeOf(pointee);
        if (size != 1) {
            out.accept(Instruction.of(Opcode.LDC_I8, new IOperand.I8(size)));
            out.accept(Instruction.of(Opcode.MUL));
        }
    }

    /* The address of one field of the struct whose address is kept in that slot. */
    private void fieldAddress(final int address, final NamedType struct, final IMemberSymbol field,
                              final Consumer<Instruction> out) {
        out.accept(Instruction.of(Opcode.LDLOC, new IOperand.Slot(address)));
        final int offset = this.layout.offsetOf(struct, field.name());
        if (offset != 0) {
            out.accept(Instruction.of(Opcode.LDC_I8, new IOperand.I8(offset)));
            out.accept(Instruction.of(Opcode.ADD));
        }
    }

    /* The struct a type is, or null for a value read in one instruction. */
    private NamedType struct(final ITypeSymbol type) {
        final NamedType named = this.rules.named(type);
        return named != null && named.kind() == NamedType.Kind.STRUCT ? named : null;
    }

    /* The read as wide as the value: a bool lies in a byte, a pointer in a long, and an enum's value in an int. */
    private static Opcode readOf(final ITypeSymbol type) {
        if (type instanceof ITypeSymbol.Primitive primitive) {
            return switch (primitive) {
                case BYTE, BOOL -> Opcode.LDIND_U1;
                case CHAR -> Opcode.LDIND_U2;
                case LONG -> Opcode.LDIND_I8;
                case FLOAT -> Opcode.LDIND_R4;
                case DOUBLE -> Opcode.LDIND_R8;
                default -> Opcode.LDIND_I4;
            };
        }
        return type instanceof ITypeSymbol.PointerType ? Opcode.LDIND_I8 : Opcode.LDIND_I4;
    }

    private static Opcode writeOf(final ITypeSymbol type) {
        if (type instanceof ITypeSymbol.Primitive primitive) {
            return switch (primitive) {
                case BYTE, BOOL -> Opcode.STIND_I1;
                case CHAR -> Opcode.STIND_I2;
                case LONG -> Opcode.STIND_I8;
                case FLOAT -> Opcode.STIND_R4;
                case DOUBLE -> Opcode.STIND_R8;
                default -> Opcode.STIND_I4;
            };
        }
        return type instanceof ITypeSymbol.PointerType ? Opcode.STIND_I8 : Opcode.STIND_I4;
    }
}
