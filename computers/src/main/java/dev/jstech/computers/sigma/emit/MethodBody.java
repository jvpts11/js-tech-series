/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.lower.IrStmt;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.Opcode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The lines of one method, and the places it keeps its values in.
 *
 * <p>Two writers put lines here: one for what the method does, one for the values it works out. They
 * share this because they are writing the same method. A slot given to a name where it is declared is
 * the slot every later mention reads from; the label a loop marks is the label a jump inside it names;
 * the object a lambda shares with the method around it is reached the same way from either. Holding
 * all of that in one place is what lets the two writers be separate at all.
 */
final class MethodBody {

    private final Emitter emitter;
    private final List<Instruction> code = new ArrayList<>();
    private final Map<IBinding.Variable, Integer> places = new IdentityHashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
    private final List<String> pending = new ArrayList<>();
    private final Deque<String> breaks = new ArrayDeque<>();
    private final Deque<String> continues = new ArrayDeque<>();
    /** What each enclosing lock took, innermost first, so a way out can name the same objects back. */
    private final Deque<IrStmt.Temporary> held = new ArrayDeque<>();
    /** Where each value the program never named ended up living. */
    private final Map<IrStmt.Temporary, Integer> temporaries = new IdentityHashMap<>();
    private final NamedType owner;
    private final ITypeSymbol returns;
    private Closure closure;
    private int closureSlot = -1;
    private boolean closureIsThis;
    private int nextLabel;
    private int nextSlot;

    MethodBody(final Emitter emitter, final NamedType owner, final ITypeSymbol returns) {
        this.emitter = emitter;
        this.owner = owner;
        this.returns = returns;
    }

    /** The type the method was written in. */
    NamedType owner() {
        return this.owner;
    }

    /** What the method answers with. */
    ITypeSymbol returns() {
        return this.returns;
    }

    /** The object this method's lambdas share, or null when they keep nothing. */
    Closure closure() {
        return this.closure;
    }

    int slotCount() {
        return this.nextSlot;
    }

    /**
     * Makes the object the lambdas of this method share, and moves into it every parameter they
     * keep. Locals they keep are written straight into it when they are declared.
     */
    void openClosure(final Closure made) {
        if (made == null) {
            return;
        }
        this.closure = made;
        this.closureSlot = this.hidden();
        this.emit(Opcode.NEWOBJ, new IOperand.Constructor(made.type(), List.of()));
        this.emit(Opcode.STLOC, new IOperand.Slot(this.closureSlot));
        if (made.holdsThis()) {
            this.pushClosure();
            this.emit(Opcode.LDTHIS);
            this.emit(Opcode.STFLD, new IOperand.Field(made.type(), Closure.OUTER));
        }
        for (final Map.Entry<IBinding.Variable, String> field : made.fields().entrySet()) {
            final Integer at = this.places.get(field.getKey());
            if (at != null) {
                this.pushClosure();
                this.emit(Opcode.LDLOC, new IOperand.Slot(at));
                this.emit(Opcode.STFLD, new IOperand.Field(made.type(), field.getValue()));
            }
        }
    }

    /** Inside a lambda, the object the method shared with it is the object the lambda belongs to. */
    void closureIsThis(final Closure made) {
        this.closure = made;
        this.closureIsThis = true;
    }

    /** Whether a variable lives in the shared object rather than in a slot of its own. */
    boolean kept(final IBinding.Variable variable) {
        return this.closure != null && this.closure.fields().containsKey(variable);
    }

    /** Puts the shared object on the stack, for a read or a write of something kept in it. */
    void pushClosure() {
        if (this.closureIsThis) {
            this.emit(Opcode.LDTHIS);
        } else {
            this.emit(Opcode.LDLOC, new IOperand.Slot(this.closureSlot));
        }
    }

    void loadKept(final IBinding.Variable variable) {
        this.pushClosure();
        this.emit(Opcode.LDFLD,
                new IOperand.Field(this.closure.type(), this.closure.fields().get(variable)));
    }

    /** Puts a value already on the stack into the shared object. The object goes on first. */
    void storeKept(final IBinding.Variable variable) {
        this.emit(Opcode.STFLD,
                new IOperand.Field(this.closure.type(), this.closure.fields().get(variable)));
    }

