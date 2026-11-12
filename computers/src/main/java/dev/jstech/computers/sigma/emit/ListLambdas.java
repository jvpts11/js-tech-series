/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.List;

/**
 * Writes out a list searched or sorted with a function of the program's own, as the third version's
 * {@code List.Find(Func<T, bool>)} and {@code List.Sort(Func<T, T, int>)} are.
 *
 * <p>The runtime answers the language's calls in Java, and Java cannot call into the program in the middle of one of
 * its instructions. So the walk is written into the program the way a foreach is, a counted loop over the list that
 * calls the function on each element through the delegate, and costs what those instructions cost. A sort is an
 * insertion sort: it keeps the order of elements the function finds equal, and each comparison is one call.
 */
final class ListLambdas {

    private final Emitter emitter;
    private final MethodBody body;
    private final ExpressionWriter values;

    private static final String LIST = "List";
    private static final String FIND = "Find";
    private static final String SORT = "Sort";
    private static final String WHOLE = "int";

    ListLambdas(final Emitter emitter, final MethodBody body, final ExpressionWriter values) {
        this.emitter = emitter;
        this.body = body;
        this.values = values;
    }

    /** Whether {@code method} is one of the list's calls this writes out rather than calls. */
    boolean writes(final IMemberSymbol.MethodSymbol method) {
        return method.owner() == this.emitter.builtIns.listType() && method.parameters().size() == 1
                && (FIND.equals(method.name()) || SORT.equals(method.name()))
                && this.emitter.rules.named(method.parameters().getFirst().type()) != null
                && this.emitter.rules.named(method.parameters().getFirst().type()).invoke() != null;
    }

    /** Writes {@code list.Find(test)} or {@code list.Sort(comparison)}: the list, then its walk. */
    void write(final IExpr list, final IExpr function, final IMemberSymbol.MethodSymbol method) {
        final ITypeSymbol element = this.emitter.rules.arguments(this.emitter.model.typeOf(list)).getFirst();
        final int held = this.body.hidden();
        final int called = this.body.hidden();
        this.values.receiver(list);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(held));
        this.values.copied(function, method.parameters().getFirst().type());
        this.body.emit(Opcode.STLOC, new IOperand.Slot(called));
        if (FIND.equals(method.name())) {
            this.find(held, called, element);
        } else {
            this.sort(held, called, element);
        }
    }

    /* The first element the test says yes to, left on the stack; the element type's empty value when none does. */
    private void find(final int held, final int called, final ITypeSymbol element) {
        final int index = this.body.hidden();
        final int item = this.body.hidden();
        final int found = this.body.hidden();
        this.emptyOf(element);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(found));
        this.whole(0, index);
        final String top = this.body.label();
        final String next = this.body.label();
        final String end = this.body.label();
        this.body.mark(top);
        this.atOrPastEnd(index, held, end);
        this.element(held, index, element);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(item));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(called));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(item));
        this.body.emit(Opcode.CALLVIRT, new IOperand.Method("Func", "Invoke", List.of(element.describe()), "bool"));
        this.body.emit(Opcode.BRFALSE, new IOperand.Label(next));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(item));
        this.body.emit(Opcode.STLOC, new IOperand.Slot(found));
        this.body.emit(Opcode.BR, new IOperand.Label(end));
        this.body.mark(next);
        this.step(index, 1);
        this.body.emit(Opcode.BR, new IOperand.Label(top));
        this.body.mark(end);
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(found));
    }

    /*
     * An insertion sort: each element from the second on is taken out and the ones before it that come after it are
     * moved up one, until one that does not, and it goes in behind that one.
     */
    private void sort(final int held, final int called, final ITypeSymbol element) {
        final int index = this.body.hidden();
        final int item = this.body.hidden();
        final int back = this.body.hidden();
        final String outer = this.body.label();
        final String inner = this.body.label();
        final String place = this.body.label();
        final String done = this.body.label();
        this.whole(1, index);
        this.body.mark(outer);
        this.atOrPastEnd(index, held, done);
        this.element(held, index, element);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(item));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(index));
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(1));
        this.body.emit(Opcode.SUB);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(back));
        this.body.mark(inner);
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(back));
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(0));
        this.body.emit(Opcode.BLT, new IOperand.Label(place));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(called));
        this.element(held, back, element);
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(item));
        this.body.emit(Opcode.CALLVIRT, new IOperand.Method("Func", "Invoke",
                List.of(element.describe(), element.describe()), WHOLE));
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(0));
        this.body.emit(Opcode.BLE, new IOperand.Label(place));
        // The one behind comes after the one taken out: it moves up a place.
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(back));
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(1));
        this.body.emit(Opcode.ADD);
        this.element(held, back, element);
        this.set(element);
        this.step(back, -1);
        this.body.emit(Opcode.BR, new IOperand.Label(inner));
        this.body.mark(place);
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(back));
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(1));
        this.body.emit(Opcode.ADD);
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(item));
        this.set(element);
        this.step(index, 1);
        this.body.emit(Opcode.BR, new IOperand.Label(outer));
        this.body.mark(done);
    }

    /* Jumps to {@code end} once the number in {@code index} has reached the list's count. */
    private void atOrPastEnd(final int index, final int held, final String end) {
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(index));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
        this.body.emit(Opcode.LDFLD, new IOperand.Field(LIST, "Count"));
        this.body.emit(Opcode.BGE, new IOperand.Label(end));
    }

    /* Puts the list's element at the number in {@code index} on the stack. */
    private void element(final int held, final int index, final ITypeSymbol element) {
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(index));
        this.body.emit(Opcode.CALL, new IOperand.Method(LIST, "Get", List.of(WHOLE), element.describe()));
    }

    /* Sets the element at the place on the stack to the value on top of it, the list beneath both. */
    private void set(final ITypeSymbol element) {
        this.body.emit(Opcode.CALL, new IOperand.Method(LIST, "Set", List.of(WHOLE, element.describe()), "void"));
    }

    private void whole(final int value, final int slot) {
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(value));
        this.body.emit(Opcode.STLOC, new IOperand.Slot(slot));
    }

    private void step(final int slot, final int by) {
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(slot));
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(by));
        this.body.emit(Opcode.ADD);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(slot));
    }

    /*
     * What a variable of the element's type starts with: nought for a number or a character, false, an empty struct,
     * for a struct is never nothing, or null.
     */
    private void emptyOf(final ITypeSymbol element) {
        if (this.values.isStruct(element)) {
            this.body.emit(Opcode.NEWOBJ, new IOperand.Constructor(element.describe(), List.of()));
            return;
        }
        if (!(element instanceof ITypeSymbol.Primitive primitive)) {
            this.body.emit(Opcode.LDNULL);
            return;
        }
        switch (primitive) {
            case LONG -> this.body.emit(Opcode.LDC_I8, new IOperand.I8(0L));
            case FLOAT -> this.body.emit(Opcode.LDC_R4, new IOperand.R4(0f));
            case DOUBLE -> this.body.emit(Opcode.LDC_R8, new IOperand.R8(0.0));
            default -> this.body.emit(Opcode.LDC_I4, new IOperand.I4(0));
        }
    }
}