    /*
     * Inside a lambda the object the method belonged to is a field of the shared object, because
     * the lambda itself belongs to that shared object and not to the type the method was in.
     */
    void pushThis() {
        this.emit(Opcode.LDTHIS);
        if (this.closureIsThis) {
            this.emit(Opcode.LDFLD, new IOperand.Field(this.closure.type(), Closure.OUTER));
        }
    }

    void parameters(final List<IDecl.Parameter> parameters) {
        for (final IDecl.Parameter parameter : parameters) {
            this.slot(this.emitter.model.declaredAt(parameter));
        }
    }

    /** The name a member is written under here: left out when it belongs to the type being written. */
    String ownerOf(final IMemberSymbol member) {
        return member.owner() == this.owner ? null : member.owner().qualifiedName();
    }

    void emit(final Opcode opcode) {
        this.add(Instruction.of(opcode));
    }

    void emit(final Opcode opcode, final IOperand operand) {
        this.add(Instruction.of(opcode, operand));
    }

    String label() {
        this.nextLabel++;
        return "L" + this.nextLabel;
    }

    void mark(final String label) {
        this.pending.add(label);
    }

    int slot(final IBinding.Variable variable) {
        if (variable == null) {
            return this.nextSlot++;
        }
        final Integer known = this.places.get(variable);
        if (known != null) {
            return known;
        }
        final int given = this.nextSlot++;
        this.places.put(variable, given);
        return given;
    }

    /** A place with no name, for what a lowered loop needs to hold on to. */
    int hidden() {
        return this.nextSlot++;
    }

    /** Where a value the program never named ends up living, worked out the first time it is asked for. */
    int placeOf(final IrStmt.Temporary temporary) {
        return this.temporaries.computeIfAbsent(temporary, one -> this.hidden());
    }

    /** Where a way out of this loop goes, and where going round it again goes. */
    void enterLoop(final String again, final String end) {
        this.continues.push(again);
        this.breaks.push(end);
    }

    void leaveLoop() {
        this.breaks.pop();
        this.continues.pop();
    }

    /** The same for a choice, which a break leaves but a continue passes straight through. */
    void enterChoice(final String end) {
        this.breaks.push(end);
    }

    void leaveChoice() {
        this.breaks.pop();
    }

    String breakTo() {
        return this.breaks.peek();
    }

    String continueTo() {
        return this.continues.peek();
    }

    /** Remembers that a lock was taken on what is kept in that place. */
    void took(final IrStmt.Temporary place) {
        this.held.push(place);
    }

    void gaveBack() {
        this.held.pop();
    }

    /** Lets go of the locks a way out of the middle of something leaves behind, nearest first. */
    void letGo(final int locks) {
        int left = locks;
        for (final IrStmt.Temporary place : this.held) {
            if (left <= 0) {
                return;
            }
            this.emit(Opcode.LDLOC, new IOperand.Slot(this.placeOf(place)));
            this.emit(Opcode.MONITOR_EXIT);
            left--;
        }
    }

    List<Instruction> finish() {
        if (this.code.isEmpty() || !this.pending.isEmpty()
                || this.code.getLast().opcode() != Opcode.RET) {
            this.emit(Opcode.RET);
        }
        final List<Instruction> resolved = new ArrayList<>();
        for (final Instruction instruction : this.code) {
            if (instruction.operand() instanceof IOperand.Label target) {
                resolved.add(new Instruction(instruction.label(), instruction.opcode(),
                        new IOperand.Label(this.resolve(target.name())), instruction.comment()));
            } else {
                resolved.add(instruction);
            }
        }
        return resolved;
    }

    private void add(final Instruction instruction) {
        Instruction written = instruction;
        if (!this.pending.isEmpty()) {
            written = written.labelled(this.pending.getFirst());
            for (int i = 1; i < this.pending.size(); i++) {
                this.aliases.put(this.pending.get(i), this.pending.getFirst());
            }
            this.pending.clear();
        }
        this.code.add(written);
    }

    // Two labels can land on the same line, and a line carries one, so the rest point at it.
    private String resolve(final String label) {
        String at = label;
        while (this.aliases.containsKey(at)) {
            at = this.aliases.get(at);
        }
        return at;
    }
}
